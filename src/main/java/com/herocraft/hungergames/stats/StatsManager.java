package com.herocraft.hungergames.stats;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Compte le nombre de victoires de chaque joueur, persisté dans
 * {@code plugins/HungerGames/stats.yml}, pour alimenter le tableau des scores
 * du lobby (voir {@code com.herocraft.hungergames.gui.LeaderboardGUI}).
 */
public class StatsManager {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Entry> stats = new LinkedHashMap<>();

    public StatsManager(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), fileName);
        load();
    }

    public synchronized void addWin(UUID playerId, String playerName) {
        Entry entry = stats.computeIfAbsent(playerId, id -> new Entry(playerName, 0));
        entry.name = playerName; // garde le pseudo à jour en cas de changement
        entry.wins++;
        save();
    }

    public synchronized int getWins(UUID playerId) {
        Entry entry = stats.get(playerId);
        return entry != null ? entry.wins : 0;
    }

    /** Classement des meilleurs joueurs (victoires décroissantes), limité à {@code limit}. */
    public synchronized List<Entry> getTop(int limit) {
        List<Entry> list = new ArrayList<>(stats.values());
        list.sort(Comparator.comparingInt((Entry e) -> e.wins).reversed());
        if (list.size() > limit) {
            return new ArrayList<>(list.subList(0, limit));
        }
        return list;
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("players");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                String name = section.getString(key + ".name", "?");
                int wins = section.getInt(key + ".wins", 0);
                stats.put(id, new Entry(name, wins));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Entry> e : stats.entrySet()) {
            String base = "players." + e.getKey();
            yaml.set(base + ".name", e.getValue().name);
            yaml.set(base + ".wins", e.getValue().wins);
        }
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible de sauvegarder stats.yml : " + e.getMessage());
        }
    }

    /** Une ligne du classement : pseudo (au moment de la dernière victoire) + nombre de victoires. */
    public static final class Entry {
        public String name;
        public int wins;

        public Entry(String name, int wins) {
            this.name = name;
            this.wins = wins;
        }
    }
}
