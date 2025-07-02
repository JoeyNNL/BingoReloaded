package io.github.steaf23.bingoreloaded.data;

import io.github.steaf23.bingoreloaded.BingoReloaded;
import io.github.steaf23.bingoreloaded.cards.CardSize;
import io.github.steaf23.bingoreloaded.cards.TaskCard;
import io.github.steaf23.bingoreloaded.lib.data.core.DataAccessor;
import io.github.steaf23.bingoreloaded.gameloop.phase.BingoGame;
import io.github.steaf23.bingoreloaded.player.team.BingoTeam;
import io.github.steaf23.bingoreloaded.player.team.TeamManager;
import io.github.steaf23.bingoreloaded.settings.BingoGamemode;

@Deprecated
public class RecoveryCardData
{
    private final DataAccessor data = BingoReloaded.getDataAccessor("data/recovered");

    public boolean loadCards(BingoGame game)
    {
        boolean success = false;
        if (data.getBoolean("ended")) return false;

//        Message.log(ChatColor.GREEN + "The last game did not finish, attempting to recover bingo card...");
        BingoGamemode mode = BingoGamemode.fromDataString(data.getString("gamemode", ""));
        CardSize size = CardSize.fromWidth(data.getInt("size", 5));

        if (game.getTeamManager().getActiveTeams().teamCount() == 0)
        {
//            Message.log(ChatColor.RED + "Could not resume game, no teams have joined the last game?!");
            return false;
        }

//        // add each team's card back
//        ConfigurationSection cards = data.getConfig().getConfigurationSection("cards");
//        if (cards == null) return false;
//        for (String key : cards.getKeys(false))
//        {
//            BingoTeam cardOwner = game.getTeamManager().activateTeamFromName(key);
//
//            BingoCard card = CardBuilder.fromMode(mode, size, game.getTeamManager().getActiveTeams().size());
//            success = fillCard(game.getTeamManager(), cardOwner, card);
//            game.getTeamManager().setCardForTeam(cardOwner, card);
//        }

        return success;
    }

    public void writeDebug(String text)
    {
        data.setString("testString", text);
        data.saveChanges();
    }

    public boolean fillCard(TeamManager manager, BingoTeam team, TaskCard card)
    {
//        List<?> itemNames = data.getConfig().getList("cards." + team.getName());
//        if (itemNames == null) return false;
//        if (itemNames.size() != card.size.fullCardSize) return false;
//
//        List<AbstractBingoTask> items = new ArrayList<>();
//        for (Object entry : itemNames)
//        {
//            String itemName = "";
//            if (!(entry instanceof HashMap<?, ?> itemMap)) continue;
//            Optional<?> mapEntry = itemMap.keySet().stream().findAny();
//            if (mapEntry.isPresent())
//            {
//                itemName = (String) mapEntry.get();
//            }
//
//            AbstractBingoTask item = new ItemTask(Material.getMaterial(itemName));
//            OfflinePlayer completedBy = Bukkit.getOfflinePlayer(UUID.fromString(itemName));
//
//            if (completedBy != null)
//            {
//                item.complete(completedBy, 0, team);
//            }
//            items.add(item);
//        }
//
//        card.tasks = items;
        return true;
    }

    public void saveCards(TeamManager manager, BingoGamemode mode, CardSize size)
    {
//        data.getConfig().set("gamemode", mode.getDataName());
//        data.getConfig().set("size", size.cardSize);
//
//        data.getConfig().set("cards", null);
//
//        for (BingoTeam t : manager.getActiveTeams())
//        {
//            List<Map<String, String>> items = new ArrayList<>();
//
//            for (int i = 0; i < t.card.tasks.size(); i++)
//            {
//                AbstractBingoTask slot = t.card.tasks.get(i);
//
//                if (slot.getWhoCompleted().isPresent())
//                {
//                    items.add(new HashMap<>() {{put(slot.material.name(), "");}});
//                }
//                else
//                {
//                    items.add(new HashMap<>() {{put(slot.material.name(), slot.getWhoCompleted().get().getName());}});
//                }
//            }
//            data.getConfig().set("cards." + t.getName(), items);
//        }
//
//        for (BingoTeam t : manager.getActiveTeams())
//        {
//            data.getConfig().set("teams." + t.getName(), t.team.getEntries().stream().toList());
//        }
//
//        data.saveConfig();
    }

    public void markCardEnded(boolean value)
    {
        data.setBoolean("ended", value);
        data.saveChanges();
    }
}
