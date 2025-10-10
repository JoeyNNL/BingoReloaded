package io.github.steaf23.bingoreloaded.util;

import io.github.steaf23.bingoreloaded.BingoReloaded;
import io.github.steaf23.bingoreloaded.data.BingoStatData;
import io.github.steaf23.bingoreloaded.data.BingoStatType;
import io.github.steaf23.bingoreloaded.data.config.BingoConfigurationData;
import io.github.steaf23.bingoreloaded.data.config.BingoOptions;
import io.github.steaf23.bingoreloaded.lib.api.PlatformResolver;
import io.github.steaf23.bingoreloaded.lib.data.core.DataAccessor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.*;

public class DiscordWebhookUtil {
    
    // Cache om UUID naar naam lookups te bewaren
    private static final Map<UUID, String> uuidNameCache = new HashMap<>();
    
    
    public static boolean sendStatsToDiscord(BingoConfigurationData config) {
        // Check eerst of bot token geconfigureerd is (voorkeur boven webhook)
        String botToken = config.getOptionValue(BingoOptions.DISCORD_BOT_TOKEN);
        String channelId = config.getOptionValue(BingoOptions.DISCORD_CHANNEL_ID);
        
        if (!botToken.isEmpty() && !channelId.isEmpty()) {
            // Gebruik bot mode
            Bukkit.getLogger().info("Gebruik Discord bot voor stats verzending");
            return sendStatsViaBot(botToken, channelId);
        }
        
        // Fallback naar webhook
        String webhookUrl = config.getOptionValue(BingoOptions.DISCORD_WEBHOOK_URL);
        if (webhookUrl.isEmpty() || webhookUrl.contains("YOUR_WEBHOOK")) {
            Bukkit.getLogger().info("Discord webhook/bot niet geconfigureerd, skip stats verzending");
            return false;
        }
        
        // Verwijder oude stats berichten (beperkte functionaliteit met webhook)
        deleteOldStatsMessages(webhookUrl);
        
        String stats = generateStatsString();
        String messageId = sendDiscordWebhook(webhookUrl, stats);
        
        return messageId != null;
    }
    
    /**
     * Verstuurt stats via Discord bot (heeft meer rechten dan webhook)
     */
    private static boolean sendStatsViaBot(String botToken, String channelId) {
        try {
            // Verwijder eerst oude stats berichten
            deleteOldBotMessages(botToken, channelId);
            
            // Genereer stats string
            String stats = generateStatsString();
            
            // Verstuur nieuw bericht via bot API
            String url = "https://discord.com/api/v10/channels/" + channelId + "/messages";
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bot " + botToken);
            conn.setDoOutput(true);
            
            // JSON body met content
            String jsonBody = "{\"content\": " + escapeJsonString(stats) + "}";
            
            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes("UTF-8"));
            }
            
            int responseCode = conn.getResponseCode();
            if (responseCode == 200 || responseCode == 201) {
                Bukkit.getLogger().info("Stats succesvol verstuurd via Discord bot");
                return true;
            } else {
                // Lees error response
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getErrorStream())
                );
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                Bukkit.getLogger().warning("Discord bot API error (" + responseCode + "): " + response);
                return false;
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("Fout bij versturen stats via Discord bot: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Verwijdert oude stats berichten die door de bot zijn verstuurd
     */
    private static void deleteOldBotMessages(String botToken, String channelId) {
        try {
            // Haal bot user ID op
            String botUserId = getBotUserId(botToken);
            if (botUserId == null) {
                Bukkit.getLogger().warning("Kan bot user ID niet ophalen, skip oude berichten verwijderen");
                return;
            }
            
            // Haal laatste 100 berichten op
            String url = "https://discord.com/api/v10/channels/" + channelId + "/messages?limit=100";
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bot " + botToken);
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                Bukkit.getLogger().warning("Kan berichten niet ophalen (code " + responseCode + ")");
                return;
            }
            
            // Parse JSON response
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(conn.getInputStream())
            );
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            String jsonResponse = response.toString();
            
            // Vind berichten van deze bot die stats bevatten
            // Discord message format: [{"type":0,"content":"...","author":{"id":"...","bot":true}},...]
            List<String> messageIdsToDelete = new ArrayList<>();
            
            // Split de JSON array op berichten
            String[] parts = jsonResponse.split("\\},\\{\"type\":");
            
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                
                // Fix de JSON structure (add back de { en } die gesplit zijn)
                if (i > 0) part = "{\"type\":" + part;
                if (i < parts.length - 1) part = part + "}";
                
                // Verwijder leading [ en trailing ]
                part = part.replace("[", "").replace("]", "");
                
                if (part.trim().isEmpty()) continue;
                
                // Extract author ID uit het author object
                int authorStart = part.indexOf("\"author\":{");
                if (authorStart == -1) continue;
                
                int authorIdStart = part.indexOf("\"id\":\"", authorStart);
                if (authorIdStart == -1) continue;
                
                authorIdStart += 6; // Skip "id":"
                int authorIdEnd = part.indexOf("\"", authorIdStart);
                String authorId = part.substring(authorIdStart, authorIdEnd);
                
                // Check of dit een bot is en onze bot
                boolean isBot = part.contains("\"bot\":true");
                
                if (isBot && authorId.equals(botUserId)) {
                    // Extract content
                    int contentStart = part.indexOf("\"content\":\"");
                    if (contentStart != -1) {
                        contentStart += 11; // Skip "content":"
                        int contentEnd = part.indexOf("\"", contentStart);
                        // Handle escaped quotes in content
                        while (contentEnd > 0 && part.charAt(contentEnd - 1) == '\\') {
                            contentEnd = part.indexOf("\"", contentEnd + 1);
                        }
                        
                        String content = part.substring(contentStart, contentEnd);
                        
                        // Unescape unicode (bijv. \ud83c\udfc6 is 🏆)
                        content = unescapeUnicode(content);
                        
                        // Check op stats markers
                        boolean isStatsMessage = content.contains("🏆") || content.contains("💀") || 
                                                 content.contains("✅") || content.contains("🔥") || content.contains("🪄") ||
                                                 content.contains("Top 10") || content.contains("Statistieken") ||
                                                 content.contains("Wins") || content.contains("Losses");
                        
                        if (isStatsMessage) {
                            // Extract message ID (het id veld VOOR het author object)
                            int msgIdStart = part.indexOf("\"id\":\"");
                            if (msgIdStart != -1 && msgIdStart < authorStart) {
                                msgIdStart += 6;
                                int msgIdEnd = part.indexOf("\"", msgIdStart);
                                String messageId = part.substring(msgIdStart, msgIdEnd);
                                messageIdsToDelete.add(messageId);
                            }
                        }
                    }
                }
            }
            
            // Verwijder oude stats berichten
            for (String messageId : messageIdsToDelete) {
                try {
                    String deleteUrl = "https://discord.com/api/v10/channels/" + channelId + "/messages/" + messageId;
                    java.net.HttpURLConnection deleteConn = (java.net.HttpURLConnection) new java.net.URL(deleteUrl).openConnection();
                    deleteConn.setRequestMethod("DELETE");
                    deleteConn.setRequestProperty("Authorization", "Bot " + botToken);
                    
                    int deleteCode = deleteConn.getResponseCode();
                    if (deleteCode == 204) {
                        Bukkit.getLogger().info("Oud stats bericht verwijderd");
                    } else {
                        Bukkit.getLogger().warning("Kon bericht niet verwijderen (code " + deleteCode + ")");
                    }
                    
                    // Rate limiting: max 5 DELETE per seconde
                    Thread.sleep(200);
                } catch (Exception e) {
                    Bukkit.getLogger().warning("Fout bij verwijderen bericht: " + e.getMessage());
                }
            }
            
            if (messageIdsToDelete.size() > 0) {
                Bukkit.getLogger().info(messageIdsToDelete.size() + " oude stats berichten verwijderd");
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("Fout bij verwijderen oude bot berichten: " + e.getMessage());
        }
    }
    
    /**
     * Haalt de bot user ID op via Discord API
     */
    private static String getBotUserId(String botToken) {
        try {
            String url = "https://discord.com/api/v10/users/@me";
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bot " + botToken);
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                return null;
            }
            
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(conn.getInputStream())
            );
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            String jsonResponse = response.toString();
            
            // Parse bot ID uit JSON
            int idIndex = jsonResponse.indexOf("\"id\":\"");
            if (idIndex != -1) {
                int idEnd = jsonResponse.indexOf("\"", idIndex + 6);
                return jsonResponse.substring(idIndex + 6, idEnd);
            }
            
            return null;
        } catch (Exception e) {
            Bukkit.getLogger().warning("Fout bij ophalen bot user ID: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Escaped een string voor gebruik in JSON
     */
    private static String escapeJsonString(String input) {
        if (input == null) return "null";
        
        StringBuilder sb = new StringBuilder();
        sb.append("\"");
        
        for (char c : input.toCharArray()) {
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(c);
            }
        }
        
        sb.append("\"");
        return sb.toString();
    }
    
    /**
     * Unescape Unicode sequences zoals backslash-u-d-8-3-c naar emoji
     */
    private static String unescapeUnicode(String input) {
        if (input == null) return null;
        
        StringBuilder sb = new StringBuilder();
        int i = 0;
        
        while (i < input.length()) {
            if (i < input.length() - 5 && input.charAt(i) == '\\' && input.charAt(i + 1) == 'u') {
                // Parse unicode escape sequence
                try {
                    String hex = input.substring(i + 2, i + 6);
                    int code = Integer.parseInt(hex, 16);
                    sb.append((char) code);
                    i += 6;
                } catch (Exception e) {
                    sb.append(input.charAt(i));
                    i++;
                }
            } else {
                sb.append(input.charAt(i));
                i++;
            }
        }
        
        return sb.toString();
    }
    
    private static String generateStatsString() {
        BingoStatData statsData = new BingoStatData(PlatformResolver.get());
        Map<String, int[]> allStats = new HashMap<>();
        
        // Haal alle spelers met stats op
        Set<UUID> playerUUIDs = new HashSet<>();
        
        // Voeg alle offline players toe
        for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
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
            OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
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
            Bukkit.getLogger().info("Verzenden Discord stats naar webhook");
            java.net.URI uri = java.net.URI.create(webhookUrl);
            java.net.HttpURLConnection con = (java.net.HttpURLConnection) uri.toURL().openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("User-Agent", "BingoReloaded/3.3.0");
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
                            Bukkit.getLogger().info("Discord stats verzonden, message ID: " + messageId);
                            return messageId;
                        }
                    }
                }
                
                Bukkit.getLogger().info("Discord stats succesvol verzonden!");
                return "unknown";
            } else {
                Bukkit.getLogger().warning("Discord webhook fout - Response code: " + responseCode);
                return null;
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("Fout bij verzenden Discord webhook: " + e.getMessage());
            return null;
        }
    }
    
    private static void deleteOldStatsMessages(String webhookUrl) {
        // Discord webhooks kunnen geen berichten ophalen, dus we gebruiken een andere strategie:
        // We sturen een "vervang" bericht door een waarschuwing te geven dat oude stats vervangen worden
        Bukkit.getLogger().info("⚠️  Oude Discord stats berichten kunnen niet automatisch worden verwijderd");
        Bukkit.getLogger().info("💡 Tip: Verwijder handmatig oude stats berichten in Discord voor de beste ervaring");
        
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
            con.setRequestProperty("User-Agent", "BingoReloaded/3.3.0");
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
            con.setRequestProperty("User-Agent", "BingoReloaded/3.3.0");
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
            con.setRequestProperty("User-Agent", "BingoReloaded/3.3.0");
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
