package com.yourname.villagerwar.world;

import com.yourname.villagerwar.VillagerWar;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 全局地图管理器
 * 负责创建和删除游戏世界实例
 */
public class WorldManager {

    private final VillagerWar plugin;
    private final Map<String, GameWorld> activeWorlds;

    public WorldManager(VillagerWar plugin) {
        this.plugin = plugin;
        this.activeWorlds = new HashMap<>();
    }

    /**
     * 根据模板名称创建游戏世界
     * @param templateName 模板地图名称（对应 maps/<templateName>/ 目录）
     * @return GameWorld 实例
     */
    public GameWorld createGameWorld(String templateName) {
        GameWorld gameWorld = new GameWorld(templateName, plugin);
        gameWorld.load();
        activeWorlds.put(gameWorld.getWorldName(), gameWorld);

        plugin.getLogger().info("游戏世界 " + templateName + " 已创建");
        return gameWorld;
    }

    /**
     * 创建游戏世界（createWorld 别名，用于和其他模块兼容）
     * @param templateName 模板地图名称
     * @return GameWorld 实例
     */
    public GameWorld createWorld(String templateName) {
        return createGameWorld(templateName);
    }

    /**
     * 删除游戏世界（卸载后清理）
     * @param gw 要删除的 GameWorld 实例
     */
    public void deleteGameWorld(GameWorld gw) {
        if (gw == null) return;

        org.bukkit.World bw = gw.getBukkitWorld();
        gw.unload();
        activeWorlds.remove(gw.getWorldName());

        // 删除世界文件夹
        if (bw != null) {
            String worldName = bw.getName();
            File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
            if (worldFolder.exists()) {
                deleteFolder(worldFolder);
                plugin.getLogger().info("世界文件夹 " + worldName + " 已删除");
            }
        }

        plugin.getLogger().info("游戏世界 " + gw.getTemplateName() + " 已删除");
    }

    /**
     * 递归删除文件夹
     */
    private void deleteFolder(File folder) {
        if (folder == null || !folder.exists()) return;
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
     * 获取指定模板名称的游戏世界
     */
    public GameWorld getGameWorld(String templateName) {
        return activeWorlds.get(templateName);
    }

    /**
     * 获取所有活跃的游戏世界列表
     * @return 所有 GameWorld 实例的集合
     */
    public Collection<GameWorld> getAvailableWorlds() {
        return Collections.unmodifiableCollection(activeWorlds.values());
    }

    /**
     * 获取可用的模板地图列表
     * @return 模板地图文件夹名列表
     */
    public List<String> getAvailableTemplates() {
        List<String> templates = new ArrayList<>();
        File mapsDir = new File(plugin.getDataFolder(), "maps");
        if (!mapsDir.isDirectory()) return templates;

        File[] dirs = mapsDir.listFiles(File::isDirectory);
        if (dirs != null) {
            for (File dir : dirs) {
                File mapYml = new File(dir, "map.yml");
                if (mapYml.exists()) {
                    templates.add(dir.getName());
                }
            }
        }
        return templates;
    }

    /**
     * 获取所有活跃的游戏世界
     */
    public Map<String, GameWorld> getActiveWorlds() {
        return Collections.unmodifiableMap(activeWorlds);
    }

    public VillagerWar getPlugin() {
        return plugin;
    }
    // ====== ReservesSeat (备战席) 管理 ======

    private final Map<String, GameWorld> reservesSeats = new HashMap<>();
    private int seatCounter = 0;

    /**
     * 创建备战席世界实例（从 maps/ 目录加载）
     */
    public GameWorld createReservesSeat() {
        String templateName = plugin.getConfig().getString("reserves_seat.teleport_to.map", "reserves_seat");
        seatCounter++;
        String seatInstanceName = "reserves_seat" + seatCounter;

        // 检查世界是否已加载（通过实例名或模板名）
        World bukkitWorld = Bukkit.getWorld(seatInstanceName);
        if (bukkitWorld != null) {
            plugin.getLogger().info("[Debug] 备战席 " + seatInstanceName + " 已加载，直接使用");
            GameWorld seat = new GameWorld(templateName, plugin, bukkitWorld);
            reservesSeats.put(seatInstanceName, seat);
            return seat;
        }

        // 从 maps/<templateName>/ 查找地图模板
        File mapDir = new File(plugin.getDataFolder(), "maps/" + templateName);
        if (!mapDir.isDirectory()) {
            plugin.getLogger().severe("模板地图不存在: " + mapDir.getAbsolutePath());
            plugin.getLogger().severe("无法创建备战席实例: " + seatInstanceName + "（模板 " + templateName + " 不存在）");
            seatCounter--;
            return null;
        }

        File templateFolder = findMapWorldFolder(mapDir);
        if (templateFolder == null) {
            plugin.getLogger().severe("未找到地图世界文件: " + mapDir.getAbsolutePath()
                + "（请放入含 level.dat 的文件夹）");
            plugin.getLogger().severe("无法创建备战席实例: " + seatInstanceName + "（模板 " + templateName + " 中无 level.dat）");
            seatCounter--;
            return null;
        }

        // 复制模板到实例文件夹
        File targetFolder = new File(Bukkit.getWorldContainer(), seatInstanceName);
        if (targetFolder.exists()) {
            plugin.getLogger().warning("目标世界文件夹已存在，将被覆盖: " + seatInstanceName);
            deleteFolder(targetFolder);
        }

        try {
            copyFolder(templateFolder, targetFolder);
            // 清理冲突文件
            File uidFile = new File(targetFolder, "uid.dat");
            if (uidFile.exists()) { uidFile.delete(); plugin.getLogger().info("[Debug] 已删除 uid.dat"); }
            File paperWorldFile = new File(targetFolder, "paper-world.yml");
            if (paperWorldFile.exists()) { paperWorldFile.delete(); }
            File sessionLockFile = new File(targetFolder, "session.lock");
            if (sessionLockFile.exists()) { sessionLockFile.delete(); }
            plugin.getLogger().info("备战席模板 " + templateName + " 已复制到 " + seatInstanceName);
        } catch (Exception e) {
            plugin.getLogger().severe("复制备战席地图失败: " + e.getMessage());
            seatCounter--;
            return null;
        }

        // 加载世界
        WorldCreator creator = new WorldCreator(seatInstanceName);
        bukkitWorld = Bukkit.createWorld(creator);
        if (bukkitWorld == null) {
            plugin.getLogger().severe("无法加载备战席世界: " + seatInstanceName);
            seatCounter--;
            return null;
        }

        plugin.getLogger().info("[Debug] 备战席 " + seatInstanceName + " 加载成功");
        GameWorld seat = new GameWorld(templateName, plugin, bukkitWorld);
        reservesSeats.put(seatInstanceName, seat);
        return seat;
    }    public GameWorld findReservesSeat(String seatName) {
        return reservesSeats.get(seatName);
    }

    public void deleteReservesSeat(String seatName) {
        GameWorld seat = reservesSeats.remove(seatName);
        if (seat != null) {
            org.bukkit.World bw = seat.getBukkitWorld();
            seat.unload();
            // 删除世界文件夹
            if (bw != null) {
                File worldFolder = new File(Bukkit.getWorldContainer(), bw.getName());
                if (worldFolder.exists()) {
                    deleteFolder(worldFolder);
                }
            }
            plugin.getLogger().info("备战席实例 " + seatName + " 已删除");
        }
    }

    /**
     * 获取备战席传送位置
     */
    public org.bukkit.Location getReservesSeatLocation(GameWorld seat) {
        if (seat == null || seat.getBukkitWorld() == null) return null;
        org.bukkit.configuration.file.FileConfiguration config = plugin.getConfig();
        double x = config.getDouble("reserves_seat.teleport_to.x", 0.5);
        double y = config.getDouble("reserves_seat.teleport_to.y", 64.0);
        double z = config.getDouble("reserves_seat.teleport_to.z", 0.5);
        float yaw = (float) config.getDouble("reserves_seat.teleport_to.yaw", 0.0);
        float pitch = (float) config.getDouble("reserves_seat.teleport_to.pitch", 0.0);
        return new org.bukkit.Location(seat.getBukkitWorld(), x, y, z, yaw, pitch);
    }
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
}
