package io.github.steaf23.bingoreloaded.gameloop.vote;

import io.github.steaf23.bingoreloaded.cards.CardSize;
import io.github.steaf23.bingoreloaded.settings.BingoGamemode;
import io.github.steaf23.bingoreloaded.settings.PlayerKit;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Each player can cast a single vote for all categories, To keep track of this a VoteTicket will be made for every player that votes on something
public class VoteTicket
{
    public static final VoteCategory<BingoGamemode> CATEGORY_GAMEMODE = new GamemodeCategory();
    public static final VoteCategory<PlayerKit> CATEGORY_KIT = new KitCategory();
    public static final VoteCategory<String> CATEGORY_CARD = new CardCategory();
    public static final VoteCategory<CardSize> CATEGORY_CARDSIZE = new CardSizeCategory();

    private final Map<VoteCategory<?>, String> votes = new HashMap<>();

    public boolean isEmpty() {
        return votes.isEmpty();
    }

    public boolean addVote(VoteCategory<?> category, String value) {
        if (category.getValidValues().contains(value)) {
            votes.put(category, value);
            return true;
        }
        return false;
    }

    public @Nullable String getVote(VoteCategory<?> category) {
        return votes.get(category);
    }

    public boolean containsCategory(VoteCategory<?> category) {
        return votes.containsKey(category);
    }

    public String toString() {
        List<String> result = new ArrayList<>();
        for (VoteCategory<?> category : votes.keySet()) {
            String part = category.getConfigName() + ": " + votes.get(category);
            result.add(part);
        }
        return String.join(", ", result);
    }

    public static VoteTicket getVoteResult(Collection<VoteTicket> tickets) {
        Map<VoteCategory<?>, Map<String, Integer>> maps = new HashMap<>();

        for (VoteTicket ticket : tickets) {
            for (VoteCategory<?> category : ticket.votes.keySet()) {
                Map<String, Integer> categoryMap = maps.getOrDefault(category, new HashMap<>());
                String vote = ticket.votes.get(category);
                categoryMap.put(vote, categoryMap.getOrDefault(vote, 0) + 1);
                maps.put(category, categoryMap);
            }
        }

        VoteTicket outcome = new VoteTicket();
        for (VoteCategory<?> category : maps.keySet()) {
            outcome.addVote(category, getVoteWithHighestCount(maps.get(category)));
        }

        return outcome;
    }

    /**
     * @param values Map of options alongside how many times was voted for it
     * @return The option from the map that contains the highest amount of votes, or a random one if there are multiple with the same highest vote count.
     */
    private static String getVoteWithHighestCount(Map<String, Integer> values) {

        // List of options sorted by amount of votes for that option, highest to lowest.
        List<String> sortedCounts = new ArrayList<>(values.keySet().stream()
                .sorted(Comparator.comparingInt(a -> -values.get(a)))
                .toList());

        int recordCount = values.get(sortedCounts.getFirst());
        int currentCount;
        for (int i = sortedCounts.size() - 1; i >= 0; i--) {
            currentCount = values.get(sortedCounts.get(i));
            if (recordCount == currentCount) {
                // Since we are looping in reverse, we can break when we run into the same count as the record, all remaining options will have the record amount.
                break;
            }
            sortedCounts.remove(i);
        }

        return sortedCounts.get((int)(Math.random() * sortedCounts.size()));
    }
}

