package com.spillhuset.furious.utils;

import com.spillhuset.furious.Furious;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;
import org.bukkit.Registry;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Tameable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Caches commonly used registry-derived data to avoid recomputation at runtime.
 * This is intentionally conservative (no heavy reflection or CraftBukkit internals)
 * and based on public Bukkit API available on modern Paper/Spigot versions.
 */
public class RegistryCache {
    private final Furious plugin;

    private List<String> biomeKeysLower = Collections.emptyList();
    private List<String> hostileKeysLower = Collections.emptyList();
    private List<String> tameableKeysLower = Collections.emptyList();
    private List<EntityType> tameableTypes = Collections.emptyList();

    public RegistryCache(Furious plugin) {
        this.plugin = plugin;
    }

    /**
     * Build the caches. Safe to call multiple times; it will rebuild lists.
     */
    public void init() {
        try {
            buildBiomeKeys();
        } catch (Throwable ignored) {}
        try {
            buildEntityLists();
        } catch (Throwable ignored) {}
    }

    @SuppressWarnings("deprecation")
    private void buildBiomeKeys() {
        List<String> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // Prefer using enum constants if Biome is still an enum on this server
        try {
            Biome[] arr = Biome.class.getEnumConstants();
            if (arr != null && arr.length > 0) {
                for (Biome b : arr) {
                    try {
                        Key key = b.key();
                        if (key == null) continue;
                        String s = key.asString().toLowerCase(Locale.ROOT);
                        if (seen.add(s)) out.add(s);
                    } catch (Throwable ignored) {}
                }
                this.biomeKeysLower = Collections.unmodifiableList(out);
                return;
            }
        } catch (Throwable ignored) {}

        // Fallback to registry iteration on versions where Biome is registry-driven
        try {
            Iterable<Biome> reg = Registry.BIOME;
            if (reg != null) {
                for (Biome b : reg) {
                    try {
                        Key key = b.key();
                        if (key == null) continue;
                        String s = key.asString().toLowerCase(Locale.ROOT);
                        if (seen.add(s)) out.add(s);
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}

        this.biomeKeysLower = Collections.unmodifiableList(out);
    }

    private void buildEntityLists() {
        List<String> hostile = new ArrayList<>();
        List<String> tameable = new ArrayList<>();
        List<EntityType> tameTypes = new ArrayList<>();
        Set<String> seenHostile = new HashSet<>();
        Set<String> seenTame = new HashSet<>();

        for (EntityType type : EntityType.values()) {
            try {
                Class<? extends Entity> cls = type.getEntityClass();
                if (cls == null) continue;
                NamespacedKey key = null;
                try { key = type.getKey(); } catch (Throwable ignored) {}
                String keyStr = key != null ? key.asString().toLowerCase(Locale.ROOT) : type.name().toLowerCase(Locale.ROOT);

                // Hostile entities implement Enemy
                if (Enemy.class.isAssignableFrom(cls)) {
                    if (seenHostile.add(keyStr)) hostile.add(keyStr);
                }
                // Tameable check
                if (Tameable.class.isAssignableFrom(cls)) {
                    if (seenTame.add(keyStr)) {
                        tameable.add(keyStr);
                        tameTypes.add(type);
                    }
                }
            } catch (Throwable ignored) {}
        }
        this.hostileKeysLower = Collections.unmodifiableList(hostile);
        this.tameableKeysLower = Collections.unmodifiableList(tameable);
        this.tameableTypes = Collections.unmodifiableList(tameTypes);
    }

    // Accessors used by listeners/commands

    public List<String> getBiomeKeysLower() {
        return biomeKeysLower;
    }

    public List<String> getHostileKeysLower() {
        return hostileKeysLower;
    }

    public List<String> getTameableKeysLower() {
        return tameableKeysLower;
    }

    public List<EntityType> getTameableTypes() {
        return tameableTypes;
    }
}
