package jfbdev.jcmdblocker;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class JCMDBlocker extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private final Map<String, List<String>> blockedCommands = new HashMap<>();
    private String noPermissionMessage;
    private String blockedCommandMessage;
    private String reloadMessage;
    private String addMessage;
    private String delMessage;
    private String notFoundMessage;
    private List<String> usageMessage;
    private String worldNotFoundMessage;
    private String alreadyBlockedMessage;
    private String invalidSubcommandMessage;
    private String listHeader;
    private String noBlocked;
    private String worldHeader;
    private String noCommands;
    private String commandsList;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("jcmdbreload")).setExecutor(this);
        Objects.requireNonNull(getCommand("cmdblocker")).setExecutor(this);
        Objects.requireNonNull(getCommand("cmdblocker")).setTabCompleter(this);
    }

    private void loadConfig() {
        blockedCommands.clear();
        ConfigurationSection worldsSection = getConfig().getConfigurationSection("worlds");
        if (worldsSection != null) {
            for (String worldName : worldsSection.getKeys(false)) {
                List<String> commands = worldsSection.getStringList(worldName);
                blockedCommands.put(worldName.toLowerCase(), new ArrayList<>(commands));
            }
        }

        noPermissionMessage = getConfig().getString("messages.no_permission", "&cУ вас нет разрешения на использование этой команды.");
        blockedCommandMessage = getConfig().getString("messages.blocked_command", "&cЭта команда заблокирована в этом мире.");
        reloadMessage = getConfig().getString("messages.reload", "&aПлагин успешно перезагружен.");
        addMessage = getConfig().getString("messages.add", "&aКоманда '%command%' заблокирована в мире '%world%'.");
        delMessage = getConfig().getString("messages.del", "&aКоманда '%command%' разблокирована в мире '%world%'.");
        notFoundMessage = getConfig().getString("messages.not_found", "&cКоманда '%command%' не найдена в мире '%world%'.");
        usageMessage = getConfig().getStringList("messages.usage");
        if (usageMessage.isEmpty()) {
            usageMessage = Arrays.asList(
                    "",
                    "&fИспользование:",
                    "&a/cmdblocker add <command> <world> - Заблокировать команду в мире",
                    "&a/cmdblocker del <command> <world> - Разблокировать команду в мире",
                    "&a/cmdblocker list - Показать список заблокированных команд",
                    ""
            );
        }
        worldNotFoundMessage = getConfig().getString("messages.world_not_found", "&cМир '%world%' не найден.");
        alreadyBlockedMessage = getConfig().getString("messages.already_blocked", "&cКоманда уже заблокирована в этом мире.");
        invalidSubcommandMessage = getConfig().getString("messages.invalid_subcommand", "&cНеверная подкоманда. Используйте add, del или list.");
        listHeader = getConfig().getString("messages.list_header", "&aСписок заблокированных команд:");
        noBlocked = getConfig().getString("messages.no_blocked", "&cНет заблокированных команд.");
        worldHeader = getConfig().getString("messages.world_header", "&eМир: %world%");
        noCommands = getConfig().getString("messages.no_commands", "&7  Нет заблокированных команд.");
        commandsList = getConfig().getString("messages.commands_list", "&7  Заблокированные команды: %commands%");
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName().toLowerCase();
        String message = event.getMessage().toLowerCase();
        if (message.startsWith("/")) {
            String command = message.split(" ")[0].substring(1);
            List<String> blocked = blockedCommands.getOrDefault(worldName, Collections.emptyList());
            if (blocked.contains(command)) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', blockedCommandMessage));
                event.setCancelled(true);
            }
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("jcmdbreload")) {
            if (!sender.hasPermission("jcmdblocker.reload")) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', noPermissionMessage));
                return true;
            }
            reloadConfig();
            loadConfig();
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', reloadMessage));
            return true;
        }

        if (command.getName().equalsIgnoreCase("cmdblocker")) {
            if (!sender.hasPermission("jcmdblocker.admin")) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', noPermissionMessage));
                return true;
            }

            if (args.length == 0) {
                for (String line : usageMessage) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
                }
                return true;
            }

            String sub = args[0].toLowerCase();

            if (sub.equals("list")) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', listHeader));
                if (blockedCommands.isEmpty()) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', noBlocked));
                } else {
                    for (Map.Entry<String, List<String>> entry : blockedCommands.entrySet()) {
                        String world = entry.getKey();
                        List<String> cmds = entry.getValue();
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', worldHeader.replace("%world%", world)));
                        if (cmds.isEmpty()) {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', noCommands));
                        } else {
                            String cmdsStr = String.join(", ", cmds);
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', commandsList.replace("%commands%", cmdsStr)));
                        }
                    }
                }
                return true;
            }

            if (args.length < 3) {
                for (String line : usageMessage) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
                }
                return true;
            }

            String cmd = args[1].toLowerCase();
            String worldName = args[2].toLowerCase();

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', worldNotFoundMessage.replace("%world%", worldName)));
                return true;
            }

            blockedCommands.computeIfAbsent(worldName, k -> new ArrayList<>());

            if (sub.equals("add")) {
                if (!blockedCommands.get(worldName).contains(cmd)) {
                    blockedCommands.get(worldName).add(cmd);
                    updateConfig();
                    String formatted = addMessage.replace("%command%", cmd).replace("%world%", worldName);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', formatted));
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', alreadyBlockedMessage));
                }
            } else if (sub.equals("del")) {
                if (blockedCommands.get(worldName).remove(cmd)) {
                    updateConfig();
                    String formatted = delMessage.replace("%command%", cmd).replace("%world%", worldName);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', formatted));
                } else {
                    String formatted = notFoundMessage.replace("%command%", cmd).replace("%world%", worldName);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', formatted));
                }
            } else {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', invalidSubcommandMessage));
            }
            return true;
        }
        return false;
    }

    private void updateConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        YamlConfiguration diskConfig = YamlConfiguration.loadConfiguration(configFile);
        diskConfig.set("worlds", null);
        for (Map.Entry<String, List<String>> entry : blockedCommands.entrySet()) {
            diskConfig.set("worlds." + entry.getKey(), entry.getValue());
        }
        try {
            diskConfig.save(configFile);
        } catch (IOException e) {
            getLogger().severe("Error saving config: " + e.getMessage());
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, Command command, @NotNull String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("cmdblocker")) {
            if (args.length == 1) {
                return Arrays.asList("add", "del", "list");
            } else if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("del"))) {
                return Collections.emptyList();
            } else if (args.length == 3 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("del"))) {
                List<String> worlds = new ArrayList<>();
                for (World w : Bukkit.getWorlds()) {
                    worlds.add(w.getName());
                }
                return worlds;
            }
        }
        return Collections.emptyList();
    }
}