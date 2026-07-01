package com.yourname.villagerwar.util;

import com.yourname.villagerwar.VillagerWar;
import com.yourname.villagerwar.config.holder.MessagesConfig;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 消息格式化工具类
 * 支持 & 颜色代码、{} 占位符替换、从 messages.yml 读取并发送
 */
public final class MessageUtil {

    private static final Pattern COLOR_PATTERN = Pattern.compile("&[0-9a-fk-or]");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{[^}]+\\}");

    private MessageUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 将 & 颜色代码转换为 Adventure API 的 Legacy 格式
     * @param message 原始消息（含 &0-&f, &l, &m, &n, &o, &r）
     * @return 转换后的字符串（含 § 颜色代码）
     */
    public static String colorize(String message) {
        if (message == null || message.isEmpty()) return "";
        return message.replace('&', '§');
    }

    /**
     * 将 & 颜色代码转换为 Adventure Component
     * @param message 原始消息（含 & 颜色代码）
     * @return Adventure Component
     */
    public static Component componentize(String message) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }

    /**
     * 格式化消息：替换所有 {} 占位符
     * 占位符按顺序替换为 args 中的参数
     * 支持 {prefix} 特殊占位符（自动替换为 messages.yml 中的 prefix）
     *
     * @param message 原始消息模板
     * @param args    占位符替换值（按顺序替换 {}）
     * @return 格式化后的字符串
     */
    public static String format(String message, Object... args) {
        if (message == null) return "";
        if (args == null || args.length == 0) return colorize(message);

        // 替换 {prefix} 特殊占位符
        if (message.contains("{prefix}")) {
            String prefix = getPrefix();
            message = message.replace("{prefix}", prefix);
        }

        // 替换 {0}, {1}, ... 索引占位符
        for (int i = 0; i < args.length; i++) {
            String placeholder = "{" + i + "}";
            if (message.contains(placeholder)) {
                message = message.replace(placeholder, String.valueOf(args[i]));
            }
        }

        // 按顺序替换 {} 占位符
        int argIndex = 0;
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(message);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String match = matcher.group();
            // 跳过已特殊处理的 {prefix} 等命名占位符
            if (match.equals("{prefix}")) continue;

            if (argIndex < args.length) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(args[argIndex])));
                argIndex++;
            } else {
                matcher.appendReplacement(sb, match);
            }
        }
        matcher.appendTail(sb);

        return colorize(sb.toString());
    }

    /**
     * 格式化消息，支持命名占位符如 {player}, {game}, {amount} 等
     * @param message 原始消息模板
     * @param placeholders 占位符名 → 值的映射
     * @return 格式化后的字符串
     */
    public static String formatMap(String message, java.util.Map<String, Object> placeholders) {
        if (message == null) return "";
        if (placeholders == null || placeholders.isEmpty()) return colorize(message);

        // 替换 {prefix}
        if (message.contains("{prefix}") && !placeholders.containsKey("prefix")) {
            message = message.replace("{prefix}", getPrefix());
        }

        for (java.util.Map.Entry<String, Object> entry : placeholders.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            if (message.contains(placeholder)) {
                message = message.replace(placeholder, String.valueOf(entry.getValue()));
            }
        }

        return colorize(message);
    }

    /**
     * 从 messages.yml 读取消息并发送给玩家
     * 支持嵌套键（如 "game.join"）
     * 自动替换占位符
     *
     * @param player 目标玩家
     * @param key    消息键名（如 "game.join"）
     * @param args   占位符替换值
     */
    public static void sendMessage(Player player, String key, Object... args) {
        if (player == null) return;

        MessagesConfig messagesConfig = getMessagesConfig();
        if (messagesConfig == null) {
            player.sendMessage(colorize("&c消息配置未加载: " + key));
            return;
        }

        String message = messagesConfig.getMessage(key);
        if (message == null || message.isEmpty()) {
            player.sendMessage(colorize("&c消息键不存在: " + key));
            return;
        }

        // 自动添加 prefix（如果消息以 {prefix} 开头）
        if (message.startsWith("{prefix}")) {
            String prefix = messagesConfig.getMessage("prefix", "&7[&6村民战争&7] ");
            message = message.replace("{prefix}", prefix);
        }

        String formatted = format(message, args);
        player.sendMessage(componentize(formatted));
    }

    /**
     * 从 messages.yml 读取消息，以 Adventure Component 形式返回
     * @param key  消息键名
     * @param args 占位符替换值
     * @return Adventure Component
     */
    public static Component getMessageComponent(String key, Object... args) {
        MessagesConfig messagesConfig = getMessagesConfig();
        if (messagesConfig == null) return Component.text("<missing config>");

        String message = messagesConfig.getMessage(key);
        if (message == null) return Component.text("<missing: " + key + ">");

        if (message.contains("{prefix}")) {
            String prefix = messagesConfig.getMessage("prefix", "&7[&6村民战争&7] ");
            message = message.replace("{prefix}", prefix);
        }

        return componentize(format(message, args));
    }

    /**
     * 广播消息
     * @param key  消息键名
     * @param args 占位符替换值
     */
    public static void broadcastMessage(String key, Object... args) {
        MessagesConfig messagesConfig = getMessagesConfig();
        if (messagesConfig == null) return;

        String message = messagesConfig.getMessage(key);
        if (message == null) return;

        if (message.contains("{prefix}")) {
            String prefix = messagesConfig.getMessage("prefix", "&7[&6村民战争&7] ");
            message = message.replace("{prefix}", prefix);
        }

        String formatted = format(message, args);
        org.bukkit.Bukkit.broadcast(componentize(formatted));
    }

    /**
     * 获取消息前缀
     */
    private static String getPrefix() {
        MessagesConfig messagesConfig = getMessagesConfig();
        if (messagesConfig == null) return "&7[&6村民战争&7] ";
        return messagesConfig.getMessage("prefix", "&7[&6村民战争&7] ");
    }

    /**
     * 从 ConfigManager 获取 MessagesConfig
     */
    private static MessagesConfig getMessagesConfig() {
        try {
            return VillagerWar.getInstance().getConfigManager().getMessagesConfig();
        } catch (Exception e) {
            return null;
        }
    }
}
