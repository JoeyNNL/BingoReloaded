package io.github.steaf23.bingoreloaded.util;

import io.github.steaf23.bingoreloaded.BingoReloaded;
import io.github.steaf23.bingoreloaded.data.BingoStatData;
import io.github.steaf23.bingoreloaded.data.BingoStatType;
import io.github.steaf23.bingoreloaded.data.core.DataAccessor;

import java.util.*;

public class DiscordWebhookUtil {
    
    // Cache om UUID naar naam lookups te bewaren
    private static final Map<UUID, String> uuidNameCache = new HashMap<>();
    
    
    public static boolean sendStatsToDiscord() {
        String webhookUrl = BingoReloaded.getInstance().getConfig().getString("discordWebhookUrl", "");
        if (webhookUrl.isEmpty() || webhookUrl.contains("YOUR_WEBHOOK")) {
            BingoReloaded.getInstance().getLogger().info("Discord webhook niet geconfigureerd, skipte stats verzending");
            return false;
        }
        
        // Verwijder alle oude stats berichten van deze webhook
        deleteOldStatsMessages(webhookUrl);
        
        String stats = generateStatsString();
        String messageId = sendDiscordWebhook(webhookUrl, stats);
        
        if (messageId != null) {
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
            
            // Als geen naam bekend is, probeer deze op te halen via Mojang API
            if (name == null) {
                // Check eerst de cache
                if (uuidNameCache.containsKey(uuid)) {
                    name = uuidNameCache.get(uuid);
                } else {
                    // Alleen API call maken als de speler daadwerkelijk stats heeft
                    // om onnodige API calls te voorkomen
                    int[] tempStats = new int[5];
                    tempStats[0] = statsData.getPlayerStat(uuid, BingoStatType.WINS);
                    tempStats[1] = statsData.getPlayerStat(uuid, BingoStatType.LOSSES);
                    tempStats[2] = statsData.getPlayerStat(uuid, BingoStatType.TASKS);
                    tempStats[3] = statsData.getPlayerStat(uuid, BingoStatType.RECORD_TASKS);
                    tempStats[4] = statsData.getPlayerStat(uuid, BingoStatType.WAND_USES);
                    
                    boolean hasStats = false;
                    for (int stat : tempStats) {
                        if (stat > 0) {
                            hasStats = true;
                            break;
                        }
                    }
                    
                    if (hasStats) {
                        name = getPlayerNameFromUUID(uuid);
                        // Cache het resultaat (ook als null)
                        uuidNameCache.put(uuid, name);
                        
                        // Kleine vertraging om rate limiting te voorkomen
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    } else {
                        // Geen stats, skip deze UUID
                        continue;
                    }
                }
                
                if (name == null) {
                    name = "Unknown-" + uuid.toString().substring(0, 8);
                }
            }
            
            // Bereken stats (mogelijk al berekend hierboven voor naamloze spelers)
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
        stats.append("🏆 Bingo Reloaded - Top 10 Statistieken 🏆\n\n");
        String[] statNames = {"🥇 Wins", "💀 Losses", "✅ Tasks completed", "🎯 Tasks Completed Record", "🪄 Wand uses"};
        
        for (int i = 0; i < statNames.length; i++) {
            int idx = i;
            var top10 = allStats.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue()[idx], a.getValue()[idx]))
                .limit(10)
                .toList();
            
            stats.append("**").append(statNames[i]).append(":**\n");
            int position = 1;
            for (var entry : top10) {
                String medal = switch (position) {
                    case 1 -> "🥇";
                    case 2 -> "🥈"; 
                    case 3 -> "🥉";
                    case 4, 5, 6, 7, 8, 9, 10 -> "🏅";
                    default -> "🏅";
                };
                stats.append(medal).append(" ").append(entry.getKey()).append(": **").append(entry.getValue()[idx]).append("**\n");
                position++;
            }
            stats.append("\n");
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
    
    private static void deleteOldStatsMessages(String webhookUrl) {
        // Discord webhooks kunnen geen berichten ophalen, dus we gebruiken een andere strategie:
        // We sturen een "vervang" bericht door een waarschuwing te geven dat oude stats vervangen worden
        BingoReloaded.getInstance().getLogger().info("⚠️  Oude Discord stats berichten kunnen niet automatisch worden verwijderd");
        BingoReloaded.getInstance().getLogger().info("💡 Tip: Verwijder handmatig oude stats berichten in Discord voor de beste ervaring");
        
        // Alternatief: We kunnen een kort "clearing" bericht sturen en daarna meteen vervangen
        sendClearingMessage(webhookUrl);
    }
    
    private static void sendClearingMessage(String webhookUrl) {
        try {
            // Stuur een kort bericht dat we gaan updaten
            java.net.URI uri = java.net.URI.create(webhookUrl);
            java.net.HttpURLConnection con = (java.net.HttpURLConnection) uri.toURL().openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("User-Agent", "BingoReloaded/3.2.0");
            con.setDoOutput(true);
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            
            String json = "{\"content\": \"🔄 Bijwerken van Bingo statistieken...\"}";
            
            try (java.io.OutputStream os = con.getOutputStream()) {
                byte[] input = json.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            
            int responseCode = con.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                // Korte pause zodat gebruikers het "updating" bericht zien
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (Exception e) {
            // Geen probleem als dit faalt, gewoon doorgaan met de echte stats
        }
    }
    
    /**
     * Haalt de spelernaam op via de Mojang API voor een gegeven UUID
     * @param uuid De UUID van de speler
     * @return De spelernaam of null als deze niet gevonden kon worden
     */
    private static String getPlayerNameFromUUID(UUID uuid) {
        // Probeer eerst de standaard SessionServer API
        try {
            String apiUrl = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replace("-", "");
            java.net.URI uri = java.net.URI.create(apiUrl);
            java.net.HttpURLConnection con = (java.net.HttpURLConnection) uri.toURL().openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "BingoReloaded/3.2.0");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            
            int responseCode = con.getResponseCode();
            
            if (responseCode == 200) {
                try (java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(con.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    
                    String responseStr = response.toString();
                    
                    // Parse de naam uit de JSON response
                    if (responseStr.contains("\"name\"")) {
                        int nameStart = responseStr.indexOf("\"name\":\"") + 8;
                        int nameEnd = responseStr.indexOf("\"", nameStart);
                        if (nameStart > 7 && nameEnd > nameStart) {
                            String name = responseStr.substring(nameStart, nameEnd);
                            return name;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Stille failure, probeer fallback
        }
        
        // Probeer alternatieve API als fallback - gebruik de PlayerDB API
        try {
            String fallbackUrl = "https://playerdb.co/api/player/minecraft/" + uuid.toString();
            java.net.URI uri = java.net.URI.create(fallbackUrl);
            java.net.HttpURLConnection con = (java.net.HttpURLConnection) uri.toURL().openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "BingoReloaded/3.2.0");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            
            int responseCode = con.getResponseCode();
            
            if (responseCode == 200) {
                try (java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(con.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    
                    String responseStr = response.toString();
                    
                    // PlayerDB heeft een andere JSON structuur: {"data":{"player":{"username":"name"}}}
                    if (responseStr.contains("\"username\"")) {
                        int nameStart = responseStr.indexOf("\"username\":\"") + 12;
                        int nameEnd = responseStr.indexOf("\"", nameStart);
                        if (nameStart > 11 && nameEnd > nameStart) {
                            String name = responseStr.substring(nameStart, nameEnd);
                            return name;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Stille failure
        }
        
        return null;
    }

}