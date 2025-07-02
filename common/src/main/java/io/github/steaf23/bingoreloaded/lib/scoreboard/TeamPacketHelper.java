package io.github.steaf23.bingoreloaded.lib.scoreboard;

import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import io.github.steaf23.bingoreloaded.lib.PlayerDisplay;
import net.kyori.adventure.text.Component;

import java.util.Collection;

public class TeamPacketHelper
{
    public static void createTeamVisibleToPlayer(Player player, String identifier, Component displayName, Component prefix, Component suffix, Collection<String> entries) {
        WrapperPlayServerTeams.ScoreBoardTeamInfo info = new WrapperPlayServerTeams.ScoreBoardTeamInfo(
                displayName,
                prefix,
                suffix,
                WrapperPlayServerTeams.NameTagVisibility.ALWAYS,
                WrapperPlayServerTeams.CollisionRule.ALWAYS,
                null,
                WrapperPlayServerTeams.OptionData.NONE
        );
        PacketWrapper<WrapperPlayServerTeams> packet = new WrapperPlayServerTeams(identifier, WrapperPlayServerTeams.TeamMode.CREATE, info, entries);
        PlayerDisplay.sendPlayerPacket(player, packet);
    }

    public static void removeTeamVisibleToPlayer(Player player, String identifier) {
        PacketWrapper<WrapperPlayServerTeams> packet = new WrapperPlayServerTeams(identifier, WrapperPlayServerTeams.TeamMode.REMOVE, (WrapperPlayServerTeams.ScoreBoardTeamInfo)null);
        PlayerDisplay.sendPlayerPacket(player, packet);
    }
}
