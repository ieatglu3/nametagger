package com.github.ieatglu3.nametagger;

import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPickItemFromEntity;
import com.github.retrooper.packetevents.wrapper.play.server.*;

import java.util.UUID;

final class PacketListener extends PacketListenerAbstract
{

  private final NametaggerPlatform platform;
  PacketListener(NametaggerPlatform platform)
  {
    this.platform = platform;
  }

  @Override
  public void onUserDisconnect(UserDisconnectEvent event)
  {
    final var user = event.getUser();
    final var userId = user.getUUID();
    if (userId == null)
      return;
    this.platform.disconnectViewer(userId, false);
  }

  @Override
  public void onPacketSend(PacketSendEvent event)
  {
    final var packetType = event.getPacketType();

    if (packetType == PacketType.Play.Server.SPAWN_LIVING_ENTITY)
    {
      final var packet = new WrapperPlayServerSpawnLivingEntity(event);
      final var viewerId = event.getUser().getUUID();
      this.handleSpawnEntity(packet.getEntityId(), Vec.from(packet.getPosition()), viewerId);
    }

    else if (packetType == PacketType.Play.Server.SPAWN_ENTITY) {
      final var packet = new WrapperPlayServerSpawnEntity(event);
      final var viewerId = event.getUser().getUUID();
      this.handleSpawnEntity(packet.getEntityId(), Vec.from(packet.getPosition()), viewerId);
    }

    else if (packetType == PacketType.Play.Server.SPAWN_PLAYER) {
      final var packet = new WrapperPlayServerSpawnPlayer(event);
      final var viewerId = event.getUser().getUUID();
      this.handleSpawnEntity(packet.getEntityId(), Vec.from(packet.getPosition()), viewerId);
    }

    else if (packetType == PacketType.Play.Server.ENTITY_METADATA) {
      final var playerId = event.getUser().getUUID();
      final var viewer = this.platform.getViewerByUUID(playerId);

      if (viewer == null)
        return;

      final var packet = new WrapperPlayServerEntityMetadata(event);
      final var entityId = packet.getEntityId();

      final var taggedEntity = viewer.getTaggedEntity(entityId);
      if (taggedEntity == null)
        return;

      final var bitsetData = TagEntity.findEntityDataByIndex(packet.getEntityMetadata(), 0, EntityDataTypes.BYTE);
      if (bitsetData == null)
        return;

      final var bitset = (byte) bitsetData.getValue();

      final var isInvisible = TagEntity.FlagBitmask.Invisibility.isSet(bitset);
      final var isSneaking = TagEntity.FlagBitmask.Sneaking.isSet(bitset);

      taggedEntity.sneaking = isSneaking;
      taggedEntity.invisible = isInvisible;

      for (final var tag : taggedEntity.tags())
      {
        final var shouldHideWhenInvisible = tag.hideWhenInvisible();
        final var shouldHideWhenSneaking = tag.hideWhenSneaking();

        if ((isInvisible && shouldHideWhenInvisible) || (isSneaking && shouldHideWhenSneaking))
          taggedEntity.hideTag(viewer, tag);
        else
          taggedEntity.showTag(viewer, tag);
      }
    }

    else if (packetType == PacketType.Play.Server.DESTROY_ENTITIES) {
      final var packet = new WrapperPlayServerDestroyEntities(event);
      final var viewerId = event.getUser().getUUID();
      final var viewer = this.platform.getViewerByUUID(viewerId);
      if (viewer != null)
      {
        for (final var entityId : packet.getEntityIds())
        {
          viewer.removeTagEntity(entityId);
          viewer.removeTaggedEntity(entityId);
        }
      }
    }

    else if (packetType == PacketType.Play.Server.ENTITY_RELATIVE_MOVE) {
      final var packet = new WrapperPlayServerEntityRelativeMove(event);
      this.handleEntityRelativeMove(event, packet.getEntityId(), packet.getDeltaX(), packet.getDeltaY(), packet.getDeltaZ());
    }

    else if (packetType == PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION) {
      final var packet = new WrapperPlayServerEntityRelativeMoveAndRotation(event);
      this.handleEntityRelativeMove(event, packet.getEntityId(), packet.getDeltaX(), packet.getDeltaY(), packet.getDeltaZ());
    }

    else if (packetType == PacketType.Play.Server.ENTITY_POSITION_SYNC) {
      final var packet = new WrapperPlayServerEntityPositionSync(event);
      final var position = packet.getValues().getPosition();
      this.handleEntityPositionSync(event, packet.getId(), position, PositionUpdateKind.Absolute);
    }

    else if (packetType == PacketType.Play.Server.ENTITY_TELEPORT) {
      final var packet = new WrapperPlayServerEntityTeleport(event);
      this.handleEntityPositionSync(event, packet.getEntityId(), packet.getPosition(), PositionUpdateKind.AbsoluteLegacy);
    }

    else if (packetType == PacketType.Play.Server.JOIN_GAME) {
      final var packet = new WrapperPlayServerJoinGame(event);
      final var entityId = packet.getEntityId();
      this.platform.startTrackingViewer(event.getUser(), entityId);
    }

    else if (packetType == PacketType.Play.Server.RESPAWN) {
      final var viewerId = event.getUser().getUUID();
      final var viewer = this.platform.getViewerByUUID(viewerId);
      if (viewer != null)
        viewer.clearTaggedEntities();
    }
  }

  private void handleEntityRelativeMove(PacketSendEvent event, int packetEntityId, double deltaX, double deltaY, double deltaZ)
  {
    final var viewerId = event.getUser().getUUID();
    final var viewer = this.platform.getViewerByUUID(viewerId);
    if (viewer != null)
      viewer.updateTagPositions(
        packetEntityId,
        deltaX,
        deltaY,
        deltaZ,
        PositionUpdateKind.Relative
      );
  }

  private void handleEntityPositionSync(PacketSendEvent event, int entityId, Vector3d position, PositionUpdateKind updateKind)
  {
    final var viewerId = event.getUser().getUUID();
    final var viewer = this.platform.getViewerByUUID(viewerId);
    if (viewer != null)
      viewer.updateTagPositions(
        entityId,
        position.getX(),
        position.getY(),
        position.getZ(),
        updateKind
      );
  }

  private void handleSpawnEntity(int packetEntityId, Vec entityPosition, UUID viewerId)
  {
    final var viewer = this.platform.getViewerByUUID(viewerId);
    if (viewer == null || viewer.isTagEntity(packetEntityId))
      return;
    viewer.addEntity(packetEntityId, entityPosition);
  }

  @Override
  public void onPacketReceive(PacketReceiveEvent event)
  {
    final var packetType = event.getPacketType();
    if (packetType == PacketType.Play.Client.INTERACT_ENTITY)
    {
      final var packet = new WrapperPlayClientInteractEntity(event);
      final var packetEntityId = packet.getEntityId();
      final var viewerId = event.getUser().getUUID();
      final var viewer = this.platform.getViewerByUUID(viewerId);

      if (viewer == null)
        return;

      final var parentEntityId = viewer.getTagEntityParent(packetEntityId);
      if (parentEntityId != null) // route interaction with tag entity to parent entity
      {
        packet.setEntityId(parentEntityId);
        event.markForReEncode(true);
      }
    }
    else if (packetType == PacketType.Play.Client.PICK_ITEM_FROM_ENTITY) {
      final var packet = new WrapperPlayClientPickItemFromEntity(event);
      final var packetEntityId = packet.getEntityId();
      final var viewerId = event.getUser().getUUID();
      final var viewer = this.platform.getViewerByUUID(viewerId);

      if (viewer == null)
        return;

      final var parentEntityId = viewer.getTagEntityParent(packetEntityId);
      if (parentEntityId != null) // route interaction with tag entity to parent entity
      {
        packet.setEntityId(parentEntityId);
        event.markForReEncode(true);
      }
    }
  }
}