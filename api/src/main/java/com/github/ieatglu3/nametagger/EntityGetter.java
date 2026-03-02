package com.github.ieatglu3.nametagger;

/**
 * Interface for getting entities by their ID
 */
@FunctionalInterface
public interface EntityGetter<E>
{

  EntityGetter<Object> NOOP = (platform, entityId) -> null;

  /**
   * Gets an entity by its ID
   *
   * @param platform the platform to get the entity from
   * @param entityId entity id
   * @return the entity
   */
  E getEntity(NametaggerPlatform platform, int entityId);
}