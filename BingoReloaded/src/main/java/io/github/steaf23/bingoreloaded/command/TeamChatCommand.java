package io.github.steaf23.bingoreloaded.command;

import io.github.steaf23.bingoreloaded.gameloop.BingoSession;
import io.github.steaf23.bingoreloaded.data.BingoTranslation;
import io.github.steaf23.bingoreloaded.player.BingoParticipant;
import io.github.steaf23.bingoreloaded.player.BingoPlayer;
import io.github.steaf23.bingoreloaded.player.team.BingoTeam;
import io.github.steaf23.bingoreloaded.player.team.TeamManager;
import io.github.steaf23.bingoreloaded.util.TranslatedMessage;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class TeamChatCommand implements Listener, TabExecutor
{
    private final List<BingoPlayer> enabledPlayers;
    private final Function<Player, BingoSession> sessionResolver;

    public TeamChatCommand(Function<Player, BingoSession> sessionResolver)
    {
        this.enabledPlayers = new ArrayList<>();
        this.sessionResolver = sessionResolver;
    }

    private BingoSession getSession(Player player)
    {
        return sessionResolver.apply(player);
    }

    @EventHandler
    public void onPlayerSendMessage(final AsyncPlayerChatEvent event)
    {
        BingoSession session = getSession(event.getPlayer());
        if (session == null)
            return;

        TeamManager teamManager = session.teamManager;

        BingoParticipant player = teamManager.getPlayerAsParticipant(event.getPlayer());
        if (!enabledPlayers.contains(player)) return;

        BingoTeam team = player.getTeam();
        if (team == null) return;

        String message = event.getMessage();
        sendMessage(team, event.getPlayer(), message);

        event.setCancelled(true);
    }

    public void sendMessage(BingoTeam team, Player player, String message)
    {
        team.getMembers()
                .forEach(member -> {
                    if (member.sessionPlayer().isEmpty()) {
                        return;
                    }

                    Player receivingPlayer = member.sessionPlayer().get();
                    receivingPlayer.spigot().sendMessage(new ComponentBuilder()
                            .append(team.getPrefix())
                            .append(ChatColor.RESET + "<" + player.getDisplayName() + "> ")
                            .append(message)
                            .build());
                });
    }

    @Override
    public boolean onCommand(@NonNull CommandSender commandSender, @NonNull Command command, @NonNull String s, String[] args)
    {
        if (commandSender instanceof Player p)
        {
            BingoSession session = getSession(p);
            if (session == null)
                return false;

            TeamManager teamManager = session.teamManager;
            BingoParticipant participant = teamManager.getPlayerAsParticipant(p);

            if (!(participant instanceof BingoPlayer player))
                return false;

            if (!teamManager.getParticipants().contains(player))
            {
                new TranslatedMessage(BingoTranslation.NO_CHAT).color(ChatColor.RED).send(p);
                return false;
            }

            if (enabledPlayers.contains(player))
            {
                enabledPlayers.remove(player);
                new TranslatedMessage(BingoTranslation.CHAT_OFF).color(ChatColor.GREEN).arg("/btc").send(p);
            }
            else
            {
                enabledPlayers.add(player);
                new TranslatedMessage(BingoTranslation.CHAT_ON).color(ChatColor.GREEN).arg("/btc").send(p);
            }
        }
        return false;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return List.of();
    }
}