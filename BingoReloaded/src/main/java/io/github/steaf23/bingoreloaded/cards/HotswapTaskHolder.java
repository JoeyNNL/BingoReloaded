package io.github.steaf23.bingoreloaded.cards;

import io.github.steaf23.bingoreloaded.data.BingoTranslation;
import io.github.steaf23.bingoreloaded.tasks.BingoTask;
import io.github.steaf23.bingoreloaded.util.timer.GameTimer;
import io.github.steaf23.easymenulib.inventory.item.ItemTemplate;
import io.github.steaf23.easymenulib.util.ChatColorGradient;
import io.github.steaf23.easymenulib.util.ExtraMath;
import net.md_5.bungee.api.ChatColor;

import java.util.Set;

public class HotswapTaskHolder
{
    public BingoTask task;
    public int expirationTimeSeconds;
    public int recoveryTime;
    public int currentTime;
    public boolean recovering;

    private final boolean showExpirationAsDurability;

    private static final ChatColorGradient EXPIRATION_GRADIENT = new ChatColorGradient()
            .addColor(ChatColor.of("#ffd200"), 0.0f)
            .addColor(ChatColor.of("#e85e21"), 0.5f)
            .addColor(ChatColor.of("#750e0e"), 0.8f)
            .addColor(ChatColor.DARK_GRAY, 1.0f);

    public HotswapTaskHolder(BingoTask task, int expirationTimeMinutes, int recoverTime, boolean showExpirationAsDurability) {
        this.task = task;
        this.expirationTimeSeconds = expirationTimeMinutes;
        this.recoveryTime = recoverTime;
        this.currentTime = expirationTimeMinutes;
        this.recovering = false;
        this.showExpirationAsDurability = showExpirationAsDurability;
    }

    public ItemTemplate convertToItem() {
        ItemTemplate item = task.toItem();
        if (isRecovering()) {
            item.addDescription("time", 1, BingoTranslation.HOTSWAP_RECOVER.asComponent(Set.of(ChatColor.of("#5cb1ff")), GameTimer.getTimeAsComponent(currentTime)));
        }
        else {
            item.addDescription("time", 1, BingoTranslation.HOTSWAP_EXPIRE.asComponent(Set.of(getColorForExpirationTime()), GameTimer.getTimeAsComponent(currentTime)));
            if (showExpirationAsDurability) {
                item.setMaxDamage(expirationTimeSeconds);
                item.setDamage(currentTime);
            }
        }
        return item;
    }

    public void startRecovering() {
        recovering = true;
        currentTime = recoveryTime;
    }

    public boolean isRecovering() {
        return recovering;
    }

    private ChatColor getColorForExpirationTime() {
        return EXPIRATION_GRADIENT.sample(ExtraMath.map(currentTime, 0.0f, expirationTimeSeconds, 1.0f, 0.0f));
    }
}
