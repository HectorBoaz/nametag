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

public class CommandsAdmin implements CommandExecutor, TabCompleter {

    private final Main plugin = Main.getInstance();

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("nametags.admin")) {
            sender.sendMessage("§cVocê não tem permissão para usar este comando.");
            return true;
        }

        if (args.length == 0) {
            // Exibir ajuda do comando
            sender.sendMessage("§e=== NameTags Admin ===");
            sender.sendMessage("§e/tagadmin criar <nome> <display> <prioridade> [permissao] §7- Cria uma nova tag");
            sender.sendMessage("§e/tagadmin editar <nome> display <texto> §7- Altera o texto de exibição da tag");
            sender.sendMessage("§e/tagadmin editar <nome> prioridade <número> §7- Altera a prioridade da tag");
            sender.sendMessage("§e/tagadmin editar <nome> permissao <permissao> §7- Altera a permissão da tag");
            sender.sendMessage("§e/tagadmin remover <nome> §7- Remove uma tag");
            sender.sendMessage("§e/tagadmin definir <jogador> <tag> §7- Define a tag de um jogador");
            sender.sendMessage("§e/tagadmin limpar <jogador> §7- Remove a tag de um jogador");
            sender.sendMessage("§e/tagadmin recarregar §7- Recarrega a configuração");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("criar") && args.length >= 4) {
            // Criar nova tag
            String tagName = args[1];
            String display = args[2];
            int priority;

            try {
                priority = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cA prioridade deve ser um número.");
                return true;
            }

            String permission = args.length > 4 ? args[4] : null;

            if (plugin.tagExists(tagName)) {
                sender.sendMessage("§cA tag '" + tagName + "' já existe.");
                return true;
            }

            plugin.getConfig().set("tags." + tagName + ".display", display);
            plugin.getConfig().set("tags." + tagName + ".prioridade", priority);
            if (permission != null) {
                plugin.getConfig().set("tags." + tagName + ".permissao", permission);
            }

            plugin.saveConfig();
            sender.sendMessage("§aTag '" + tagName + "' criada com sucesso.");

            // Atualizar todos os jogadores
            Events events = new Events();
            for (Player player : Bukkit.getOnlinePlayers()) {
                events.updateScoreboardTeam(player);
            }

            return true;
        }

        if (subCommand.equals("editar") && args.length >= 4) {
            // Editar tag existente
            String tagName = args[1];
            String property = args[2].toLowerCase();
            String value = args[3];

            if (!plugin.tagExists(tagName)) {
                sender.sendMessage("§cA tag '" + tagName + "' não existe.");
                return true;
            }

            switch (property) {
                case "display":
                    plugin.getConfig().set("tags." + tagName + ".display", value);
                    sender.sendMessage("§aDisplay da tag '" + tagName + "' alterado para: " + value);
                    break;

                case "prioridade":
                    try {
                        int priority = Integer.parseInt(value);
                        plugin.getConfig().set("tags." + tagName + ".prioridade", priority);
                        sender.sendMessage("§aPrioridade da tag '" + tagName + "' alterada para: " + priority);
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cA prioridade deve ser um número.");
                        return true;
                    }
                    break;

                case "permissao":
                    plugin.getConfig().set("tags." + tagName + ".permissao", value);
                    sender.sendMessage("§aPermissão da tag '" + tagName + "' alterada para: " + value);
                    break;

                default:
                    sender.sendMessage("§cPropriedade inválida. Use 'display', 'prioridade' ou 'permissao'.");
                    return true;
            }

            plugin.saveConfig();

            // Atualizar todos os jogadores
            Events events = new Events();
            for (Player player : Bukkit.getOnlinePlayers()) {
                events.updateScoreboardTeam(player);
            }

            return true;
        }

        if (subCommand.equals("remover") && args.length >= 2) {
            // Remover tag
            String tagName = args[1];

            if (!plugin.tagExists(tagName)) {
                sender.sendMessage("§cA tag '" + tagName + "' não existe.");
                return true;
            }

            // Remover tag de todos os jogadores que a possuem
            for (Player player : Bukkit.getOnlinePlayers()) {
                String playerTag = plugin.getPlayerTag(player.getUniqueId());
                if (tagName.equals(playerTag)) {
                    plugin.setPlayerTag(player.getUniqueId(), null);
                }
            }

            plugin.getConfig().set("tags." + tagName, null);
            plugin.saveConfig();
            sender.sendMessage("§aTag '" + tagName + "' removida com sucesso.");

            // Atualizar todos os jogadores
            Events events = new Events();
            for (Player player : Bukkit.getOnlinePlayers()) {
                events.updatePlayerTag(player);
                events.updateScoreboardTeam(player);
            }

            return true;
        }

        if (subCommand.equals("definir") && args.length >= 3) {
            // Definir tag de jogador
            String playerName = args[1];
            String tagName = args[2];

            Player target = Bukkit.getPlayer(playerName);
            if (target == null) {
                sender.sendMessage("§cJogador '" + playerName + "' não está online.");
                return true;
            }

            if (!plugin.tagExists(tagName)) {
                sender.sendMessage("§cA tag '" + tagName + "' não existe.");
                return true;
            }

            plugin.setPlayerTag(target.getUniqueId(), tagName);
            sender.sendMessage("§aTag '" + tagName + "' definida para o jogador " + target.getName() + ".");

            // Atualizar jogador
            Events events = new Events();
            events.updatePlayerTag(target);

            // Atualizar todos os jogadores
            for (Player player : Bukkit.getOnlinePlayers()) {
                events.updateScoreboardTeam(player);
            }

            return true;
        }

        if (subCommand.equals("limpar") && args.length >= 2) {
            // Limpar tag de jogador
            String playerName = args[1];

            Player target = Bukkit.getPlayer(playerName);
            if (target == null) {
                sender.sendMessage("§cJogador '" + playerName + "' não está online.");
                return true;
            }

            plugin.setPlayerTag(target.getUniqueId(), null);
            sender.sendMessage("§aTag do jogador " + target.getName() + " foi removida.");

            // Atualizar jogador
            Events events = new Events();
            events.updatePlayerTag(target);

            // Atualizar todos os jogadores
            for (Player player : Bukkit.getOnlinePlayers()) {
                events.updateScoreboardTeam(player);
            }

            return true;
        }

        if (subCommand.equals("recarregar")) {
            // Recarregar configuração
            plugin.reloadConfig();
            plugin.loadPlayerTags();
            sender.sendMessage("§aConfiguração recarregada com sucesso.");

            // Atualizar todos os jogadores
            Events events = new Events();
            for (Player player : Bukkit.getOnlinePlayers()) {
                events.updatePlayerTag(player);
                events.updateScoreboardTeam(player);
            }

            return true;
        }

        // Comando não reconhecido
        sender.sendMessage("§cComando não reconhecido. Use /tagadmin para ver a ajuda.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("nametags.admin")) {
            return completions;
        }

        if (args.length == 1) {
            String[] subCommands = {"criar", "editar", "remover", "definir", "limpar", "recarregar"};
            for (String sub : subCommands) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            if (subCommand.equals("editar") || subCommand.equals("remover")) {
                // Sugerir tags existentes
                ConfigurationSection tagsSection = plugin.getConfig().getConfigurationSection("tags");
                if (tagsSection != null) {
                    for (String tagName : tagsSection.getKeys(false)) {
                        if (tagName.toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(tagName);
                        }
                    }
                }
            } else if (subCommand.equals("definir") || subCommand.equals("limpar")) {
                // Sugerir jogadores online
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(player.getName());
                    }
                }
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();

            if (subCommand.equals("editar")) {
                // Sugerir propriedades editáveis
                String[] properties = {"display", "prioridade", "permissao"};
                for (String prop : properties) {
                    if (prop.startsWith(args[2].toLowerCase())) {
                        completions.add(prop);
                    }
                }
            } else if (subCommand.equals("definir")) {
                // Sugerir tags existentes
                ConfigurationSection tagsSection = plugin.getConfig().getConfigurationSection("tags");
                if (tagsSection != null) {
                    for (String tagName : tagsSection.getKeys(false)) {
                        if (tagName.toLowerCase().startsWith(args[2].toLowerCase())) {
                            completions.add(tagName);
                        }
                    }
                }
            }
        }

        return completions;
    }
}