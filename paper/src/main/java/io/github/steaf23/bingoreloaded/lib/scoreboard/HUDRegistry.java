package io.github.steaf23.bingoreloaded.lib.scoreboard;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HUDRegistry implements Listener
{
    List<PlayerHUD> huds;

    public HUDRegistry() {
        huds = new ArrayList<>();
    }

    @EventHandler
    public void handlePlayerJoined(final PlayerJoinEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        huds.stream()
                .filter(hud -> hud.getPlayerId().equals(playerId))
                .forEach(PlayerHUD::update);
    }

    public void addPlayerHUD(PlayerHUD hud) {
        huds.add(hud);
    }

    public void removePlayerHUD(UUID playerId) {
        for (PlayerHUD hud : huds) {
            if (playerId.equals(hud.getPlayerId())) {
                hud.removeFromPlayer();
                huds.remove(hud);
                break;
            }
        }
    }
}
