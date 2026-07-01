package com.yourname.villagerwar.bridge;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/**
 * MythicMobs 统一调用层
 * 纯反射实现，无编译时依赖
 * 在运行时动态调用 MythicMobs API
 */
public final class MMBridge {

    private static Object mythicBukkit = null;
    private static Class<?> mythicBukkitClass = null;
    private static Class<?> bukkitAdapterClass = null;

    private MMBridge() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 初始化反射类引用
     */
    private static boolean init() {
        if (mythicBukkitClass != null) return true;

        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("MythicMobs");
            if (plugin == null || !plugin.isEnabled()) return false;

            mythicBukkit = plugin;
            mythicBukkitClass = plugin.getClass();
            bukkitAdapterClass = Class.forName("io.lumine.mythic.bukkit.BukkitAdapter");
            return true;
        } catch (ClassNotFoundException e) {
            Bukkit.getLogger().warning("[MMBridge] 无法加载 MythicMobs API 类");
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
            Bukkit.getLogger().warning("[MMBridge] 获取 MobManager 失败: " + e.getMessage());
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
            Bukkit.getLogger().warning("[MMBridge] 获取 SkillManager 失败: " + e.getMessage());
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

            // getMythicMob(String) → Optional<MythicMob>
            Method getMythicMob = mobManager.getClass().getMethod("getMythicMob", String.class);
            Optional<?> mobOpt = (Optional<?>) getMythicMob.invoke(mobManager, mobId);
            if (mobOpt.isEmpty()) {
                Bukkit.getLogger().warning("[MMBridge] MythicMobs 实体未找到: " + mobId);
                return null;
            }

            Object mythicMob = mobOpt.get();
            Object adaptedLoc = adaptLocation(loc);
            if (adaptedLoc == null) {
                Bukkit.getLogger().warning("[MMBridge] 无法转换位置");
                return null;
            }

            // MythicMob.spawn(AbstractLocation, int) → ActiveMob
            Method spawn = mythicMob.getClass().getMethod("spawn",
                    Class.forName("io.lumine.mythic.api.adapters.AbstractLocation"), int.class);
            return spawn.invoke(mythicMob, adaptedLoc, 1);

        } catch (Exception e) {
            Bukkit.getLogger().warning("[MMBridge] 生成实体 " + mobId + " 失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 释放 MythicMobs 技能
     * 优先使用 APIHelper.castSkill()，失败则使用命令回退
     * @param skillName 技能名称
     * @param caster    施法者实体
     * @param target    目标实体（可为 null）
     * @return 技能是否成功释放
     */
    public static boolean castSkill(String skillName, Entity caster, Entity target) {
        // 尝试 APIHelper 方式
        try {
            Object apiHelper = getAPIHelper();
            if (apiHelper != null) {
                Method castSkill = apiHelper.getClass().getMethod("castSkill",
                        Entity.class, String.class, Location.class,
                        Optional.class, float.class);
                castSkill.invoke(apiHelper, caster, skillName,
                        caster.getLocation(),
                        target != null ? Optional.of(target) : Optional.empty(),
                        1.0f);
                return true;
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[MMBridge] APIHelper.castSkill 失败: " + e.getMessage());
        }

        // 尝试 SkillCaster 方式
        try {
            Object adaptedCaster = adaptEntity(caster);
            if (adaptedCaster == null) return false;

            Object skillManager = getSkillManager();
            if (skillManager == null) return false;

            Method getSkill = skillManager.getClass().getMethod("getSkill", String.class);
            Optional<?> skillOpt = (Optional<?>) getSkill.invoke(skillManager, skillName);
            if (skillOpt.isEmpty()) {
                Bukkit.getLogger().warning("[MMBridge] MythicMobs 技能未找到: " + skillName);
                return false;
            }

            Object skill = skillOpt.get();

            // 尝试通过 SkillCaster 接口执行
            if (adaptedCaster instanceof java.util.concurrent.Callable) {
                // Unlikely branch, just a safety check
                return false;
            }

            // 使用反射创建 SkillMetadata
            Class<?> metaClass = Class.forName("io.lumine.mythic.core.skills.SkillMetadataImpl");
            Class<?> triggersClass = Class.forName("io.lumine.mythic.core.skills.SkillTriggers");

            Object apiTrigger;
            try {
                apiTrigger = triggersClass.getDeclaredField("API").get(null);
            } catch (Exception e) {
                // Try enum constant
                apiTrigger = triggersClass.getEnumConstants()[0];
            }

            // 尝试多种构造函数
            for (var constructor : metaClass.getConstructors()) {
                try {
                    Class<?>[] paramTypes = constructor.getParameterTypes();
                    Object[] args = new Object[paramTypes.length];
                    boolean canUse = true;

                    for (int i = 0; i < paramTypes.length; i++) {
                        Class<?> t = paramTypes[i];
                        if (t.isInstance(apiTrigger)) {
                            args[i] = apiTrigger;
                        } else if (t.isInstance(adaptedCaster)) {
                            args[i] = adaptedCaster;
                        } else if (t.isInstance(adaptedCaster) && i > 0) {
                            args[i] = adaptedCaster; // caster entity (AbstractEntity)
                        } else if (t.getName().contains("AbstractLocation")) {
                            args[i] = adaptLocation(caster.getLocation());
                        } else if (t == float.class || t == Float.class) {
                            args[i] = 1.0f;
                        } else if (t == Optional.class || t == Optional.empty().getClass()) {
                            args[i] = Optional.empty();
                        } else {
                            args[i] = null;
                        }
                    }

                    if (canUse) {
                        Object meta = constructor.newInstance(args);
                        Method execute = skill.getClass().getMethod("execute", metaClass);
                        execute.invoke(skill, meta);
                        return true;
                    }
                } catch (Exception ignored) {
                }
            }

            // 最终回退: 使用控制台命令
            return castSkillViaCommand(skillName, caster, target);

        } catch (Exception e) {
            Bukkit.getLogger().warning("[MMBridge] 技能释放失败: " + e.getMessage());
            return castSkillViaCommand(skillName, caster, target);
        }
    }

    /**
     * 通过控制台命令释放技能（最终回退方案）
     */
    private static boolean castSkillViaCommand(String skillName, Entity caster, Entity target) {
        try {
            String cmd = "mm.cast " + skillName + " @e" + caster.getUniqueId();
            if (target != null) {
                cmd += " @e" + target.getUniqueId();
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            return true;
        } catch (Exception e) {
            Bukkit.getLogger().warning("[MMBridge] 命令回退技能释放也失败了: " + e.getMessage());
            return false;
        }
    }

    /**
     * 检查 MythicMobs 是否已加载
     */
    public static boolean isMythicMobsLoaded() {
        return init();
    }

    /**
     * 检查指定 ID 的 MythicMob 是否存在
     */
    public static boolean hasMythicMob(String mobId) {
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
     * 检查指定名称的技能是否存在
     */
    public static boolean hasSkill(String skillName) {
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
}