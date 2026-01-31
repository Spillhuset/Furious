package com.spillhuset.furious.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Bank;
import com.spillhuset.furious.utils.Guild;
import com.spillhuset.furious.utils.GuildRole;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;

public class ApiService {
    private final Furious plugin;
    private HttpServer server;
    private final Gson gson;

    public ApiService(Furious plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("api.enabled", false)) {
            return;
        }

        int port = plugin.getConfig().getInt("api.port", 8080);
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/guilds", new GuildsHandler());
            server.createContext("/banks", new BanksHandler());
            server.setExecutor(null); // creates a default executor
            server.start();
            plugin.getLogger().info("API Service started on port " + port);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not start API Service", e);
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            plugin.getLogger().info("API Service stopped.");
        }
    }

    private class GuildsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                return;
            }

            List<Map<String, Object>> guildList = new ArrayList<>();
            
            // Accessing GuildService should ideally be thread-safe or done on main thread if needed
            // However, GuildService seems to keep data in memory after load()
            for (String name : plugin.guildService.getAllGuildNames()) {
                Guild guild = plugin.guildService.getGuildByName(name);
                if (guild == null) continue;

                Map<String, Object> gMap = new LinkedHashMap<>();
                gMap.put("uuid", guild.getUuid().toString());
                gMap.put("name", guild.getName());
                gMap.put("type", guild.getType().name());
                gMap.put("owner", guild.getOwner() != null ? guild.getOwner().toString() : null);
                gMap.put("open", guild.isOpen());

                List<Map<String, String>> membersList = new ArrayList<>();
                for (Map.Entry<UUID, GuildRole> entry : guild.getMembers().entrySet()) {
                    Map<String, String> mInfo = new LinkedHashMap<>();
                    mInfo.put("uuid", entry.getKey().toString());
                    mInfo.put("name", Bukkit.getOfflinePlayer(entry.getKey()).getName());
                    mInfo.put("role", entry.getValue().name());
                    membersList.add(mInfo);
                }
                gMap.put("members", membersList);
                
                guildList.add(gMap);
            }

            String response = gson.toJson(guildList);
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private class BanksHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            List<Map<String, Object>> bankList = new ArrayList<>();
            for (Bank bank : plugin.banksService.getBanks()) {
                if (bank == null) continue;

                Map<String, Object> bMap = new LinkedHashMap<>();
                bMap.put("id", bank.getId().toString());
                bMap.put("name", bank.getName());
                bMap.put("interest", bank.getInterest());
                bMap.put("type", bank.getType().name());
                bMap.put("open", bank.isOpen());
                bMap.put("interestHistory", bank.getInterestHistory());

                bankList.add(bMap);
            }

            String response = gson.toJson(bankList);
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}
