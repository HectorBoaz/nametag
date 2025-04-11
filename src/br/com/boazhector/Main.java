package br.com.boazhector;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Main extends JavaPlugin {

    private static Main instance;
    private File tagsFile;
    private FileConfiguration tagsConfig;
    private Map<UUID, String> playerTags = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;

        // Salvar configurações padrão
        saveDefaultConfig();
        setupTagsConfig();

        // Registrar eventos
        Bukkit.getPluginManager().registerEvents(new Events(), this);

        // Registrar comandos
        getCommand("tag").setExecutor(new Commands());
        getCommand("tagadmin").setExecutor(new CommandsAdmin());

        // Carregar tags dos jogadores
        loadPlayerTags();

        getLogger().info("Plugin de NameTags foi ativado com sucesso!");
    }

    @Override
    public void onDisable() {
        saveTagsConfig();
        getLogger().info("Plugin de NameTags foi desativado!");
    }

    public static Main getInstance() {
        return instance;
    }

    // Configurar arquivo de tags
    private void setupTagsConfig() {
        tagsFile = new File(getDataFolder(), "tags.yml");
        if (!tagsFile.exists()) {
            tagsFile.getParentFile().mkdirs();
            saveResource("tags.yml", false);
        }

        tagsConfig = YamlConfiguration.loadConfiguration(tagsFile);
    }

    // Salvar configuração de tags
    public void saveTagsConfig() {
        try {
            tagsConfig.save(tagsFile);
        } catch (Exception e) {
            getLogger().severe("Não foi possível salvar tags.yml!");
            e.printStackTrace();
        }
    }

    // Obter configuração de tags
    public FileConfiguration getTagsConfig() {
        return tagsConfig;
    }

    // Carregar tags dos jogadores da configuração
    public void loadPlayerTags() {
        playerTags.clear();

        if (tagsConfig.contains("players")) {
            for (String uuidStr : tagsConfig.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String tagName = tagsConfig.getString("players." + uuidStr);
                    playerTags.put(uuid, tagName);
                } catch (IllegalArgumentException e) {
                    getLogger().warning("UUID inválido em tags.yml: " + uuidStr);
                }
            }
        }
    }

    // Obter tag do jogador
    public String getPlayerTag(UUID uuid) {
        return playerTags.getOrDefault(uuid, null);
    }

    // Definir tag do jogador
    public void setPlayerTag(UUID uuid, String tagName) {
        if (tagName == null) {
            playerTags.remove(uuid);
            tagsConfig.set("players." + uuid.toString(), null);
        } else {
            playerTags.put(uuid, tagName);
            tagsConfig.set("players." + uuid.toString(), tagName);
        }

        saveTagsConfig();
    }

    // Obter prioridade da tag (para ordenação no TAB)
    public int getTagPriority(String tagName) {
        if (tagName == null) return Integer.MAX_VALUE;
        return getConfig().getInt("tags." + tagName + ".prioridade", Integer.MAX_VALUE);
    }

    // Obter texto de exibição da tag
    public String getTagDisplay(String tagName) {
        if (tagName == null) return "";
        return getConfig().getString("tags." + tagName + ".display", "").replace("&", "§");
    }

    // Obter permissão da tag
    public String getTagPermission(String tagName) {
        if (tagName == null) return null;
        return getConfig().getString("tags." + tagName + ".permissao", null);
    }

    // Verificar se a tag existe
    public boolean tagExists(String tagName) {
        return getConfig().contains("tags." + tagName);
    }
}