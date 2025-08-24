# Locks System Report

Date: 2025-08-24 19:02

This document provides an overview and technical report of the Locks feature implemented in the Furious plugin. It covers scope, commands, permissions, world enablement, persistence format, user flows, costs, constraints, edge cases, and maintenance.

## Scope and Goals
The Locks system lets players protect interactive blocks (e.g., chests and doors). A locked block can only be used and broken by its owner; others require a matching Key item to interact. Operators (ops) have bypass privileges for breaking and can manage world enablement.

Goals:
- Simple per-block ownership to prevent unauthorized access.
- Sensible grouping for multi-block structures (double chests, two-block doors, paired double doors).
- World-by-world enable/disable management for lock enforcement.
- Minimal item-based workflow: Lock Tool, Unlock Tool, and personal Key.

## Key Concepts
- Locked block: A block recorded as owned by a player UUID. Enforcement denies interaction/break to non-owners unless they hold a matching Key.
- Grouped locking: Certain blocks are handled as a group, so locking applies to all meaningful parts:
  - Chests: A double chest locks both halves together.
  - Doors: Includes both the top and bottom halves. If two side-by-side doors form a pair with opposite hinges, they are treated as a group.
- Tools and Keys:
  - Lock Tool: A consumable item that locks a target block (and its group) for the user.
  - Unlock Tool: A consumable item that unlocks a block (owner or op only).
  - Key: A personal key bound to a specific player UUID; allows interacting with another player’s locked block if their UUID matches the lock owner.
- Enabled worlds: Enforcement and tool usage are only allowed in worlds explicitly enabled for locks.

## Commands Overview
All base commands are registered under `/locks`.

Player subcommands (default permissions true unless noted):
- `/locks lock` — gives you a Lock Tool (consumed when used on a block to lock it). Cost may apply (see Costs).
- `/locks unlock` — gives you an Unlock Tool (consumed when used on a locked block to unlock it; owner/op only).
- `/locks key` — creates a personal Key item bound to yourself (used to access others’ locks when permitted). Cost may apply.

Operator subcommands:
- `/locks worlds` — lists worlds and whether locks are enabled for each (requires `furious.locks.worlds`).
- `/locks worlds <world> <enable|disable>` — toggles lock enforcement for a world (requires `furious.locks.worlds`).

## Permissions
Declared in `src/main/resources/plugin.yml` (relevant entries):
- `furious.locks` — access to `/locks` and user-level subcommands (default: true)
- `furious.locks.worlds` — manage enabled worlds (default: op)

## World Enablement
- Enforcement and tool usage only occur in enabled worlds.
- When a world is disabled:
  - Interactions and block breaks are not restricted by locks.
  - Attempting to use Lock/Unlock tools is blocked with a message: “Locks are disabled in this world.”
- Manage via `/locks worlds` commands. Data key: `locks.enabled-worlds` in `config.yml` stores world UUIDs.

## Persistence
- Data file: `plugins/Furious/locks.yml`.
- Structure:
  - `locks` — map keyed by world UUID strings.
    - Within each world, keys are `"x,y,z"` strings of block coordinates, with values being the owner UUID string.
- Example:
```
locks:
  <world-uuid>:
    "123,64,-45": <owner-uuid>
```
- Enabled worlds are persisted in `config.yml` under `locks.enabled-worlds` (list of world UUIDs).

## Implementation Summary
- Service: `com.spillhuset.furious.services.LocksService`
  - Loads/saves `locks.yml` and `locks.enabled-worlds`.
  - Methods include `isWorldEnabled`, `getOwner`, `canAccess`, `lockBlock`, `unlockBlock`.
  - Groups related blocks with `relatedGroup(Block)` handling double chests and doors (including paired double doors).
- Listener: `com.spillhuset.furious.listeners.LocksListener`
  - Enforces interaction and breaking rules.
  - Consumes Lock/Unlock tools on use; prevents dropping these tools.
  - Allows non-owners to interact if they hold a Key item bound to the lock owner’s UUID.
  - Synchronizes paired doors so both open/close together after interaction.
- Commands:
  - Router: `com.spillhuset.furious.commands.LocksCommand`
  - Subcommands:
    - `LocksCommands.LockSub` — gives Lock Tool; applies configurable cost.
    - `LocksCommands.UnlockSub` — gives Unlock Tool.
    - `LocksCommands.KeySub` — crafts a personal Key item; applies configurable cost.
    - `LocksCommands.WorldsCommand` — lists/toggles enabled worlds for locks.
- Registration: Services and listeners are initialized in `Furious.onEnable()`; `/locks` is defined in plugin.yml.

## Tools and Items Details
- Lock Tool:
  - Item: OAK_SIGN with PDC: `locks_tool = "lock"`. Display name “Lock Tool”.
  - Usage: Right-click a block to lock it (and its related group). Consumed by one on success.
  - Drop prevention: Listener cancels drop attempts for any item with `locks_tool` PDC.
- Unlock Tool:
  - Item: SHEARS with PDC: `locks_tool = "unlock"`. Display name “Unlock Tool”.
  - Usage: Right-click a locked block. Only the owner or an op can successfully unlock. Consumed by one on success.
- Key:
  - Item: TRIPWIRE_HOOK with PDC: `locks_key_owner = <uuid>`. Display name “Key” with lore showing the owner’s name and UUID.
  - Usage: If you are not the owner and the block is locked, holding a Key whose owner UUID matches the lock owner allows interaction (but not breaking).

## User Flow
1. Acquire tools/keys
   - `/locks lock` to receive a Lock Tool (cost may apply).
   - `/locks unlock` to receive an Unlock Tool.
   - `/locks key` to craft a personal Key bound to yourself (cost may apply).

2. Locking a target
   - Ensure locks are enabled in the current world.
   - If in a claimed guild chunk and you’re not op, you must belong to the owning guild to lock here (enforced via `guildService`).
   - Right-click the block with the Lock Tool. If any block in the related group is owned by someone else, the lock is denied. On success, the tool is consumed and ownership stored for the entire group.

3. Unlocking a target
   - Right-click the block with the Unlock Tool. Only the owner or an op can unlock. On success, the tool is consumed and ownership entries are removed for the entire group.

4. Interacting with locked targets
   - Owners and ops can interact normally.
   - Non-owners must hold a Key that matches the owner UUID to interact. Otherwise, interactions are cancelled with “This block is locked.”

5. Breaking blocks
   - Ops bypass lock checks and can break any block.
   - Non-owners cannot break locked blocks; attempts are cancelled with a message.

## Costs and Economy Integration
- `locks.lock-cost` (double; default 10.0): Charged when a non-op obtains a Lock Tool via `/locks lock`.
- `locks.key-cost` (double; default 10.0): Charged when crafting a personal Key via `/locks key`.
- Wallet integration: Uses `walletService` for balance checks, charges, and formatted amounts. If `walletService` is absent or cost is 0, items are free.

## Edge Cases and Validations
- World disabled: Enforcement is skipped. Attempting to use Lock/Unlock tools is blocked with a message.
- Double chests: Both halves are treated as one. Lock denies if the other half is owned by someone else. Unlock removes ownership for both.
- Doors:
  - Both halves are grouped. Paired double doors (adjacent, same facing, opposite hinge) are grouped together.
  - On successful interaction, a synchronization task mirrors the open state to the paired door next tick.
- Ownership checks: `lockBlock` refuses to lock if any block in the group is owned by a different player.
- Break protection: Non-owners cannot break locked blocks. Ops bypass.
- Tool handling: Tools are marked in PersistentDataContainer and are consumed on successful use. Dropping tools is prevented.
- Key items: Matching is done via PDC UUID string; malformed UUID in a key’s PDC is treated as non-matching.

## Configuration Keys
- `locks.enabled-worlds` — List of world UUIDs where locks are enforced.
- `locks.lock-cost` — Cost to obtain a Lock Tool (default 10.0).
- `locks.key-cost` — Cost to craft a Key (default 10.0).

## Troubleshooting
- “Locks are disabled in this world.” — Enable the world via `/locks worlds <world> enable` (op required) or move to an enabled world.
- “This block (or its pair) is already locked.” — Choose another block or unlock first.
- “Could not lock this block.” — Another owner likely has a conflicting part of the grouped structure.
- “You are not the owner, or it isn’t locked.” — Only owners or ops can unlock. Ensure you targeted a locked block.
- “This block is locked.” — You need a Key matching the owner UUID to interact as a non-owner.
- “Not enough money …” — Ensure sufficient balance if costs are configured.

## Future Enhancements (Optional)
- Add per-guild or per-world lock limits and costs, or different pricing per block type.
- Add admin commands to transfer ownership or forcibly unlock without a tool.
- Add a way to create guest keys that grant temporary access or allow multiple UUIDs per lock.
- Visual indicators (particles, map markers) to show locked status to owners.
- Logging/auditing of lock/unlock events for moderation.
