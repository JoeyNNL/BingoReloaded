package io.github.steaf23.bingoreloaded.tasks.tracker;

import io.github.steaf23.bingoreloaded.lib.api.PlayerHandle;
import io.github.steaf23.bingoreloaded.event.BingoStatisticCompletedEvent;
import io.github.steaf23.bingoreloaded.player.BingoParticipant;
import io.github.steaf23.bingoreloaded.tasks.BingoStatistic;

public class StatisticProgress
{
    private final BingoStatistic statistic;
    private final BingoParticipant player;
    private int progressLeft;

    private int previousGlobalProgress;

    public StatisticProgress(BingoStatistic statistic, BingoParticipant player, int targetScore)
    {
        this.statistic = statistic;
        this.player = player;
        this.progressLeft = targetScore;
        if (statistic.getCategory() == BingoStatistic.StatisticCategory.TRAVEL)
        {
            progressLeft *= 1000;
        }

        this.previousGlobalProgress = 0;

//        setPlayerTotalScore(0);
    }

    public boolean done()
    {
        return progressLeft <= 0;
    }

    /**
     * Updates the progress for statistics that don't get updated with the default Increment event
     */
    public void updatePeriodicProgress()
    {
        if (statistic.getsUpdatedWithIncrementEvent())
            return;

        int newProgress = getParticipantTotalScore();
        setProgress(newProgress);
    }

    public void setProgress(int newProgress)
    {
        int progressDelta = newProgress - previousGlobalProgress;

        progressLeft -= Math.max(0, progressDelta);

        previousGlobalProgress = newProgress;

        if (done()) {
            var event = new BingoStatisticCompletedEvent(statistic, player);
            Bukkit.getPluginManager().callEvent(event);
        }
    }

    public int getParticipantTotalScore()
    {
        PlayerHandle gamePlayer = player.sessionPlayer().orElse(null);
        if (gamePlayer == null) {
            return 0;
        }

        int value;
        if (statistic.hasMaterialComponent())
        {
            value = gamePlayer.getStatistic(statistic.stat(), statistic.materialType());
        }
        else if (statistic.hasEntityComponent())
        {
            value = gamePlayer.getStatistic(statistic.stat(), statistic.entityType());
        }
        else
        {
            value = gamePlayer.getStatistic(statistic.stat());
        }
        return value;
    }

    public BingoStatistic getStatistic() {
        return statistic;
    }

    public int getProgressLeft() {
        return progressLeft;
    }

    public BingoParticipant getParticipant() {
        return player;
    }
}
