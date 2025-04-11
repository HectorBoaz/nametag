package br.com.boazhector;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.ChatColor;
import br.com.boazhector.levelplugin.Main;

import java.util.UUID;

public class Events implements Listener {

    private final br.com.boazhector.Main plugin = br.com.boazhector.Main.getInstance();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Atualizar a tag deste jogador
        updatePlayerTag(player);

        // Atualizar a visualização de todos os jogadores
        for (Player online : Bukkit.getOnlinePlayers()) {
            updateScoreboardTeam(online);
        }

        // Atualizar a visualização deste jogador para todos os outros
        updateScoreboardTeam(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Remover jogador dos times quando sair
        Player player = event.getPlayer();
        for (Player online : Bukkit.getOnlinePlayers()) {
            Scoreboard scoreboard = online.getScoreboard();
            if (scoreboard == null) continue;

            Team team = scoreboard.getEntryTeam(player.getName());
            if (team != null) {
                team.removeEntry(player.getName());
            }
        }
    }

    // Atualizar a tag do jogador
    public void updatePlayerTag(Player player) {
        String tagName = plugin.getPlayerTag(player.getUniqueId());
        String tagDisplay = plugin.getTagDisplay(tagName);
        UUID playerUUID = player.getUniqueId();
        String suffix = String.valueOf(Main.m.getPlayerLevel(playerUUID));

        // Atualizar o nome visível acima da cabeça
        if (plugin.getConfig().getBoolean("mostrar-na-cabeca", true)) {
            String prefix = tagDisplay + (tagDisplay.isEmpty() ? "" : " ");
            player.setDisplayName(prefix + player.getName() + " §a[" + suffix + "]");
            player.setPlayerListName(prefix + player.getName() + " §a[" + suffix + "]");
        }
    }

    // Atualizar os times de scoreboard para exibição correta
    public void updateScoreboardTeam(Player viewer) {
        // Garantir que o jogador tenha um scoreboard
        UUID playerUUID = viewer.getUniqueId();
        String suffix = String.valueOf(Main.m.getPlayerLevel(playerUUID));

        if (viewer.getScoreboard() == Bukkit.getScoreboardManager().getMainScoreboard()) {
            viewer.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }

        Scoreboard scoreboard = viewer.getScoreboard();

        // Criar ou obter times para cada tag
        for (Player target : Bukkit.getOnlinePlayers()) {
            String tagName = plugin.getPlayerTag(target.getUniqueId());
            String teamName = "tag_" + (tagName != null ? tagName : "default");

            // Garantir que o nome da equipe não exceda 16 caracteres
            if (teamName.length() > 16) {
                teamName = teamName.substring(0, 16);
            }

            Team team = scoreboard.getTeam(teamName);
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
                String tagDisplay = plugin.getTagDisplay(tagName);
                team.setPrefix(tagDisplay + (tagDisplay.isEmpty() ? "" : " "));
                team.setSuffix(" §a[" + suffix + "]");

                // Definir prioridade para ordem no TAB
                int priority = plugin.getTagPriority(tagName);
                try {
                    // Esta funcionalidade requer Minecraft 1.8+
                    team.getClass().getMethod("setOption", Team.Option.class, Team.OptionStatus.class)
                            .invoke(team, Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
                } catch (Exception ignored) {}
            }

            // Adicionar jogador ao time
            if (!team.hasEntry(target.getName())) {
                team.addEntry(target.getName());
            }
        }
    }
}