package com.herocraft.hungergames.command;

import com.herocraft.hungergames.HungerGamesPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class HGCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("join", "leave", "kit", "arenas", "gui", "unspectate", "top");

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
            player.sendMessage(Component.text("/hg join [nom] | leave | kit | arenas | top | unspectate", NamedTextColor.YELLOW));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "join" -> {
                if (args.length >= 2) {
                    plugin.getArenaManager().joinPlayerByName(player, args[1]);
                } else {
                    plugin.getArenaManager().joinPlayer(player);
                }
            }
            case "leave" -> {
                if (plugin.getArenaManager().getArenaOf(player).isPresent()) {
                    plugin.getArenaManager().leavePlayer(player);
                } else if (plugin.getArenaManager().findSpectatorArenaOf(player).isPresent()) {
                    plugin.getArenaManager().unspectate(player);
                } else {
                    player.sendMessage(Component.text("Tu n'es dans aucune partie.", NamedTextColor.RED));
                }
            }
            case "arenas", "gui" -> plugin.getArenaGUI().open(player);
            case "top", "scores" -> plugin.getLeaderboardGUI().open(player);
            case "unspectate" -> plugin.getArenaManager().unspectate(player);
            case "kit" -> {
                if (plugin.getArenaManager().getArenaOf(player).isEmpty()) {
                    player.sendMessage(Component.text("Rejoins d'abord une partie avec /hg join.", NamedTextColor.RED));
                    return true;
                }
                player.openInventory(plugin.getKitSelectorGUI().build(player));
            }
            default -> player.sendMessage(Component.text("/hg join [nom] | leave | kit | arenas | top | unspectate", NamedTextColor.YELLOW));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterStartsWith(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
            List<String> names = new ArrayList<>();
            for (var arena : plugin.getArenaManager().getNamedArenasOrdered()) {
                names.add(arena.getName());
            }
            return filterStartsWith(names, args[1]);
        }
        return List.of();
    }

    static List<String> filterStartsWith(List<String> options, String partial) {
        String lower = partial.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }
}
