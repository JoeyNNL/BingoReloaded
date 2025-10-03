package io.github.steaf23.bingoreloaded.event;

import io.github.steaf23.bingoreloaded.BingoReloaded;
import io.github.steaf23.bingoreloaded.util.DiscordWebhookUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class DiscordEventListener implements Listener {
    
    @EventHandler
    public void onBingoEnded(BingoEndedEvent event) {
        // Verzend automatisch stats naar Discord na elke game
        BingoReloaded.getInstance().getLogger().info("Game beëindigd, verzenden stats naar Discord...");
        
        // Wacht een korte tijd zodat alle stats zijn bijgewerkt
        org.bukkit.Bukkit.getScheduler().runTaskLater(BingoReloaded.getInstance(), () -> {
            boolean success = DiscordWebhookUtil.sendStatsToDiscord();
            if (success) {
                BingoReloaded.getInstance().getLogger().info("Discord stats automatisch verzonden na game einde");
            } else {
                BingoReloaded.getInstance().getLogger().info("Discord stats niet verzonden (webhook niet geconfigureerd of fout)");
            }
        }, 20L); // 1 seconde wachten
    }
}