package com.github.ieatglu3.nametagger.example_spigot;

import com.github.ieatglu3.nametagger.NametaggerPlatform;
import com.github.ieatglu3.nametagger.platformutil.NametaggerPlatformUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public final class StatusNametags extends JavaPlugin implements Listener
{

  // our nametag renderer
  private final StatusNametagRenderer nametagRenderer = new StatusNametagRenderer();

  @Override
  public void onEnable()
  {

    // get the platform instance, which should have been initialized by the spigot platform implementation
    NametaggerPlatform platform =  NametaggerPlatformUtil.platform();

    // listen for viewers joining
    platform.listenForViewerJoin((innerPlatform, viewer) -> {

      UUID playerId = viewer.uuid();

      Player player = Bukkit.getPlayer(playerId);
      player.sendMessage("§aLiving entities will have a health tag above their head, showing their current health with a heart symbol");

      // attach our nametag renderer to this viewer
      viewer.attachRenderer(this.nametagRenderer);
    });

    // you may also listen for viewers being removed
    // the platform automatically cleans up viewer data on remove, so you don't need to worry about cleaning up viewer data
    platform.listenForViewerRemove((innerPlatform, removedViewer) -> {
      UUID playerId = removedViewer.uuid();
      Player player = Bukkit.getPlayer(playerId);
      if (player != null)
        player.sendMessage("§cYour health tag will no longer be visible to other players");
    });
  }

  @Override
  public void onDisable()
  {
    // platform will automatically clean up all viewer data and attached renderers
  }
}