package io.github.steaf23.bingoreloaded.gui.inventory;

import io.github.steaf23.bingoreloaded.data.BingoMessage;
import io.github.steaf23.bingoreloaded.data.TeamData;
import io.github.steaf23.bingoreloaded.lib.api.item.ItemTypePaper;
import io.github.steaf23.bingoreloaded.lib.api.MenuBoard;
import io.github.steaf23.bingoreloaded.lib.api.player.PlayerHandle;
import io.github.steaf23.bingoreloaded.lib.inventory.BasicMenu;
import io.github.steaf23.bingoreloaded.lib.inventory.ColorPickerMenu;
import io.github.steaf23.bingoreloaded.lib.inventory.FilterType;
import io.github.steaf23.bingoreloaded.lib.inventory.InventoryMenu;
import io.github.steaf23.bingoreloaded.lib.inventory.PaginatedSelectionMenu;
import io.github.steaf23.bingoreloaded.lib.inventory.action.MenuAction;
import io.github.steaf23.bingoreloaded.lib.inventory.action.NameEditAction;
import io.github.steaf23.bingoreloaded.lib.item.ItemTemplate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TeamEditorMenu extends PaginatedSelectionMenu
{
    private final TeamData teamData;

    private static final TeamData.TeamTemplate DEFAULT_NEW_TEAM = new TeamData.TeamTemplate("MyTeam", TextColor.fromHexString("#808080"));

    private static final ItemTemplate RESTORE_DEFAULT = new ItemTemplate(2, 5, ItemTypePaper.of(Material.TNT),
            Component.text("Restore Default Teams").color(NamedTextColor.RED).decorate(TextDecoration.BOLD),
            Component.text("This option will remove all created teams!"));

    private static final ItemTemplate CREATE_TEAM = new ItemTemplate(6, 5, ItemTypePaper.of(Material.EMERALD),
            Component.text("Create New Team").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD));

    public TeamEditorMenu(MenuBoard manager) {
        super(manager, Component.text("Edit Teams"), new ArrayList<>(), FilterType.DISPLAY_NAME);
        this.teamData = new TeamData();

        addAction(RESTORE_DEFAULT, arguments -> {
            teamData.reset();
            updateDisplay();
        });
        addAction(CREATE_TEAM, arguments -> createTeamEditor("").open(arguments.player()));
    }

    public void updateDisplay() {
        clearFilter();
        clearItems();
        List<ItemTemplate> items = new ArrayList<>();

        var teamMap = teamData.getTeams();
        for (String key : teamMap.keySet()) {
            TeamData.TeamTemplate template = teamMap.get(key);
            items.add(ItemTemplate.createColoredLeather(template.color(), ItemTypePaper.of(Material.LEATHER_HELMET))
                    .setName(template.nameComponent().color(template.color()).decorate(TextDecoration.BOLD))
                    .setLore(Component.text("id: ").append(Component.text(key).color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC)))
                    .setCompareKey(key)
                    .addDescription("input", 5,
                            InventoryMenu.INPUT_LEFT_CLICK.append(Component.text("edit team")),
                            InventoryMenu.INPUT_RIGHT_CLICK.append(Component.text("remove team"))));
        }
        addItemsToSelect(items);
    }

    public BasicMenu createTeamEditor(@NotNull String teamKey) {
        return new TeamEdit(getMenuBoard(), teamData.getTeam(teamKey, DEFAULT_NEW_TEAM), editedTemplate -> teamData.addTeam(teamKey, editedTemplate));
    }

    @Override
    public void onOptionClickedDelegate(InventoryClickEvent event, ItemTemplate clickedOption, PlayerHandle player) {
        String key = clickedOption.getCompareKey();
        if (event.getClick() == ClickType.RIGHT) {
            teamData.removeTeam(key);
            updateDisplay();
        } else {
            createTeamEditor(key).open(player);
        }
    }

    @Override
    public void beforeOpening(PlayerHandle player) {
        updateDisplay();
        super.beforeOpening(player);
    }

    static class TeamEdit extends BasicMenu
    {
        private final Consumer<TeamData.TeamTemplate> finishedCallback;
        private TeamData.TeamTemplate templateToEdit;

        public TeamEdit(MenuBoard manager, TeamData.TeamTemplate teamToEdit, Consumer<TeamData.TeamTemplate> callback) {
            super(manager, Component.text("Edit team"), 3);
            this.templateToEdit = teamToEdit;
            this.finishedCallback = callback;

            addAction(getTeamNameAction());

            // Add action to change the team's color.
            ItemTemplate teamColorItem = new ItemTemplate(4, 1, ItemTypePaper.of(Material.LEATHER_CHESTPLATE), Component.text("Color").color(templateToEdit.color()).decorate(TextDecoration.BOLD))
                    .setLeatherColor(templateToEdit.color());

            // TODO: maybe find a less cursed way to fix this?
            addAction(teamColorItem, args -> {
                new ColorPickerMenu(getMenuBoard(), Component.text("Pick team color"), (result) -> {
                    // Update template
                    templateToEdit = new TeamData.TeamTemplate(templateToEdit.stringName(), result);

                    // Update menu item
                    teamColorItem.setLeatherColor(templateToEdit.color())
                            .setName(Component.text("Color").color(templateToEdit.color()).decorate(TextDecoration.BOLD));
                    this.addItem(teamColorItem);
                }).open(args.player());
            });

            addCloseAction(new ItemTemplate(6, 1, ItemTypePaper.of(Material.BARRIER),
                    BingoMessage.MENU_EXIT.asPhrase().color(NamedTextColor.RED).decorate(TextDecoration.BOLD)));
        }

        private @NotNull MenuAction getTeamNameAction() {
            ItemTemplate teamNameItem = new ItemTemplate(2, 1, ItemTypePaper.of(Material.WRITABLE_BOOK),
                    templateToEdit.nameComponent(),
                    Component.text("Supports minimessage formatting").color(NamedTextColor.AQUA).decorate(TextDecoration.ITALIC));

            MenuAction action = new NameEditAction(Component.text("Edit team name"), getMenuBoard(), templateToEdit.stringName(), (value, item) -> {
                templateToEdit = new TeamData.TeamTemplate(value, templateToEdit.color());
                //TODO: find a way to do addItem(teamNameItem); automatically??
                addItem(item);
            });
            action.setItem(teamNameItem);
            return action;
        }

        @Override
        public void beforeClosing(PlayerHandle player) {
            super.beforeClosing(player);
            finishedCallback.accept(templateToEdit);
        }
    }
}
