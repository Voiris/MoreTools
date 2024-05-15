package org.voiris.moretools;

import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.voiris.moretools.commands.MoreToolsCommand;

import java.io.File;
import java.nio.file.Path;
import java.util.logging.Logger;

public final class MoreTools extends JavaPlugin {
    private final Logger logger = getLogger();
    private FileConfiguration config = getConfig();
    static private MoreTools instance = null;

    // Data dirs
    static private File dataDir;
    static private File tempPermsDir;

    @Override
    public void onEnable() {
        // Loading config file with defaults
        loadConfig();
        // Load command executors
        PluginCommand moreToolsCommand = getCommand("moretools");
        if (moreToolsCommand != null) {
            moreToolsCommand.setExecutor(new MoreToolsCommand());
        }
        // Making data dirs
        dataDir = new File(getDataFolder(), "data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        tempPermsDir = new File(dataDir.getPath(), "tempperms");
        if (!tempPermsDir.exists()) {
            tempPermsDir.mkdirs();
        }

        instance = this;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private void loadConfig() {
        reloadConfig();
        config = super.getConfig();
        config.addDefault("messages.help",
                "[MoreTools] Команды:\n" +
                        "/mt user [user]\n" +
                        "/mt reload");
        config.addDefault("messages.reload",
                "[MoreTools] Плагин перезагружен!");
        config.addDefault("messages.user.main",
                "[MoreTools] Команды управления пользователем:\n" +
                        "/mt user [user] tempperms");
        config.addDefault("messages.user.tempperms.main",
                "[MoreTools] Команды временных привилегий:\n" +
                        "/mt user [user] tempperms pause [permission]\n" +
                        "/mt user [user] tempperms unpause [permission]\n" +
                        "/mt user [user] tempperms addtime [seconds]\n" +
                        "/mt user [user] tempperms settime [permission] [seconds]\n" +
                        "/mt user [user] tempperms taketime [permission] [seconds]");
        config.addDefault("messages.user.tempperms.error.noPermissionNode",
                "[MoreTools] У игрока нет ноды с этим правом!");
        config.addDefault("messages.user.tempperms.error.permissionIsNotExpiry",
                "[MoreTools] Это право не является временным!");
        config.addDefault("messages.user.tempperms.pause.paused",
                "[MoreTools] Временное право было остановлено!");
        config.addDefault("messages.user.tempperms.unpause.unpaused",
                "[MoreTools] Остановленное временное право было возвращено!");
        config.addDefault("messages.user.tempperms.unpause.permissionNotPaused",
                "[MoreTools] Временная привилегия не остановлена!");
        config.addDefault("messages.user.tempperms.edittime.noTimeOrPermissionArgument",
                "[MoreTools] Не хватает аргумента! (времени в секундах или права)");
        config.addDefault("messages.user.tempperms.edittime.edited",
                "[MoreTools] Время истечения этого право изменено!");
        config.options().copyDefaults(true);
        super.saveConfig();
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        super.saveConfig();
        this.config = super.getConfig();
    }

    public static File getDataDir() {
        return dataDir;
    }

    public static File getTempPermsDir() {
        return tempPermsDir;
    }

    static public MoreTools getInstance() {
        return instance;
    }

    static public @NotNull FileConfiguration getStaticConfig() {
        return MoreTools.getInstance().getConfig();
    }

    static public @NotNull Logger getStaticLogger() {
        return MoreTools.getInstance().getLogger();
    }
}
