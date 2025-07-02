package io.github.steaf23.bingoreloaded.hologram;

import io.github.steaf23.bingoreloaded.lib.util.ConsoleMessenger;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;

public class HologramManager
{
    private final Map<String, Hologram> holograms;

//    private YmlDataManager data = new YmlDataManager(BingoReloaded.get(), "holograms.yml");

    public HologramManager()
    {
        this.holograms = new HashMap<>();
    }

    public Hologram create(String id, Location location, String... lines)
    {
        if (holograms.containsKey(id))
        {
            ConsoleMessenger.warn("Hologram with id " + id + " already exists");
            return holograms.get(id);
        }

        Hologram holo = new Hologram(location, lines);
        holograms.put(id, holo);
        return holo;
    }

    public void destroy(String id)
    {
        if (holograms.containsKey(id))
        {
            holograms.get(id).destroy();
            holograms.remove(id);
        }
        else
        {
            ConsoleMessenger.warn("Hologram with id " + id + " does not exist");
        }
    }
}
