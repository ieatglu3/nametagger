package com.github.ieatglu3.nametagger;

import com.github.retrooper.packetevents.protocol.entity.EntityPositionData;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityPositionSync;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import net.kyori.adventure.text.Component;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a single component tag
 */
public final class ComponentTag
{

  private static final VarHandle NEEDS_UPDATE;
  private static final VarHandle SHOWN;
  private static final VarHandle OFFSET;
  private static final VarHandle COMPONENT;
  private static final VarHandle MARKED_FOR_REMOVAL;
  static {
    try {
      final MethodHandles.Lookup lookup = MethodHandles.lookup();
      NEEDS_UPDATE = lookup.findVarHandle(ComponentTag.class, "needsUpdate", boolean.class);
      SHOWN = lookup.findVarHandle(ComponentTag.class, "shown", boolean.class);
      OFFSET = lookup.findVarHandle(ComponentTag.class, "offset", Vec.class);
      COMPONENT = lookup.findVarHandle(ComponentTag.class, "component", Component.class);
      MARKED_FOR_REMOVAL = lookup.findVarHandle(ComponentTag.class, "markedForRemoval", boolean.class);
    } catch (ReflectiveOperationException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  final TagEntity entity;

  volatile boolean needsUpdate = false;
  volatile boolean shown = false;

  volatile boolean hideWhenInvisible = true;
  volatile boolean hideWhenSneaking = true;

  volatile Vec offset;
  volatile Component component;
  private volatile boolean markedForRemoval = false;

  final UUID uuid;

  ComponentTag(TagEntity entity, Component name, UUID uuid, Vec offset)
  {
    Objects.requireNonNull(entity, "entity cannot be null");
    Objects.requireNonNull(name, "name cannot be null");
    Objects.requireNonNull(uuid, "uuid cannot be null");
    Objects.requireNonNull(offset, "offset cannot be null");
    this.entity = entity;
    this.component = name;
    this.uuid = uuid;
    this.offset = offset;
  }

  boolean isMarkedForRemoval()
  {
    return (boolean) MARKED_FOR_REMOVAL.getAcquire(this);
  }

  void markForRemoval()
  {
    MARKED_FOR_REMOVAL.setRelease(this, true);
  }

  void updatePosition(Viewer viewer, double x, double y, double z, PositionUpdateKind positionUpdateKind)
  {
    final var entityId = this.entityId();
    PacketWrapper<?> packet;
    switch (positionUpdateKind)
    {
      case Relative:
        packet = new WrapperPlayServerEntityRelativeMove(entityId, x, y, z, true);
        break;
      case Absolute:
      {
        final Vec offset = (Vec) OFFSET.getAcquire(this);
        packet = new WrapperPlayServerEntityPositionSync(
          entityId,
          new EntityPositionData(offset.add(x, y, z).toPacketEventsVector3d(), Vector3d.zero(), 0, 0),
          true
        );
        break;
      }
      case AbsoluteLegacy:
      {
        final Vec offset = (Vec) OFFSET.getAcquire(this);
        packet = new WrapperPlayServerEntityTeleport(
          entityId,
          new Location(offset.add(x, y, z).toPacketEventsVector3d(), 0, 0),
          true
        );
        break;
      }
      default:
        throw new IllegalArgumentException();
    }
    viewer.sendPacket(packet);
  }

  void tick(Viewer viewer)
  {
    if (NEEDS_UPDATE.weakCompareAndSet(this, true, false))
    {
      final var nameUpdatePacket = this.entity.namePacket(this.component);
      viewer.sendPacket(nameUpdatePacket);
    }
  }

  boolean compareAndSetShown(boolean expected, boolean updated)
  {
    return SHOWN.compareAndSet(this, expected, updated);
  }

  /**
   * Sets whether this tag should be hidden when the parent entity is invisible
   * @param hideWhenInvisible
   * @return this tag list
   */
  public ComponentTag setHideWhenInvisible(boolean hideWhenInvisible)
  {
    this.hideWhenInvisible = hideWhenInvisible;
    return this;
  }

  /**
   * Sets whether this tag should be hidden when the parent entity is sneaking
   * @param hideWhenSneaking
   * @return this tag list
   */
  public ComponentTag setHideWhenSneaking(boolean hideWhenSneaking)
  {
    this.hideWhenSneaking = hideWhenSneaking;
    return this;
  }

  /**
   * If this tag should be hidden when the parent entity is invisible
   * @return should hide when invisible
   */
  public boolean hideWhenInvisible()
  {
    return this.hideWhenInvisible;
  }

  /**
   * If this tag should be hidden when the parent entity is sneaking
   * @return should hide when sneaking
   */
  public boolean hideWhenSneaking()
  {
    return this.hideWhenSneaking;
  }

  /**
   * Gets the UUID of this tag. This is used internally to track tags, and is not related to the underlying entity's UUID
   * @return uuid
   */
  public UUID uuid()
  {
    return this.uuid;
  }

  /**
   * Sets the component of this tag. The component cannot be null.
   * Calling this will not immediately update the tag for viewers; {@link #update()} must be called for updates
   *
   * @param name the new component for this tag
   * @return this tag
   * @throws NullPointerException if the provided name is null
   */
  public ComponentTag setComponent(Component name)
  {
    Objects.requireNonNull(name, "name cannot be null");
    COMPONENT.setRelease(this, name);
    return this;
  }

  /**
   * Sets the offset of this tag. The offset cannot be null
   *
   * @param offset the new offset for this tag
   * @return this tag
   * @throws NullPointerException if the provided offset is null
   */
  public ComponentTag setOffset(Vec offset)
  {
    Objects.requireNonNull(offset, "offset cannot be null");
    OFFSET.setRelease(this, offset.subtract(0, this.entity.entityHeight(), 0));
    return this;
  }

  /**
   * Marks this tag for an update on the next tick
   */
  public void update()
  {
    NEEDS_UPDATE.setRelease(this, true);
  }

  /**
   * Gets the component of this tag.
   * @return component
   */
  public Component component()
  {
    return this.component;
  }

  /**
   * Gets the offset of this tag
   * @return offset
   */
  public Vec offset()
  {
    return this.offset;
  }

  /**
   * Gets the entity ID of this tag's underlying entity
   * @return entity ID
   */
  public int entityId()
  {
    return this.entity.id;
  }

  /**
   * Returns whether this tag is currently visible
   * @return visibility
   */
  public boolean isVisible()
  {
    return this.shown;
  }
}