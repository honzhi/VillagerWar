package com.yourname.villagerwar.command.arguments;

import com.yourname.villagerwar.VillagerWar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MapArgument implements SubCommand {

    private final VillagerWar plugin;

    public MapArgument(VillagerWar plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "map";
    }

    @Override
    public String getDescription() {
        return "地图管理：创建 / 列出 / 删除地图";
    }

    @Override
    public String getUsage() {
        return "/vw map <create|list|delete> [名称]";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("villagerwar.admin")) {
            sender.sendMessage("§c你没有权限执行此命令");
            return true;
        }

        if (args.length < 1) {
            sendHelp(sender);
            return true;
        }

        String action = args[0].toLowerCase();
        switch (action) {
            case "create":
                return handleCreate(sender, Arrays.copyOfRange(args, 1, args.length));
            case "list":
                return handleList(sender);
            case "delete":
                return handleDelete(sender, Arrays.copyOfRange(args, 1, args.length));
            default:
                sendHelp(sender);
                return true;
        }
    }

    // ========== create ==========

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§c用法: /vw map create <地图名称>");
            return true;
        }

        String mapName = args[0].trim();

        // 校验名称是否包含非法字符
        if (mapName.contains("/") || mapName.contains("\\") || mapName.contains("..") || mapName.contains(":")) {
            sender.sendMessage("§c地图名称包含非法字符");
            return true;
        }

        File mapDir = new File(plugin.getDataFolder(), "maps/" + mapName);
        if (mapDir.exists()) {
            sender.sendMessage("§c地图 [" + mapName + "] 已存在");
            return true;
        }

        // 创建文件夹
        if (!mapDir.mkdirs()) {
            sender.sendMessage("§c创建地图文件夹失败");
            return true;
        }

        // 从 map_defaults.yml 复制 map.yml
        File targetFile = new File(mapDir, "map.yml");
        try {
            // 先从 jar 中读取默认配置
            InputStream in = plugin.getResource("map_defaults.yml");
            if (in != null) {
                byte[] buffer = new byte[in.available()];
                in.read(buffer);
                in.close();

                // 替换显示名称为地图名称
                String content = new String(buffer, "UTF-8");
                content = content.replace("display_name: \"新地图\"", "display_name: \"" + mapName + "\"");

                OutputStream out = new FileOutputStream(targetFile);
                out.write(content.getBytes("UTF-8"));
                out.close();
            } else {
                // 如果 jar 中没有，尝试从数据文件夹读取
                File defaultsFile = new File(plugin.getDataFolder(), "map_defaults.yml");
                if (defaultsFile.exists()) {
                    java.nio.file.Files.copy(defaultsFile.toPath(), targetFile.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } else {
                    sender.sendMessage("§c未找到 map_defaults.yml 模版文件");
                    return true;
                }
            }
        } catch (Exception e) {
            sender.sendMessage("§c创建地图配置文件失败: " + e.getMessage());
            plugin.getLogger().warning("创建地图 map.yml 失败: " + e.getMessage());
            return true;
        }

        // 刷新地图列表
        plugin.getConfigManager().refreshMapConfigs();

        sender.sendMessage("§7[§6村民战争§7] §a地图 §e[" + mapName + "] §a创建成功！");
        sender.sendMessage("§7请将 Minecraft 地图文件放入 §emaps/" + mapName + "/ §7目录中");
        sender.sendMessage("§7（包含 level.dat、region/ 等文件）");
        sender.sendMessage("§7创建后可使用 §e/vw map list §7查看状态");
        return true;
    }

    // ========== list ==========

    private boolean handleList(CommandSender sender) {
        File mapsDir = new File(plugin.getDataFolder(), "maps");
        if (!mapsDir.isDirectory()) {
            sender.sendMessage("§7[§6村民战争§7] §e暂无地图");
            return true;
        }

        File[] dirs = mapsDir.listFiles(File::isDirectory);
        if (dirs == null || dirs.length == 0) {
            sender.sendMessage("§7[§6村民战争§7] §e暂无地图");
            return true;
        }

        sender.sendMessage("§7===== §6地图列表 §7=====");

        for (File dir : dirs) {
            String mapName = dir.getName();
            File mapYml = new File(dir, "map.yml");
            boolean hasConfig = mapYml.exists();
            boolean hasWorldData = hasLevelDatFile(dir);

            String status;
            if (!hasConfig) {
                status = "§c缺少 map.yml";
            } else if (!hasWorldData) {
                status = "§e未放入地图";
            } else {
                status = "§a就绪";
            }

            sender.sendMessage("  §e" + mapName + " §7- " + status);
        }
        return true;
    }

    // ========== delete ==========

    private boolean handleDelete(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§c用法: /vw map delete <地图名称>");
            return true;
        }

        String mapName = args[0].trim();

        // 校验名称
        if (mapName.contains("/") || mapName.contains("\\") || mapName.contains("..")) {
            sender.sendMessage("§c地图名称包含非法字符");
            return true;
        }

        File mapDir = new File(plugin.getDataFolder(), "maps/" + mapName);
        if (!mapDir.exists()) {
            sender.sendMessage("§c地图 [" + mapName + "] 不存在");
            return true;
        }

        // 确认删除
        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            sender.sendMessage("§c确定要删除地图 [" + mapName + "] 吗？所有文件将被删除");
            sender.sendMessage("§c使用 §e/vw map delete " + mapName + " confirm §c确认删除");
            return true;
        }

        // 递归删除
        deleteFolder(mapDir);

        // 刷新地图列表
        plugin.getConfigManager().refreshMapConfigs();

        sender.sendMessage("§7[§6村民战争§7] §a地图 §e[" + mapName + "] §a已删除");
        return true;
    }

    // ========== 工具方法 ==========

    /**
     * 检查地图文件夹中是否包含 level.dat（任意子文件夹均可）
     */
    private boolean hasLevelDatFile(File mapDir) {
        File[] subDirs = mapDir.listFiles(File::isDirectory);
        if (subDirs == null) return false;
        for (File subDir : subDirs) {
            if (new File(subDir, "level.dat").exists()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 递归删除文件夹
     */
    private void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFolder(file);
                } else {
                    file.delete();
                }
            }
        }
        folder.delete();
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§7===== §6地图管理 §7=====");
        sender.sendMessage("§e/vw map create <名称> §7- 创建新地图");
        sender.sendMessage("§e/vw map list §7- 列出所有地图");
        sender.sendMessage("§e/vw map delete <名称> §7- 删除地图");
    }

    @Override
    public @Nullable List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return Arrays.asList("create", "list", "delete").stream()
                    .filter(a -> a.startsWith(partial))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            // 列出可用地图
            File mapsDir = new File(plugin.getDataFolder(), "maps");
            if (mapsDir.isDirectory()) {
                File[] dirs = mapsDir.listFiles(File::isDirectory);
                if (dirs != null) {
                    String partial = args[1].toLowerCase();
                    return Arrays.stream(dirs)
                            .map(File::getName)
                            .filter(n -> n.toLowerCase().startsWith(partial))
                            .collect(Collectors.toList());
                }
            }
        }

        return Collections.emptyList();
    }
}
