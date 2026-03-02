package com.github.ieatglu3.nametagger;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import net.kyori.adventure.text.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

/**
 * Represents a list of tags that are attached to a parent entity
 */
public final class TaggedEntity
{

  final int parentEntityId;

  private final AtomicReference<Vec> parentPosition;

  private final UnusedEntityIdProvider unusedEntityIdProvider;

  private final CopyOnWriteArrayList<ComponentTag> tags = new CopyOnWriteArrayList<>();
  private final Map<UUID, ComponentTag> tagsByUuid = new ConcurrentHashMap<>();

  private final ClientVersion clientVersion;

  volatile boolean sneaking = false;
  volatile boolean invisible = false;

  TaggedEntity(int parentEntityId, Vec parentPosition, UnusedEntityIdProvider unusedEntityIdProvider, ClientVersion clientVersion)
  {
    this.parentEntityId = parentEntityId;
    this.parentPosition = new AtomicReference<>(parentPosition);
    this.unusedEntityIdProvider = unusedEntityIdProvider;
    this.clientVersion = clientVersion;
  }

  /**
   * Returns whether the parent entity is sneaking
   * @return sneaking
   */
  public boolean isSneaking()
  {
    return this.sneaking;
  }

  /**
   * Returns whether the parent entity is invisible
   * @return invisible
   */
  public boolean isInvisible()
  {
    return this.invisible;
  }

  /**
   * Gets the backing list of tags
   * @return backing list of tags
   */
  public CopyOnWriteArrayList<ComponentTag> tags()
  {
    return this.tags;
  }

  /**
   * Gets the entity ID of the tracked parent entity
   * @return parent entity ID
   */
  public int entityId()
  {
    return this.parentEntityId;
  }

  /**
   * Creates a new tag with the specified component and adds it to the list of tags
   * @param component the component of the {@link ComponentTag}
   * @return the created {@link ComponentTag}
   */
  public ComponentTag createComponentTag(Component component)
  {
    return this.createComponentTag(component, Vec.ZERO);
  }

  /**
   * Gets or creates a tag at the specified index with the specified component, and adds it to the list of tags
   * If a tag already exists at the specified index, it will be returned
   * @param index index
   * @param component the component of the {@link ComponentTag}
   * @return the created or existing {@link ComponentTag}
   */
  public ComponentTag getOrCreateComponentTag(int index, Component component)
  {
    return this.getOrCreateComponentTag(index, component, Vec.ZERO);
  }

  /**
   * Gets or creates a tag at the specified index with the specified component and offset, and adds it to the list of tags
   * If a tag already exists at the specified index, it will be returned
   * @param index index
   * @param component the component of the {@link ComponentTag}
   * @param offset the offset of the {@link ComponentTag} from the parent entity
   * @return the created or existing {@link ComponentTag}
   */
  public ComponentTag getOrCreateComponentTag(int index, Component component, Vec offset)
  {
    final var existingTag = this.getOrNull(index);
    if (existingTag != null)
      return existingTag;
    return this.createComponentTag(component, offset);
  }

  /**
   * Creates a new tag with the specified content and offset, and adds it to the list of tags
   * @param component the component of the {@link ComponentTag}
   * @param offset the offset of the {@link ComponentTag} from the parent entity
   * @return the created {@link ComponentTag}
   */
  public ComponentTag createComponentTag(Component component, Vec offset)
  {
    final var tagEntity = new TagEntity(this.unusedEntityIdProvider.next(), this.nextUuid(), this.clientVersion);
    final var tag = new ComponentTag(tagEntity, component).setOffset(offset);
    this.tags.add(tag);
    this.tagsByUuid.put(tag.uuid(), tag);
    return tag;
  }

  private UUID nextUuid()
  {
    UUID uuid;
    do
    {
      uuid = UUID.randomUUID();
    } while (this.tagsByUuid.containsKey(uuid));
    return uuid;
  }

  /**
   * Removes a tag from this tag list by its index. The tag will be marked for removal and will be removed on the next tick
   * @param index index
   */
  public void remove(int index)
  {
    final var tag = this.getOrNull(index);
    if (tag != null)
      tag.markedForRemoval = true;
  }

  /**
   * Gets the position of the parent entity
   * @return the position of the tracked parent entity
   */
  public Vec parentPosition()
  {
    return this.parentPosition.get();
  }

  /**
   * Gets the tag at the specified index in this tag list
   * @param index index
   * @return the tag at the specified index in this tag list
   * @throws IndexOutOfBoundsException
   */
  public ComponentTag get(int index)
  {
    return this.tags.get(index);
  }

  /**
   * Gets the tag with the specified UUID in this tag list, or null if no such tag exists.
   * @param uuid the UUID of the tag to get
   * @return the tag with the specified UUID in this tag list, or null if no such tag exists
   */
  public ComponentTag get(UUID uuid)
  {
    return this.tagsByUuid.get(uuid);
  }

  /**
   * Gets the tag at the specified index in this tag list, or null if the index is out of bounds.
   * @param index index
   * @return tag at index, or null if index is out of bounds
   */
  public ComponentTag getOrNull(int index)
  {
    if (index < 0 || index >= this.tags.size())
      return null;
    return this.tags.get(index);
  }

  /**
   * Shows the specified tag to the specified viewer
   *
   * @param viewer the viewer to show the tag to
   * @param tag the tag to show
   */
  public void showTag(Viewer viewer, ComponentTag tag)
  {
    if (!this.tagsByUuid.containsKey(tag.uuid()))
      throw new IllegalArgumentException("Tag is not in this tag list");
    if (!tag.compareAndSetShown(false, true))
      return;
    tag.entity.setFlag(TagEntity.FlagBitmask.Invisibility, true);
    viewer.nextTick(tickingViewer -> {
      final var spawnPos = this.parentPosition().add(tag.offset);
      final var spawnLocation = new Location(spawnPos.toPacketEventsVector3d(), 0, 0);
      final var spawnPackets = tag.entity.spawnPackets(spawnLocation);
      tickingViewer.addTagEntity(tag.entityId(), this.parentEntityId);
      for (final var packet : spawnPackets)
        tickingViewer.sendPacket(packet);
      tag.update();
    });
  }

  /**
   * Hides the specified tag from the specified viewer
   *
   * @param viewer the viewer to hide the tag from
   * @param tag the tag to hide
   */
  public void hideTag(Viewer viewer, ComponentTag tag)
  {
    if (!this.tagsByUuid.containsKey(tag.uuid()))
      throw new IllegalArgumentException("Tag is not in this tag list");
    if (!tag.compareAndSetShown(true, false))
      return;
    viewer.nextTick(tickingViewer -> {
      final var destroyPacket = TagEntity.destroyEntitiesPacket(tag.entityId());
      tickingViewer.sendPacket(destroyPacket);
    });
  }

  void show(Viewer viewer)
  {
    for (final var tag : this.tags)
      showTag(viewer, tag);
  }

  void hide(Viewer viewer)
  {
    this.hide0(viewer, true, (v, packet) -> v.nextTick(tickingViewer -> tickingViewer.sendPacket(packet)));
  }

  void hideNow(Viewer viewer)
  {
    this.hide0(viewer, false, Viewer::sendPacket);
  }

  void hide0(Viewer viewer, boolean checkIfShown, BiConsumer<Viewer, WrapperPlayServerDestroyEntities> packetSendingConsumer)
  {
    var removedEntities = new int[1];
    var removedEntitiesSize = 0;
    for (final var tag : this.tags)
    {
      if (checkIfShown && !tag.compareAndSetShown(true, false))
        continue;
      removedEntities[removedEntitiesSize++] = tag.entityId();
      if (removedEntitiesSize >= removedEntities.length)
        removedEntities = Arrays.copyOf(removedEntities, removedEntitiesSize * 2);
    }
    final var removeArray = Arrays.copyOf(removedEntities, removedEntitiesSize);
    packetSendingConsumer.accept(viewer, TagEntity.destroyEntitiesPacket(removeArray));
  }

  void updatePosition(Viewer viewer, double x, double y, double z, PositionUpdateKind positionUpdateKind)
  {
    switch (positionUpdateKind)
    {
      case Relative:
        this.parentPosition.updateAndGet(prev -> prev.add(x, y, z));
        break;
      case Absolute:
      case AbsoluteLegacy:
        this.parentPosition.set(Vec.of(x, y, z));
        break;
    }

    for (final var tag : this.tags)
      if (tag.isVisible())
        tag.updatePosition(viewer, x, y, z, positionUpdateKind);
  }

  void tick(Viewer viewer)
  {

    final var removeQueue = new ArrayList<ComponentTag>();
    for (final var tag : this.tags)
    {
      if (tag.markedForRemoval)
        removeQueue.add(tag);
      else
        if (tag.isVisible())
          tag.tick(viewer);
    }

    for (final var tag : removeQueue)
    {
      // todo; better to send this in a batch
      final var destroyPacket = TagEntity.destroyEntitiesPacket(tag.entityId());
      viewer.sendPacket(destroyPacket);
      this.tags.remove(tag);
      this.tagsByUuid.remove(tag.uuid());
    }
  }

  void removeAll()
  {
    for (final var tag : this.tags)
      tag.markedForRemoval = true;
  }
}