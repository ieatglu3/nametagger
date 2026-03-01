package com.github.ieatglu3.nametagger;

import java.util.UUID;

/**
 * Represents a viewer that has been removed/disconnected
 */
public final class RemovedViewer
{
  private final UUID uuid;
  private final int entityId;

  public RemovedViewer(UUID uuid, int entityId)
  {
    this.uuid = uuid;
    this.entityId = entityId;
  }

  /**
   * Gets the entity ID that was assigned to this viewer
   * @return entity id
   */
  public int entityId()
  {
    return entityId;
  }

  /**
   * Gets the UUID of this viewer
   * @return UUID of this viewer
   */
  public UUID uuid()
  {
    return uuid;
  }
}