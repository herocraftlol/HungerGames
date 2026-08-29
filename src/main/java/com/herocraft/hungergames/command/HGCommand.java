package com.herocraft.hungergames.command;

import com.herocraft.hungergames.HungerGamesPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HGCommand implements CommandExecutor {

    private final HungerGamesPlugin plugin;

    public HGCommand(HungerGamesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Cette commande est réservée aux joueurs.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("/hg join | leave | kit", NamedTextColor.YELLOW));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "join" -> plugin.getArenaManager().joinPlayer(player);
            case "leave" -> plugin.getArenaManager().leavePlayer(player);
            case "kit" -> {
                if (plugin.getArenaManager().getArenaOf(player).isEmpty()) {
                    player.sendMessage(Component.text("Rejoins d'abord une partie avec /hg join.", NamedTextColor.RED));
                    return true;
                }
                player.openInventory(plugin.getKitSelectorGUI().build(player));
            }
            default -> player.sendMessage(Component.text("/hg join | leave | kit", NamedTextColor.YELLOW));
        }
        return true;
    }
}
