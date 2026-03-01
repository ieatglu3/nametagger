package com.github.ieatglu3.nametagger;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A player/client viewing tags
 */
public final class Viewer
{

  private static final Logger LOGGER = Logger.getLogger(Viewer.class.getName());

  private final CopyOnWriteArrayList<NametagRenderer> renderers = new CopyOnWriteArrayList<>();

  private final ViewMap entities = new ViewMap();
  private final ConcurrentHashMap<Integer, Integer> pendingTagEntities = new ConcurrentHashMap<>();

  final User user;
  final int entityId;
  private final TaskExecutor<NametaggerPlatform> taskExecutor = new TaskExecutor<>();

  private final UnusedEntityIdProvider unusedEntityIdProvider;

  volatile boolean closed = false;

  Viewer(User user, int entityId, UnusedEntityIdProvider unusedEntityIdProvider)
  {
    Objects.requireNonNull(user, "user cannot be null");
    this.user = user;
    this.entityId = entityId;
    this.unusedEntityIdProvider = unusedEntityIdProvider;
  }

  /**
   * Gets the UUID of this viewer
   * @return UUID of this viewer
   */
  public UUID uuid()
  {
    return this.user.getUUID();
  }

  /**
   * Gets the name of this viewer
   * @return name of this viewer
   */
  public String name()
  {
    return this.user.getName();
  }

  /**
   * Gets the entity ID of this viewer
   * @return entity ID of this viewer
   */
  public int entityId()
  {
    return this.entityId;
  }

  /**
   * Checks if this viewer is closed
   * @return closed
   */
  public boolean isClosed()
  {
    return this.closed;
  }

  /**
   * Gets the client version of this viewer
   * @return client version of this viewer
   */
  public ClientVersion clientVersion()
  {
    return this.user.getClientVersion();
  }

  /**
   * Adds a tag renderer to this viewer
   * @param renderer the tag renderer to add
   * @throws NullPointerException if the tag renderer is null
   */
  public void attachRenderer(NametagRenderer renderer)
  {
    Objects.requireNonNull(renderer, "tagRenderer cannot be null");
    if (this.closed)
      return;
    this.taskExecutor.submit((platform) ->
    {
      this.invokeRendererAttach(platform, renderer);
      this.renderers.add(renderer);
    });
  }

  /**
   * Removes a tag renderer from this viewer
   * @param renderer the tag renderer to remove
   * @throws NullPointerException if the tag renderer is null
   */
  public void detachRenderer(NametagRenderer renderer)
  {
    Objects.requireNonNull(renderer, "tagRenderer cannot be null");
    if (this.closed)
      return;
    this.taskExecutor.submit((platform) ->
    {
      this.invokeRendererDetach(platform, renderer);
      this.renderers.remove(renderer);
    });
  }

  private void invokeRendererAttach(NametaggerPlatform platform, NametagRenderer renderer)
  {
    try
    {
      renderer.attached(platform, this, this.entities);
    }
    catch (Exception e) {
      LOGGER.log(
        Level.SEVERE,
        String.format("Exception while initializing renderer '%s' for viewer '%s'", renderer.name(), this.name()),
        e
      );
    }
  }

  private void invokeRendererDetach(NametaggerPlatform platform, NametagRenderer renderer)
  {
    try
    {
      renderer.detached(platform, this, this.entities);
    }
    catch (Exception e) {
      LOGGER.log(
        Level.SEVERE,
        String.format("Exception while detaching renderer '%s' for viewer '%s'", renderer.name(), this.name()),
        e
      );
    }
  }

  private void invokeRendererStartViewingEntity(NametaggerPlatform platform, NametagRenderer renderer, AttachedTagList tags)
  {
    try
    {
      renderer.startViewingEntity(platform, this, tags);
    }
    catch (Exception e) {
      LOGGER.log(
        Level.SEVERE,
        String.format("Exception while invoking startViewingEntity for renderer '%s' and viewer '%s'", renderer.name(), this.name()),
        e
      );
    }
  }

  private void invokeRendererStopViewingEntity(NametaggerPlatform platform, NametagRenderer renderer, AttachedTagList tags)
  {
    try
    {
      renderer.stopViewingEntity(platform, this, tags);
    }
    catch (Exception e) {
      LOGGER.log(
        Level.SEVERE,
        String.format("Exception while invoking stopViewingEntity for renderer '%s' and viewer '%s'", renderer.name(), this.name()),
        e
      );
    }
  }

  /**
   * Returns the backing list of renderers for this viewer
   * Only use this if you know what you're doing!
   * @return backing list of renderers for this viewer
   */
  public CopyOnWriteArrayList<NametagRenderer> renderers()
  {
    return this.renderers;
  }

  /**
   * Gets a collection of all the currently viewed tags for this viewer
   * @return viewing tags
   */
  public Collection<AttachedTagList> viewing()
  {
    return this.entities.values();
  }

  /**
   * Checks if the specified entity has tags/is tagged
   * @param entityId the entity ID to check
   * @return true if the entity is tagged, false otherwise
   */
  public boolean isEntityTagged(int entityId)
  {
    return this.entities.containsKey(entityId);
  }

  /**
   * Gets the tag list for the specified entity ID
   * @param entityId the entity ID to get the tag list for
   * @return the tag list for the specified entity ID, or null if the entity is not tagged
   */
  public AttachedTagList getTaggedEntity(int entityId)
  {
    return this.entities.get(entityId);
  }

  /**
   * Refreshes all the tags for this viewer
   */
  public void refreshTags()
  {
    for (final var tags : this.entities.values())
    {
      this.hideEntityTags(tags.parentEntityId);
      this.initTagList(tags);
    }
  }

  /**
   * Refreshes the tags for the specified entity
   * @param entityId entity ID to refresh the tags for
   */
  public void refreshEntity(int entityId)
  {
    final var tags = this.entities.get(entityId);
    if (tags == null)
      return;
    this.hideEntityTags(entityId);
    this.initTagList(tags);
  }

  /**
   * Removes the specified entity from this viewer's tag list
   * @param entityId entity ID to remove
   */
  public void removeTaggedEntity(int entityId)
  {
    this.hideEntityTags(entityId);
    final var tags = this.entities.remove(entityId);
    this.taskExecutor.submit((platform) ->
    {
      for (final var renderer : this.renderers)
        this.invokeRendererStopViewingEntity(platform, renderer, tags);
    });
  }

  /**
   * Clears all tagged entities from this viewer
   */
  public void clearTaggedEntities()
  {
    for (final var entityId : this.entities.keySet())
      this.removeTaggedEntity(entityId);
  }

  /**
   * Shows the tags for the specified entity. If the tags for the entity are already shown, this method does nothing
   * @param entityId entity ID to show the tags for
   */
  public void showEntityTags(int entityId)
  {
    final var tags = this.getTaggedEntity(entityId);
    if (tags == null)
      return;
    tags.show(this);
  }

  /**
   * Hides the tags for the specified entity. The tags will still be cached and can be shown again with {@link #showEntityTags(int)}
   * @param entityId entity ID to hide the tags for
   */
  public void hideEntityTags(int entityId)
  {
    final var tags = this.getTaggedEntity(entityId);
    if (tags == null)
      return;
    tags.hide(this);
  }

  /**
   * Clears the tags for the specified entity. The tags will be removed and cannot be shown again without re-adding the entity with {@link #addEntity(int, Vec)}
   * @param entityId entity ID
   */
  public void clearEntityTags(int entityId)
  {
    final var tags = this.getTaggedEntity(entityId);
    if (tags == null)
      return;
    tags.removeAll();
  }

  /**
   * Sends a packet to this viewer, does not check if the viewer is closed
   * @param packet packet
   */
  public void sendPacket(PacketWrapper<?> packet)
  {
    this.user.sendPacket(packet);
  }

  boolean isTagEntity(int entityId)
  {
    return this.pendingTagEntities.containsKey(entityId);
  }

  void removeTagEntity(int entityId)
  {
    this.pendingTagEntities.remove(entityId);
  }

  Integer getTagEntityParent(int entityId)
  {
    return this.pendingTagEntities.get(entityId);
  }

  void addTagEntity(int entityId, int parent)
  {
    this.pendingTagEntities.put(entityId, parent);
  }

  void addEntity(int entityId, Vec entityPosition)
  {
    final var tags = new AttachedTagList(
      entityId,
      entityPosition,
      this.unusedEntityIdProvider,
      this.clientVersion()
    );
    if (this.entities.containsKey(entityId))
    {
      LOGGER.warning(String.format("Attempted to add entity '%d' to viewer '%s', but the entity is already tagged. Existing tags will be replaced.",
        entityId,
        this.name()
      ));
      this.hideEntityTags(entityId);
    }
    this.entities.put(entityId, tags);
    this.initTagList(tags);
  }

  void initTagList(AttachedTagList tags)
  {
    this.taskExecutor.submit((platform) ->
    {
      for (final var renderer : this.renderers)
        this.invokeRendererStartViewingEntity(platform, renderer, tags);
    });
  }

  void updateTagPositions(int source, double x, double y, double z, PositionUpdateKind positionUpdateKind)
  {
    final var tags = this.entities.get(source);
    if (tags == null)
      return;
    tags.updatePosition(this, x, y, z, positionUpdateKind);
  }

  void nextTick(Consumer<Viewer> task)
  {
    if (this.closed)
      return;
    this.taskExecutor.submit(platform -> task.accept(this));
  }

  void tick(NametaggerPlatform platform)
  {
    this.taskExecutor.executeAll(platform);
    for (final var renderer : this.renderers)
    {
      try {
        renderer.render(platform, this, this.entities);
      }
      catch (Exception e) {
        LOGGER.log(
          Level.SEVERE,
          String.format("Exception while rendering tags for viewer '%s' with renderer '%s'", this.name(), renderer.name()),
          e
        );
      }
    }
    for (final var tags : this.entities.values())
      tags.tick(this);
  }

  void closeInternal()
  {
    this.closed = true;
  }

  void close()
  {
    this.closed = true;
    this.pendingTagEntities.clear();
    for (final var tags : this.entities.values())
      this.removeTaggedEntity(tags.parentEntityId);
  }
}