package com.herocraft.hungergames;

import com.herocraft.hungergames.arena.ArenaManager;
import com.herocraft.hungergames.command.HGAdminCommand;
import com.herocraft.hungergames.command.HGCommand;
import com.herocraft.hungergames.kit.KitManager;
import com.herocraft.hungergames.kit.KitSelectorGUI;
import com.herocraft.hungergames.listener.CombatListener;
import com.herocraft.hungergames.listener.PlayerListener;
import org.bukkit.plugin.java.JavaPlugin;

public class HungerGamesPlugin extends JavaPlugin {

    private KitManager kitManager;
    private KitSelectorGUI kitSelectorGUI;
    private ArenaManager arenaManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        String kitsFile = getConfig().getString("storage.kits-file", "kits.yml");
        this.kitManager = new KitManager(this, kitsFile);
        this.kitSelectorGUI = new KitSelectorGUI(kitManager);
        this.arenaManager = new ArenaManager(this);

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);

        getCommand("hg").setExecutor(new HGCommand(this));
        getCommand("hgadmin").setExecutor(new HGAdminCommand(this));

        getLogger().info("HungerGames activé. Monde: " + getConfig().getString("world", "world"));
    }

    @Override
    public void onDisable() {
        getLogger().info("HungerGames désactivé.");
    }

    public KitManager getKitManager() {
        return kitManager;
    }

    public KitSelectorGUI getKitSelectorGUI() {
        return kitSelectorGUI;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }
}
