package io.github.steaf23.bingoreloaded.data.helper;

import io.github.steaf23.bingoreloaded.lib.api.PlayerHandle;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class SerializablePlayer
{
    public String pluginVersion;
    public UUID playerId;
    public Location location;
    public double health;
    public int hunger;
    public GameMode gamemode;
    public Location spawnPoint;
    public int xpLevel;
    public float xpPoints;
    public ItemStack[] inventory;
    public ItemStack[] enderInventory;

    public static @NotNull SerializablePlayer fromPlayer(@NotNull JavaPlugin plugin, @NotNull Player player)
    {
        SerializablePlayer data = new SerializablePlayer();
        data.pluginVersion = plugin.getPluginMeta().getVersion();
        data.playerId = player.getUniqueId();
        data.location = player.getLocation();
        data.health = player.getHealth();
        data.hunger = player.getFoodLevel();
        data.gamemode = player.getGameMode();
        data.spawnPoint = player.getRespawnLocation() == null ? player.getWorld().getSpawnLocation() : player.getRespawnLocation();
        data.xpLevel = player.getLevel();
        data.xpPoints = player.getExp();
        data.inventory = player.getInventory().getContents();
        data.enderInventory = player.getEnderChest().getContents();
        return data;
    }

    /**
     * Reset all player data and set location
     */
    public static SerializablePlayer reset(JavaPlugin plugin, Player player, Location location)
    {
        SerializablePlayer data = new SerializablePlayer();
        data.pluginVersion = plugin.getPluginMeta().getVersion();
        data.location = location;
        data.playerId = player.getUniqueId();
        data.health = 20.0;
        data.hunger = 20;
        data.gamemode = player.getGameMode();
        data.spawnPoint = null;
        data.xpLevel = 0;
        data.xpPoints = 0.0f;
        data.inventory = new ItemStack[player.getInventory().getSize()];
        data.enderInventory = new ItemStack[player.getEnderChest().getSize()];
        return data;
    }

    public SerializablePlayer() {
    }

    public void apply(PlayerHandle player)
    {
        if (!playerId.equals(player.getUniqueId()))
            return;

        player.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN);

        player.setHealth(health);
        player.setFoodLevel(hunger);
        player.setGameMode(gamemode);
        player.setRespawnLocation(spawnPoint, true);
        player.setLevel(xpLevel);
        player.setExp(xpPoints);

        player.getInventory().clear();
        if (inventory != null)
        {
            player.getInventory().setContents(inventory);
        }
        player.getEnderChest().clear();
        if (enderInventory != null)
        {
            player.getEnderChest().setContents(enderInventory);
        }
        player.updateInventory();
    }
}
