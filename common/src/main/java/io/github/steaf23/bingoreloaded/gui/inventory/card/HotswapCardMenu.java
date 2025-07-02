package io.github.steaf23.bingoreloaded.gui.inventory.card;

import io.github.steaf23.bingoreloaded.cards.hotswap.HotswapTaskHolder;

import java.util.List;

public interface HotswapCardMenu extends CardMenu
{
    void updateTaskHolders(List<HotswapTaskHolder> taskHolders);
}
