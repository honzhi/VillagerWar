package com.yourname.villagerwar.command.arguments;

import com.yourname.villagerwar.VillagerWar;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;

public class MapArgument implements SubCommand {

    private final VillagerWar plugin;
    // 记录玩家进入地图前的原始位置
    private static final Map<UUID, Location> previousLocations = new HashMap<>();
    // 记录玩家当前编辑的地图名称
    private static final Map<UUID, String> editingMaps = new HashMap<>();
    // 记录自动保存任务
    private static final Map<UUID, BukkitTask> autoSaveTasks = new HashMap<>();

    public MapArgument(VillagerWar plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "map";
    }

    @Override
    public String getDescription() {
        return "地图管理：创建 / 列出 / 删除 / 进入 / 离开 / 保存";
    }

    @Override
    public String getUsage() {
        return "/vw map <create|list|delete|join|leave|save> [名称]";
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
            case "join":
                return handleJoin(sender, Arrays.copyOfRange(args, 1, args.length));
            case "leave":
                return handleLeave(sender);
            case "save":
                return handleSave(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }

    // ========== join ==========

    private boolean handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c此命令仅限玩家执行");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage("§c用法: /vw map join <地图名称>");
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        // 检查是否已在编辑地图
        if (editingMaps.containsKey(uuid)) {
            String currentMap = editingMaps.get(uuid);
            sender.sendMessage("§c你正在编辑地图 §e[" + currentMap + "] §c，请先使用 §e/vw map leave §c离开");
            return true;
        }

        String mapName = args[0].trim();

        // 检查地图模板是否存在
        File mapDir = new File(plugin.getDataFolder(), "maps/" + mapName);
        if (!mapDir.exists()) {
            sender.sendMessage("§c地图 [" + mapName + "] 不存在");
            return true;
        }

        // 查找 maps/<mapName>/ 下含 level.dat 的子文件夹
        File templateFolder = findMapWorldFolder(mapDir);
        if (templateFolder == null) {
            sender.sendMessage("§c地图 [" + mapName + "] 未放入地图文件（缺少 level.dat）");
            return true;
        }

        // 保存玩家当前位置
        previousLocations.put(uuid, player.getLocation());
        editingMaps.put(uuid, mapName);

        // 目标世界名
        String targetWorldName = "map_editor";

        // 如果世界已加载，直接传送
        World existing = Bukkit.getWorld(targetWorldName);
        if (existing != null) {
            player.teleport(existing.getSpawnLocation());
            sender.sendMessage("§7[§6村民战争§7] §a已进入地图 §e[" + mapName + "] §a进行编辑");
            startAutoSave(player, mapName);
            return true;
        }

        // 复制模板到服务器根目录
        File targetFolder = new File(Bukkit.getWorldContainer(), targetWorldName);
        if (targetFolder.exists()) {
            deleteFolder(targetFolder);
        }
        try {
            copyFolder(templateFolder, targetFolder);
        } catch (Exception e) {
            editingMaps.remove(uuid);
            previousLocations.remove(uuid);
            sender.sendMessage("§c复制地图文件失败: " + e.getMessage());
            return true;
        }

        // 加载世界
        try {
            World world = Bukkit.createWorld(new WorldCreator(targetWorldName));
            if (world == null) {
                editingMaps.remove(uuid);
                previousLocations.remove(uuid);
                sender.sendMessage("§c无法加载地图世界");
                return true;
            }
            player.teleport(world.getSpawnLocation());
            sender.sendMessage("§7[§6村民战争§7] §a已进入地图 §e[" + mapName + "] §a进行编辑");
            sender.sendMessage("§7使用 §e/vw map leave §7传送回原地");
            sender.sendMessage("§7使用 §e/vw map save §7保存修改");
            startAutoSave(player, mapName);
        } catch (Exception e) {
            editingMaps.remove(uuid);
            previousLocations.remove(uuid);
            sender.sendMessage("§c加载地图世界失败: " + e.getMessage());
        }
        return true;
    }

    // ========== leave ==========

    private boolean handleLeave(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c此命令仅限玩家执行");
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();
        String mapName = editingMaps.remove(uuid);
        Location prev = previousLocations.remove(uuid);

        if (mapName == null || prev == null) {
            sender.sendMessage("§7[§6村民战争§7] §c你没有进入任何地图");
            return true;
        }

        // 停止自动保存任务
        cancelAutoSave(uuid);

        // 自动保存
        if (plugin.getConfigManager().getPluginConfig().isMapEditorAutoSave()) {
            doSave(mapName, player);
            player.sendMessage("§7[§6村民战争§7] §a已自动保存地图修改");
        }

        // 传送回原地
        player.teleport(prev);

        // 卸载并删除世界
        String targetWorldName = "map_editor";
        World world = Bukkit.getWorld(targetWorldName);
        if (world != null) {
            Bukkit.unloadWorld(world, false);
            plugin.getLogger().info("已卸载编辑世界: " + targetWorldName);
        }
        File targetFolder = new File(Bukkit.getWorldContainer(), targetWorldName);
        if (targetFolder.exists()) {
            deleteFolder(targetFolder);
            plugin.getLogger().info("已删除编辑世界文件夹: " + targetWorldName);
        }

        sender.sendMessage("§7[§6村民战争§7] §a已离开地图，传送回原地");
        return true;
    }

    // ========== save ==========

    private boolean handleSave(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c此命令仅限玩家执行");
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();
        String mapName = editingMaps.get(uuid);

        if (mapName == null) {
            sender.sendMessage("§7[§6村民战争§7] §c你没有在编辑任何地图");
            return true;
        }

        if (doSave(mapName, player)) {
            sender.sendMessage("§7[§6村民战争§7] §a地图 §e[" + mapName + "] §a已保存");
        }
        return true;
    }

    /**
     * 执行保存：仅将地形数据（region/ + level.dat）从编辑世界同步到模板
     */
    private boolean doSave(String mapName, Player player) {
        String targetWorldName = "map_editor";
        World world = Bukkit.getWorld(targetWorldName);
        if (world == null) {
            if (player != null) player.sendMessage("§c编辑世界未加载，无法保存");
            return false;
        }

        // 保存世界（确保区块落地）
        world.save();

        // 找到模板文件夹中的子文件夹
        File mapDir = new File(plugin.getDataFolder(), "maps/" + mapName);
        File templateFolder = findMapWorldFolder(mapDir);
        if (templateFolder == null) {
            if (player != null) player.sendMessage("§c找不到地图模板文件夹，无法保存");
            return false;
        }

        File sourceFolder = new File(Bukkit.getWorldContainer(), targetWorldName);

        try {
            // 仅复制 region/（区块数据）和 level.dat（世界设置）
            // 跳过 playerdata/、stats/、uid.dat、session.lock 等服务端特定文件
            copyIfExists(sourceFolder, templateFolder, "region");
            copyIfExists(sourceFolder, templateFolder, "level.dat");
            plugin.getLogger().info("地图 " + mapName + " 地形数据已保存（region + level.dat）");
            return true;
        } catch (Exception e) {
            if (player != null) player.sendMessage("§c保存地图失败: " + e.getMessage());
            plugin.getLogger().warning("保存地图 " + mapName + " 失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 如果源文件/文件夹存在，则复制到目标
     */
    private void copyIfExists(File sourceDir, File targetDir, String name) throws java.io.IOException {
        File src = new File(sourceDir, name);
        if (!src.exists()) return;
        File dst = new File(targetDir, name);
        if (dst.exists()) {
            deleteFolder(dst);
        }
        copyFolder(src, dst);
    }

    // ========== 自动保存 ==========

    private void startAutoSave(Player player, String mapName) {
        UUID uuid = player.getUniqueId();
        cancelAutoSave(uuid);

        if (!plugin.getConfigManager().getPluginConfig().isMapEditorAutoSave()) return;

        int interval = plugin.getConfigManager().getPluginConfig().getMapEditorAutoSaveInterval();
        // 转换为 tick（秒 * 20）
        long intervalTicks = Math.max(interval * 20L, 100L); // 最少5秒

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // 检查玩家是否还在编辑该地图
            if (!editingMaps.containsKey(uuid) || !editingMaps.get(uuid).equals(mapName)) {
                cancelAutoSave(uuid);
                return;
            }
            // 检查玩家是否在线
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) {
                cancelAutoSave(uuid);
                return;
            }

            if (doSave(mapName, null)) {
                p.sendActionBar("§a§l✔ 已自动保存修改");
            }
        }, intervalTicks, intervalTicks);

        autoSaveTasks.put(uuid, task);
    }

    private void cancelAutoSave(UUID uuid) {
        BukkitTask task = autoSaveTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    // ========== create ==========

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§c用法: /vw map create <地图名称>");
            return true;
        }

        String mapName = args[0].trim();

        if (mapName.contains("/") || mapName.contains("\\") || mapName.contains("..") || mapName.contains(":")) {
            sender.sendMessage("§c地图名称包含非法字符");
            return true;
        }

        File mapDir = new File(plugin.getDataFolder(), "maps/" + mapName);
        if (mapDir.exists()) {
            sender.sendMessage("§c地图 [" + mapName + "] 已存在");
            return true;
        }

        if (!mapDir.mkdirs()) {
            sender.sendMessage("§c创建地图文件夹失败");
            return true;
        }

        File targetFile = new File(mapDir, "map.yml");
        try {
            InputStream in = plugin.getResource("map_defaults.yml");
            if (in != null) {
                byte[] buffer = new byte[in.available()];
                in.read(buffer);
                in.close();

                String content = new String(buffer, "UTF-8");
                content = content.replace("display_name: \"新地图\"", "display_name: \"" + mapName + "\"");

                OutputStream out = new FileOutputStream(targetFile);
                out.write(content.getBytes("UTF-8"));
                out.close();
            } else {
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

        if (mapName.contains("/") || mapName.contains("\\") || mapName.contains("..")) {
            sender.sendMessage("§c地图名称包含非法字符");
            return true;
        }

        File mapDir = new File(plugin.getDataFolder(), "maps/" + mapName);
        if (!mapDir.exists()) {
            sender.sendMessage("§c地图 [" + mapName + "] 不存在");
            return true;
        }

        // 检查是否正在被编辑
        if (editingMaps.containsValue(mapName)) {
            sender.sendMessage("§c地图 [" + mapName + "] 正在被编辑，无法删除");
            return true;
        }

        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            sender.sendMessage("§c确定要删除地图 [" + mapName + "] 吗？所有文件将被删除");
            sender.sendMessage("§c使用 §e/vw map delete " + mapName + " confirm §c确认删除");
            return true;
        }

        deleteFolder(mapDir);
        plugin.getConfigManager().refreshMapConfigs();

        sender.sendMessage("§7[§6村民战争§7] §a地图 §e[" + mapName + "] §a已删除");
        return true;
    }

    // ========== 工具方法 ==========

    /**
     * 在地图文件夹下找到含 level.dat 的子文件夹
     */
    private File findMapWorldFolder(File mapDir) {
        if (!mapDir.isDirectory()) return null;
        File[] subDirs = mapDir.listFiles(File::isDirectory);
        if (subDirs != null) {
            for (File subDir : subDirs) {
                if (new File(subDir, "level.dat").exists()) {
                    return subDir;
                }
            }
        }
        return null;
    }

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

    /**
     * 清空文件夹内容（保留文件夹本身）
     */
    private void deleteFolderContents(File folder) {
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
    }

    /**
     * 复制文件夹（递归）
     */
    private void copyFolder(File source, File target) throws java.io.IOException {
        if (source.isDirectory()) {
            if (!target.exists() && !target.mkdirs()) {
                throw new java.io.IOException("无法创建目录: " + target);
            }
            File[] children = source.listFiles();
            if (children != null) {
                for (File child : children) {
                    copyFolder(child, new File(target, child.getName()));
                }
            }
        } else {
            java.nio.file.Files.copy(source.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§7===== §6地图管理 §7=====");
        sender.sendMessage("§e/vw map create <名称> §7- 创建新地图");
        sender.sendMessage("§e/vw map list §7- 列出所有地图");
        sender.sendMessage("§e/vw map delete <名称> §7- 删除地图");
        sender.sendMessage("§e/vw map join <名称> §7- 进入地图进行编辑");
        sender.sendMessage("§e/vw map leave §7- 离开地图（自动保存+传送回原地）");
        sender.sendMessage("§e/vw map save §7- 手动保存地图修改");
    }

    @Override
    public @Nullable List<String> tabComplete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("villagerwar.admin")) return Collections.emptyList();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return Arrays.asList("create", "list", "delete", "join", "leave", "save").stream()
                    .filter(a -> a.startsWith(partial))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("join")) {
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
        }

        return Collections.emptyList();
    }
}