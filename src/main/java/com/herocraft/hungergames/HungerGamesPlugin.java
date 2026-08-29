package com.herocraft.hungergames;

import com.herocraft.hungergames.arena.ArenaManager;
import com.herocraft.hungergames.command.HGAdminCommand;
import com.herocraft.hungergames.command.HGCommand;
import com.herocraft.hungergames.gui.ArenaGUI;
import com.herocraft.hungergames.gui.ArenaGUIListener;
import com.herocraft.hungergames.kit.KitManager;
import com.herocraft.hungergames.kit.KitSelectorGUI;
import com.herocraft.hungergames.listener.CombatListener;
import com.herocraft.hungergames.listener.PlayerListener;
import com.herocraft.hungergames.listener.SpectatorListener;
import org.bukkit.plugin.java.JavaPlugin;

public class HungerGamesPlugin extends JavaPlugin {

    private KitManager kitManager;
    private KitSelectorGUI kitSelectorGUI;
    private ArenaManager arenaManager;
    private ArenaGUI arenaGUI;

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
        this.arenaGUI = new ArenaGUI(this);

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new SpectatorListener(this), this);
        getServer().getPluginManager().registerEvents(new ArenaGUIListener(this, arenaGUI), this);

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

    public ArenaGUI getArenaGUI() {
        return arenaGUI;
    }
}
