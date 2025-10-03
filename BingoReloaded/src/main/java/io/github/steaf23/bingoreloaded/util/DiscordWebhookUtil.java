package io.github.steaf23.bingoreloaded.util;

import io.github.steaf23.bingoreloaded.BingoReloaded;
import io.github.steaf23.bingoreloaded.data.BingoStatData;
import io.github.steaf23.bingoreloaded.data.BingoStatType;
import io.github.steaf23.bingoreloaded.data.core.DataAccessor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DiscordWebhookUtil {
    
    private static String lastMessageId = null;
    
    public static boolean sendStatsToDiscord() {
        String webhookUrl = BingoReloaded.getInstance().getConfig().getString("discordWebhookUrl", "");
        if (webhookUrl.isEmpty() || webhookUrl.contains("YOUR_WEBHOOK")) {
            BingoReloaded.getInstance().getLogger().info("Discord webhook niet geconfigureerd, skipte stats verzending");
            return false;
        }
        
        // Verwijder vorige bericht als het bestaat
        if (lastMessageId != null) {
            deleteDiscordMessage(webhookUrl, lastMessageId);
        }
        
        String stats = generateStatsString();
        String messageId = sendDiscordWebhook(webhookUrl, stats);
        
        if (messageId != null) {
            lastMessageId = messageId;
            return true;
        }
        
        return false;
    }
    
    private static String generateStatsString() {
        BingoStatData statsData = new BingoStatData();
        Map<String, int[]> allStats = new HashMap<>();
        
        // Haal alle spelers met stats op
        Set<UUID> playerUUIDs = new HashSet<>();
        
        // Voeg alle offline players toe
        for (org.bukkit.OfflinePlayer p : org.bukkit.Bukkit.getOfflinePlayers()) {
            playerUUIDs.add(p.getUniqueId());
        }
        
        // Voeg ook alle UUIDs uit de stats database toe
        DataAccessor statsDB = BingoReloaded.getDataAccessor("data/player_stats");
        for (String key : statsDB.getKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                playerUUIDs.add(uuid);
            } catch (IllegalArgumentException e) {
                // Negeer ongeldige UUID keys
            }
        }
        
        for (UUID uuid : playerUUIDs) {
            org.bukkit.OfflinePlayer p = org.bukkit.Bukkit.getOfflinePlayer(uuid);
            String name = p.getName();
            if (name == null) {
                name = "Unknown-" + uuid.toString().substring(0, 8);
            }
            
            int[] stats = new int[5];
            stats[0] = statsData.getPlayerStat(uuid, BingoStatType.WINS);
            stats[1] = statsData.getPlayerStat(uuid, BingoStatType.LOSSES);
            stats[2] = statsData.getPlayerStat(uuid, BingoStatType.TASKS);
            stats[3] = statsData.getPlayerStat(uuid, BingoStatType.RECORD_TASKS);
            stats[4] = statsData.getPlayerStat(uuid, BingoStatType.WAND_USES);
            
            // Voeg alleen spelers toe die daadwerkelijk stats hebben
            boolean hasAnyStats = false;
            for (int stat : stats) {
                if (stat > 0) {
                    hasAnyStats = true;
                    break;
                }
            }
            
            if (hasAnyStats) {
                allStats.put(name, stats);
            }
        }
        
        StringBuilder stats = new StringBuilder();
        stats.append("🏆 Bingo Reloaded - Top 5 Statistieken 🏆\\n\\n");
        String[] statNames = {"🥇 Wins", "💀 Losses", "✅ Tasks completed", "🎯 Tasks Completed Record", "🪄 Wand uses"};
        
        for (int i = 0; i < statNames.length; i++) {
            int idx = i;
            var top5 = allStats.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue()[idx], a.getValue()[idx]))
                .limit(5)
                .toList();
            
            stats.append("**").append(statNames[i]).append(":**\\n");
            int position = 1;
            for (var entry : top5) {
                String medal = switch (position) {
                    case 1 -> "🥇";
                    case 2 -> "🥈"; 
                    case 3 -> "🥉";
                    default -> "🏅";
                };
                stats.append(medal).append(" ").append(entry.getKey()).append(": **").append(entry.getValue()[idx]).append("**\\n");
                position++;
            }
            stats.append("\\n");
        }
        
        return stats.toString();
    }
    
    private static String sendDiscordWebhook(String webhookUrl, String content) {
        try {
            BingoReloaded.getInstance().getLogger().info("Verzenden Discord stats naar webhook");
            java.net.URI uri = java.net.URI.create(webhookUrl);
            java.net.HttpURLConnection con = (java.net.HttpURLConnection) uri.toURL().openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("User-Agent", "BingoReloaded/3.2.0");
            con.setDoOutput(true);
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            
            // Escape content voor JSON
            String escapedContent = content
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
            
            String json = "{\"content\": \"" + escapedContent + "\"}";
            
            try (java.io.OutputStream os = con.getOutputStream()) {
                byte[] input = json.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            
            int responseCode = con.getResponseCode();
            
            if (responseCode >= 200 && responseCode < 300) {
                // Lees response om message ID te krijgen
                try (java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(con.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    
                    // Parse message ID uit response JSON
                    String responseStr = response.toString();
                    if (responseStr.contains("\"id\"")) {
                        int idStart = responseStr.indexOf("\"id\":\"") + 6;
                        int idEnd = responseStr.indexOf("\"", idStart);
                        if (idStart > 5 && idEnd > idStart) {
                            String messageId = responseStr.substring(idStart, idEnd);
                            BingoReloaded.getInstance().getLogger().info("Discord stats verzonden, message ID: " + messageId);
                            return messageId;
                        }
                    }
                }
                
                BingoReloaded.getInstance().getLogger().info("Discord stats succesvol verzonden!");
                return "unknown";
            } else {
                BingoReloaded.getInstance().getLogger().warning("Discord webhook fout - Response code: " + responseCode);
                return null;
            }
        } catch (Exception e) {
            BingoReloaded.getInstance().getLogger().severe("Fout bij verzenden Discord webhook: " + e.getMessage());
            return null;
        }
    }
    
    private static void deleteDiscordMessage(String webhookUrl, String messageId) {
        if (messageId.equals("unknown")) return;
        
        try {
            String deleteUrl = webhookUrl + "/messages/" + messageId;
            java.net.URI uri = java.net.URI.create(deleteUrl);
            java.net.HttpURLConnection con = (java.net.HttpURLConnection) uri.toURL().openConnection();
            con.setRequestMethod("DELETE");
            con.setRequestProperty("User-Agent", "BingoReloaded/3.2.0");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            
            int responseCode = con.getResponseCode();
            if (responseCode == 204 || responseCode == 404) {
                BingoReloaded.getInstance().getLogger().info("Oude Discord stats bericht verwijderd");
            } else {
                BingoReloaded.getInstance().getLogger().warning("Kon oude Discord bericht niet verwijderen - Response code: " + responseCode);
            }
        } catch (Exception e) {
            BingoReloaded.getInstance().getLogger().warning("Fout bij verwijderen oude Discord bericht: " + e.getMessage());
        }
    }
}