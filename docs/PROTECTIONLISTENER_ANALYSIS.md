# ProtectionListener Analysis

This document summarizes what `ProtectionListener` enforces today, notes edge cases and potential issues, and suggests improvements. It is based on review of `src/main/java/com/spillhuset/furious/listeners/ProtectionListener.java` (579 LOC).

## High-level goals implemented

Protection varies by guild claim type at a chunk level:

- SAFE territory (GuildType.SAFE)
  - No building or breaking.
  - No mob spawning (hostile or otherwise) and hostile mobs are removed on entry/teleport/target.
  - No explosions; explosion effects are fully canceled and their block list cleared.
  - Players cannot take any damage while standing in SAFE.
  - Projectiles (Arrow, Snowball, Fireball) are canceled if they would hit a player in SAFE.
  - Harmful potions (splash/lingering) are blocked.
  - TNTPrimed spawn is blocked.

- OWNED territory (GuildType.OWNED)
  - Building/breaking is allowed only for owning guild members with roles MODERATOR or ADMIN (ops bypass all protections).
  - Outsiders cannot interact with certain interactables (doors, trapdoors, buttons, chests, barrels, shulkers, hoppers, etc.).
  - Outsiders cannot open entity containers (storage/hopper minecarts, chest boat).
  - Hostile mob natural spawn is blocked; incoming hostiles (teleport/move/target) get removed if inside OWNED.
  - Fire spread, lava ignition, and general fire burning are blocked inside OWNED. Flint-and-steel ignition is allowed only for MODERATOR/ADMIN of the owning guild.
  - Emptying water/lava buckets inside OWNED is restricted to MODERATOR/ADMIN of the owning guild.
  - Flow of water/lava into OWNED is blocked unless the source is the same owning guild.

- WAR territory (GuildType.WAR)
  - Building/breaking is prohibited (ops bypass).
  - Explosions do not damage terrain (block lists cleared) but are not canceled, so entities can still take explosion damage. [FIXED 2025-08-23]
  - Entity-caused block changes are canceled.

## Event coverage

- Blocks/building:
  - BlockBreakEvent, BlockPlaceEvent
  - BlockIgniteEvent, BlockSpreadEvent, BlockBurnEvent
  - PlayerBucketEmptyEvent
  - BlockFromToEvent (water/lava flow)
  - EntityChangeBlockEvent
  - BlockExplodeEvent, EntityExplodeEvent

- Entities/mobs:
  - CreatureSpawnEvent (hostile block in SAFE/OWNED, blanket block in SAFE)
  - EntitySpawnEvent (TNTPrimed in SAFE; hostiles handled via CreatureSpawnEvent)
  - EntityTeleportEvent (remove hostiles entering SAFE/OWNED)
  - EntityMoveEvent (Paper event: remove hostiles moving within SAFE/OWNED)
  - EntityTargetEvent (remove hostiles if attacker or player target is in SAFE/OWNED)

- Players/combat and interactions:
  - EntityDamageEvent (players invulnerable in SAFE)
  - EntityDamageByEntityEvent (cancel projectiles in SAFE and remove them)
  - PotionSplashEvent, AreaEffectCloudApplyEvent (block harmful effects in SAFE)
  - PlayerInteractEvent (outsider interaction with protected interactables in OWNED)
  - PlayerInteractAtEntityEvent (outsider opening entity containers in OWNED)
  - PlayerMoveEvent (garbage-collect hostiles near players entering SAFE/OWNED)

## Role and membership checks

- Building/breaking, flint-and-steel ignition, bucket emptying are allowed for owning guild members only if their role is MODERATOR or ADMIN.
- Ops (`Player#isOp`) bypass protections in several places.
- Outsiders are blocked from interacting with a curated list of interactable blocks and entity containers in OWNED claims.

## Helper methods

- isInSafeGuild(Location), isInWarGuild(Location), getOwningGuild(Location), isInOwnedGuild(Location)
- notifyDenied(Player, String) uses MessageThrottle if available for action bar throttling; falls back to chat message.
- ownedBySuffix(Guild) appends guild name when available.

## Notable strengths

- Consistent guild lookup and territory checks using `guildService.getClaimOwner(worldId, chunkX, chunkZ)` and `getGuildById`.
- Reasonable null-safety and try/catch guards in places where API may throw.
- Avoids noisy chat by using a throttled action bar, if available.
- Uses Paper-specific `EntityMoveEvent` to proactively remove hostiles entering protected areas, complementing spawn/teleport/target events.
- Selective, minimal coupling: logic is centralized in this listener without leaking into other services.

## Potential issues and edge cases

1. PlayerMoveEvent null checks:
  - `event.getTo()` can be null during some transitions; the code uses it directly. There is a `World world = to.getWorld()` guard, but a null `to` would NPE earlier. Consider a null guard for `getTo()`. [FIXED 2025-08-23] Added early return if `event.getTo()` is null in onPlayerMove.

2. PlayerMoveEvent chunk-change condition:
  - The early-return condition uses both chunk coords equality and world equality check, but only checks world equality if chunks are also equal. If worlds differ but chunks coincidentally have the same coords, logic proceeds (good), but the compound condition is slightly confusing. A clearer approach: return if `to` is null or same world and same chunk; otherwise continue. [FIXED 2025-08-23] Simplified early-return to only skip when same world and same chunk; proceed otherwise.

3. EntityDamageByEntityEvent cast:
   - The code cancels for Arrow/Snowball/Fireball then casts to Projectile unconditionally and removes. Snowball and Arrow are Projectile, Fireball instances (e.g., LargeFireball) implement Fireball which implements Projectile in Bukkit, so cast is safe. If future projectiles are added (e.g., ThrownPotion separately handled), current checks should remain safe.

4. Harmful potion detection:
   - Splash uses `potion.getEffects()`; lingering uses `getBasePotionType()` and `getCustomEffects()`. Some effects can be beneficial but have side effects (e.g., slow falling not harmful). The current curated lists look okay, but they may need adjustment depending on server preference.

5. Interactable whitelist in OWNED:
  - The list is comprehensive for doors, trapdoors, buttons, plates, storage, and redstone. It now also includes common workstation UIs (beacon, anvils, smithing table, enchanting table, grindstone, stonecutter, loom, cartography table, lectern) and bell. [PARTIAL FIXED 2025-08-23]
  - Further review may be needed for newer blocks (e.g., vault blocks, trial spawner UI, decorated pots, hanging signs, fletching table behavior), depending on server policy.

6. Fire handling in OWNED:
  - `BlockIgniteEvent` cancels SPREAD/LAVA causes and gates flint-and-steel by role. Campfire lighting or candles via flint-and-steel falls under FLINT_AND_STEEL and is thus gated. Fire spread into OWNED is canceled via BlockSpreadEvent when source is FIRE. However, lightning/explosion-based ignitions are now explicitly handled and canceled (LIGHTNING, ENDER_CRYSTAL, EXPLOSION). [FIXED 2025-08-23]

7. Explosions in WAR:
  - WAR explosions now allow entity damage but clear the block list to prevent terrain damage (no event cancellation). [FIXED 2025-08-23]

8. EntityChangeBlock in WAR/SAFE:
   - Blanket cancel prevents endermen griefing, withers, ravagers, etc. This is fine, but confirm WAR intent.

9. CreatureSpawnEvent and EntitySpawnEvent overlap:
  - Consolidated: Hostile spawn is handled in CreatureSpawnEvent; EntitySpawnEvent now only blocks TNTPrimed in SAFE. [FIXED 2025-08-23]

10. Ownership/role retrieval:
   - In several places, code fetches `GuildRole role = g.getMembers().get(playerId)` without null checks; null implies member not found and will fail the permission `can` check, which is acceptable. If `getMembers()` can be null, it would NPE; assume it is a non-null map.

11. Message texts:
   - Messages are hard-coded English strings and not using a localization/config system. Also, using action bar throttling is good, but for persistent messages, consider chat plus action bar or sound cues. Include the guild name via `ownedBySuffix` where helpful.

12. Op bypass consistency:
   - Ops bypass building/breaking/placing, bucket empty, interact blocks in OWNED, flint-and-steel, etc. Verify whether ops should also bypass SAFE invulnerability or entity removal behaviors (currently they do not, which is correct for invulnerability since it applies to damage to players in SAFE, not actions by them).

13. Performance considerations:
  - PlayerMoveEvent scans nearby entities within 32x16x32 on chunk changes. Added a lightweight per-player 1s cooldown to reduce scan frequency on busy servers. [IMPROVED 2025-08-23]

14. Paper-specific dependency:
   - Uses `io.papermc.paper.event.entity.EntityMoveEvent`; if the plugin is expected to run on Spigot without Paper, guard with `paper`-only registration or feature flags.

## Recommendations

- Add null guards: [DONE 2025-08-23]
  - In `onPlayerMove`, handle `event.getTo() == null` early. [DONE]

- Clarify WAR explosion policy: [DONE 2025-08-23]
  - Implemented: explosions are not canceled in WAR; block lists are cleared to prevent terrain damage while allowing entity damage.

- Expand ignition causes handling in OWNED: [DONE 2025-08-23]
  - Implemented: Explicitly cancel LIGHTNING, ENDER_CRYSTAL, and EXPLOSION ignitions in OWNED claims.

- Review interactable list: [PARTIAL DONE 2025-08-23]
  - Expanded to include beacon, anvils (all states), smithing table, enchanting table, grindstone, stonecutter, loom, cartography table, lectern, and bell. Further additions may be made based on policy.

- Consolidate hostile spawn blocking: [DONE 2025-08-23]
  - Hostile mobs are handled in `CreatureSpawnEvent`; `EntitySpawnEvent` only blocks TNTPrimed in SAFE.

- Add tests (if feasible):
  - While Minecraft plugins are hard to unit test, consider abstracting the claim check and permission logic into a service class that can be unit tested, leaving only thin event wrappers in the listener.

- Configurability:
  - Messages and policies (e.g., `allowMemberInteractablesInOwned`, `warExplosionsBlockDamageOnly`) could be made configurable via `config.yml`.

## Quick behavior matrix

- SAFE:
  - Build/break/place: blocked
  - Liquids: not specifically handled for players; but environment interactions are heavily blocked; explosions canceled
  - Damage to players: blocked
  - Hostiles: do not spawn; removed on entry/teleport/target
  - Projectiles/potions: harmful blocked

- OWNED:
  - Build/break/place: owner guild MOD/ADMIN only; ops bypass
  - Liquids: bucket empty restricted to MOD/ADMIN; water/lava flow from other claims/unclaimed blocked
  - Fire: spread/burn canceled; flint-and-steel gated to MOD/ADMIN
  - Hostiles: natural spawn blocked; removed on entry/teleport/target
  - Interactions: outsiders blocked from curated interactables and entity containers; members allowed regardless of role

- WAR:
  - Build/break/place: blocked
  - Explosions: block damage prevented (block lists cleared), entity damage allowed [FIXED 2025-08-23]
  - Entity-change: canceled

This analysis should help guide any adjustments or refactors you want for ProtectionListener. If you want, I can implement specific suggestions (e.g., the PlayerMove null guard or war explosion policy) as a follow-up change.
