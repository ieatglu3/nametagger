package com.github.ieatglu3.nametagger.platformutil;

import com.github.ieatglu3.nametagger.NametaggerPlatform;
import com.github.ieatglu3.nametagger.UnusedEntityIdProvider;
import com.github.ieatglu3.nametagger.platformutil.entity.DelegatedEntityIdProvider;
import com.github.ieatglu3.nametagger.platformutil.entity.EntityIdProvider;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for building platform-specific implementations
 */
public final class NametaggerPlatformUtil
{

  private static final Logger LOGGER = Logger.getLogger(NametaggerPlatformUtil.class.getName());

  private static final int FALLBACK_ENTITY_ID_START = 2_357_156;
  private static UnusedEntityIdProvider platformEntityIdProvider;

  private static final AtomicReference<NametaggerPlatform> platform = new AtomicReference<>(null);

  /**
   * Gets this platform instance, or null if the platform is not initialized
   *
   * @return NametaggerPlatform instance
   */
  public static NametaggerPlatform platform()
  {
    return platform.get();
  }

  /**
   * Initializes the platform with the given instance, should only be called once and only if the platform is not initialized yet
   * @param newPlatform platform instance to initialize with
   */
  public static NametaggerPlatform initializePlatform(NametaggerPlatform newPlatform)
  {
    if (!platform.compareAndSet(null, newPlatform))
      throw new IllegalStateException("Nametagger platform is already initialized");
    return newPlatform;
  }

  /**
   * Shuts down the platform, should only be called once and only if the platform is initialized and not already shutting down or shutdown
   */
  public static void shutdownPlatform()
  {
    final var platform = NametaggerPlatformUtil.platform.getAndSet(null);
    if (platform == null)
      throw new IllegalStateException("Nametagger platform is not initialized");
    if (platform.isShutOrShuttingDown())
      LOGGER.warning("Nametagger platform is already shutting down or was shut down, this should not happen");
    else
      platform.shutdown();
  }

  /**
   * Gets a platform-specific {@link UnusedEntityIdProvider} implementation, this is based on the server software and version.
   * <p>
   * If none was found, a naive fallback implementation will be used that generates entity IDs starting from {@link #FALLBACK_ENTITY_ID_START}; This MAY cause collisions with other plugins.
   * @return platform-specific {@link UnusedEntityIdProvider} implementation
   */
  public static synchronized UnusedEntityIdProvider platformEntityIdProvider()
  {
    if (platformEntityIdProvider != null)
      return platformEntityIdProvider;
    DelegatedEntityIdProvider provider;
    try
    {
      provider = EntityIdProvider.Mojang.get(0);
    }
    catch (Exception e) {
      provider = EntityIdProvider.Naive.getUnchecked(FALLBACK_ENTITY_ID_START);
      LOGGER.log(Level.WARNING, "Failed to find an entity id provider implementation, using fallback NaiveEntityIdProvider: ", e);
    }
    LOGGER.info("Using " + provider.name + " as the platform entity ID provider");
    return platformEntityIdProvider = provider;
  }

  private NametaggerPlatformUtil() { throw new InstantiationError(); }
}