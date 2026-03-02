package com.github.ieatglu3.nametagger.example.spigot;

import com.github.ieatglu3.nametagger.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class StatusNametagRenderer extends NametagRenderer
{

  StatusNametagRenderer()
  {
    super("status-nametag-renderer"); // renderer name doesn't have to be unique but should be descriptive
  }

  @Override
  public void render(NametaggerPlatform platform, Viewer viewer, ViewMap viewMap)
  {
    viewMap.forEachEntry((entityId, taggedEntity) ->
    {

      // get the other entity by their entity ID
      Entity otherEntity = platform.getEntity(entityId);
      if (!(otherEntity instanceof LivingEntity))
        return;

      LivingEntity otherEntityLiving = (LivingEntity) otherEntity;

      // offset so that we render slightly above the entity's head
      double otherEntityEyeHeight = otherEntityLiving.getEyeHeight();
      Vec offset = Vec.of(0, otherEntityEyeHeight + 0.4, 0);

      ComponentTag healthTag = taggedEntity.getOrCreateComponentTag(0, Component.empty(), offset);

      // only needs to be called once
      // for simplicity we call it every render since it will not show again if it is already shown
      taggedEntity.showTag(viewer, healthTag);

      // creating the health tag component with a heart symbol and the entity's rounded health, colored red
      double otherEntityHealth = Math.round(otherEntityLiving.getHealth());
      Component healthDisplay = Component.text("❤", NamedTextColor.RED)
        .appendSpace()
        .append(Component.text(otherEntityHealth, NamedTextColor.WHITE));

      // update tag component so we're showing the entity's up-to-date health
      healthTag.setComponent(healthDisplay);

      // trigger an update
      healthTag.update();
    });
  }
}