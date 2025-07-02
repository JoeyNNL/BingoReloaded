package io.github.steaf23.bingoreloaded.lib.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Statistic;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class ComponentUtils
{
    public static Component[] createComponentsFromString(String... strings) {
        return Arrays.stream(strings).map(s -> LegacyComponentSerializer.legacySection().deserialize(s)).toList().toArray(new Component[]{});
    }

    public static Component itemName(io.github.steaf23.bingoreloaded.lib.api.ItemType item)
    {
        return Component.translatable(itemKey(item));
    }

    public static Component advancementTitle(@NotNull Advancement advancement) {
        return Component.translatable(advancementKey(advancement) + ".title");
    }

    public static Component advancementDescription(@NotNull Advancement advancement) {
        return Component.translatable(advancementKey(advancement) + ".description");
    }

    public static Component statistic(Statistic statistic, Component... with)
    {
        return Component.translatable(statisticKey(statistic), with);
    }

    public static Component entityName(EntityType entity)
    {
        return Component.translatable(entityKey(entity));
    }

    private static String advancementKey(@NotNull Advancement advancement)
    {
        String result = advancement.getKey().getKey().replace("/", ".");
        result = switch (result) // Needed to correct Spigot on some advancement names vs how they appear in the lang files
        {
            case "husbandry.obtain_netherite_hoe" -> "husbandry.netherite_hoe";
            case "husbandry.bred_all_animals" -> "husbandry.breed_all_animals";
            case "adventure.read_power_of_chiseled_bookshelf" -> "adventure.read_power_from_chiseled_bookshelf";
            default -> result;
        };
        return "advancements." + result;
    }

    private static String statisticKey(Statistic statistic)
    {
        String prefix = statistic.isSubstatistic() ? "stat_type.minecraft." : "stat.minecraft.";
        String result = io.github.steaf23.bingoreloaded.lib.util.StatisticsKeyConverter.getMinecraftTranslationKey(statistic);
        return !result.isEmpty() ? prefix + result : statistic.name();
    }

    private static String itemKey(io.github.steaf23.bingoreloaded.lib.api.ItemType item)
    {
        return (item.isBlock() ? "block" : "item") + ".minecraft." + item.getKey().getKey();
    }

    private static String entityKey(EntityType entity)
    {
        // Note: before 1.20.6 , mooshroom and snow_golem needed to be translated manually.
        return "entity.minecraft." + entity.name().toLowerCase();
    }
}
