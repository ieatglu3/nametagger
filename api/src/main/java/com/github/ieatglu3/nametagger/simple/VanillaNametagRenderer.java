package com.github.ieatglu3.nametagger.simple;

import com.github.ieatglu3.nametagger.NametaggerPlatform;
import com.github.ieatglu3.nametagger.Viewer;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Renders the vanilla nametag for a viewer.
 */
@FunctionalInterface
public interface VanillaNametagRenderer
{
  VanillaNametagRenderer NOOP = (platform, viewer, target) -> VanillaNametag.of(NamedTextColor.WHITE, null, null);
  /**
   * Renders a vanilla nametag for the target viewer as seen by the viewer.
   *
   * @param platform the platform
   * @param viewer the viewer
   * @param target the target
   * @return the rendered nametag
   */
  VanillaNametag render(NametaggerPlatform platform, Viewer viewer, Viewer target);
}