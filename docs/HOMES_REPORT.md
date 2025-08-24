# Homes System Report

Date: 2025-08-24 18:42

This document provides an overview and technical report of the Homes feature implemented in the Furious plugin. It covers commands, permissions, persistence format, user flows, constraints, and maintenance considerations.

## Scope and Goals
The Homes system allows players to set named home locations per player and teleport to them later. Homes can be purchased beyond a default allowance. Operators can restrict in which worlds homes are allowed and can manage homes for other players. Each home is optionally represented by an invisible ArmorStand marker that is visible only to operators and is used as a resilient anchor for the home’s exact location.

The implementation follows the project’s Commands + Services pattern and uses YAML-based persistence with per-player entries. Teleports are performed via the shared TeleportsService.

## Key Concepts
- Home: A record with ID, owner UUID, display name, stored coordinates (world UUID, x/y/z, yaw/pitch) and an optional ArmorStand UUID. If the ArmorStand exists, its current location is preferred when teleporting or listing.
- Default and purchased slots: Players start with a configurable default number of home slots and can purchase more at an exponentially increasing cost.
- Enabled worlds: Only worlds explicitly enabled may contain homes set/moved by commands.
- Territory rules: Setting/moving homes is constrained by guild claim type rules (see below).

## Commands Overview
All base commands are registered under `/homes`.

Player subcommands (default permissions true):
- `/homes list` — lists your homes.
- `/homes set <name>` — sets a home with the given name at your current location (world must be enabled and area allowed).
- `/homes move <name>` — moves an existing home to your current location (world must be enabled and area allowed).
- `/homes rename <old> <new>` — renames an existing home; fails if the new name is already used.
- `/homes remove <name>` — two-step removal: shows confirmation hint first, then `/homes remove <name> confirm` removes it.
- `/homes teleport [name]` — teleports to the named home (defaults to "default").
- `/homes buy [confirm]` — shows the price for the next extra slot; with `confirm`, charges and grants one slot.

Operator subcommands (op-only where noted by permission):
- `/homes list <player>` — list a specific player’s homes.
- `/homes set <player> <name>` — set a home for another player at your current position (follows world/territory checks against the target’s membership).
- `/homes move <player> <name>` — move a player’s home to your location.
- `/homes rename <player> <old> <new>` — rename a player’s home.
- `/homes remove <player> <name> [confirm]` — remove a player’s home (same two-step confirmation).
- `/homes teleport <player> <name>` — teleport yourself to a player’s home.
- `/homes worlds` — show enabled/disabled worlds for homes, with usage hint.
- `/homes worlds <world> <enable|disable>` — toggle whether homes can be set/moved in a given world.

## Permissions
Declared in `src/main/resources/plugin.yml` (relevant entries):
- `furious.homes` — base permission (default: true)
- `furious.homes.set` — set homes (default: true)
- `furious.homes.remove` — remove homes (default: true)
- `furious.homes.teleport` — teleport to homes (default: true)
- `furious.homes.list` — list homes (default: true)
- `furious.homes.move` — move homes (default: true)
- `furious.homes.rename` — rename homes (default: true)
- `furious.homes.buy` — buy extra home slots (default: true)
- `furious.homes.worlds` — manage enabled worlds (default: op)
- `furious.homes.*.others` — op-level variants for acting on other players (see plugin.yml)
- `furious.homes.admin` — aggregates admin permissions (default: op)

## User Flow Details
1. Setting a home
   - Ensure that the current world is enabled for homes (see worlds management).
   - Ensure the area is allowed by territory rules: FREE anywhere, OWNED only if you are a member of the owning guild, SAFE and WAR are disallowed.
   - Run `/homes set <name>`.
   - An invisible ArmorStand marker is spawned at the location and registered; only operators can see it. The home is persisted.

2. Moving a home
   - Same checks as setting a home (world enabled + territory rules).
   - Run `/homes move <name>` to update the stored location and move the ArmorStand if it exists.

3. Renaming a home
   - Run `/homes rename <old> <new>`.
   - The ArmorStand’s name is updated to reflect the new home name and owner name.

4. Removing a home
   - Run `/homes remove <name>` to see a confirmation prompt.
   - Run `/homes remove <name> confirm` to delete it; the ArmorStand (if present) is removed and the record is purged.

5. Teleporting to a home
   - Run `/homes teleport [name]` (defaults to `default`).
   - Teleport is queued via TeleportsService to the home’s location; if the world is missing/invalid, an error is shown.

6. Purchasing an extra slot
   - Run `/homes buy` to view the current price.
   - Run `/homes buy confirm` to pay and increase your slot count by 1.

## Territory and World Rules
- World restriction: Homes can only be set/moved in worlds enabled via `/homes worlds <world> <enable|disable>`. Listing and teleporting are not restricted by these world toggles.
- Guild claims restriction when setting/moving (from HomesService):
  - FREE: allowed.
  - OWNED: allowed only if the player is a member of the owning guild.
  - SAFE or WAR: disallowed.

## Teleport and ArmorStand Marker
- When a home is created, the service tries to spawn an invisible ArmorStand marker named: `Home <name> by <playerName>`.
- Visibility: For each viewer, ops see the stand while non-ops have it hidden via `showEntity/hideEntity` calls.
- The ArmorStand UUID is stored with the home and a removal callback is registered. If the ArmorStand disappears, the service can respawn/register it during `ensureArmorStands()` and continues to enforce op-only visibility.
- Teleports use `teleportsService.queueTeleport(player, location, reason)`.

## Persistence
- Data file: `plugins/Furious/homes.yml`.
- Structure:
  - `players` — map keyed by player UUID.
    - `purchased` — number of extra slots purchased.
    - `homes` — map keyed by home UUID, each entry:
      - `world` — world UUID
      - `name` — home name
      - `x`, `y`, `z` — doubles
      - `yaw`, `pitch` — doubles
      - `armorStand` — optional ArmorStand UUID
- Configuration keys in `config.yml`:
  - `homes.default` — default home slots per player (int; default 5)
  - `homes.cost` — base cost of first extra slot (double; default 5000.0)
  - `homes.multiplier` — exponential multiplier per purchased slot (double; default 1.5)
  - `homes.enabled-worlds` — list of world UUIDs where setting/moving homes is allowed

## Edge Cases and Validations
- Duplicate names per player are rejected (set/move/rename logic ensures uniqueness).
- Remove uses a two-step confirmation token per player+name to prevent accidental deletion.
- Teleport validates that the stored world exists; otherwise, an informative error is sent.
- If ArmorStand creation fails (chunk not ready, etc.), the home still works via stored coordinates; the stand may be respawned later by `ensureArmorStands()`.
- Listing/teleport operate regardless of world enablement; the enablement only applies to set/move.
- Renaming updates the ArmorStand’s displayed name when present.

## Troubleshooting
- “You already have the maximum amount of homes.” — Purchase more slots with `/homes buy`, or remove a home.
- “You can’t set homes in this world.” — Enable the world via `/homes worlds <world> enable` (requires op) or move to an enabled world.
- “You must be a member of this guild to set a home here.” — The location is inside an OWNED claim you don’t belong to.
- “You can’t set homes in this guild territory.” — The location is inside SAFE or WAR territory.
- “A home with this name already exists.” — Use a different name or remove/rename the existing one.
- “Home location is invalid.” — The stored world is missing; set/move the home again in a valid world.
- “Not enough money to purchase a home slot.” — Ensure sufficient balance for `/homes buy confirm`.

## Implementation Summary
- Service: `com.spillhuset.furious.services.HomesService`
- Model: `com.spillhuset.furious.utils.Home`
- Command router: `com.spillhuset.furious.commands.HomesCommand`
- Subcommands: `com.spillhuset.furious.commands.HomesCommands.*` (Buy, List, Remove, Move, Rename, Set, Teleport, Worlds)
- Integration:
  - Teleports via `teleportsService`
  - Territory checks via `guildService` and `GuildType`
  - Marker lifecycle via `armorStandManager` and listeners (apply visibility and cleanup)
- Registration: in `Furious.onEnable()` (creates and loads HomesService, registers homes command)
- Permissions: declared in `plugin.yml`

## Future Enhancements (Optional)
- Add cooldowns or warm-up for `/homes teleport`.
- Per-world or per-guild slot limits and pricing modifiers.
- Safe-teleport adjustment if target is obstructed (scan for safe Y or nearest safe spot).
- GUI for browsing homes and purchasing slots.
- Command to temporarily toggle visibility of home markers per viewer.
- Migration command to rebuild/repair ArmorStands for all homes.
