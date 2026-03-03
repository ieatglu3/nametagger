package com.github.ieatglu3.nametagger;

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

final class PrefixSuffixUpdater
{

  static void add(NametaggerPlatform platform, Viewer viewer)
  {
    final var createPacket = createTeamsPacket(viewer, WrapperPlayServerTeams.TeamMode.CREATE);
    viewer.sendPacket(createPacket);
    for (final var otherViewer : platform.platformThreadViewers.values())
    {
      if (otherViewer == viewer)
        continue;
      final WrapperPlayServerTeams otherCreatePacket = createTeamsPacket(otherViewer, WrapperPlayServerTeams.TeamMode.CREATE);
      otherViewer.sendPacket(createPacket);
      viewer.sendPacket(otherCreatePacket);
    }
  }

  static void update(NametaggerPlatform platform, Viewer viewer)
  {
    final var updatePacket = createTeamsPacket(viewer, WrapperPlayServerTeams.TeamMode.UPDATE);
    for (final var otherViewer : platform.platformThreadViewers.values())
      otherViewer.sendPacket(updatePacket);
  }

  static void remove(NametaggerPlatform platform, Viewer viewer)
  {
    final var removePacket = createTeamsPacket(viewer, WrapperPlayServerTeams.TeamMode.REMOVE);
    for (final var otherViewer : platform.platformThreadViewers.values())
      otherViewer.sendPacket(removePacket);
  }

  private static WrapperPlayServerTeams createTeamsPacket(Viewer viewer, WrapperPlayServerTeams.TeamMode mode)
  {
    return new WrapperPlayServerTeams(
      viewer.psPacketTeamName,
      mode,
      createTeamInfoForViewer(viewer),
      viewer.name()
    );
  }

  private static WrapperPlayServerTeams.ScoreBoardTeamInfo createTeamInfoForViewer(Viewer viewer)
  {
    return new WrapperPlayServerTeams.ScoreBoardTeamInfo(
      Component.text(viewer.psPacketTeamName),
      viewer.nametagPrefix,
      viewer.nametagSuffix,
      WrapperPlayServerTeams.NameTagVisibility.ALWAYS,
      WrapperPlayServerTeams.CollisionRule.ALWAYS,
      NamedTextColor.WHITE,
      WrapperPlayServerTeams.OptionData.NONE
    );
  }
}