package com.github.ieatglu3.nametagger;

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

final class VanillaNametagUpdater
{

  static void add(NametaggerPlatform platform, Viewer viewer)
  {
    final var createPacket = createTeamsPacket(viewer, WrapperPlayServerTeams.TeamMode.CREATE, NamedTextColor.WHITE, null, null);
    viewer.sendPacket(createPacket);
    for (final var otherViewer : platform.platformThreadViewers.values())
    {
      if (otherViewer == viewer)
        continue;
      final WrapperPlayServerTeams otherCreatePacket = createTeamsPacket(
        otherViewer,
        WrapperPlayServerTeams.TeamMode.CREATE,
        NamedTextColor.WHITE,
        null,
        null
      );
      otherViewer.sendPacket(createPacket);
      viewer.sendPacket(otherCreatePacket);
    }
  }

  static void update(NametaggerPlatform platform, Viewer viewer)
  {
    for (final var otherViewer : platform.platformThreadViewers.values())
    {
      final var viewerRenderer = viewer.vanillaNametagRenderer;
      if (viewerRenderer == null)
        continue;

      final var nametag = viewerRenderer.render(platform, otherViewer, viewer);
      final var updatePacket = createTeamsPacket(viewer, WrapperPlayServerTeams.TeamMode.UPDATE, nametag.color, nametag.prefix, nametag.suffix);
      otherViewer.sendPacket(updatePacket);
    }
  }

  static void remove(NametaggerPlatform platform, Viewer viewer)
  {
    final var removePacket = createTeamsPacket(viewer, WrapperPlayServerTeams.TeamMode.REMOVE, NamedTextColor.WHITE, null, null);
    for (final var otherViewer : platform.platformThreadViewers.values())
      otherViewer.sendPacket(removePacket);
  }

  private static WrapperPlayServerTeams createTeamsPacket(
    Viewer viewer,
    WrapperPlayServerTeams.TeamMode mode,
    NamedTextColor color,
    Component prefix,
    Component suffix
  )
  {
    return new WrapperPlayServerTeams(
      viewer.psPacketTeamName,
      mode,
      createTeamInfoForViewer(viewer, color, prefix, suffix),
      viewer.name()
    );
  }

  private static WrapperPlayServerTeams.ScoreBoardTeamInfo createTeamInfoForViewer(Viewer viewer, NamedTextColor color, Component prefix, Component suffix)
  {
    return new WrapperPlayServerTeams.ScoreBoardTeamInfo(
      Component.text(viewer.psPacketTeamName),
      prefix,
      suffix,
      WrapperPlayServerTeams.NameTagVisibility.ALWAYS,
      WrapperPlayServerTeams.CollisionRule.ALWAYS,
      color,
      WrapperPlayServerTeams.OptionData.NONE
    );
  }
}