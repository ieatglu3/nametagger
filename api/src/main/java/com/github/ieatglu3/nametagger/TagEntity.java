package com.github.ieatglu3.nametagger;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataType;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;
import io.github.retrooper.packetevents.adventure.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

// todo; display entity support
final class TagEntity
{

  enum FlagBitmask
  {
    Sneaking(1), Invisibility(5);

    final int index;

    FlagBitmask(int index)
    {
      this.index = index;
    }

    boolean isSet(byte flags)
    {
      return (flags & (1 << this.index)) != 0;
    }
  }

  private static final int DATA_WATCHER_SHARED_FLAGS = 0;

  private static final int DATA_WATCHER_NAME_FLAG = 2;
  private static final int DATA_WATCHER_NAME_VISIBLE_FLAG = 3;

  final int id;

  private final UUID uuid;
  private final AtomicInteger sharedFlags = new AtomicInteger(0);
  private final ClientVersion clientVersion;

  TagEntity(int id, UUID uuid, ClientVersion clientVersion)
  {
    this.id = id;
    this.uuid = uuid;
    this.clientVersion = clientVersion;
  }

  WrapperPlayServerEntityMetadata namePacket(Component name)
  {
    final var isModern = this.clientVersion.isNewerThanOrEquals(ClientVersion.V_1_13);
    final var nameData = isModern ?
      new EntityData<>(DATA_WATCHER_NAME_FLAG, EntityDataTypes.OPTIONAL_ADV_COMPONENT, Optional.ofNullable(name)) :
      new EntityData<>(DATA_WATCHER_NAME_FLAG, EntityDataTypes.STRING, name == null ? "" : LegacyComponentSerializer.legacySection().serialize(name));

    final var nameVisibleData = isModern ?
      new EntityData<>(DATA_WATCHER_NAME_VISIBLE_FLAG, EntityDataTypes.BOOLEAN, name != null) :
      new EntityData<>(DATA_WATCHER_NAME_VISIBLE_FLAG, EntityDataTypes.BYTE, (byte) (name != null ? 1 : 0));

    return new WrapperPlayServerEntityMetadata(
      this.id,
      List.of(nameData, nameVisibleData)
    );
  }

  void setFlag(FlagBitmask mask, boolean value)
  {
    updateFlags(this.sharedFlags, mask.index, value);
  }

  WrapperPlayServerEntityMetadata flagsPacket(byte flags, int index)
  {
    return new WrapperPlayServerEntityMetadata(this.id, List.of(new EntityData<>(index, EntityDataTypes.BYTE, flags)));
  }

  PacketWrapper<?>[] spawnPackets(Location location)
  {
    final var spawnPacket = new WrapperPlayServerSpawnEntity(
      this.id,
      this.uuid,
      EntityTypes.ARMOR_STAND,
      location,
      location.getYaw(),
      0,
      null
    );
    return new PacketWrapper<?>[]{
      spawnPacket,
      this.flagsPacket((byte) this.sharedFlags.get(), DATA_WATCHER_SHARED_FLAGS)
    };
  }

  float entityHeight()
  {
    return 1.975F; // todo; refer to comment in class header
  }

  static EntityData<?> findEntityDataByIndex(List<EntityData<?>> dataList, int index, EntityDataType<?> type)
  {
    for (int i = 0, dataListSize = dataList.size(); i < dataListSize; i++)
    {
      final var data = dataList.get(i);
      if (data.getIndex() == index && data.getType() == type)
        return data;
    }
    return null;
  }

  static void updateFlags(AtomicInteger flags, int index, boolean value)
  {
    byte newSharedFlags;
    int current;
    do
    {
      current = flags.get();
      newSharedFlags = value ? (byte) (current | (1 << index)) : (byte) (current & ~(1 << index));
    } while (!flags.compareAndSet(current, newSharedFlags));
  }

  static WrapperPlayServerDestroyEntities destroyEntitiesPacket(int... entityIds)
  {
    return new WrapperPlayServerDestroyEntities(entityIds);
  }
}