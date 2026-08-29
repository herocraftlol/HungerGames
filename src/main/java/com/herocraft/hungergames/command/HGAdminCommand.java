package com.herocraft.hungergames.command;

import com.herocraft.hungergames.HungerGamesPlugin;
import com.herocraft.hungergames.kit.Kit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class HGAdminCommand implements CommandExecutor {

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
            case "list" -> {
                sender.sendMessage(Component.text("Arènes actives : " + plugin.getArenaManager().getArenas().size(), NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("Zones utilisées au total : " + plugin.getArenaManager().getZoneAllocator().getUsedCount(), NamedTextColor.YELLOW));
            }
            default -> sendHelp(sender);
        }
        return true;
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
        sender.sendMessage(Component.text("/hgadmin kit create <id> <nom>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/hgadmin kit delete <id>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/hgadmin kit additem <id>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/hgadmin kit seticon <id>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/hgadmin kit list", NamedTextColor.YELLOW));
    }
}
