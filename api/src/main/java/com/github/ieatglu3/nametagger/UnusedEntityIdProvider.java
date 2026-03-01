package com.github.ieatglu3.nametagger;

/**
 * Serves as a proxy between the server and the platform for generating unique entity ids
 */
@FunctionalInterface
public interface UnusedEntityIdProvider
{
  /**
   * Gets the next unused entity ID
   *
   * @return next unused entity ID
   */
  int next();
}