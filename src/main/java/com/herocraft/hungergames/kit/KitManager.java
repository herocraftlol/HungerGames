package com.herocraft.hungergames.kit;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class KitManager {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, Kit> kits = new LinkedHashMap<>();

    public KitManager(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
        load();
    }

    public void load() {
        kits.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("kits");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            ConfigurationSection kitSection = section.getConfigurationSection(id);
            if (kitSection == null) continue;

            String displayName = kitSection.getString("display-name", id);
            Material iconMat = parseMaterial(kitSection.getString("icon", "STONE"));
            Kit kit = new Kit(id, displayName, new ItemStack(iconMat));

            for (String itemLine : kitSection.getStringList("items")) {
                ItemStack item = parseItemLine(itemLine);
                if (item != null) {
                    kit.addItem(item);
                }
            }
            kits.put(id.toLowerCase(), kit);
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Kit kit : kits.values()) {
            String base = "kits." + kit.getId();
            yaml.set(base + ".display-name", kit.getDisplayName());
            yaml.set(base + ".icon", kit.getIcon().getType().name());
            java.util.List<String> lines = new java.util.ArrayList<>();
            for (ItemStack item : kit.getItems()) {
                lines.add(item.getType().name() + ":" + item.getAmount());
            }
            yaml.set(base + ".items", lines);
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible de sauvegarder les kits : " + e.getMessage());
        }
    }

    public Optional<Kit> get(String id) {
        return Optional.ofNullable(kits.get(id.toLowerCase()));
    }

    public Map<String, Kit> getAll() {
        return kits;
    }

    public Kit createKit(String id, String displayName) {
        Kit kit = new Kit(id.toLowerCase(), displayName, new ItemStack(Material.STONE));
        kits.put(id.toLowerCase(), kit);
        save();
        return kit;
    }

    public boolean deleteKit(String id) {
        boolean removed = kits.remove(id.toLowerCase()) != null;
        if (removed) save();
        return removed;
    }

    private Material parseMaterial(String name) {
        Material mat = Material.matchMaterial(name);
        return mat != null ? mat : Material.STONE;
    }

    private ItemStack parseItemLine(String line) {
        String[] parts = line.split(":");
        Material mat = parseMaterial(parts[0]);
        int amount = 1;
        if (parts.length > 1) {
            try {
                amount = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {
            }
        }
        return new ItemStack(mat, Math.max(1, amount));
    }
}
