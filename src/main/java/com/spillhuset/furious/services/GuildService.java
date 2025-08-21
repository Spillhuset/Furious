package com.spillhuset.furious.services;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.Guild;
import com.spillhuset.furious.utils.GuildRole;
import com.spillhuset.furious.utils.GuildType;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GuildService {
    private final Furious plugin;

    // Outposts: allowance per guild and centers per guild per world
    private final Map<UUID, Integer> outpostAllowance = new HashMap<>();
    // centers: worldId -> (guildId -> set of packed (x,z) centers)
    private final Map<UUID, Map<UUID, Set<Long>>> outpostCenters = new HashMap<>();

    // Enabled worlds for guild features (claims etc.)
    private Set<UUID> enabledWorlds = new HashSet<>();

    private final Map<UUID, Guild> guildsById = new HashMap<>();
    private final Map<String, UUID> guildIdByName = new HashMap<>();
    private final Map<UUID, UUID> guildIdByMember = new HashMap<>(); // player -> guildId
    // Invitations: target player -> set of guild IDs that invited them
    private final Map<UUID, Set<UUID>> pendingInvitesByTarget = new HashMap<>();
    // Invitations: guild ID -> set of target player UUIDs
    private final Map<UUID, Set<UUID>> pendingInvitesByGuild = new HashMap<>();

    // Join requests: player -> set of guild IDs the player requested to join
    private final Map<UUID, Set<UUID>> pendingJoinByPlayer = new HashMap<>();
    // Join requests: guild ID -> set of players who requested to join
    private final Map<UUID, Set<UUID>> pendingJoinByGuild = new HashMap<>();

    // Claims: worldUUID -> x -> z -> guildId
    // Stored in YAML as claims.<world>.<x>.<z> = guildId
    private final Map<UUID, Map<Integer, Map<Integer, UUID>>> claims = new HashMap<>();

    // Wooden axe selection points per player (transient)
    private final Map<UUID, org.bukkit.Location> selectionPos1 = new HashMap<>();
    private final Map<UUID, org.bukkit.Location> selectionPos2 = new HashMap<>();

    // Configurable limits
    private int maxClaimsPerGuild = 25;

    private File guildsFile;
    private FileConfiguration guildsConfig;

    public GuildService(Furious instance) {
        this.plugin = instance.getInstance();
    }

    public Collection<String> getAllGuildNames() {
        List<String> names = new ArrayList<>();
        for (Guild g : guildsById.values()) {
            if (g != null && g.getName() != null) names.add(g.getName());
        }
        return names;
    }

    public Guild getGuildById(UUID id) {
        return guildsById.get(id);
    }

    public int getMaxClaimsPerGuild() {
        return maxClaimsPerGuild;
    }

    public void load() {
            outpostAllowance.clear();
            outpostCenters.clear();
        loadEnabledWorldsFromConfig();
        // Load configuration values
        maxClaimsPerGuild = Math.max(1, plugin.getConfig().getInt("guild.max-claims-per-guild", 25));
        guildsFile = new File(plugin.getDataFolder(), "guilds.yml");
        try {
            if (!guildsFile.exists()) guildsFile.createNewFile();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed creating guilds file: " + e.getMessage());
        }
        guildsConfig = YamlConfiguration.loadConfiguration(guildsFile);

        guildsById.clear();
        guildIdByName.clear();
        guildIdByMember.clear();
        pendingInvitesByTarget.clear();
        pendingInvitesByGuild.clear();
        pendingJoinByPlayer.clear();
        pendingJoinByGuild.clear();

        ConfigurationSection section = guildsConfig.getConfigurationSection("guilds");
        if (section != null) {
            for (String idStr : section.getKeys(false)) {
                try {
                    UUID id = UUID.fromString(idStr);
                    ConfigurationSection g = section.getConfigurationSection(idStr);
                    if (g == null) continue;
                    String name = g.getString("name");
                    String typeStr = g.getString("type", GuildType.FREE.name());
                    GuildType type = GuildType.valueOf(typeStr);
                    String ownerStr = g.getString("owner", null);
                    UUID owner = ownerStr == null ? null : UUID.fromString(ownerStr);

                    Guild guild = new Guild(id, name, type, owner);
                    // open flag (default false -> invitedOnly)
                    boolean open = g.getBoolean("open", false);
                    guild.setOpen(open);

                    // Outposts allowance and centers
                    int allowed = g.getInt("outposts.allowed", 0);
                    if (allowed > 0) outpostAllowance.put(id, allowed);
                    ConfigurationSection centersSec = g.getConfigurationSection("outposts.centers");
                    if (centersSec != null) {
                        for (String worldKey : centersSec.getKeys(false)) {
                            try {
                                UUID worldId = UUID.fromString(worldKey);
                                ConfigurationSection xs = centersSec.getConfigurationSection(worldKey);
                                if (xs == null) continue;
                                for (String xKey : xs.getKeys(false)) {
                                    int x = Integer.parseInt(xKey);
                                    ConfigurationSection zs = xs.getConfigurationSection(xKey);
                                    if (zs == null) continue;
                                    for (String zKey : zs.getKeys(false)) {
                                        int z = Integer.parseInt(zKey);
                                        if (zs.getBoolean(zKey, false)) {
                                            outpostCenters
                                                    .computeIfAbsent(worldId, w -> new HashMap<>())
                                                    .computeIfAbsent(id, gset -> new HashSet<>())
                                                    .add(key(x, z));
                                        }
                                    }
                                }
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                    }

                    ConfigurationSection members = g.getConfigurationSection("members");
                    if (members != null) {
                        for (String memStr : members.getKeys(false)) {
                            UUID member = UUID.fromString(memStr);
                            GuildRole role = GuildRole.valueOf(members.getString(memStr, GuildRole.MEMBER.name()));
                            guild.getMembers().put(member, role);
                            guildIdByMember.put(member, id);
                        }
                    }

                    guildsById.put(id, guild);
                    if (name != null) guildIdByName.put(name.toLowerCase(), id);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed loading guild entry: " + idStr + " -> " + e.getMessage());
                }
            }
        }

        // Load claims
        ConfigurationSection claimsSection = guildsConfig.getConfigurationSection("claims");
        claims.clear();
        if (claimsSection != null) {
            for (String worldKey : claimsSection.getKeys(false)) {
                try {
                    UUID worldId = UUID.fromString(worldKey);
                    ConfigurationSection xs = claimsSection.getConfigurationSection(worldKey);
                    if (xs == null) continue;
                    for (String xKey : xs.getKeys(false)) {
                        int x = Integer.parseInt(xKey);
                        ConfigurationSection zs = xs.getConfigurationSection(xKey);
                        if (zs == null) continue;
                        for (String zKey : zs.getKeys(false)) {
                            int z = Integer.parseInt(zKey);
                            String gidStr = zs.getString(zKey, null);
                            if (gidStr == null) continue;
                            try {
                                UUID gid = UUID.fromString(gidStr);
                                claims.computeIfAbsent(worldId, w -> new HashMap<>())
                                        .computeIfAbsent(x, xx -> new HashMap<>())
                                        .put(z, gid);
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        // Ensure default unmanned guilds
        ensureDefaultGuild("S_A_F_E", GuildType.SAFE);
        ensureDefaultGuild("WILDLIFE", GuildType.FREE);
        ensureDefaultGuild("WARZONE", GuildType.WAR);
    }

    private void loadEnabledWorldsFromConfig() {
        java.util.List<String> ids = plugin.getConfig().getStringList("guild.enabled-worlds");
        enabledWorlds = new HashSet<>();
        for (String s : ids) {
            try {
                enabledWorlds.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save() {
        FileConfiguration out = new YamlConfiguration();
        ConfigurationSection root = out.createSection("guilds");
        for (Map.Entry<UUID, Guild> entry : guildsById.entrySet()) {
            UUID id = entry.getKey();
            Guild g = entry.getValue();
            ConfigurationSection gs = root.createSection(id.toString());
            gs.set("name", g.getName());
            gs.set("type", g.getType().name());
            if (g.getOwner() != null) {
                gs.set("owner", g.getOwner().toString());
            } else {
                gs.set("owner", null);
            }
            gs.set("open", g.isOpen());
            ConfigurationSection mem = gs.createSection("members");
            for (Map.Entry<UUID, GuildRole> me : g.getMembers().entrySet()) {
                mem.set(me.getKey().toString(), me.getValue().name());
            }
        }
        // Save outposts allowance and centers per guild
                for (Map.Entry<UUID, Guild> entry : guildsById.entrySet()) {
                    UUID gid = entry.getKey();
                    ConfigurationSection gs = out.getConfigurationSection("guilds").getConfigurationSection(gid.toString());
                    if (gs != null) {
                        int allowed = outpostAllowance.getOrDefault(gid, 0);
                        if (allowed > 0) gs.set("outposts.allowed", allowed);
                        // centers
                        Map<UUID, Set<Long>> byWorld = new HashMap<>();
                        for (Map.Entry<UUID, Map<UUID, Set<Long>>> we : outpostCenters.entrySet()) {
                            UUID worldId = we.getKey();
                            Set<Long> centers = we.getValue().get(gid);
                            if (centers == null || centers.isEmpty()) continue;
                            byWorld.put(worldId, centers);
                        }
                        if (!byWorld.isEmpty()) {
                            ConfigurationSection centersRoot = gs.createSection("outposts.centers");
                            for (Map.Entry<UUID, Set<Long>> e : byWorld.entrySet()) {
                                UUID worldId = e.getKey();
                                ConfigurationSection xs = centersRoot.createSection(worldId.toString());
                                Map<Integer, Set<Integer>> grouped = new HashMap<>();
                                for (long k : e.getValue()) {
                                    int x = kx(k); int z = kz(k);
                                    grouped.computeIfAbsent(x, xx -> new HashSet<>()).add(z);
                                }
                                for (Map.Entry<Integer, Set<Integer>> ge : grouped.entrySet()) {
                                    ConfigurationSection zs = xs.createSection(String.valueOf(ge.getKey()));
                                    for (int z : ge.getValue()) {
                                        zs.set(String.valueOf(z), true);
                                    }
                                }
                            }
                        }
                    }
                }

                // Save claims
        ConfigurationSection claimsRoot = out.createSection("claims");
        for (Map.Entry<UUID, Map<Integer, Map<Integer, UUID>>> we : claims.entrySet()) {
            UUID world = we.getKey();
            ConfigurationSection xs = claimsRoot.createSection(world.toString());
            for (Map.Entry<Integer, Map<Integer, UUID>> xe : we.getValue().entrySet()) {
                ConfigurationSection zs = xs.createSection(String.valueOf(xe.getKey()));
                for (Map.Entry<Integer, UUID> ze : xe.getValue().entrySet()) {
                    zs.set(String.valueOf(ze.getKey()), ze.getValue().toString());
                }
            }
        }
        try {
            out.save(guildsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed saving guilds: " + e.getMessage());
        }
    }

    private void ensureDefaultGuild(String name, GuildType type) {
        if (name == null || type == null) return;
        if (!guildIdByName.containsKey(name.toLowerCase())) {
            // Create as unmanned
            createGuild(null, name, type);
        }
    }

    public boolean createGuild(UUID owner, String name, GuildType type) {
        if (name == null || name.isBlank()) return false;
        if (guildIdByName.containsKey(name.toLowerCase())) return false;
        if (owner != null && guildIdByMember.containsKey(owner)) return false; // already in a guild
        UUID id = UUID.randomUUID();
        Guild guild = new Guild(id, name, type, owner);
        if (owner != null) {
            guild.getMembers().put(owner, GuildRole.ADMIN);
            guildIdByMember.put(owner, id);
        }
        guildsById.put(id, guild);
        guildIdByName.put(name.toLowerCase(), id);
        save();
        return true;
    }

    public Guild getGuildByName(String name) {
        UUID id = guildIdByName.get(name.toLowerCase());
        return id == null ? null : guildsById.get(id);
    }

    public UUID getGuildIdForMember(UUID player) {
        return guildIdByMember.get(player);
    }

    public boolean renameGuildByMember(UUID member, String newName) {
        if (newName == null || newName.isBlank()) return false;
        UUID gid = guildIdByMember.get(member);
        if (gid == null) return false;
        if (guildIdByName.containsKey(newName.toLowerCase())) return false;
        Guild guild = guildsById.get(gid);
        if (guild == null) return false;
        // Require member to have ADMIN role (owner is set as ADMIN upon creation)
        GuildRole role = guild.getMembers().get(member);
        if (role != GuildRole.ADMIN) return false;
        String oldName = guild.getName();
        if (oldName != null) guildIdByName.remove(oldName.toLowerCase());
        guild.setName(newName);
        guildIdByName.put(newName.toLowerCase(), gid);
        save();
        return true;
    }

    public boolean renameGuildByName(String oldName, String newName) {
        if (oldName == null || newName == null) return false;
        if (newName.isBlank()) return false;
        UUID gid = guildIdByName.get(oldName.toLowerCase());
        if (gid == null) return false;
        if (guildIdByName.containsKey(newName.toLowerCase())) return false;
        Guild guild = guildsById.get(gid);
        if (guild == null) return false;
        guildIdByName.remove(oldName.toLowerCase());
        guild.setName(newName);
        guildIdByName.put(newName.toLowerCase(), gid);
        save();
        return true;
    }

    private void removeGuildInternal(UUID gid) {
        Guild guild = guildsById.get(gid);
        if (guild == null) return;
        String oldName = guild.getName();
        if (oldName != null) {
            guildIdByName.remove(oldName.toLowerCase());
        }
        // Clean up member index
        for (UUID member : new ArrayList<>(guild.getMembers().keySet())) {
            guildIdByMember.remove(member);
        }
        // Clean up pending invites related to this guild
        Set<UUID> targets = pendingInvitesByGuild.remove(gid);
        if (targets != null) {
            for (UUID t : targets) {
                Set<UUID> set = pendingInvitesByTarget.get(t);
                if (set != null) {
                    set.remove(gid);
                    if (set.isEmpty()) pendingInvitesByTarget.remove(t);
                }
            }
        }
        // Clean up pending join requests related to this guild
        Set<UUID> reqPlayers = pendingJoinByGuild.remove(gid);
        if (reqPlayers != null) {
            for (UUID p : reqPlayers) {
                Set<UUID> gs = pendingJoinByPlayer.get(p);
                if (gs != null) {
                    gs.remove(gid);
                    if (gs.isEmpty()) pendingJoinByPlayer.remove(p);
                }
            }
        }
        // Purge any land claims owned by this guild across all worlds
        for (Map<Integer, Map<Integer, UUID>> xs : new ArrayList<>(claims.values())) {
            for (Map.Entry<Integer, Map<Integer, UUID>> xe : new ArrayList<>(xs.entrySet())) {
                Map<Integer, UUID> zs = xe.getValue();
                zs.entrySet().removeIf(e -> gid.equals(e.getValue()));
                if (zs.isEmpty()) xs.remove(xe.getKey());
            }
        }
        // Remove empty worlds from claims
        claims.entrySet().removeIf(e -> e.getValue() == null || e.getValue().isEmpty());
        guildsById.remove(gid);
        save();
    }

    public boolean deleteGuildByMember(UUID member) {
        if (member == null) return false;
        UUID gid = guildIdByMember.get(member);
        if (gid == null) return false;
        Guild guild = guildsById.get(gid);
        if (guild == null) return false;
        GuildRole role = guild.getMembers().get(member);
        if (role != GuildRole.ADMIN) return false;
        removeGuildInternal(gid);
        return true;
    }

    public boolean deleteGuildByName(String name) {
        if (name == null || name.isBlank()) return false;
        UUID gid = guildIdByName.get(name.toLowerCase());
        if (gid == null) return false;
        removeGuildInternal(gid);
        return true;
    }

    // ===== Open flag APIs =====
    public boolean setOpenByMember(UUID actor, boolean open) {
        if (actor == null) return false;
        UUID gid = guildIdByMember.get(actor);
        if (gid == null) return false;
        Guild guild = guildsById.get(gid);
        if (guild == null) return false;
        GuildRole role = guild.getMembers().get(actor);
        if (role != GuildRole.ADMIN) return false;
        guild.setOpen(open);
        save();
        return true;
    }

    public boolean setOpenByName(String guildName, boolean open) {
        if (guildName == null || guildName.isBlank()) return false;
        UUID gid = guildIdByName.get(guildName.toLowerCase());
        if (gid == null) return false;
        Guild guild = guildsById.get(gid);
        if (guild == null) return false;
        guild.setOpen(open);
        save();
        return true;
    }

    public boolean isGuildOpen(UUID guildId) {
        Guild g = guildsById.get(guildId);
        return g != null && g.isOpen();
    }

    // ===== Join APIs =====
    public enum JoinResult {
        SUCCESS,
        GUILD_NOT_FOUND,
        ALREADY_IN_GUILD,
        ALREADY_REQUESTED,
        REQUESTED
    }

    public JoinResult joinOrRequest(UUID player, String guildName) {
        if (player == null || guildName == null || guildName.isBlank()) return JoinResult.GUILD_NOT_FOUND;
        if (guildIdByMember.containsKey(player)) return JoinResult.ALREADY_IN_GUILD;
        UUID gid = guildIdByName.get(guildName.toLowerCase());
        if (gid == null) return JoinResult.GUILD_NOT_FOUND;
        Guild guild = guildsById.get(gid);
        if (guild == null) return JoinResult.GUILD_NOT_FOUND;
        if (guild.isOpen()) {
            // join directly
            guild.getMembers().put(player, GuildRole.MEMBER);
            guildIdByMember.put(player, gid);
            // clear any invites and join requests for this player
            Set<UUID> others = pendingInvitesByTarget.remove(player);
            if (others != null) {
                for (UUID otherG : others) {
                    Set<UUID> ts = pendingInvitesByGuild.get(otherG);
                    if (ts != null) {
                        ts.remove(player);
                        if (ts.isEmpty()) pendingInvitesByGuild.remove(otherG);
                    }
                }
            }
            Set<UUID> reqs = pendingJoinByPlayer.remove(player);
            if (reqs != null) {
                for (UUID rg : reqs) {
                    Set<UUID> ps = pendingJoinByGuild.get(rg);
                    if (ps != null) {
                        ps.remove(player);
                        if (ps.isEmpty()) pendingJoinByGuild.remove(rg);
                    }
                }
            }
            save();
            return JoinResult.SUCCESS;
        } else {
            // invitedOnly -> create request
            Set<UUID> reqs = pendingJoinByPlayer.computeIfAbsent(player, k -> new HashSet<>());
            if (reqs.contains(gid)) return JoinResult.ALREADY_REQUESTED;
            reqs.add(gid);
            pendingJoinByGuild.computeIfAbsent(gid, k -> new HashSet<>()).add(player);
            return JoinResult.REQUESTED;
        }
    }

    public boolean hasJoinRequest(UUID guildId, UUID player) {
        Set<UUID> ps = pendingJoinByGuild.get(guildId);
        return ps != null && ps.contains(player);
    }

    public Set<UUID> getJoinRequestsForGuild(UUID guildId) {
        return pendingJoinByGuild.getOrDefault(guildId, Collections.emptySet());
    }

    public boolean acceptJoinRequestByAdmin(UUID admin, UUID target) {
        if (admin == null || target == null) return false;
        UUID gid = guildIdByMember.get(admin);
        if (gid == null) return false;
        Guild guild = guildsById.get(gid);
        if (guild == null) return false;
        GuildRole role = guild.getMembers().get(admin);
        if (role != GuildRole.ADMIN) return false;
        // ensure target not already in a guild
        if (guildIdByMember.containsKey(target)) return false;
        // ensure request exists
        if (!hasJoinRequest(gid, target)) return false;
        // add member
        guild.getMembers().put(target, GuildRole.MEMBER);
        guildIdByMember.put(target, gid);
        // clear this request and any others from the player
        Set<UUID> reqs = pendingJoinByPlayer.remove(target);
        if (reqs != null) {
            for (UUID rg : reqs) {
                Set<UUID> ps = pendingJoinByGuild.get(rg);
                if (ps != null) {
                    ps.remove(target);
                    if (ps.isEmpty()) pendingJoinByGuild.remove(rg);
                }
            }
        }
        // Also clear any invites for this player, since they joined a guild
        Set<UUID> invs = pendingInvitesByTarget.remove(target);
        if (invs != null) {
            for (UUID ig : invs) {
                Set<UUID> ts = pendingInvitesByGuild.get(ig);
                if (ts != null) {
                    ts.remove(target);
                    if (ts.isEmpty()) pendingInvitesByGuild.remove(ig);
                }
            }
        }
        save();
        return true;
    }

    public boolean declineJoinRequestByAdmin(UUID admin, UUID target) {
        if (admin == null || target == null) return false;
        UUID gid = guildIdByMember.get(admin);
        if (gid == null) return false;
        Guild guild = guildsById.get(gid);
        if (guild == null) return false;
        GuildRole role = guild.getMembers().get(admin);
        if (role != GuildRole.ADMIN) return false;
        if (!hasJoinRequest(gid, target)) return false;
        // remove request
        Set<UUID> ps = pendingJoinByGuild.get(gid);
        if (ps != null) {
            ps.remove(target);
            if (ps.isEmpty()) pendingJoinByGuild.remove(gid);
        }
        Set<UUID> gs = pendingJoinByPlayer.get(target);
        if (gs != null) {
            gs.remove(gid);
            if (gs.isEmpty()) pendingJoinByPlayer.remove(target);
        }
        return true;
    }

    public enum InviteResult {
        SUCCESS,
        NOT_IN_GUILD,
        NOT_ADMIN,
        GUILD_NOT_OWNED_OR_UNMANNED,
        TARGET_INVALID,
        TARGET_ALREADY_IN_GUILD,
        ALREADY_INVITED
    }

    public enum KickResult {
        SUCCESS,
        ACTOR_NOT_IN_GUILD,
        ACTOR_NOT_ADMIN,
        GUILD_NOT_FOUND,
        TARGET_INVALID,
        TARGET_NOT_IN_GUILD,
        TARGET_NOT_IN_SAME_GUILD,
        TARGET_IS_ADMIN,
        CANNOT_KICK_SELF
    }

    public enum RoleChangeResult {
        SUCCESS,
        ACTOR_NOT_IN_GUILD,
        ACTOR_NOT_ADMIN,
        GUILD_NOT_FOUND,
        TARGET_INVALID,
        TARGET_NOT_IN_GUILD,
        TARGET_NOT_IN_SAME_GUILD,
        TARGET_ALREADY_AT_MAX,
        TARGET_ALREADY_AT_MIN,
        CANNOT_CHANGE_SELF,
        CANNOT_CHANGE_ADMIN
    }

    public boolean isInvited(UUID guildId, UUID target) {
        Set<UUID> set = pendingInvitesByTarget.get(target);
        return set != null && set.contains(guildId);
    }

    public InviteResult invite(UUID inviter, UUID target) {
        if (inviter == null || target == null || inviter.equals(target)) return InviteResult.TARGET_INVALID;
        UUID gid = guildIdByMember.get(inviter);
        if (gid == null) return InviteResult.NOT_IN_GUILD;
        Guild guild = guildsById.get(gid);
        if (guild == null) return InviteResult.NOT_IN_GUILD;
        if (guild.getType() != GuildType.OWNED || guild.getOwner() == null) {
            return InviteResult.GUILD_NOT_OWNED_OR_UNMANNED;
        }
        GuildRole role = guild.getMembers().get(inviter);
        if (role != GuildRole.ADMIN) return InviteResult.NOT_ADMIN;
        // Target cannot already be in a guild
        if (guildIdByMember.containsKey(target)) return InviteResult.TARGET_ALREADY_IN_GUILD;
        // Avoid duplicate invites
        if (isInvited(gid, target)) return InviteResult.ALREADY_INVITED;
        // Store invite
        pendingInvitesByTarget.computeIfAbsent(target, k -> new HashSet<>()).add(gid);
        pendingInvitesByGuild.computeIfAbsent(gid, k -> new HashSet<>()).add(target);
        return InviteResult.SUCCESS;
    }

    public List<String> getInvitingGuildNamesFor(UUID target) {
        Set<UUID> set = pendingInvitesByTarget.getOrDefault(target, Collections.emptySet());
        List<String> names = new ArrayList<>();
        for (UUID gid : set) {
            Guild g = guildsById.get(gid);
            if (g != null && g.getName() != null) names.add(g.getName());
        }
        return names;
    }

    private void clearInvite(UUID gid, UUID target) {
        Set<UUID> ts = pendingInvitesByGuild.get(gid);
        if (ts != null) {
            ts.remove(target);
            if (ts.isEmpty()) pendingInvitesByGuild.remove(gid);
        }
        Set<UUID> gs = pendingInvitesByTarget.get(target);
        if (gs != null) {
            gs.remove(gid);
            if (gs.isEmpty()) pendingInvitesByTarget.remove(target);
        }
    }

    public boolean acceptInvite(UUID target, String guildNameOrNull) {
        if (target == null) return false;
        if (guildIdByMember.containsKey(target)) return false; // already in a guild
        Set<UUID> invites = pendingInvitesByTarget.get(target);
        if (invites == null || invites.isEmpty()) return false;
        UUID chosen = null;
        if (guildNameOrNull != null) {
            UUID gid = guildIdByName.get(guildNameOrNull.toLowerCase());
            if (gid != null && invites.contains(gid)) chosen = gid;
        } else if (invites.size() == 1) {
            chosen = invites.iterator().next();
        } else {
            return false; // ambiguous without name
        }
        if (chosen == null) return false;
        Guild guild = guildsById.get(chosen);
        if (guild == null) {
            clearInvite(chosen, target);
            return false;
        }
        // Only OWNED guilds with owner can accept members (should always be true from invite())
        if (guild.getType() != GuildType.OWNED || guild.getOwner() == null) {
            clearInvite(chosen, target);
            return false;
        }
        // Add as MEMBER
        guild.getMembers().put(target, GuildRole.MEMBER);
        guildIdByMember.put(target, chosen);
        // Clear all invites for this target now that they joined a guild
        Set<UUID> others = pendingInvitesByTarget.remove(target);
        if (others != null) {
            for (UUID otherG : others) {
                Set<UUID> ts = pendingInvitesByGuild.get(otherG);
                if (ts != null) {
                    ts.remove(target);
                    if (ts.isEmpty()) pendingInvitesByGuild.remove(otherG);
                }
            }
        }
        save();
        return true;
    }

    public boolean declineInvite(UUID target, String guildNameOrNull) {
        if (target == null) return false;
        Set<UUID> invites = pendingInvitesByTarget.get(target);
        if (invites == null || invites.isEmpty()) return false;
        UUID chosen = null;
        if (guildNameOrNull != null) {
            UUID gid = guildIdByName.get(guildNameOrNull.toLowerCase());
            if (gid != null && invites.contains(gid)) chosen = gid;
        } else if (invites.size() == 1) {
            chosen = invites.iterator().next();
        } else {
            return false; // ambiguous without name
        }
        if (chosen == null) return false;
        clearInvite(chosen, target);
        return true;
    }

    public KickResult kick(UUID actor, UUID target, String reason) {
        if (actor == null || target == null) return KickResult.TARGET_INVALID;
        if (actor.equals(target)) return KickResult.CANNOT_KICK_SELF;
        UUID actorGid = guildIdByMember.get(actor);
        if (actorGid == null) return KickResult.ACTOR_NOT_IN_GUILD;
        Guild guild = guildsById.get(actorGid);
        if (guild == null) return KickResult.GUILD_NOT_FOUND;
        GuildRole actorRole = guild.getMembers().get(actor);
        if (actorRole != GuildRole.ADMIN) return KickResult.ACTOR_NOT_ADMIN;
        UUID targetGid = guildIdByMember.get(target);
        if (targetGid == null) return KickResult.TARGET_NOT_IN_GUILD;
        if (!actorGid.equals(targetGid)) return KickResult.TARGET_NOT_IN_SAME_GUILD;
        GuildRole targetRole = guild.getMembers().get(target);
        if (targetRole == GuildRole.ADMIN) return KickResult.TARGET_IS_ADMIN;
        // Remove member
        guild.getMembers().remove(target);
        guildIdByMember.remove(target);
        save();
        return KickResult.SUCCESS;
    }

    public KickResult kickByName(String guildName, UUID target, String reason) {
        if (guildName == null || guildName.isBlank() || target == null) return KickResult.TARGET_INVALID;
        UUID gid = guildIdByName.get(guildName.toLowerCase());
        if (gid == null) return KickResult.GUILD_NOT_FOUND;
        Guild guild = guildsById.get(gid);
        if (guild == null) return KickResult.GUILD_NOT_FOUND;
        UUID targetGid = guildIdByMember.get(target);
        if (targetGid == null) return KickResult.TARGET_NOT_IN_GUILD;
        if (!gid.equals(targetGid)) return KickResult.TARGET_NOT_IN_SAME_GUILD;
        GuildRole targetRole = guild.getMembers().get(target);
        if (targetRole == GuildRole.ADMIN) return KickResult.TARGET_IS_ADMIN;
        guild.getMembers().remove(target);
        guildIdByMember.remove(target);
        save();
        return KickResult.SUCCESS;
    }

    private GuildRole nextRoleUp(GuildRole role) {
        if (role == null) return null;
        return switch (role) {
            case MEMBER -> GuildRole.MODERATOR;
            case MODERATOR -> GuildRole.ADMIN;
            case ADMIN -> GuildRole.ADMIN;
        };
    }

    private GuildRole nextRoleDown(GuildRole role) {
        if (role == null) return null;
        return switch (role) {
            case ADMIN -> GuildRole.MODERATOR;
            case MODERATOR -> GuildRole.MEMBER;
            case MEMBER -> GuildRole.MEMBER;
        };
    }

    public RoleChangeResult promote(UUID actor, UUID target) {
        if (actor == null || target == null) return RoleChangeResult.TARGET_INVALID;
        if (actor.equals(target)) return RoleChangeResult.CANNOT_CHANGE_SELF;
        UUID actorGid = guildIdByMember.get(actor);
        if (actorGid == null) return RoleChangeResult.ACTOR_NOT_IN_GUILD;
        Guild guild = guildsById.get(actorGid);
        if (guild == null) return RoleChangeResult.GUILD_NOT_FOUND;
        GuildRole actorRole = guild.getMembers().get(actor);
        if (actorRole != GuildRole.ADMIN) return RoleChangeResult.ACTOR_NOT_ADMIN;
        UUID targetGid = guildIdByMember.get(target);
        if (targetGid == null) return RoleChangeResult.TARGET_NOT_IN_GUILD;
        if (!actorGid.equals(targetGid)) return RoleChangeResult.TARGET_NOT_IN_SAME_GUILD;
        GuildRole targetRole = guild.getMembers().get(target);
        if (targetRole == GuildRole.ADMIN) return RoleChangeResult.TARGET_ALREADY_AT_MAX;
        GuildRole newRole = nextRoleUp(targetRole);
        if (newRole == null || newRole == targetRole) return RoleChangeResult.TARGET_ALREADY_AT_MAX;
        guild.getMembers().put(target, newRole);
        save();
        return RoleChangeResult.SUCCESS;
    }

    public RoleChangeResult promoteByName(String guildName, UUID target) {
        if (guildName == null || guildName.isBlank() || target == null) return RoleChangeResult.TARGET_INVALID;
        UUID gid = guildIdByName.get(guildName.toLowerCase());
        if (gid == null) return RoleChangeResult.GUILD_NOT_FOUND;
        Guild guild = guildsById.get(gid);
        if (guild == null) return RoleChangeResult.GUILD_NOT_FOUND;
        UUID targetGid = guildIdByMember.get(target);
        if (targetGid == null) return RoleChangeResult.TARGET_NOT_IN_GUILD;
        if (!gid.equals(targetGid)) return RoleChangeResult.TARGET_NOT_IN_SAME_GUILD;
        GuildRole targetRole = guild.getMembers().get(target);
        if (targetRole == GuildRole.ADMIN) return RoleChangeResult.TARGET_ALREADY_AT_MAX;
        GuildRole newRole = nextRoleUp(targetRole);
        if (newRole == null || newRole == targetRole) return RoleChangeResult.TARGET_ALREADY_AT_MAX;
        guild.getMembers().put(target, newRole);
        save();
        return RoleChangeResult.SUCCESS;
    }

    public RoleChangeResult demote(UUID actor, UUID target) {
        if (actor == null || target == null) return RoleChangeResult.TARGET_INVALID;
        if (actor.equals(target)) return RoleChangeResult.CANNOT_CHANGE_SELF;
        UUID actorGid = guildIdByMember.get(actor);
        if (actorGid == null) return RoleChangeResult.ACTOR_NOT_IN_GUILD;
        Guild guild = guildsById.get(actorGid);
        if (guild == null) return RoleChangeResult.GUILD_NOT_FOUND;
        GuildRole actorRole = guild.getMembers().get(actor);
        if (actorRole != GuildRole.ADMIN) return RoleChangeResult.ACTOR_NOT_ADMIN;
        UUID targetGid = guildIdByMember.get(target);
        if (targetGid == null) return RoleChangeResult.TARGET_NOT_IN_GUILD;
        if (!actorGid.equals(targetGid)) return RoleChangeResult.TARGET_NOT_IN_SAME_GUILD;
        GuildRole targetRole = guild.getMembers().get(target);
        if (targetRole == GuildRole.ADMIN) return RoleChangeResult.CANNOT_CHANGE_ADMIN;
        if (targetRole == GuildRole.MEMBER) return RoleChangeResult.TARGET_ALREADY_AT_MIN;
        GuildRole newRole = nextRoleDown(targetRole);
        if (newRole == null || newRole == targetRole) return RoleChangeResult.TARGET_ALREADY_AT_MIN;
        guild.getMembers().put(target, newRole);
        save();
        return RoleChangeResult.SUCCESS;
    }

    public RoleChangeResult demoteByName(String guildName, UUID target) {
        if (guildName == null || guildName.isBlank() || target == null) return RoleChangeResult.TARGET_INVALID;
        UUID gid = guildIdByName.get(guildName.toLowerCase());
        if (gid == null) return RoleChangeResult.GUILD_NOT_FOUND;
        Guild guild = guildsById.get(gid);
        if (guild == null) return RoleChangeResult.GUILD_NOT_FOUND;
        UUID targetGid = guildIdByMember.get(target);
        if (targetGid == null) return RoleChangeResult.TARGET_NOT_IN_GUILD;
        if (!gid.equals(targetGid)) return RoleChangeResult.TARGET_NOT_IN_SAME_GUILD;
        GuildRole targetRole = guild.getMembers().get(target);
        if (targetRole == GuildRole.ADMIN) return RoleChangeResult.CANNOT_CHANGE_ADMIN;
        if (targetRole == GuildRole.MEMBER) return RoleChangeResult.TARGET_ALREADY_AT_MIN;
        GuildRole newRole = nextRoleDown(targetRole);
        if (newRole == null || newRole == targetRole) return RoleChangeResult.TARGET_ALREADY_AT_MIN;
        guild.getMembers().put(target, newRole);
        save();
        return RoleChangeResult.SUCCESS;
    }

    // ===================== Claims API =====================
    public enum ClaimResult {
        SUCCESS,
        NOT_PLAYER_IN_GUILD,
        NOT_ADMIN,
        ALREADY_CLAIMED_BY_OTHER,
        INVALID_SELECTION,
        GUILD_NOT_FOUND,
        MAX_LIMIT_REACHED,
        NOT_CONNECTED,
        TOO_CLOSE_TO_OTHERS,
        OUTPOSTS_LIMIT_REACHED,
        OUTPOST_RANGE_EXCEEDED,
        WORLD_DISABLED
    }

    public Location getSelectionPos1(UUID player) {
        return selectionPos1.get(player);
    }

    public Location getSelectionPos2(UUID player) {
        return selectionPos2.get(player);
    }

    public void setSelectionPos1(UUID player, Location loc) {
        if (loc != null) selectionPos1.put(player, loc);
    }

    public void setSelectionPos2(UUID player, Location loc) {
        if (loc != null) selectionPos2.put(player, loc);
    }

    public UUID getClaimOwner(java.util.UUID worldId, int cx, int cz) {
        Map<Integer, Map<Integer, UUID>> xs = claims.get(worldId);
        if (xs == null) return null;
        Map<Integer, UUID> zs = xs.get(cx);
        if (zs == null) return null;
        return zs.get(cz);
    }


    private void setClaim(java.util.UUID worldId, int cx, int cz, java.util.UUID guildId) {
        claims.computeIfAbsent(worldId, w -> new HashMap<>())
                .computeIfAbsent(cx, x -> new HashMap<>())
                .put(cz, guildId);
    }

    private int countClaimsForGuild(UUID guildId) {
        int count = 0;
        for (Map<Integer, Map<Integer, UUID>> xs : claims.values()) {
            for (Map<Integer, UUID> zs : xs.values()) {
                for (UUID gid : zs.values()) {
                    if (guildId.equals(gid)) count++;
                }
            }
        }
        return count;
    }

    private int countClaimsForGuildInWorld(UUID guildId, UUID worldId) {
        Map<Integer, Map<Integer, UUID>> xs = claims.get(worldId);
        if (xs == null) return 0;
        int count = 0;
        for (Map<Integer, UUID> zs : xs.values()) {
            for (UUID gid : zs.values()) {
                if (guildId.equals(gid)) count++;
            }
        }
        return count;
    }

    private boolean hasAdjacentClaim(UUID guildId, UUID worldId, int cx, int cz) {
        return guildId.equals(getClaimOwner(worldId, cx + 1, cz))
                || guildId.equals(getClaimOwner(worldId, cx - 1, cz))
                || guildId.equals(getClaimOwner(worldId, cx, cz + 1))
                || guildId.equals(getClaimOwner(worldId, cx, cz - 1));
    }

    // Outpost helpers
        public int getOutpostAllowance(UUID guildId) { return outpostAllowance.getOrDefault(guildId, 0); }
        public int getOutpostCount(UUID guildId) {
            int count = 0;
            for (Map<UUID, Set<Long>> byGuild : outpostCenters.values()) {
                Set<Long> set = byGuild.get(guildId);
                if (set != null) count += set.size();
            }
            return count;
        }
        public void addOutpostAllowance(UUID guildId, int amount) {
            if (guildId == null || amount <= 0) return;
            outpostAllowance.put(guildId, getOutpostAllowance(guildId) + amount);
            save();
        }
        private void addOutpostCenter(UUID guildId, UUID worldId, int x, int z) {
            outpostCenters.computeIfAbsent(worldId, w -> new HashMap<>())
                    .computeIfAbsent(guildId, g -> new HashSet<>())
                    .add(key(x, z));
        }
        private boolean hasOutpostCenterAt(UUID guildId, UUID worldId, int x, int z) {
            Map<UUID, Set<Long>> byGuild = outpostCenters.get(worldId);
            if (byGuild == null) return false;
            Set<Long> set = byGuild.get(guildId);
            return set != null && set.contains(key(x, z));
        }
        private boolean isWithinOutpostRange(UUID guildId, UUID worldId, int x, int z, int radius) {
            Map<UUID, Set<Long>> byGuild = outpostCenters.get(worldId);
            if (byGuild == null) return false;
            Set<Long> centers = byGuild.get(guildId);
            if (centers == null || centers.isEmpty()) return false;
            int r = Math.max(0, radius);
            for (long k : centers) {
                int cx = kx(k), cz = kz(k);
                int dx = Math.abs(cx - x), dz = Math.abs(cz - z);
                if (Math.max(dx, dz) <= r) return true;
            }
            return false;
        }

        // Returns true if any chunk owned by a different guild exists within (minDistance-1) chunk radius (Chebyshev)
    private boolean isTooCloseToOtherGuilds(UUID worldId, int cx, int cz, UUID myGuildId, int minDistance) {
        int radius = Math.max(0, minDistance - 1);
        if (radius == 0) return false;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                UUID owner = getClaimOwner(worldId, cx + dx, cz + dz);
                if (owner != null && !owner.equals(myGuildId)) {
                    return true;
                }
            }
        }
        return false;
    }

    public ClaimResult claimChunk(java.util.UUID actor, org.bukkit.Chunk chunk) {
        if (actor == null || chunk == null) return ClaimResult.NOT_PLAYER_IN_GUILD;
        java.util.UUID worldId = chunk.getWorld().getUID();
        if (!isWorldEnabled(worldId)) return ClaimResult.WORLD_DISABLED;
        UUID gid = guildIdByMember.get(actor);
        if (gid == null) return ClaimResult.NOT_PLAYER_IN_GUILD;
        Guild guild = guildsById.get(gid);
        if (guild == null) return ClaimResult.NOT_PLAYER_IN_GUILD;
        GuildRole role = guild.getMembers().get(actor);
        if (role != GuildRole.ADMIN) return ClaimResult.NOT_ADMIN;
        int cx = chunk.getX();
        int cz = chunk.getZ();
        UUID owner = getClaimOwner(worldId, cx, cz);
        if (owner != null && !owner.equals(gid)) return ClaimResult.ALREADY_CLAIMED_BY_OTHER;
        // Enforce proximity buffer to other guilds: must be at least 2 chunks away (no other-guild claims within radius 1)
        if (isTooCloseToOtherGuilds(worldId, cx, cz, gid, 2)) return ClaimResult.TOO_CLOSE_TO_OTHERS;
        // Enforce max claims per guild (only applies to player single-claim)
        if (countClaimsForGuild(gid) >= maxClaimsPerGuild) return ClaimResult.MAX_LIMIT_REACHED;
        // Enforce connectivity and outpost rules
        GuildType gtype = guild.getType();
        boolean bypassConnectivity = (gtype == GuildType.SAFE || gtype == GuildType.WAR);
        int countInWorld = countClaimsForGuildInWorld(gid, worldId);
        boolean hasAdj = hasAdjacentClaim(gid, worldId, cx, cz);
        if (!bypassConnectivity) {
            if (countInWorld == 0) {
                // First claim in this world -> main territory
            } else if (!hasAdj) {
                // Disconnected placement: Can only be a new outpost center if allowance available and not within existing outpost radius
                if (isWithinOutpostRange(gid, worldId, cx, cz, 2)) {
                    return ClaimResult.NOT_CONNECTED; // must connect to outpost claims when expanding
                }
                int have = getOutpostCount(gid);
                int allowed = getOutpostAllowance(gid);
                if (have >= allowed) {
                    return ClaimResult.OUTPOSTS_LIMIT_REACHED;
                }
                // Create new outpost center by allowing this claim and recording the center
                setClaim(worldId, cx, cz, gid);
                addOutpostCenter(gid, worldId, cx, cz);
                save();
                return ClaimResult.SUCCESS;
            } else {
                // Connected to existing claims: if this component is an outpost component, enforce 5x5 radius
                // Determine connected component starting from one adjacent tile
                int[][] nbs = new int[][]{{1,0},{-1,0},{0,1},{0,-1}};
                // find one adjacent owned tile as start
                int sx = 0, sz = 0;
                boolean found = false;
                for (int[] d : nbs) {
                    int ax = cx + d[0], az = cz + d[1];
                    UUID o = getClaimOwner(worldId, ax, az);
                    if (gid.equals(o)) { sx = ax; sz = az; found = true; break; }
                }
                if (found) {
                    // BFS to collect component
                    Set<Long> comp = new HashSet<>();
                    Deque<long[]> dq = new ArrayDeque<>();
                    dq.add(new long[]{sx, sz});
                    comp.add(key(sx, sz));
                    while (!dq.isEmpty()) {
                        long[] p = dq.removeFirst();
                        int px = (int)p[0], pz = (int)p[1];
                        for (int[] d : nbs) {
                            int nx = px + d[0], nz = pz + d[1];
                            if (!gid.equals(getClaimOwner(worldId, nx, nz))) continue;
                            long kk = key(nx, nz);
                            if (comp.add(kk)) dq.addLast(new long[]{nx, nz});
                        }
                    }
                    // Check if this component includes any outpost center in this world
                    boolean componentHasOutpost = false;
                    Map<UUID, Set<Long>> byGuild = outpostCenters.get(worldId);
                    Set<Long> centers = byGuild == null ? null : byGuild.get(gid);
                    if (centers != null && !centers.isEmpty()) {
                        for (long ckey : centers) {
                            if (comp.contains(ckey)) { componentHasOutpost = true; break; }
                        }
                    }
                    if (componentHasOutpost) {
                        if (!isWithinOutpostRange(gid, worldId, cx, cz, 2)) {
                            return ClaimResult.OUTPOST_RANGE_EXCEEDED;
                        }
                    }
                }
            }
        }
        // Normal placement (or SAFE/WAR bypass): proceed
        setClaim(worldId, cx, cz, gid);
        save();
        return ClaimResult.SUCCESS;
    }

    public static class BulkClaimResult {
        public int total;
        public int claimed;
        public int skipped;
    }

    public BulkClaimResult claimChunksForGuild(String guildName, org.bukkit.World world, int x1, int z1, int x2, int z2) {
        BulkClaimResult res = new BulkClaimResult();
        if (guildName == null || world == null) return res;
        UUID gid = guildIdByName.get(guildName.toLowerCase());
        if (gid == null) return res;
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);
        java.util.UUID worldId = world.getUID();
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                res.total++;
                UUID owner = getClaimOwner(worldId, cx, cz);
                if (owner != null && !owner.equals(gid)) {
                    res.skipped++;
                    continue;
                }
                // OP bulk claims are unlimited and do not enforce connectivity or max cap
                setClaim(worldId, cx, cz, gid);
                res.claimed++;
            }
        }
        save();
        return res;
    }

    // ===================== Unclaim API =====================
    public enum UnclaimResult {
        SUCCESS,
        NOT_PLAYER_IN_GUILD,
        NOT_ADMIN,
        NOT_CLAIMED,
        NOT_OWNED,
        DISCONNECTS_TERRITORY,
        WORLD_DISABLED
    }

    private void clearClaim(java.util.UUID worldId, int cx, int cz) {
        Map<Integer, Map<Integer, UUID>> xs = claims.get(worldId);
        if (xs == null) return;
        Map<Integer, UUID> zs = xs.get(cx);
        if (zs == null) return;
        zs.remove(cz);
        if (zs.isEmpty()) xs.remove(cx);
        if (xs.isEmpty()) claims.remove(worldId);
    }

    public UnclaimResult unclaimChunk(java.util.UUID actor, org.bukkit.Chunk chunk) {
        if (actor == null || chunk == null) return UnclaimResult.NOT_PLAYER_IN_GUILD;
        java.util.UUID worldId = chunk.getWorld().getUID();
        if (!isWorldEnabled(worldId)) return UnclaimResult.WORLD_DISABLED;
        UUID gid = guildIdByMember.get(actor);
        if (gid == null) return UnclaimResult.NOT_PLAYER_IN_GUILD;
        Guild guild = guildsById.get(gid);
        if (guild == null) return UnclaimResult.NOT_PLAYER_IN_GUILD;
        GuildRole role = guild.getMembers().get(actor);
        if (role != GuildRole.ADMIN) return UnclaimResult.NOT_ADMIN;
        int cx = chunk.getX();
        int cz = chunk.getZ();
        UUID owner = getClaimOwner(worldId, cx, cz);
        if (owner == null) return UnclaimResult.NOT_CLAIMED;
        if (!gid.equals(owner)) return UnclaimResult.NOT_OWNED;
        // Check connectivity: abort if removing would disconnect territory in this world
        if (wouldDisconnectAfterRemoval(gid, worldId, cx, cz)) {
            return UnclaimResult.DISCONNECTS_TERRITORY;
        }
        clearClaim(worldId, cx, cz);
        save();
        return UnclaimResult.SUCCESS;
    }

    public static class BulkUnclaimResult {
        public int total;
        public int removed;
    }

    public BulkUnclaimResult unclaimChunksInArea(org.bukkit.World world, int x1, int z1, int x2, int z2) {
        BulkUnclaimResult res = new BulkUnclaimResult();
        if (world == null) return res;
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);
        java.util.UUID worldId = world.getUID();
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                res.total++;
                UUID owner = getClaimOwner(worldId, cx, cz);
                if (owner != null) {
                    clearClaim(worldId, cx, cz);
                    res.removed++;
                }
            }
        }
        save();
        return res;
    }

    public BulkUnclaimResult unclaimChunksForGuild(String guildName, org.bukkit.World world, int x1, int z1, int x2, int z2) {
        BulkUnclaimResult res = new BulkUnclaimResult();
        if (guildName == null || world == null) return res;
        java.util.UUID gid = guildIdByName.get(guildName.toLowerCase());
        if (gid == null) return res;
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);
        java.util.UUID worldId = world.getUID();
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                res.total++;
                UUID owner = getClaimOwner(worldId, cx, cz);
                if (gid.equals(owner)) {
                    clearClaim(worldId, cx, cz);
                    res.removed++;
                }
            }
        }
        save();
        return res;
    }

    // Connectivity check for unclaim: will removal split the guild's claims in this world?
    private boolean wouldDisconnectAfterRemoval(UUID guildId, UUID worldId, int removeX, int removeZ) {
        Map<Integer, Map<Integer, UUID>> xs = claims.get(worldId);
        if (xs == null) return false;
        // Build set of positions claimed by guild in this world excluding the one we plan to remove
        java.util.Set<Long> positions = new java.util.HashSet<>();
        for (Map.Entry<Integer, Map<Integer, UUID>> xe : xs.entrySet()) {
            int x = xe.getKey();
            for (Map.Entry<Integer, UUID> ze : xe.getValue().entrySet()) {
                int z = ze.getKey();
                UUID gid = ze.getValue();
                if (!guildId.equals(gid)) continue;
                if (x == removeX && z == removeZ) continue;
                positions.add((((long) x) << 32) ^ (z & 0xffffffffL));
            }
        }
        int size = positions.size();
        if (size <= 1) return false; // zero or one remaining cannot be disconnected
        // BFS from an arbitrary position
        java.util.Iterator<Long> it = positions.iterator();
        long start = it.next();
        java.util.Deque<Long> dq = new java.util.ArrayDeque<>();
        java.util.Set<Long> visited = new java.util.HashSet<>();
        dq.add(start);
        visited.add(start);
        while (!dq.isEmpty()) {
            long key = dq.removeFirst();
            int x = (int) (key >> 32);
            int z = (int) key;
            long[] neighbors = new long[]{
                    (((long) (x + 1)) << 32) ^ (long) (z),
                    (((long) (x - 1)) << 32) ^ (long) (z),
                    (((long) (x)) << 32) ^ (long) (z + 1),
                    (((long) (x)) << 32) ^ (long) (z - 1)
            };
            for (long nb : neighbors) {
                if (positions.contains(nb) && !visited.contains(nb)) {
                    visited.add(nb);
                    dq.addLast(nb);
                }
            }
        }
        return visited.size() != size;
    }

    // ===== Worlds enable/disable =====
    public boolean isWorldEnabled(UUID worldId) {
        return enabledWorlds.contains(worldId);
    }

    public void setWorldEnabled(UUID worldId, boolean enabled) {
        if (enabled) enabledWorlds.add(worldId);
        else enabledWorlds.remove(worldId);
    }

    public void setWorldEnabled(CommandSender sender, UUID worldId, boolean enable) {
        World world = Bukkit.getServer().getWorld(worldId);
        if (world == null) {
            Components.sendErrorMessage(sender, "Invalid world");
            return;
        }
        setWorldEnabled(worldId, enable);
        // persist to plugin config
        List<String> worldIds = new ArrayList<>();
        for (UUID id : enabledWorlds) worldIds.add(id.toString());
        plugin.getConfig().set("guild.enabled-worlds", worldIds);
        plugin.saveConfig();
        if (enable) {
            Components.sendSuccess(sender, Components.t("World "), Components.valueComp(world.getName()), Components.t(" enabled for guilds."));
        } else {
            Components.sendSuccess(sender, Components.t("World "), Components.valueComp(world.getName()), Components.t(" disabled for guilds."));
        }
    }

    public void listWorlds(CommandSender sender) {
        Components.sendInfo(sender, Components.t("Worlds guilds status:"));
        for (World world : plugin.getServer().getWorlds()) {
            Components.sendInfo(sender,
                    Components.valueComp(world.getName()),
                    Components.t(": "),
                    (isWorldEnabled(world.getUID()) ? Components.t("ENABLED", NamedTextColor.GREEN) : Components.t("DISABLED", NamedTextColor.RED))
            );
        }
    }

    // ===== Connectivity analysis and visualization =====
    public static class ConnectivityReport {
        public int components;
        public java.util.List<Integer> componentSizes = new java.util.ArrayList<>();
        public java.util.List<long[]> samples = new java.util.ArrayList<>(); // each entry: [x, z]
    }

    private java.util.Set<Long> positionsOfGuildInWorld(java.util.UUID guildId, java.util.UUID worldId) {
        java.util.Map<Integer, java.util.Map<Integer, java.util.UUID>> xs = claims.get(worldId);
        java.util.Set<Long> positions = new java.util.HashSet<>();
        if (xs == null) return positions;
        for (java.util.Map.Entry<Integer, java.util.Map<Integer, java.util.UUID>> xe : xs.entrySet()) {
            int x = xe.getKey();
            for (java.util.Map.Entry<Integer, java.util.UUID> ze : xe.getValue().entrySet()) {
                int z = ze.getKey();
                java.util.UUID gid = ze.getValue();
                if (guildId.equals(gid)) {
                    positions.add((((long) x) << 32) ^ (z & 0xffffffffL));
                }
            }
        }
        return positions;
    }

    private static long key(int x, int z) { return (((long) x) << 32) ^ (z & 0xffffffffL); }
    private static int kx(long k) { return (int) (k >> 32); }
    private static int kz(long k) { return (int) k; }

    public ConnectivityReport analyzeConnectivity(java.util.UUID guildId, java.util.UUID worldId) {
        ConnectivityReport r = new ConnectivityReport();
        java.util.Set<Long> positions = positionsOfGuildInWorld(guildId, worldId);
        if (positions.isEmpty()) { r.components = 0; return r; }
        java.util.Set<Long> visited = new java.util.HashSet<>();
        for (long start : new java.util.HashSet<>(positions)) {
            if (visited.contains(start)) continue;
            int size = 0;
            java.util.Deque<Long> dq = new java.util.ArrayDeque<>();
            dq.add(start);
            visited.add(start);
            long sample = start;
            while (!dq.isEmpty()) {
                long cur = dq.removeFirst();
                size++;
                int x = kx(cur);
                int z = kz(cur);
                long[] nbs = new long[]{ key(x+1,z), key(x-1,z), key(x,z+1), key(x,z-1) };
                for (long nb : nbs) {
                    if (positions.contains(nb) && !visited.contains(nb)) {
                        visited.add(nb);
                        dq.addLast(nb);
                    }
                }
            }
            r.components++;
            r.componentSizes.add(size);
            r.samples.add(new long[]{ kx(sample), kz(sample) });
        }
        return r;
    }

    public ConnectivityReport analyzeConnectivityAfterRemoval(java.util.UUID guildId, java.util.UUID worldId, int removeX, int removeZ) {
        ConnectivityReport r = new ConnectivityReport();
        java.util.Set<Long> positions = positionsOfGuildInWorld(guildId, worldId);
        long rem = key(removeX, removeZ);
        positions.remove(rem);
        if (positions.isEmpty()) { r.components = 0; return r; }
        java.util.Set<Long> visited = new java.util.HashSet<>();
        for (long start : new java.util.HashSet<>(positions)) {
            if (visited.contains(start)) continue;
            int size = 0;
            java.util.Deque<Long> dq = new java.util.ArrayDeque<>();
            dq.add(start);
            visited.add(start);
            long sample = start;
            while (!dq.isEmpty()) {
                long cur = dq.removeFirst();
                size++;
                int x = kx(cur);
                int z = kz(cur);
                long[] nbs = new long[]{ key(x+1,z), key(x-1,z), key(x,z+1), key(x,z-1) };
                for (long nb : nbs) {
                    if (positions.contains(nb) && !visited.contains(nb)) {
                        visited.add(nb);
                        dq.addLast(nb);
                    }
                }
            }
            r.components++;
            r.componentSizes.add(size);
            r.samples.add(new long[]{ kx(sample), kz(sample) });
        }
        return r;
    }

    public java.util.List<String> buildAsciiMap(java.util.UUID guildId, java.util.UUID worldId, int centerX, int centerZ, int radius, java.lang.Integer removeX, java.lang.Integer removeZ) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        int r = Math.max(1, Math.min(16, radius));
        for (int dz = r; dz >= -r; dz--) {
            StringBuilder sb = new StringBuilder();
            for (int dx = -r; dx <= r; dx++) {
                int x = centerX + dx;
                int z = centerZ + dz;
                char ch;
                if (removeX != null && removeZ != null && x == removeX && z == removeZ) {
                    ch = 'X';
                } else {
                    java.util.UUID owner = getClaimOwner(worldId, x, z);
                    if (owner == null) ch = '.';
                    else if (owner.equals(guildId)) ch = '#';
                    else ch = '+';
                }
                sb.append(ch);
            }
            lines.add(sb.toString());
        }
        return lines;
    }
}
