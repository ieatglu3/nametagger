package com.github.ieatglu3.nametagger;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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
    private EntityGetter<Object> entityGetter = EntityGetter.NOOP;
    private ThreadFactory threadFactory = NametaggerThreadFactory.create();
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
     * Sets the thread factory to use for the platform's executor service
     * <br><br>
     * Default implementation creates daemon threads with the name "NametaggerPlatform-Thread-%d"
     * @param threadFactory thread factory
     * @return this builder
     */
    public Builder threadFactory(ThreadFactory threadFactory)
    {
      this.threadFactory = threadFactory;
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
      if (this.threadFactory == null)
        throw new IllegalStateException("Thread factory must be set");
      if (this.tickFrequency.isNegative() || this.tickFrequency.isZero())
        throw new IllegalStateException("Tick frequency must be positive");
      if (this.entityGetter == null)
        throw new IllegalStateException("Entity getter must be set");
      return new NametaggerPlatform(this.threadFactory, this.unusedEntityIdProvider, this.entityGetter, this.tickFrequency);
    }
  }

  /**
   * Creates a new builder for the NametaggerPlatform
   * @return builder
   */
  public static Builder builder()
  {
    return new Builder();
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

    public void onViewerRemove(NametaggerPlatform platform, RemovedViewer viewer)
    {
      for (final var listener : this.viewerRemoveListeners)
        listener.accept(platform, viewer);
    }
  }

  private static final Logger LOGGER = Logger.getLogger(NametaggerPlatform.class.getName());

  private final ConcurrentHashMap<UUID, Viewer> viewers = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<Integer, Viewer> entityIdToViewer = new ConcurrentHashMap<>();

  private final ReentrantReadWriteLock stopLock = new ReentrantReadWriteLock();

  private volatile PacketListener packetListener;
  private volatile State state = State.Idle;

  private final UnusedEntityIdProvider unusedEntityIdProvider;
  private final EntityGetter<Object> entityGetter;
  private final ScheduledExecutorService executorService;

  private final ConcurrentTaskExecutor<NametaggerPlatform> taskExecutor = new ConcurrentTaskExecutor<>();
  private final InternalEventBus internalEventBus = new InternalEventBus();
  private final Duration tickFrequency;

  private static final AtomicReferenceFieldUpdater<NametaggerPlatform, State> STATE =
    AtomicReferenceFieldUpdater.newUpdater(NametaggerPlatform.class, State.class, "state");

  NametaggerPlatform(
    ThreadFactory threadFactory,
    UnusedEntityIdProvider unusedEntityIdProvider,
    EntityGetter<Object> entityGetter,
    Duration tickFrequency
  )
  {
    this.executorService = Executors.newSingleThreadScheduledExecutor(threadFactory);
    this.unusedEntityIdProvider = unusedEntityIdProvider;
    this.entityGetter = entityGetter;
    this.tickFrequency = tickFrequency;
    this.packetListener = new PacketListener(this);
    PacketEvents.getAPI().getEventManager().registerListener(this.packetListener);

    if (!this.hasEntityGetter())
      LOGGER.warning("using no-op entity getter; getEntity() will always return null");
  }

  /**
   * Checks if this platform has an entity getter function provided
   * @return has entity getter
   */
  public boolean hasEntityGetter()
  {
    return this.entityGetter != EntityGetter.NOOP;
  }

  /**
   * Gets the single-threaded {@link ScheduledExecutorService} used by this platform
   * @return executor service
   */
  public ScheduledExecutorService executorService()
  {
    return this.executorService;
  }

  /**
   * Registers a listener to be called when a new player is created, which is when the platform starts tracking them
   * <br><br>
   * The listener is invoked on the network thread immediately after the player is created
   * @param listener listener to register
   */
  public void listenForViewerJoin(BiConsumer<NametaggerPlatform, Viewer> listener)
  {
    this.internalEventBus.registerViewerJoinListener(listener);
  }

  /**
   * Registers a listener to be called when a player is removed, which is when the platform stops tracking them
   * <br><br>
   * The listener is invoked on the next tick after the player is removed; the player is already closed at that point
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
   * May block if viewers are still being added or removed
   */
  public void shutdown()
  {
    if (!STATE.compareAndSet(this, State.Running, State.ShuttingDown))
      throw new IllegalStateException("Platform can only be shutdown from the Running state");

    this.stopLock.writeLock().lock();
    try
    {

      if (this.packetListener != null)
      {
        PacketEvents.getAPI().getEventManager().unregisterListener(this.packetListener);
        this.packetListener = null;
      }

      for (final UUID uuid : this.viewers.keySet())
        this.disconnectViewer(uuid, true);

      this.taskExecutor.executeAll(this);
      this.taskExecutor.shutdown();

      this.executorService.shutdown();
      try
      {
        if (!this.executorService.awaitTermination(5, TimeUnit.SECONDS))
          LOGGER.warning("Executor service did not shut down within the timeout");
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        LOGGER.warning("Interrupted while waiting for executor service to shut down");
      }
    }
    finally {
      this.state = State.Shutdown;
      this.stopLock.writeLock().unlock();
    }
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
   * Gets the viewer by their entity ID, and maps it to another object using the provided mapping function
   * @param entityId the entity ID
   * @param mapper the mapping function
   * @return mapped object, or null if no viewer was found or the viewer has no UUID
   */
  public <T> T mapViewerByEntityId(int entityId, Function<Viewer, T> mapper)
  {
    final var viewer = this.entityIdToViewer.get(entityId);
    if (viewer == null)
      return null;
    return mapper.apply(viewer);
  }

  /**
   * Closes the viewer forcibly with the given UUID, removing them from the platform
   * @param uuid the UUID of the viewer to close
   * @return future that completes with null if the platform was shut or shutting down, or if no viewer with the given UUID was found
   */
  public CompletableFuture<Viewer> closeViewer(UUID uuid)
  {
    return this.disconnectViewer(uuid, true);
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

  void startTrackingViewer(User user, int entityId)
  {
    final var uuid = user.getUUID();
    if (uuid == null)
      throw new IllegalArgumentException("User must have a UUID");

    if (this.isShutOrShuttingDown())
      return;

    final ReentrantReadWriteLock.ReadLock stopLock = this.stopLock.readLock();
    if (!stopLock.tryLock())
      return;

    final var viewer = new Viewer(user, entityId, this.unusedEntityIdProvider);
    try
    {
      final Viewer oldViewer = this.viewers.put(uuid, viewer);
      if (oldViewer != null) // todo; dont think this is very reliable but wtf am i really meant to do, this shouldn't happen
      {
        LOGGER.warning("Viewer with UUID " + uuid + " already exists, closing and replacing it");
        this.executeNextTick(platform -> oldViewer.close(platform, true));
        this.entityIdToViewer.remove(oldViewer.entityId);
      }
      this.entityIdToViewer.put(viewer.entityId, viewer);
      this.internalEventBus.onViewerJoin(this, viewer);
    }
    finally {
      stopLock.unlock();
    }
  }

  CompletableFuture<Viewer> disconnectViewer(UUID uuid, boolean forcibly)
  {
    final CompletableFuture<Viewer> future = new CompletableFuture<>();
    final ReentrantReadWriteLock.ReadLock stopLock = this.stopLock.readLock();

    if (!stopLock.tryLock())
    {
      future.complete(null);
      return future;
    }

    try
    {
      final Viewer viewer = this.viewers.remove(uuid);
      if (viewer != null)
        this.entityIdToViewer.remove(viewer.entityId);

      this.executeNextTick(platform ->
      {
        if (viewer != null)
        {
          viewer.close(platform, forcibly);
          platform.internalEventBus.onViewerRemove(platform, new RemovedViewer(uuid, viewer.entityId));
        }
        future.complete(viewer);
      });
    }
    finally {
      stopLock.unlock();
    }

    return future;
  }
}