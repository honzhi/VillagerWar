package com.yourname.villagerwar.bridge;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * MythicMobs 统一调用层
 * 纯反射实现，无编译时依赖
 * 在运行时动态调用 MythicMobs API
 */
public final class MMBridge {

    private static final Logger LOGGER = Bukkit.getLogger();
    private static Object mythicBukkit = null;
    private static Class<?> mythicBukkitClass = null;
    private static Class<?> bukkitAdapterClass = null;
    private static boolean initialized = false;
    private static boolean available = false;

    private MMBridge() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 初始化反射类引用
     */
    private static boolean init() {
        if (initialized) return available;

        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("MythicMobs");
            if (plugin == null || !plugin.isEnabled()) {
                LOGGER.info("[MMBridge] MythicMobs 插件未安装或未启用");
                initialized = true;
                available = false;
                return false;
            }

            mythicBukkit = plugin;
            mythicBukkitClass = plugin.getClass();
            bukkitAdapterClass = Class.forName("io.lumine.mythic.bukkit.BukkitAdapter");
            initialized = true;
            available = true;
            LOGGER.info("[MMBridge] MythicMobs 已就绪 (v" + plugin.getDescription().getVersion() + ")");
            return true;
        } catch (Exception e) {
            LOGGER.warning("[MMBridge] MythicMobs 初始化失败: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            initialized = true;
            available = false;
            return false;
        }
    }

    /**
     * 获取 MythicMobManager
     */
    private static Object getMobManager() {
        try {
            if (!init()) return null;
            Method getMobManager = mythicBukkitClass.getMethod("getMobManager");
            return getMobManager.invoke(mythicBukkit);
        } catch (Exception e) {
            LOGGER.warning("[MMBridge] 获取 MobManager 失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 获取 SkillManager
     */
    private static Object getSkillManager() {
        try {
            if (!init()) return null;
            Method getSkillManager = mythicBukkitClass.getMethod("getSkillManager");
            return getSkillManager.invoke(mythicBukkit);
        } catch (Exception e) {
            LOGGER.warning("[MMBridge] 获取 SkillManager 失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 获取 MythicMobs API Helper
     */
    private static Object getAPIHelper() {
        try {
            if (!init()) return null;
            Method getAPIHelper = mythicBukkitClass.getMethod("getAPIHelper");
            return getAPIHelper.invoke(mythicBukkit);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将 Bukkit Location 转换为 MythicMobs AbstractLocation
     */
    private static Object adaptLocation(Location loc) {
        try {
            if (bukkitAdapterClass == null) return null;
            Method adapt = bukkitAdapterClass.getMethod("adapt", Location.class);
            return adapt.invoke(null, loc);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将 Bukkit Entity 转换为 MythicMobs AbstractEntity
     */
    private static Object adaptEntity(Entity entity) {
        try {
            if (bukkitAdapterClass == null) return null;
            Method adapt = bukkitAdapterClass.getMethod("adapt", Entity.class);
            return adapt.invoke(null, entity);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 生成 MythicMobs 实体
     * @param mobId MythicMobs 实体 ID
     * @param loc   生成位置
     * @return 生成的 ActiveMob 实例（Object），失败返回 null
     */
    public static Object spawnMob(String mobId, Location loc) {
        try {
            Object mobManager = getMobManager();
            if (mobManager == null) return null;

            Method getMythicMob = mobManager.getClass().getMethod("getMythicMob", String.class);
            Optional<?> mobOpt = (Optional<?>) getMythicMob.invoke(mobManager, mobId);
            if (mobOpt.isEmpty()) {
                LOGGER.warning("[MMBridge] MythicMobs 实体未找到: " + mobId);
                return null;
            }

            Object mythicMob = mobOpt.get();
            Object adaptedLoc = adaptLocation(loc);
            if (adaptedLoc == null) {
                LOGGER.warning("[MMBridge] 无法转换位置");
                return null;
            }

            Method spawn = mythicMob.getClass().getMethod("spawn",
                    Class.forName("io.lumine.mythic.api.adapters.AbstractLocation"), int.class);
            return spawn.invoke(mythicMob, adaptedLoc, 1);
        } catch (Exception e) {
            LOGGER.warning("[MMBridge] 生成实体失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 生成 MythicMobs 实体（指定等级）
     * @param mobId  MythicMobs 实体 ID
     * @param loc    生成位置
     * @param level  实体等级
     * @return 生成的 ActiveMob 实例（Object），失败返回 null
     */
    public static Object spawnMob(String mobId, Location loc, int level) {
        try {
            Object mobManager = getMobManager();
            if (mobManager == null) return null;

            Method getMythicMob = mobManager.getClass().getMethod("getMythicMob", String.class);
            Optional<?> mobOpt = (Optional<?>) getMythicMob.invoke(mobManager, mobId);
            if (mobOpt.isEmpty()) {
                LOGGER.warning("[MMBridge] MythicMobs 实体未找到: " + mobId);
                return null;
            }

            Object mythicMob = mobOpt.get();
            Object adaptedLoc = adaptLocation(loc);
            if (adaptedLoc == null) {
                LOGGER.warning("[MMBridge] 无法转换位置");
                return null;
            }

            Method spawn = mythicMob.getClass().getMethod("spawn",
                    Class.forName("io.lumine.mythic.api.adapters.AbstractLocation"), int.class);
            return spawn.invoke(mythicMob, adaptedLoc, level);
        } catch (Exception e) {
            LOGGER.warning("[MMBridge] 生成实体失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 移除 MythicMobs 实体
     * @param uuid 实体 UUID
     */
    public static void removeMob(UUID uuid) {
        try {
            Object mobManager = getMobManager();
            if (mobManager == null) return;
            Method getActiveMob = mobManager.getClass().getMethod("getActiveMob", UUID.class);
            Optional<?> activeMobOpt = (Optional<?>) getActiveMob.invoke(mobManager, uuid);
            if (activeMobOpt.isEmpty()) return;
            Object activeMob = activeMobOpt.get();
            Method remove = activeMob.getClass().getMethod("remove");
            remove.invoke(activeMob);
        } catch (Exception e) {
            LOGGER.warning("[MMBridge] 移除实体失败: " + e.getMessage());
        }
    }

    /**
     * 检查指定 MythicMobs 技能是否存在
     */
    public static boolean hasSkill(String skillName) {
        // 使用缓存的结果快速返回
        if (!isMythicMobsLoaded()) return false;
        try {
            Object skillManager = getSkillManager();
            if (skillManager == null) return false;
            Method getSkill = skillManager.getClass().getMethod("getSkill", String.class);
            Optional<?> result = (Optional<?>) getSkill.invoke(skillManager, skillName);
            return result != null && result.isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查指定 MythicMobs 实体是否存在
     */
    public static boolean hasMythicMob(String mobId) {
        if (!isMythicMobsLoaded()) return false;
        try {
            Object mobManager = getMobManager();
            if (mobManager == null) return false;
            Method getMythicMob = mobManager.getClass().getMethod("getMythicMob", String.class);
            Optional<?> result = (Optional<?>) getMythicMob.invoke(mobManager, mobId);
            return result != null && result.isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查 MythicMobs 是否已加载
     */
    public static boolean isMythicMobsLoaded() {
        init();
        return available;
    }

    /**
     * 强制重新检测 MythicMobs
     */
    public static void reload() {
        initialized = false;
        available = false;
        mythicBukkit = null;
        mythicBukkitClass = null;
        bukkitAdapterClass = null;
        init();
    }
}