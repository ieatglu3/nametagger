package com.github.ieatglu3.nametagger.simple;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Represents a vanilla nametag, which consists of a prefix, suffix, and color.
 */
public final class VanillaNametag
{
  public final Component prefix;
  public final Component suffix;
  public final NamedTextColor color;

  VanillaNametag(NamedTextColor color, Component prefix, Component suffix)
  {
    this.prefix = prefix;
    this.suffix = suffix;
    this.color = color;
  }

  /**
   * Creates a new vanilla nametag with the given color, prefix, and suffix.
   * @param color color
   * @param prefix prefix
   * @param suffix suffix
   * @return a new vanilla nametag with the given color, prefix, and suffix
   */
  public static VanillaNametag of(NamedTextColor color, Component prefix, Component suffix)
  {
    return new VanillaNametag(color, prefix, suffix);
  }

  /**
   * Creates a new vanilla nametag with the given prefix and suffix, and a default color of white.
   * @param prefix prefix
   * @param suffix suffix
   * @return a new vanilla nametag with the given prefix and suffix, and a default color of white
   */
  public static VanillaNametag of(Component prefix, Component suffix)
  {
    return new VanillaNametag(NamedTextColor.WHITE, prefix, suffix);
  }
}