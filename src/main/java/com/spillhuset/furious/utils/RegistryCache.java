package com.spillhuset.furious.utils;

import com.spillhuset.furious.Furious;
import org.bukkit.block.Biome;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Tameable;

import java.util.*;

/**
 * Caches registry-derived keys to avoid repeated traversal during runtime.
 * Provides lowercase string keys for:
 * - Biomes
 * - Hostile entity types (Enemy)
 * - Tameable entity types (Tameable)
 */
public class RegistryCache {
    private final Furious plugin;

    private List<String> biomeKeysLower = Collections.emptyList();
    private List<String> hostileKeysLower = Collections.emptyList();
    private List<String> tameableKeysLower = Collections.emptyList();
    private List<EntityType> tameableTypes = Collections.emptyList();

    public RegistryCache(Furious plugin) {
        this.plugin = plugin != null ? plugin.getInstance() : null;
    }

    public void init() {
        // Build caches defensively; failures should not crash plugin startup
        try {
            List<String> biomes = new ArrayList<>();
            try {
                // Preferred modern Paper API via reflection (avoids compile-time dependency)
                Object server = plugin.getServer();
                java.lang.reflect.Method getRegAccess = server.getClass().getMethod("getRegistryAccess");
                Object access = getRegAccess.invoke(server);
                Class<?> registryKeyCls = Class.forName("io.papermc.paper.registry.RegistryKey");
                java.lang.reflect.Field biomeField = registryKeyCls.getField("BIOME");
                Object biomeKey = biomeField.get(null);
                java.lang.reflect.Method getRegistry = access.getClass().getMethod("getRegistry", biomeField.getType());
                Object reg = getRegistry.invoke(access, biomeKey);
                java.lang.reflect.Method getKeys = reg.getClass().getMethod("getKeys");
                java.util.Set<?> keys = (java.util.Set<?>) getKeys.invoke(reg);
                for (Object k : keys) {
                    try {
                        String keyStr = (String) k.getClass().getMethod("asString").invoke(k);
                        if (keyStr != null && !keyStr.isBlank()) biomes.add(keyStr.toLowerCase(Locale.ROOT));
                    } catch (Throwable ignored) {
                    }
                }
            } catch (Throwable ignored) {
            }

            // As a last resort for older APIs, attempt deprecated registry iteration
            if (biomes.isEmpty()) {
                try {
                    @SuppressWarnings({"deprecation"})
                    Iterable<Biome> legacy = org.bukkit.Registry.BIOME;
                    for (Biome b : legacy) {
                        try {
                            String key = b.key().asString();
                            if (!key.isBlank()) biomes.add(key.toLowerCase(Locale.ROOT));
                        } catch (Throwable ignored) {
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
            biomeKeysLower = Collections.unmodifiableList(biomes);
        } catch (Throwable ignored) {
        }

        try {
            List<String> hostile = new ArrayList<>();
            for (EntityType type : EntityType.values()) {
                try {
                    Class<? extends Entity> cls = type.getEntityClass();
                    if (cls != null && Enemy.class.isAssignableFrom(cls)) {
                        String key = type.getKey().asString();
                        if (!key.isBlank()) hostile.add(key.toLowerCase(Locale.ROOT));
                    }
                } catch (Throwable ignored) {
                }
            }
            Collections.sort(hostile);
            hostileKeysLower = Collections.unmodifiableList(hostile);
        } catch (Throwable ignored) {
        }

        try {
            List<String> tameable = new ArrayList<>();
            List<EntityType> tameTypes = new ArrayList<>();
            for (EntityType type : EntityType.values()) {
                try {
                    Class<? extends Entity> cls = type.getEntityClass();
                    if (cls != null && Tameable.class.isAssignableFrom(cls)) {
                        String key = type.getKey().asString();
                        if (!key.isBlank()) {
                            tameable.add(key.toLowerCase(Locale.ROOT));
                            tameTypes.add(type);
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
            Collections.sort(tameable);
            tameableKeysLower = Collections.unmodifiableList(tameable);
            tameableTypes = Collections.unmodifiableList(tameTypes);
        } catch (Throwable ignored) {
        }
    }

    public List<String> getBiomeKeysLower() {
        return biomeKeysLower;
    }

    public List<String> getHostileKeysLower() {
        return hostileKeysLower;
    }

    public List<String> getTameableKeysLower() {
        return tameableKeysLower;
    }

    // Used by TamingCommand for tab-completion
    public Collection<EntityType> getTameableTypes() {
        return tameableTypes;
    }
}
