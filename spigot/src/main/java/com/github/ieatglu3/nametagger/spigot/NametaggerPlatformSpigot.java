package com.github.ieatglu3.nametagger.spigot;

import com.github.ieatglu3.bukec.LazyEntityCache;
import com.github.ieatglu3.nametagger.NametaggerPlatform;
import com.github.ieatglu3.nametagger.platformutil.NametaggerPlatformUtil;
import com.github.ieatglu3.nametagger.platformutil.NametaggerThreadFactory;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.concurrent.Executors;

/**
 * Spigot implementation a Nametagger platform
 */
public final class NametaggerPlatformSpigot extends JavaPlugin implements Listener
{

  private LazyEntityCache entityCache = null;

  private boolean reloaded = false;

  @Override
  public void onEnable()
  {
    if (this.reloaded)
      this.getLogger().warning("nametagger was reloaded, which is not supported and may cause issues");

    this.getServer().getPluginManager().registerEvents(this, this);

    final var platform = NametaggerPlatform.builder()
      .unusedEntityIdProvider(NametaggerPlatformUtil.platformEntityIdProvider())
      .executorService(Executors.newSingleThreadScheduledExecutor(NametaggerThreadFactory.create()))
      .entityGetter((entityPlatform, entityId) -> this.entityCache.get(entityId))
      .build();

    // run the gc on the platform thread, should be fine since the gc is fast and runs infrequently
    // don't see a reason to run it on a separate thread, at least for now
    this.entityCache = LazyEntityCache.create(platform.executorService(), Duration.ofMinutes(1));
    this.entityCache.link(this);
    this.entityCache.startGC();

    final var initializedPlatform = NametaggerPlatformUtil.initializePlatform(platform);
    initializedPlatform.start();
  }

  @Override
  public void onDisable()
  {
    this.entityCache.unlink();
    this.entityCache.stopGC();
    NametaggerPlatformUtil.shutdownPlatform();
    this.reloaded = true;
  }
}