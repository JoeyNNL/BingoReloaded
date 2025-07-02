package io.github.steaf23.bingoreloaded.lib.scoreboard;

import io.github.steaf23.bingoreloaded.lib.api.PlayerHandle;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class PlayerHUDGroup
{
    protected final Map<String, Component[]> registeredFields;
    private final List<PlayerHUD> huds;
    private final HUDRegistry registry;

    public PlayerHUDGroup(HUDRegistry registry) {
        this.huds = new ArrayList<>();
        this.registry = registry;
        this.registeredFields = new HashMap<>();
    }

    protected abstract PlayerHUD createHUDForPlayer(PlayerHandle player);

    public void addPlayer(PlayerHandle player) {
        PlayerHUD hud = createHUDForPlayer(player);
        // Don't re-add players if they are already added
        if (huds.stream().anyMatch(other -> hud.getPlayerId().equals(other.getPlayerId()))) {
            return;
        }
        registry.addPlayerHUD(hud);
        huds.add(hud);
    }

    public void removePlayer(PlayerHandle player) {
        registry.removePlayerHUD(player.uniqueId());
        huds.removeIf(h -> h.getPlayerId().equals(player.uniqueId()));
    }

    public void removeAllPlayers() {
        for (PlayerHUD hud : huds) {
            registry.removePlayerHUD(hud.getPlayerId());
        }

        huds.clear();
    }

    public void updateVisible() {
        huds.forEach(PlayerHUD::update);
    }

    public void addSidebarArgument(String key, Component... text) {
        registeredFields.put(key, text);
    }
}
