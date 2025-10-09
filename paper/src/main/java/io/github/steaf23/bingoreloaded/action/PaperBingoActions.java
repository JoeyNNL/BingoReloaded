package io.github.steaf23.bingoreloaded.action;

import io.github.steaf23.bingoreloaded.data.BingoSession;
import io.github.steaf23.bingoreloaded.data.config.BingoConfigurationData;
import io.github.steaf23.bingoreloaded.data.config.BingoOptions;
import io.github.steaf23.bingoreloaded.player.ActionResult;
import io.github.steaf23.bingoreloaded.player.BingoPlayerSender;
import io.github.steaf23.bingoreloaded.player.PlayerHandle;
import io.github.steaf23.bingoreloaded.player.paper.PlayerHandlePaper;
import io.github.steaf23.bingoreloaded.util.DiscordWebhookUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Paper-specific BingoAction implementations that require Bukkit API access.
 * These actions cannot be in the common module as they use platform-specific code.
 */
public class PaperBingoActions {

    /**
     * Creates a BiFunction for the /bingo discordstats command.
     * Sends current player statistics to a configured Discord webhook.
     */
    public static BiFunction<String[], BingoSession, ActionResult> createDiscordStatsAction(
            BingoAction bingoAction, BingoConfigurationData config) {
        
        return (args, session) -> {
            String webhookUrl = config.getOptionValue(BingoOptions.DISCORD_WEBHOOK_URL);

            if (webhookUrl.isEmpty()) {
                Component msg = Component.text("Discord webhook URL is not configured!")
                        .color(NamedTextColor.RED);
                BingoPlayerSender.sendMessage(msg, bingoAction.getLastUser());
                return ActionResult.IGNORED;
            }

            Component msg = Component.text("Sending statistics to Discord...")
                    .color(NamedTextColor.YELLOW);
            BingoPlayerSender.sendMessage(msg, bingoAction.getLastUser());

            // Send stats asynchronously to avoid blocking
            boolean success = DiscordWebhookUtil.sendStatsToDiscord(config);

            if (success) {
                Component successMsg = Component.text("Statistics successfully sent to Discord!")
                        .color(NamedTextColor.GREEN);
                BingoPlayerSender.sendMessage(successMsg, bingoAction.getLastUser());
                return ActionResult.SUCCESS;
            } else {
                Component errorMsg = Component.text("Failed to send statistics to Discord. Check console for details.")
                        .color(NamedTextColor.RED);
                BingoPlayerSender.sendMessage(errorMsg, bingoAction.getLastUser());
                return ActionResult.IGNORED;
            }
        };
    }

    /**
     * Creates a BiFunction for the /bingo tpteammate command.
     * Allows players to teleport to their teammates during an active game.
     */
    public static BiFunction<String[], BingoSession, ActionResult> createTeammateTeleportAction(BingoAction bingoAction) {
        return (args, session) -> {
            // Only players can use this command
            if (!(bingoAction.getLastUser() instanceof PlayerHandle playerHandle)) {
                Component msg = Component.text("Only players can use this command!")
                        .color(NamedTextColor.RED);
                BingoPlayerSender.sendMessage(msg, bingoAction.getLastUser());
                return ActionResult.IGNORED;
            }

            // Must be in an active game
            if (!session.sessionIsActive()) {
                Component msg = Component.text("You must be in an active game to use this command!")
                        .color(NamedTextColor.RED);
                BingoPlayerSender.sendMessage(msg, playerHandle);
                return ActionResult.IGNORED;
            }

            // Get the Bukkit player
            if (!(playerHandle instanceof PlayerHandlePaper paperHandle)) {
                return ActionResult.IGNORED;
            }

            Optional<Player> optionalPlayer = paperHandle.sessionPlayer();
            if (optionalPlayer.isEmpty()) {
                Component msg = Component.text("Could not find your player!")
                        .color(NamedTextColor.RED);
                BingoPlayerSender.sendMessage(msg, playerHandle);
                return ActionResult.IGNORED;
            }

            Player player = optionalPlayer.get();
            String targetName = args.length > 1 ? args[1] : null;

            // If no target specified, show usage
            if (targetName == null || targetName.isEmpty()) {
                Component msg = Component.text("Usage: /bingo tpteammate <player>")
                        .color(NamedTextColor.RED);
                BingoPlayerSender.sendMessage(msg, playerHandle);
                return ActionResult.IGNORED;
            }

            // Find the target player
            Player targetPlayer = org.bukkit.Bukkit.getPlayer(targetName);
            if (targetPlayer == null || !targetPlayer.isOnline()) {
                Component msg = Component.text("Player '" + targetName + "' is not online!")
                        .color(NamedTextColor.RED);
                BingoPlayerSender.sendMessage(msg, playerHandle);
                return ActionResult.IGNORED;
            }

            // Check if target is on the same team
            var playerTeam = session.getPlayerTeam(playerHandle);
            var targetTeam = session.getPlayerTeam(new PlayerHandlePaper(targetPlayer));

            if (playerTeam.isEmpty() || targetTeam.isEmpty() || !playerTeam.get().equals(targetTeam.get())) {
                Component msg = Component.text("You can only teleport to teammates!")
                        .color(NamedTextColor.RED);
                BingoPlayerSender.sendMessage(msg, playerHandle);
                return ActionResult.IGNORED;
            }

            // Perform the teleport
            player.teleport(targetPlayer.getLocation());

            Component successMsg = Component.text("Teleported to " + targetPlayer.getName() + "!")
                    .color(NamedTextColor.GREEN);
            BingoPlayerSender.sendMessage(successMsg, playerHandle);

            return ActionResult.SUCCESS;
        };
    }
}
