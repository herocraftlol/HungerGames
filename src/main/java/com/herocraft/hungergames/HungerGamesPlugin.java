package com.herocraft.hungergames;

import com.herocraft.hungergames.arena.ArenaManager;
import com.herocraft.hungergames.command.HGAdminCommand;
import com.herocraft.hungergames.command.HGCommand;
import com.herocraft.hungergames.gui.ArenaGUI;
import com.herocraft.hungergames.gui.ArenaGUIListener;
import com.herocraft.hungergames.gui.DeathSpectateGUI;
import com.herocraft.hungergames.gui.LeaderboardGUI;
import com.herocraft.hungergames.hub.HubNpcListener;
import com.herocraft.hungergames.kit.KitManager;
import com.herocraft.hungergames.kit.KitSelectorGUI;
import com.herocraft.hungergames.listener.CombatListener;
import com.herocraft.hungergames.listener.DeadChatListener;
import com.herocraft.hungergames.listener.DeathSpectateListener;
import com.herocraft.hungergames.listener.PlayerListener;
import com.herocraft.hungergames.listener.SpectatorListener;
import com.herocraft.hungergames.listener.WaitingRoomListener;
import com.herocraft.hungergames.stats.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class HungerGamesPlugin extends JavaPlugin {

    private KitManager kitManager;
    private KitSelectorGUI kitSelectorGUI;
    private ArenaManager arenaManager;
    private ArenaGUI arenaGUI;
    private StatsManager statsManager;
    private LeaderboardGUI leaderboardGUI;
    private DeathSpectateGUI deathSpectateGUI;

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
        this.statsManager = new StatsManager(this, getConfig().getString("storage.stats-file", "stats.yml"));
        this.leaderboardGUI = new LeaderboardGUI(this);
        this.deathSpectateGUI = new DeathSpectateGUI(this);

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new SpectatorListener(this), this);
        getServer().getPluginManager().registerEvents(new WaitingRoomListener(this), this);
        getServer().getPluginManager().registerEvents(new ArenaGUIListener(this, arenaGUI), this);
        getServer().getPluginManager().registerEvents(new HubNpcListener(this), this);
        getServer().getPluginManager().registerEvents(new DeathSpectateListener(this), this);
        getServer().getPluginManager().registerEvents(new DeadChatListener(this), this);

        // Rafraîchit le GUI des arènes toutes les secondes pour les joueurs qui l'ont ouvert.
        Bukkit.getScheduler().runTaskTimer(this, arenaGUI::refreshOpenViewers, 20L, 20L);

        HGCommand hgCommand = new HGCommand(this);
        getCommand("hg").setExecutor(hgCommand);
        getCommand("hg").setTabCompleter(hgCommand);

        HGAdminCommand hgAdminCommand = new HGAdminCommand(this);
        getCommand("hgadmin").setExecutor(hgAdminCommand);
        getCommand("hgadmin").setTabCompleter(hgAdminCommand);

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

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public LeaderboardGUI getLeaderboardGUI() {
        return leaderboardGUI;
    }

    public DeathSpectateGUI getDeathSpectateGUI() {
        return deathSpectateGUI;
    }
}
