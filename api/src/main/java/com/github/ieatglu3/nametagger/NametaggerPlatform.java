package com.github.ieatglu3.nametagger;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.time.Duration;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * The Nametagger platform
 */
public class NametaggerPlatform
{

  /**
   * The state of the platform
   */
  public enum State
  {
    Idle,
    Running,
    ShuttingDown,
    Shutdown
  }

  /**
   * Platform builder for defining implementation specific settings such as the {@link UnusedEntityIdProvider}, {@link ScheduledExecutorService} and tick frequency
   * <br><br>
   * Default tick frequency is {@code 125ms} (8 ticks per second)
   */
  public static final class Builder {
    private UnusedEntityIdProvider unusedEntityIdProvider;
    private EntityGetter<Object> entityGetter = EntityGetter.DEFAULT;
    private ScheduledExecutorService executorService;
    private Duration tickFrequency = Duration.ofMillis(125);

    /**
     * Sets the {@link UnusedEntityIdProvider} to use for this platform
     * @param unusedEntityIdProvider provider
     * @return this builder
     */
    public Builder unusedEntityIdProvider(UnusedEntityIdProvider unusedEntityIdProvider)
    {
      this.unusedEntityIdProvider = unusedEntityIdProvider;
      return this;
    }

    /**
     * Sets the entity getter function, which is used to get the entity object from an entity ID
     * <br><br>
     * Default implementation returns null
     * @param entityGetter entity getter function
     * @return this builder
     */
    public Builder entityGetter(EntityGetter<Object> entityGetter)
    {
      this.entityGetter = entityGetter;
      return this;
    }

    /**
     * Sets the {@link ScheduledExecutorService} to use for this platform
     * @param executorService executor service
     * @return this builder
     */
    public Builder executorService(ScheduledExecutorService executorService)
    {
      this.executorService = executorService;
      return this;
    }

    /**
     * Sets the tick frequency for this platform, this is how often the platform will tick players and execute tasks scheduled with {@link #executeNextTick(Consumer)}.
     * Default is 125ms (10 ticks per second).
     * @param tickFrequency tick frequency
     * @return this builder
     */
    public Builder tickFrequency(Duration tickFrequency)
    {
      this.tickFrequency = tickFrequency;
      return this;
    }

    /**
     * Builds the NametaggerPlatform with the provided settings
     * @return built NametaggerPlatform
     */
    public NametaggerPlatform build()
    {
      if (this.unusedEntityIdProvider == null)
        throw new IllegalStateException("UnusedEntityIdProvider must be set");
      if (this.executorService == null)
        throw new IllegalStateException("ExecutorService must be set");
      return new NametaggerPlatform(this.executorService, this.unusedEntityIdProvider, this.entityGetter, this.tickFrequency);
    }
  }

  /** Creates a new builder for the NametaggerPlatform
   *
   * @return builder
   */
  public static Builder builder()
  {
    return new Builder();
  }

  private static final class BiDirectionalPlayerCache {
    private final ConcurrentHashMap<UUID, Integer> playerToEntityId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, UUID> entityIdToPlayer = new ConcurrentHashMap<>();

    public void put(UUID playerId, int entityId)
    {
      this.playerToEntityId.put(playerId, entityId);
      this.entityIdToPlayer.put(entityId, playerId);
    }

    public UUID getPlayer(int entityId)
    {
      return this.entityIdToPlayer.get(entityId);
    }

    public int getEntityId(UUID playerId)
    {
      return this.playerToEntityId.get(playerId);
    }

    public void removeByPlayerId(UUID playerId)
    {
      Integer entityId = this.playerToEntityId.remove(playerId);
      if (entityId != null)
        this.entityIdToPlayer.remove(entityId);
    }
  }

  private static final class InternalEventBus {
    private final CopyOnWriteArrayList<BiConsumer<NametaggerPlatform, Viewer>> viewerJoinListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<BiConsumer<NametaggerPlatform, RemovedViewer>> viewerRemoveListeners = new CopyOnWriteArrayList<>();

    public void registerViewerJoinListener(BiConsumer<NametaggerPlatform, Viewer> listener)
    {
      this.viewerJoinListeners.add(listener);
    }

    public void registerViewerRemoveListener(BiConsumer<NametaggerPlatform, RemovedViewer> listener)
    {
      this.viewerRemoveListeners.add(listener);
    }

    public void onViewerJoin(NametaggerPlatform platform, Viewer viewer)
    {
      for (final var listener : this.viewerJoinListeners)
        listener.accept(platform, viewer);
    }

    public void onViewerLeave(NametaggerPlatform platform, RemovedViewer viewer)
    {
      for (final var listener : this.viewerRemoveListeners)
        listener.accept(platform, viewer);
    }

  }

  private static final VarHandle STATE;
  static
  {
    try
    {
      STATE = MethodHandles.lookup().findVarHandle(NametaggerPlatform.class, "state", State.class);
    }
    catch (ReflectiveOperationException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private static final Logger LOGGER = Logger.getLogger(NametaggerPlatform.class.getName());

  private final ConcurrentHashMap<UUID, Viewer> viewers = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<Integer, Viewer> entityIdToViewer = new ConcurrentHashMap<>();
  private final BiDirectionalPlayerCache biDirectionalPlayerCache = new BiDirectionalPlayerCache();

  private volatile PacketListener packetListener;
  private volatile State state = State.Idle;

  private final UnusedEntityIdProvider unusedEntityIdProvider;
  private final EntityGetter<Object> entityGetter;
  private final ScheduledExecutorService executorService;

  private final TaskExecutor<NametaggerPlatform> taskExecutor = new TaskExecutor<>();
  private final InternalEventBus internalEventBus = new InternalEventBus();
  private final Duration tickFrequency;
  NametaggerPlatform(
    ScheduledExecutorService executorService,
    UnusedEntityIdProvider unusedEntityIdProvider,
    EntityGetter<Object> entityGetter,
    Duration tickFrequency
  )
  {
    this.executorService = executorService;
    this.unusedEntityIdProvider = unusedEntityIdProvider;
    this.entityGetter = entityGetter;
    this.tickFrequency = tickFrequency;
    this.packetListener = new PacketListener(this);
    PacketEvents.getAPI().getEventManager().registerListener(this.packetListener);
  }

  /**
   * Gets the {@link ScheduledExecutorService} used by this platform for ticking
   * @return executor service
   */
  public ScheduledExecutorService executorService()
  {
    return this.executorService;
  }

  /**
   * Registers a listener to be called when a new player is created, which is when the platform starts tracking them
   * <br><br>
   * The listener will be executed on the next tick after the player is created
   * @param listener listener to register
   */
  public void listenForViewerJoin(BiConsumer<NametaggerPlatform, Viewer> listener)
  {
    this.internalEventBus.registerViewerJoinListener(listener);
  }

  /**
   * Registers a listener to be called when a player is removed, which is when the platform stops tracking them
   * <br><br>
   * The listener will be executed on the next tick after the player is removed
   * @param listener listener to register
   */
  public void listenForViewerRemove(BiConsumer<NametaggerPlatform, RemovedViewer> listener)
  {
    this.internalEventBus.registerViewerRemoveListener(listener);
  }

  /**
   * Gets the entity object for the given entity ID, using the entity getter function provided in the builder
   * <br><br>
   * Implementations are guaranteed to provide thread safe access to getting the entity, but not the entity object itself.
   * <br><br>
   * Implementations will lazily or instantly remove entities that are no longer valid. If lazy, the returned entity may not be valid by the time it is used.
   * @param entityId entity ID
   * @return entity object, or null if no entity was found
   */
  public <T> T getEntity(int entityId)
  {
    final var entity = this.entityGetter.getEntity(this, entityId);
    if (entity == null)
      return null;
    return (T) entity;
  }

  /**
   * Schedules a task to be executed on the next tick of the platform
   * @param task task to execute
   */
  public void executeNextTick(Consumer<NametaggerPlatform> task)
  {
    this.taskExecutor.submit(task);
  }

  /**
   * Gets the tick frequency of the platform
   * @return tick frequency
   */
  public Duration tickFrequency()
  {
    return this.tickFrequency;
  }

  /**
   * Returns if the platform is currently running
   * @return running
   */
  public boolean isRunning()
  {
    return this.state == State.Running;
  }

  /**
   * Returns if the platform is currently shutdown
   * @return shutdown
   */
  public boolean isShutdown()
  {
    return this.state == State.Shutdown;
  }

  /**
   * Returns if the platform is currently shutting down or already shutdown
   * @return shutting down or shutdown
   */
  public boolean isShutOrShuttingDown()
  {
    final var state = this.state;
    return state == State.Shutdown || state == State.ShuttingDown;
  }

  /**
   * Starts the platform, which will start ticking players
   */
  public void start()
  {
    if (!STATE.compareAndSet(this, State.Idle, State.Running))
      throw new IllegalStateException("Platform can only be started from the Idle state");
    final long tickFrequencyMillis = this.tickFrequency.toMillis();
    this.executorService.scheduleAtFixedRate(this::tick, tickFrequencyMillis, tickFrequencyMillis, TimeUnit.MILLISECONDS);
  }

  /**
   * Shuts down the platform, which will stop ticking players and unregister packet listeners
   */
  public void shutdown()
  {
    if (!STATE.compareAndSet(this, State.Running, State.ShuttingDown))
      throw new IllegalStateException("Platform can only be shutdown from the Running state");

    if (this.packetListener != null)
    {
      PacketEvents.getAPI().getEventManager().unregisterListener(this.packetListener);
      this.packetListener = null;
    }

    this.executorService.shutdownNow();
    try
    {
      if (!this.executorService.awaitTermination(5, TimeUnit.SECONDS))
        LOGGER.warning("Executor service did not shut down within the timeout");
    }
    catch (InterruptedException e) {
      LOGGER.warning("Interrupted while waiting for executor service to shut down");
    }

    for (final var player : this.viewers.values())
      player.close();

    this.viewers.clear();
    this.entityIdToViewer.clear();
    this.state = State.Shutdown;
  }

  /**
   * Gets the viewer by their UUID.
   * @param uuid the UUID of the viewer to get
   * @return viewer
   */
  public Viewer getViewerByUUID(UUID uuid)
  {
    return this.viewers.get(uuid);
  }

  /**
   * Gets the viewer by their entity ID.
   * @param entityId the entity ID of the viewer to get
   * @return viewer
   */
  public Viewer getViewerByEntityId(int entityId)
  {
    return this.entityIdToViewer.get(entityId);
  }

  /**
   * Gets the player by their entity ID, using a mapper function to map from the player's UUID to the desired type
   * @param entityId the entity ID
   * @param mapper the mapping function
   * @return mapped player, or null
   */
  public <T> T getPlayerByEntityId(int entityId, Function<UUID, T> mapper)
  {
    final var playerId = this.biDirectionalPlayerCache.getPlayer(entityId);
    if (playerId == null)
      return null;
    return mapper.apply(playerId);
  }

  /**
   * Closes the viewer with the given UUID, removing them from the platform
   * @param uuid the UUID of the viewer to close
   * @return a future that will complete on next tick with the closed viewer, or null if no viewer with the given UUID was found
   */
  public CompletableFuture<Viewer> closeViewer(UUID uuid)
  {
    final var future = new CompletableFuture<Viewer>();
    this.executeNextTick((platform) ->
    {
      final var viewer = platform.viewers.remove(uuid);
      if (viewer != null)
      {
        viewer.close();
        platform.entityIdToViewer.remove(viewer.entityId);
        platform.internalEventBus.onViewerLeave(platform, new RemovedViewer(uuid, viewer.entityId));
      }
      future.complete(viewer);
    });
    return future;
  }

  /**
   * Returns a collection of all viewers/clients currently being tracked by the platform
   * @return viewers
   */
  public Collection<Viewer> viewers()
  {
    return this.viewers.values();
  }

  private void tick()
  {
    this.taskExecutor.executeAll(this);
    for (final var player : this.viewers.values())
      player.tick(this);
  }

  Viewer createViewerPlayer(User user, int entityId)
  {
    final var uuid = user.getUUID();
    if (uuid == null)
      throw new IllegalArgumentException("User must have a UUID");

    if (this.viewers.containsKey(uuid))
      return this.viewers.get(uuid);

    final var viewer = new Viewer(user, entityId, this.unusedEntityIdProvider);
    this.viewers.put(uuid, viewer);
    this.entityIdToViewer.put(viewer.entityId, viewer);

    this.biDirectionalPlayerCache.put(uuid, entityId);

    this.executeNextTick(platform -> platform.internalEventBus.onViewerJoin(platform, viewer));
    return viewer;
  }

  void disconnectViewer(UUID uuid)
  {
    this.executeNextTick(platform ->
    {
      final var viewer = platform.viewers.remove(uuid);
      if (viewer != null)
      {
        viewer.closeInternal();
        platform.entityIdToViewer.remove(viewer.entityId);
        platform.internalEventBus.onViewerLeave(platform, new RemovedViewer(uuid, viewer.entityId));
      }
      platform.biDirectionalPlayerCache.removeByPlayerId(uuid);
    });
  }
}