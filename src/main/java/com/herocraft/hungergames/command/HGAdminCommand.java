package com.herocraft.hungergames.command;

import com.herocraft.hungergames.HungerGamesPlugin;
import com.herocraft.hungergames.arena.Arena;
import com.herocraft.hungergames.hub.HubBuilder;
import com.herocraft.hungergames.kit.Kit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class HGAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> TOP_SUBCOMMANDS = List.of("reload", "list", "hub", "zone", "kit");
    private static final List<String> ZONE_SUBCOMMANDS = List.of("create", "delete", "rename", "list", "info", "tp", "forcestart");
    private static final List<String> ZONE_NAME_ARG_SUBCOMMANDS = List.of("delete", "remove", "cancel", "rename", "info", "tp", "forcestart");
    private static final List<String> HUB_SUBCOMMANDS = List.of("build", "delete");
    private static final List<String> KIT_SUBCOMMANDS = List.of("create", "delete", "additem", "seticon", "list");
    private static final List<String> KIT_ID_ARG_SUBCOMMANDS = List.of("delete", "additem", "seticon");

    private final HungerGamesPlugin plugin;

    public HGAdminCommand(HungerGamesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadConfig();
                plugin.getKitManager().load();
                sender.sendMessage(Component.text("Configuration et kits rechargés.", NamedTextColor.GREEN));
            }
            case "kit" -> handleKit(sender, args);
            case "zone" -> handleZone(sender, args);
            case "hub" -> handleHub(sender, args);
            case "list" -> {
                sender.sendMessage(Component.text("Zones actives : " + plugin.getArenaManager().getArenas().size(), NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("Chaque zone vit dans son propre monde Bukkit dédié (voir /hgadmin zone info <nom>).", NamedTextColor.GRAY));
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return HGCommand.filterStartsWith(TOP_SUBCOMMANDS, args[0]);
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "zone" -> HGCommand.filterStartsWith(ZONE_SUBCOMMANDS, args[1]);
                case "hub" -> HGCommand.filterStartsWith(HUB_SUBCOMMANDS, args[1]);
                case "kit" -> HGCommand.filterStartsWith(KIT_SUBCOMMANDS, args[1]);
                default -> List.of();
            };
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("zone") && ZONE_NAME_ARG_SUBCOMMANDS.contains(args[1].toLowerCase())) {
                List<String> names = new ArrayList<>();
                for (Arena arena : plugin.getArenaManager().getNamedArenasOrdered()) {
                    names.add(arena.getName());
                }
                return HGCommand.filterStartsWith(names, args[2]);
            }
            if (args[0].equalsIgnoreCase("kit") && KIT_ID_ARG_SUBCOMMANDS.contains(args[1].toLowerCase())) {
                List<String> ids = new ArrayList<>(plugin.getKitManager().getAll().keySet());
                return HGCommand.filterStartsWith(ids, args[2]);
            }
        }
        return List.of();
    }

    private void handleZone(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("/hgadmin zone <create|delete|rename|list|info|tp|forcestart> ...", NamedTextColor.YELLOW));
            return;
        }
        switch (args[1].toLowerCase()) {
            case "create" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /hgadmin zone create <nom>", NamedTextColor.RED));
                    return;
                }
                String name = args[2];
                if (!plugin.getArenaManager().isValidName(name)) {
                    sender.sendMessage(Component.text("Nom invalide (lettres/chiffres/_/- uniquement, 32 caractères max).", NamedTextColor.RED));
                    return;
                }
                plugin.getArenaManager().createNamedZone(name).ifPresentOrElse(arena -> {
                    sender.sendMessage(Component.text("Zone '" + name + "' créée sur son propre monde dédié ('" +
                            arena.getWorld().getName() + "'), taille " + arena.getZone().size() +
                            ". Préchargement en cours...", NamedTextColor.GREEN));
                }, () -> sender.sendMessage(Component.text("Impossible de créer la zone (nom déjà pris ou invalide).", NamedTextColor.RED)));
            }
            case "delete", "remove", "cancel" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /hgadmin zone delete <nom>", NamedTextColor.RED));
                    return;
                }
                boolean removed = plugin.getArenaManager().deleteZone(args[2]);
                sender.sendMessage(removed
                        ? Component.text("Zone '" + args[2] + "' supprimée (la partie en cours, s'il y en avait une, a été annulée).", NamedTextColor.GREEN)
                        : Component.text("Zone introuvable.", NamedTextColor.RED));
            }
            case "rename" -> {
                if (args.length < 4) {
                    sender.sendMessage(Component.text("Usage: /hgadmin zone rename <ancien nom> <nouveau nom>", NamedTextColor.RED));
                    return;
                }
                boolean ok = plugin.getArenaManager().renameZone(args[2], args[3]);
                sender.sendMessage(ok
                        ? Component.text("Zone renommée en '" + args[3] + "'.", NamedTextColor.GREEN)
                        : Component.text("Impossible de renommer (zone introuvable, ou nouveau nom invalide/déjà pris).", NamedTextColor.RED));
            }
            case "forcestart" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /hgadmin zone forcestart <nom>", NamedTextColor.RED));
                    return;
                }
                plugin.getArenaManager().findByName(args[2]).ifPresentOrElse(arena -> {
                    boolean ok = arena.forceStart();
                    sender.sendMessage(ok
                            ? Component.text("Lancement forcé de la zone '" + args[2] + "'.", NamedTextColor.GREEN)
                            : Component.text("Impossible de forcer le lancement (zone vide, ou partie déjà commencée).", NamedTextColor.RED));
                }, () -> sender.sendMessage(Component.text("Zone introuvable.", NamedTextColor.RED)));
            }
            case "tp" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Cette sous-commande doit être exécutée par un joueur.");
                    return;
                }
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /hgadmin zone tp <nom>", NamedTextColor.RED));
                    return;
                }
                plugin.getArenaManager().findByName(args[2]).ifPresentOrElse(arena -> {
                    player.teleport(arena.getLobbyLocation());
                    player.sendMessage(Component.text("Téléporté au lobby de la zone '" + args[2] + "'.", NamedTextColor.GREEN));
                }, () -> sender.sendMessage(Component.text("Zone introuvable.", NamedTextColor.RED)));
            }
            case "info" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /hgadmin zone info <nom>", NamedTextColor.RED));
                    return;
                }
                plugin.getArenaManager().findByName(args[2]).ifPresentOrElse(arena -> {
                    sender.sendMessage(Component.text("=== Zone '" + arena.getName() + "' ===", NamedTextColor.GOLD));
                    sender.sendMessage(Component.text("État : " + arena.getState(), NamedTextColor.YELLOW));
                    sender.sendMessage(Component.text("Monde dédié : " + arena.getWorld().getName() +
                            " (taille " + arena.getZone().size() + ")", NamedTextColor.YELLOW));
                    sender.sendMessage(Component.text("Joueurs : " + arena.getPlayers().size() + "/" + arena.getMaxPlayers(), NamedTextColor.YELLOW));
                    sender.sendMessage(Component.text("Spectateurs : " + arena.getSpectatorCount(), NamedTextColor.YELLOW));
                }, () -> sender.sendMessage(Component.text("Zone introuvable.", NamedTextColor.RED)));
            }
            case "list" -> {
                var zones = plugin.getArenaManager().getNamedArenasOrdered();
                if (zones.isEmpty()) {
                    sender.sendMessage(Component.text("Aucune zone créée pour le moment.", NamedTextColor.GRAY));
                    return;
                }
                sender.sendMessage(Component.text("=== Zones (" + zones.size() + ") ===", NamedTextColor.GOLD));
                for (Arena arena : zones) {
                    sender.sendMessage(Component.text(" - " + arena.getName() + " : " + arena.getState() +
                            " (" + arena.getPlayers().size() + "/" + arena.getMaxPlayers() + " joueurs)", NamedTextColor.GRAY));
                }
            }
            default -> sender.sendMessage(Component.text("/hgadmin zone <create|delete|rename|list|info|tp|forcestart> ...", NamedTextColor.YELLOW));
        }
    }

    private void handleHub(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /hgadmin hub <build|delete>", NamedTextColor.YELLOW));
            return;
        }
        switch (args[1].toLowerCase()) {
            case "build" -> HubBuilder.build(plugin, sender);
            case "delete" -> HubBuilder.delete(plugin, sender);
            default -> sender.sendMessage(Component.text("Usage: /hgadmin hub <build|delete>", NamedTextColor.YELLOW));
        }
    }

    private void handleKit(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("/hgadmin kit <create|delete|additem|seticon|list> ...", NamedTextColor.YELLOW));
            return;
        }
        switch (args[1].toLowerCase()) {
            case "create" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /hgadmin kit create <id> <nom affiché...>", NamedTextColor.RED));
                    return;
                }
                String id = args[2];
                String displayName = args.length > 3 ? String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length)) : id;
                plugin.getKitManager().createKit(id, displayName);
                sender.sendMessage(Component.text("Kit '" + id + "' créé.", NamedTextColor.GREEN));
            }
            case "delete" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /hgadmin kit delete <id>", NamedTextColor.RED));
                    return;
                }
                boolean removed = plugin.getKitManager().deleteKit(args[2]);
                sender.sendMessage(removed
                        ? Component.text("Kit supprimé.", NamedTextColor.GREEN)
                        : Component.text("Kit introuvable.", NamedTextColor.RED));
            }
            case "additem" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Cette sous-commande doit être exécutée par un joueur (ajoute l'item en main).");
                    return;
                }
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /hgadmin kit additem <id> (avec l'item en main)", NamedTextColor.RED));
                    return;
                }
                ItemStack inHand = player.getInventory().getItemInMainHand();
                if (inHand == null || inHand.getType().isAir()) {
                    sender.sendMessage(Component.text("Tu dois tenir un item en main.", NamedTextColor.RED));
                    return;
                }
                plugin.getKitManager().get(args[2]).ifPresentOrElse(kit -> {
                    kit.addItem(inHand.clone());
                    plugin.getKitManager().save();
                    sender.sendMessage(Component.text("Item ajouté au kit '" + args[2] + "'.", NamedTextColor.GREEN));
                }, () -> sender.sendMessage(Component.text("Kit introuvable.", NamedTextColor.RED)));
            }
            case "seticon" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Cette sous-commande doit être exécutée par un joueur (utilise l'item en main).");
                    return;
                }
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /hgadmin kit seticon <id> (avec l'item en main)", NamedTextColor.RED));
                    return;
                }
                ItemStack inHand = player.getInventory().getItemInMainHand();
                plugin.getKitManager().get(args[2]).ifPresentOrElse(kit -> {
                    kit.setIcon(inHand.clone());
                    plugin.getKitManager().save();
                    sender.sendMessage(Component.text("Icône mise à jour pour '" + args[2] + "'.", NamedTextColor.GREEN));
                }, () -> sender.sendMessage(Component.text("Kit introuvable.", NamedTextColor.RED)));
            }
            case "list" -> {
                sender.sendMessage(Component.text("Kits disponibles :", NamedTextColor.YELLOW));
                for (Kit kit : plugin.getKitManager().getAll().values()) {
                    sender.sendMessage(Component.text(" - " + kit.getId() + " (" + kit.getItems().size() + " items)", NamedTextColor.GRAY));
                }
            }
            default -> sender.sendMessage(Component.text("/hgadmin kit <create|delete|additem|seticon|list> ...", NamedTextColor.YELLOW));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== HungerGames Admin ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/hgadmin reload", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/hgadmin list", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/hgadmin hub build", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/hgadmin hub delete", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/hgadmin zone create <nom>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/hgadmin zone delete <nom>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/hgadmin zone rename <ancien> <nouveau>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/hgadmin zone list", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/hgadmin zone info <nom>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/hgadmin zone tp <nom>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/hgadmin zone forcestart <nom>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/hgadmin kit create <id> <nom>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/hgadmin kit delete <id>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/hgadmin kit additem <id>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/hgadmin kit seticon <id>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/hgadmin kit list", NamedTextColor.YELLOW));
    }
}
