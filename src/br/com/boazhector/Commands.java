package br.com.boazhector;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class Commands implements CommandExecutor, TabCompleter {

    private final Main plugin = Main.getInstance();

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando só pode ser usado por jogadores.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            // Listar tags disponíveis
            sender.sendMessage("§e=== Tags Disponíveis ===");
            ConfigurationSection tagsSection = plugin.getConfig().getConfigurationSection("tags");
            if (tagsSection != null) {
                for (String tagName : tagsSection.getKeys(false)) {
                    String permission = plugin.getTagPermission(tagName);
                    String display = plugin.getTagDisplay(tagName);
                    boolean hasPermission = permission == null || player.hasPermission(permission);

                    String message = hasPermission ? "§a" : "§c";
                    message += tagName + " §7- " + display.replace("§", "&") + (hasPermission ? "" : " §7(Sem permissão)");
                    sender.sendMessage(message);
                }
            } else {
                sender.sendMessage("§cNenhuma tag configurada.");
            }

            // Mostrar a tag atual
            String currentTag = plugin.getPlayerTag(player.getUniqueId());
            if (currentTag != null) {
                sender.sendMessage("§eSua tag atual: §r" + plugin.getTagDisplay(currentTag) + " §e(" + currentTag + ")");
            } else {
                sender.sendMessage("§eVocê não possui uma tag selecionada.");
            }

            sender.sendMessage("§eUso: §f/tag <nome> §7- Seleciona uma tag");
            sender.sendMessage("§eUso: §f/tag remover §7- Remove sua tag atual");
            return true;
        }

        if (args[0].equalsIgnoreCase("remover")) {
            // Remover tag atual
            String currentTag = plugin.getPlayerTag(player.getUniqueId());
            if (currentTag == null) {
                sender.sendMessage("§cVocê não possui uma tag para remover.");
                return true;
            }

            plugin.setPlayerTag(player.getUniqueId(), null);
            Events events = new Events();
            events.updatePlayerTag(player);

            for (Player online : Bukkit.getOnlinePlayers()) {
                events.updateScoreboardTeam(online);
            }

            sender.sendMessage("§aSua tag foi removida com sucesso.");
            return true;
        }

        // Selecionar tag
        String tagName = args[0];
        if (!plugin.tagExists(tagName)) {
            sender.sendMessage("§cA tag '" + tagName + "' não existe.");
            return true;
        }

        // Verificar permissão
        String permission = plugin.getTagPermission(tagName);
        if (permission != null && !player.hasPermission(permission)) {
            sender.sendMessage("§cVocê não tem permissão para usar esta tag.");
            return true;
        }

        // Definir tag
        plugin.setPlayerTag(player.getUniqueId(), tagName);
        Events events = new Events();
        events.updatePlayerTag(player);

        for (Player online : Bukkit.getOnlinePlayers()) {
            events.updateScoreboardTeam(online);
        }

        sender.sendMessage("§aSua tag foi definida para: §r" + plugin.getTagDisplay(tagName));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("remover");

            if (sender instanceof Player) {
                Player player = (Player) sender;
                ConfigurationSection tagsSection = plugin.getConfig().getConfigurationSection("tags");
                if (tagsSection != null) {
                    for (String tagName : tagsSection.getKeys(false)) {
                        String permission = plugin.getTagPermission(tagName);
                        if (permission == null || player.hasPermission(permission)) {
                            if (tagName.toLowerCase().startsWith(args[0].toLowerCase())) {
                                completions.add(tagName);
                            }
                        }
                    }
                }
            }
        }

        return completions;
    }
}