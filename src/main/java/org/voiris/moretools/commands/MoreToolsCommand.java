package org.voiris.moretools.commands;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.PermissionNode;
import net.luckperms.api.platform.PlayerAdapter;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.voiris.moretools.MoreTools;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.voiris.moretools.MoreTools.*;

public class MoreToolsCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, String[] args) {
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("help")) {
                sender.sendMessage(getStaticConfig().getString("messages.help"));
                return true;
            } else if (args[0].equalsIgnoreCase("reload")) {
                getInstance().reloadConfig();
                sender.sendMessage(getStaticConfig().getString("messages.reload"));
                return true;
            } else if (args.length > 1 && args[0].equalsIgnoreCase("user")) {
                Player player = getInstance().getServer().getPlayer(args[1]);
                if (args.length > 2) {
                    if (args[2].equalsIgnoreCase("tempperms")) {
                        if (args.length > 4) {
                            PlayerAdapter<Player> adapter = LuckPermsProvider.get().getPlayerAdapter(Player.class);
                            String permission = args[4];
                            User user = adapter.getUser(player);
                            File userFieldFile = new File(getTempPermsDir().getPath(), String.format("%s.yml", player.getUniqueId()));
                            if (args[3].equalsIgnoreCase("pause")) {
                                PermissionNode permissionNode = (PermissionNode) adapter.getPermissionData(player).queryPermission(permission).node();
                                if (permissionNode != null) {
                                    if (permissionNode.hasExpiry()) {
                                        FileConfiguration userField = getUserField(userFieldFile);
                                        long timeRemaining = getTimeRemaining(permissionNode.getExpiry());
                                        boolean value = permissionNode.getValue();
                                        userField.set(String.format("tempPausedPermission.%s.%s", permission.replace('.', '/'), "timeRemaining"), timeRemaining);
                                        userField.set(String.format("tempPausedPermission.%s.%s", permission.replace('.', '/'), "value"), value);
                                        saveFileConfiguration(userField, userFieldFile);
                                        if (user.data().remove(permissionNode).wasSuccessful()) {
                                            LuckPermsProvider.get().getUserManager().saveUser(user);
                                            sender.sendMessage(getStaticConfig().getString("messages.user.tempperms.pause.paused"));
                                        }
                                    } else {
                                        sender.sendMessage(getStaticConfig().getString("messages.user.tempperms.error.permissionIsNotExpiry"));
                                    }
                                } else {
                                    sender.sendMessage(getStaticConfig().getString("messages.user.tempperms.error.noPermissionNode"));
                                }
                                return true;
                            } else if (args[3].equalsIgnoreCase("unpause")) {
                                FileConfiguration userField = getUserField(userFieldFile);
                                long timeRemaining = userField.getLong(String.format("tempPausedPermission.%s.%s", permission.replace('.', '/'), "timeRemaining"));
                                boolean value = userField.getBoolean(String.format("tempPausedPermission.%s.%s", permission.replace('.', '/'), "value"));
                                saveFileConfiguration(userField, userFieldFile);
                                PermissionNode permissionNode = PermissionNode.builder(permission).value(value).expiry(Duration.ofSeconds(timeRemaining)).build();
                                if (user.data().add(permissionNode).wasSuccessful()) {
                                    LuckPermsProvider.get().getUserManager().saveUser(user);
                                    sender.sendMessage(getStaticConfig().getString("messages.user.tempperms.unpause.unpaused"));
                                    userField.set(String.format("tempPausedPermission.%s", permission.replace('.', '/')), null);
                                    saveFileConfiguration(userField, userFieldFile);
                                } else {
                                    sender.sendMessage(getStaticConfig().getString("messages.user.tempperms.unpause.permissionNotPaused"));
                                }

                                return true;
                            } else if (args.length > 5) {
                                PermissionNode permissionNode = (PermissionNode) adapter.getPermissionData(player).queryPermission(permission).node();
                                if (permissionNode != null) {
                                    if (permissionNode.hasExpiry()) {
                                        if (args[3].equalsIgnoreCase("addtime")) {
                                            long duration = Long.parseLong(args[5]);
                                            user.data().remove(permissionNode);
                                            user.data().add(PermissionNode.builder(permission).value(permissionNode.getValue()).expiry(
                                                    Duration.ofSeconds(getTimeRemaining(permissionNode.getExpiry())+duration)).build());
                                            LuckPermsProvider.get().getUserManager().saveUser(user);
                                            sender.sendMessage(getStaticConfig().getString("messages.user.tempperms.edittime.edited"));
                                        } else if (args[3].equalsIgnoreCase("taketime")) {
                                            long duration = Long.parseLong(args[5]);
                                            user.data().remove(permissionNode);
                                            long newDuration = (getTimeRemaining(permissionNode.getExpiry())-duration);
                                            if (newDuration > 0) {
                                                user.data().add(PermissionNode.builder(permission).value(permissionNode.getValue()).expiry(
                                                        Duration.ofSeconds(newDuration)).build());
                                                LuckPermsProvider.get().getUserManager().saveUser(user);
                                            }
                                            sender.sendMessage(getStaticConfig().getString("messages.user.tempperms.edittime.edited"));
                                        } else if (args[3].equalsIgnoreCase("settime")) {
                                            long duration = Long.parseLong(args[5]);
                                            user.data().remove(permissionNode);
                                            user.data().add(PermissionNode.builder(permission).value(permissionNode.getValue()).expiry(Duration.ofSeconds(duration)).build());
                                            LuckPermsProvider.get().getUserManager().saveUser(user);
                                            sender.sendMessage(getStaticConfig().getString("messages.user.tempperms.edittime.edited"));
                                        } else if (args[3].equalsIgnoreCase("copytime")) {
                                            Node secondPermissionNode = adapter.getPermissionData(player).queryPermission(args[5]).node();
                                            user.data().add(InheritanceNode.builder(args[5]).value(permissionNode.getValue()).expiry(permissionNode.getExpiry()).build());
                                            LuckPermsProvider.get().getUserManager().saveUser(user);
                                            sender.sendMessage(getStaticConfig().getString("messages.user.tempperms.edittime.edited"));
                                        } else {
                                            sender.sendMessage(getStaticConfig().getString("messages.user.tempperms.edittime.noTimeOrPermissionArgument"));
                                        }
                                    } else {
                                        sender.sendMessage(getStaticConfig().getString("messages.user.tempperms.error.permissionIsNotExpiry"));
                                    }
                                } else {
                                    sender.sendMessage(getStaticConfig().getString("messages.user.tempperms.error.noPermissionNode"));
                                }
                                return true;
                            }
                            return true;
                        }
                        sender.sendMessage(getStaticConfig().getString("messages.user.tempperms.main"));
                        return true;
                    }
                }
                sender.sendMessage(getStaticConfig().getString("messages.user.main"));
                return true;
            }
        }
        sender.sendMessage("[MoreTools] Made by Voiris!");
        sender.sendMessage(getStaticConfig().getString("messages.help"));
        return true;
    }

    private FileConfiguration getUserField(File userFieldFile) {
        FileConfiguration userField = new YamlConfiguration();
        try {
            if (!userFieldFile.exists()) {
                userFieldFile.createNewFile();
            }
            userField.load(userFieldFile);
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
        return userField;
    }

    private void saveFileConfiguration(FileConfiguration fileConfiguration, File file) {
        try {
            fileConfiguration.save(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private long getTimeRemaining(Instant instant) {
        return Instant.now().until(instant, ChronoUnit.SECONDS);
    }

    private boolean hasPermission(User user, String permission) {
        return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        List<String> progress_list = new ArrayList<>();
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            progress_list.add("user");
            progress_list.add("reload");
            progress_list.add("help");
        } else if (args.length > 1 && !(args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("reload"))) {
            if (args.length == 2) {
                for (Player p : MoreTools.getInstance().getServer().getOnlinePlayers()) {
                    progress_list.add(p.getName());
                }
            } else if (args.length == 3) {
                progress_list.add("tempperms");
            } else if (args[2].equalsIgnoreCase("tempperms")) {
                if (args.length == 4) {
                    progress_list.add("permission");
                } else if (args.length == 5) {
                    progress_list.add("pause");
                    progress_list.add("unpause");
                    progress_list.add("addtime");
                    progress_list.add("settime");
                    progress_list.add("taketime");
                    progress_list.add("copytime");
                } else if (args.length == 6) {
                    if (args[4].equalsIgnoreCase("addtime") || args[4].equalsIgnoreCase("settime")
                            || args[4].equalsIgnoreCase("taketime")) {
                        progress_list = Arrays.asList("1800", "3600", "5400", "7200", "9000");
                    } else if (args[4].equalsIgnoreCase("copytime")) {
                        progress_list.add("permission");
                    }
                }
            }
        }
        if (progress_list.isEmpty()) {
            list = progress_list;
        } else {
            list = getPossibleCompletions(args, progress_list);
        }
        return list;
    }
    public static List<String> getPossibleCompletions(final String[] args, final List<String> possibilitiesOfCompletion) {
        final String argumentToFindCompletionFor = args[args.length - 1];

        final List<String> listOfPossibleCompletions = new ArrayList<String>();

        for (final String foundString : possibilitiesOfCompletion) {
            if (foundString.regionMatches(true, 0, argumentToFindCompletionFor, 0, argumentToFindCompletionFor.length())) {
                listOfPossibleCompletions.add(foundString);
            }
        }
        return listOfPossibleCompletions;
    }
}
