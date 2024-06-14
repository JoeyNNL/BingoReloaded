package io.github.steaf23.bingoreloaded.gui.inventory;

import io.github.steaf23.bingoreloaded.settings.BingoGamemode;
import io.github.steaf23.bingoreloaded.data.BingoTranslation;
import io.github.steaf23.bingoreloaded.data.ConfigData;
import io.github.steaf23.bingoreloaded.gameloop.phase.PregameLobby;
import io.github.steaf23.bingoreloaded.settings.PlayerKit;
import io.github.steaf23.easymenulib.inventory.BasicMenu;
import io.github.steaf23.easymenulib.inventory.MenuBoard;
import io.github.steaf23.easymenulib.inventory.item.ItemTemplate;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;

import java.util.function.BiConsumer;

public class VoteMenu extends BasicMenu
{
    private BasicMenu gamemodeOptions;
    private BasicMenu kitOptions;
    private BasicMenu cardOptions;
    private final PregameLobby lobby;

    private static final ItemTemplate EXIT = new ItemTemplate(8, Material.BARRIER,
                    "" + ChatColor.RED + ChatColor.BOLD + BingoTranslation.MENU_EXIT.translate());

    public VoteMenu(MenuBoard menuBoard, ConfigData.VoteList voteList, PregameLobby lobbyPhase)
    {
        super(menuBoard, BingoTranslation.OPTIONS_VOTE.translate(), 3);

        this.lobby = lobbyPhase;

        if (voteList.gamemodes().size() > 1)
        {
            gamemodeOptions = new BasicMenu(menuBoard, BingoTranslation.VOTE_GAMEMODE.translate(), 1);

            int itemIndex = 0;
            if (voteList.gamemodes().contains("regular_5"))
            {
                gamemodeOptions.addAction(new ItemTemplate(itemIndex, Material.LIME_CONCRETE,
                        ChatColor.BOLD + BingoGamemode.REGULAR.displayName + " - 5x5",
                        BingoTranslation.INFO_REGULAR_DESC.translate().split("\\n")), (args) -> {
                    HumanEntity player = args.player();
                    lobby.voteGamemode("regular_5", player);
                    gamemodeOptions.close(player);
                });
                itemIndex++;
            }
            if (voteList.gamemodes().contains("regular_3"))
            {
                gamemodeOptions.addAction(new ItemTemplate(itemIndex, Material.GREEN_CONCRETE,
                        ChatColor.BOLD + BingoGamemode.REGULAR.displayName + " - 3x3",
                        BingoTranslation.INFO_REGULAR_DESC.translate().split("\\n")), (args) -> {
                    HumanEntity player = args.player();
                    lobby.voteGamemode("regular_3", player);
                    gamemodeOptions.close(player);
                });
                itemIndex++;
            }
            if (voteList.gamemodes().contains("lockout_5"))
            {
                gamemodeOptions.addAction(new ItemTemplate(itemIndex, Material.PINK_CONCRETE,
                        ChatColor.BOLD + BingoGamemode.LOCKOUT.displayName + " - 5x5",
                        BingoTranslation.INFO_LOCKOUT_DESC.translate().split("\\n")), (args) -> {
                    HumanEntity player = args.player();
                    lobby.voteGamemode("lockout_5", player);
                    gamemodeOptions.close(player);
                });
                itemIndex++;
            }
            if (voteList.gamemodes().contains("lockout_3"))
            {
                gamemodeOptions.addAction(new ItemTemplate(itemIndex, Material.PURPLE_CONCRETE,
                        ChatColor.BOLD + BingoGamemode.LOCKOUT.displayName + " - 3x3",
                        BingoTranslation.INFO_LOCKOUT_DESC.translate().split("\\n")), (args) -> {
                    HumanEntity player = args.player();
                    lobby.voteGamemode("lockout_3", player);
                    gamemodeOptions.close(player);
                });
                itemIndex++;
            }
            if (voteList.gamemodes().contains("complete_5"))
            {
                gamemodeOptions.addAction(new ItemTemplate(itemIndex, Material.LIGHT_BLUE_CONCRETE,
                        ChatColor.BOLD + BingoGamemode.COMPLETE.displayName + " - 5x5",
                        BingoTranslation.INFO_COMPLETE_DESC.translate().split("\\n")), (args) -> {
                    HumanEntity player = args.player();
                    lobby.voteGamemode("complete_5", player);
                    gamemodeOptions.close(player);
                });
                itemIndex++;
            }
            if (voteList.gamemodes().contains("complete_3"))
            {
                gamemodeOptions.addAction(new ItemTemplate(itemIndex, Material.BLUE_CONCRETE,
                        ChatColor.BOLD + BingoGamemode.COMPLETE.displayName + " - 3x3",
                        BingoTranslation.INFO_COMPLETE_DESC.translate().split("\\n")), (args) -> {
                    HumanEntity player = args.player();
                    lobby.voteGamemode("complete_3", player);
                    gamemodeOptions.close(player);
                });
                itemIndex++;
            }
            if (voteList.gamemodes().contains("hotswap_5"))
            {
                gamemodeOptions.addAction(new ItemTemplate(itemIndex, Material.YELLOW_CONCRETE,
                        ChatColor.BOLD + BingoGamemode.HOTSWAP.displayName + " - 5x5",
                        BingoTranslation.INFO_HOTSWAP_DESC.translate(String.valueOf(lobby.getSession().settingsBuilder.view().hotswapGoal())).split("\\n")), (args) -> {
                    HumanEntity player = args.player();
                    lobby.voteGamemode("hotswap_5", player);
                    gamemodeOptions.close(player);
                });
                itemIndex++;
            }
            if (voteList.gamemodes().contains("hotswap_3"))
            {
                gamemodeOptions.addAction(new ItemTemplate(itemIndex, Material.ORANGE_CONCRETE,
                        ChatColor.BOLD + BingoGamemode.HOTSWAP.displayName + " - 3x3",
                        BingoTranslation.INFO_HOTSWAP_DESC.translate(String.valueOf(lobby.getSession().settingsBuilder.view().hotswapGoal())).split("\\n")), (args) -> {
                    HumanEntity player = args.player();
                    lobby.voteGamemode("hotswap_3", player);
                    gamemodeOptions.close(player);
                });
                itemIndex++;
            }
            gamemodeOptions.addCloseAction(EXIT.copy());

            addAction(new ItemTemplate(2, 1, Material.ENCHANTED_BOOK, TITLE_PREFIX + BingoTranslation.VOTE_GAMEMODE.translate()), p -> {
                gamemodeOptions.open(p);
            });
        }

        if (voteList.kits().size() > 1)
        {
            kitOptions = new BasicMenu(menuBoard, BingoTranslation.VOTE_KIT.translate(), 1);

            int itemIndex = 0;
            if (voteList.kits().contains("hardcore"))
            {
                kitOptions.addAction(new ItemTemplate(itemIndex, Material.RED_DYE,
                        PlayerKit.HARDCORE.getDisplayName(),
                        BingoTranslation.KIT_HARDCORE_DESC.translate().split("\\n")).setGlowing(true), (args) -> {
                    HumanEntity player = args.player();
                    lobby.voteKit(PlayerKit.HARDCORE.configName, player);
                    kitOptions.close(player);
                });
                itemIndex++;
            }
            if (voteList.kits().contains("normal"))
            {
                kitOptions.addAction(new ItemTemplate(itemIndex, Material.YELLOW_DYE,
                        PlayerKit.NORMAL.getDisplayName(),
                        BingoTranslation.KIT_NORMAL_DESC.translate().split("\\n")).setGlowing(true), (args) -> {
                    HumanEntity player = args.player();
                    lobby.voteKit(PlayerKit.NORMAL.configName, player);
                    kitOptions.close(player);
                });
                itemIndex++;
            }
            if (voteList.kits().contains("overpowered"))
            {
                kitOptions.addAction(new ItemTemplate(itemIndex, Material.PURPLE_DYE,
                        PlayerKit.OVERPOWERED.getDisplayName(),
                        BingoTranslation.KIT_OVERPOWERED_DESC.translate().split("\\n")).setGlowing(true), (args) -> {
                    HumanEntity player = args.player();
                    lobby.voteKit(PlayerKit.OVERPOWERED.configName, player);
                    kitOptions.close(player);
                });
                itemIndex++;
            }
            if (voteList.kits().contains("reloaded"))
            {
                kitOptions.addAction(new ItemTemplate(itemIndex, Material.CYAN_DYE,
                        PlayerKit.RELOADED.getDisplayName(),
                        BingoTranslation.KIT_RELOADED_DESC.translate().split("\\n")).setGlowing(true), (args) -> {
                    HumanEntity player = args.player();
                    lobby.voteKit(PlayerKit.RELOADED.configName, player);
                    kitOptions.close(player);
                });
                itemIndex++;
            }

            BiConsumer<PlayerKit, Integer> addCustomKit = (kit, slot) -> {
                kitOptions.addAction(new ItemTemplate(slot, Material.GRAY_DYE,
                        kit.getDisplayName()).setGlowing(true), (args) -> {
                    HumanEntity player = args.player();
                    lobby.voteKit(kit.configName, player);
                    kitOptions.close(player);
                });
            };

            if (voteList.kits().contains("custom_1") && PlayerKit.getCustomKit(PlayerKit.CUSTOM_1) != null)
            {
                addCustomKit.accept(PlayerKit.CUSTOM_1, itemIndex);
                itemIndex++;
            }
            if (voteList.kits().contains("custom_2") && PlayerKit.getCustomKit(PlayerKit.CUSTOM_2) != null)
            {
                addCustomKit.accept(PlayerKit.CUSTOM_2, itemIndex);
                itemIndex++;
            }
            if (voteList.kits().contains("custom_3") && PlayerKit.getCustomKit(PlayerKit.CUSTOM_3) != null)
            {
                addCustomKit.accept(PlayerKit.CUSTOM_3, itemIndex);
                itemIndex++;
            }
            if (voteList.kits().contains("custom_4") && PlayerKit.getCustomKit(PlayerKit.CUSTOM_4) != null)
            {
                addCustomKit.accept(PlayerKit.CUSTOM_4, itemIndex);
                itemIndex++;
            }
            if (voteList.kits().contains("custom_5") && PlayerKit.getCustomKit(PlayerKit.CUSTOM_5) != null)
            {
                addCustomKit.accept(PlayerKit.CUSTOM_5, itemIndex);
                itemIndex++;
            }

            if (itemIndex < 8)
                kitOptions.addCloseAction(EXIT.copy());

            addAction(new ItemTemplate(4, 1, Material.ENCHANTED_BOOK, TITLE_PREFIX + BingoTranslation.VOTE_KIT.translate()), p -> {
                kitOptions.open(p);
            });
        }

        if (voteList.cards().size() > 1)
        {
            cardOptions = new BasicMenu(menuBoard, BingoTranslation.VOTE_CARD.translate(), 1);

            int itemIndex = 0;
            for (String card : voteList.cards()) {
                String displayName = ChatColor.BOLD + card;
                Material material = Material.PAPER;
                ItemTemplate ItemTemplate = new ItemTemplate(itemIndex, material, displayName);

                // Add the menu item to the cardOptions menu
                cardOptions.addAction(ItemTemplate, (args) -> {
                    HumanEntity player = args.player();
                    lobby.voteCard(card, player);
                    cardOptions.close(player);
                });

                itemIndex++;
            }

            cardOptions.addCloseAction(EXIT.copy());
            addAction(new ItemTemplate(6, 1, Material.ENCHANTED_BOOK, TITLE_PREFIX + BingoTranslation.VOTE_CARD.translate()), p -> {
                cardOptions.open(p);
            });
        }

        addCloseAction(EXIT.copyToSlot(0, 2));
    }
}
