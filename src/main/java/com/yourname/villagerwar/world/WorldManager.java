package com.yourname.villagerwar.world;

import com.yourname.villagerwar.VillagerWar;
import org.bukkit.Bukkit;
import org.bukkit.World;

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
        if (activeWorlds.containsKey(templateName)) {
            plugin.getLogger().warning("游戏世界 " + templateName + " 已存在，返回现有实例");
            return activeWorlds.get(templateName);
        }

        GameWorld gameWorld = new GameWorld(templateName, plugin);
        gameWorld.load();
        activeWorlds.put(templateName, gameWorld);

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

        gw.unload();
        activeWorlds.remove(gw.getTemplateName());

        // 删除世界文件夹
        World bukkitWorld = gw.getBukkitWorld();
        if (bukkitWorld != null) {
            String worldName = bukkitWorld.getName();
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
}