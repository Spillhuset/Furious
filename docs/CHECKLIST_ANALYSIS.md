# Checklist System Analysis

This document provides an in‑depth analysis of the Checklist system in the Furious plugin. It covers architecture, responsibilities, data flow, persistence, and extension points, with a focus on Biomes and Monsters checklists that are currently implemented.

## Goals and Scope

- Track player progress through enumerations of things to "complete" (e.g., visit all biomes, remove all hostile monsters).
- Record various "firsts" (server‑first, year‑first, month‑first) for items in a checklist.
- Grant a one‑time reward upon completion of a checklist per player and notify them properly using the wallet's formatting (currency name/icon via WalletService).
- Persist all required state to YAML files in the plugin data folder.

## High‑Level Architecture

- ChecklistService (abstract):
  - Base class providing a unified reward mechanism and persistence helpers for the rewarded‑players set.
  - Key responsibilities:
    - Maintain a Set<UUID> completionRewarded.
    - Serialize/deserialize reward flags in the service’s YAML file under `rewards.completed`.
    - Provide `grantCompletionReward(UUID, amount, checklistName)` which:
      - Credits WalletService (if present).
      - Sends a congrats message to the player (if online) using Components.amountComp for proper currency formatting.
      - Marks and saves the reward flag.
  - Requires subclasses to implement `save()`, so base logic can persist after marking rewards.

- BiomesService (extends ChecklistService):
  - Tracks per‑player visited biomes and global discovery state.
  - Records firsts: server‑first (all time), this‑year‑first, and this‑month‑first.
  - Persists to `plugins/Furious/biomes.yml`.

- MonstersService (extends ChecklistService):
  - Tracks per‑player removed hostile monsters and global removals.
  - Records the same firsts breakdown as BiomesService.
  - Persists to `plugins/Furious/monsters.yml`.

- Commands:
  - `/checklist biomes` (BiomesCommand):
    - Shows a player’s biome checklist progress.
    - Shows firsts for a biome if queried by biome.
  - `/checklist monsters` (MonstersCommand):
    - Shows a player’s monster checklist progress.
    - Shows firsts for a monster if queried by monster.
    - Admin subcommands to clear month/year/all firsts.

- Listeners:
  - BiomeTrackListener: Detects when players move/ join and records biome visits.
  - MonsterTrackListener: Detects hostile monster deaths and records removals.

## Data Model and Persistence

Each checklist service owns a YAML file. Common pieces:

- players: map of `UUID -> List<String>` of canonical item keys visited/removed.
- global: List<String> of all globally discovered/removed keys (derived if missing).
- first:
  - server: `itemKey -> UUID` for all‑time server‑first.
  - year:
    - current: integer.
    - winners: `year -> itemKey -> UUID` for per‑year firsts.
  - month:
    - currentYear: integer
    - currentMonth: integer (1‑12)
    - winners: `YYYY-MM -> itemKey -> UUID` for this‑month firsts.
- rewards:
  - completed: List<String UUIDs> of players who have received the completion reward.

Files:
- `biomes.yml`
- `monsters.yml`

Canonicalization:
- BiomesService: Maps legacy names or enum constants to namespaced keys (e.g., `minecraft:plains`), stored in lowercase.
- MonstersService: Stores namespaced entity keys (e.g., `minecraft:zombie`) lowercased.

## Lifecycle

- Furious.onEnable(): creates and loads services, registers listeners.
- Furious.onDisable(): saves all services, ensuring changes persist.
- Services:
  - `load()` reads YAML, resets in‑memory caches, and ensures period (year/month) is current.
  - `save()` writes current in‑memory state back to YAML, including reward flags via ChecklistService helpers.

## Reward System

- Unified in ChecklistService:
  - `grantCompletionReward(playerId, amount, checklistName)`:
    - Adds balance via WalletService if available.
    - Sends a message: "Congratulations! You completed the <Name> checklist and earned <formatted amount>." using Components.amountComp and wallet formatting.
    - Marks `completionRewarded` and triggers `save()` in the concrete service.

- Completion Criteria:
  - Biomes: When a player’s visited set contains all existing biomes (derived from Bukkit Registry/enum) and they haven’t been rewarded yet.
  - Monsters: When a player’s removed set contains all hostile monsters (EntityType values whose class implements Enemy) and they haven’t been rewarded yet.

- Amounts (configurable):
  - Each checklist now reads reward amounts from its own YAML file under `rewards.amounts.*` with fallback to `config.yml` under `rewards.*` and then to code defaults.
  - Example keys: `your-first-Biomes`, `server-first-per-Biomes`, `this-year-first-per-Biomes`, `this-month-first-per-Biomes`, `50%-Biomes`, `complete-Biomes`; similarly for `Monsters` and `Taming`.

## Event Flow

- Biome visit:
  1. Player moves (block change) or joins → BiomeTrackListener.tryMark.
  2. Determine biome key and call `biomesService.visit(uuid, key, includeGlobal)`.
     - includeGlobal is false for ops to avoid modifying global/firsts via staff.
  3. VisitResult firstForPlayer → send action bar notice.
  4. VisitResult firstForServer → broadcast discovery message.
  5. If the player’s visited set covers all biomes and not rewarded yet → `grantCompletionReward`.

- Monster removal:
  1. Hostile entity dies with a player killer → MonsterTrackListener.onEntityDeath.
  2. Determine entity key and call `monstersService.recordRemoval(uuid, key, includeGlobal)`.
  3. Similar action bar and broadcast logic for firsts.
  4. If the player’s removed set covers all hostile monsters and not rewarded yet → `grantCompletionReward`.

## Commands and Permissions

- Root command: `/checklist` implemented in ChecklistCommand, delegating to subcommands.
- BiomesCommand (permission: `furious.checklist.biomes`):
  - `/checklist biomes` shows your progress.
  - `/checklist biomes <player>` shows another player’s progress (requires elevated permissions via can()).
  - `/checklist biomes <biome>` shows firsts for the biome.
  - Output uses color coding: green check for completed items, red cross otherwise, with firsts suffixes (month/year/server) and resolver for player names.

- MonstersCommand (permission: `furious.checklist.monsters`):
  - `/checklist monsters` shows your progress.
  - `/checklist monsters <player>` shows another player’s progress (requires elevated permissions via can()).
  - `/checklist monsters <monster>` shows firsts for the monster.
  - Admin maintenance:
    - `/checklist monsters clear month`
    - `/checklist monsters clear year`
    - `/checklist monsters clear all confirm` (dangerous; clears all firsts and global removals).

## Period Handling (Year/Month)

- Each service stores currentYear and currentMonth.
- `ensurePeriodCurrent()` updates these values using LocalDate.now() and clears monthFirst on month rollover; for year rollover, ensures a new (empty) map exists in yearFirstByYear for the new year.

## Edge Cases and Considerations

- Ops/staff: Listeners set includeGlobal=false for operators to prevent staff activity from affecting global stats and firsts.
- Registry access across versions: Code defensively tries both enum constants and the Registry API to enumerate biomes and entity keys, catching Throwable to avoid breaking on version differences.
- Offline players in firsts: Names are resolved via OfflinePlayer; if missing, the UUID string is shown instead.
- Reward idempotency: The rewarded set is persisted per‑service, so rejoining or restarts do not duplicate rewards.
- Concurrency: Bukkit main thread context; services use simple maps/sets; no explicit synchronization.
- Data growth: Yearly winners map grows per year; month winners only persisted for the current month key. Clearing helpers are provided.

## YAML Structure Examples

biomes.yml (excerpt):
```yaml
players:
  "<player-uuid>":
    - minecraft:plains
    - minecraft:desert
first:
  server:
    minecraft:plains: "<uuid>"
  year:
    current: 2025
    winners:
      "2025":
        minecraft:plains: "<uuid>"
  month:
    currentYear: 2025
    currentMonth: 08
    winners:
      "2025-08":
        minecraft:plains: "<uuid>"
rewards:
  completed:
    - "<uuid>"
```

monsters.yml mirrors the same structure but with monster keys.

## Extension Points and How to Add a New Checklist

To add another checklist type (e.g., Advancements, Structures):
1. Create a new Service extending ChecklistService.
   - Implement data model (per‑player sets, global sets, firsts, current period, config/file fields).
   - Implement `load()` and `save()` using `loadRewards(config)` and `saveRewards(config)`.
2. Implement appropriate Listener(s) to record progress.
3. Add a SubCommand to view progress and administrative functions if needed.
4. Register the service and listener(s) in Furious.onEnable(), and save in onDisable().
5. Determine completion criteria and ensure you use `grantCompletionReward()` when met.

## Recommendations / Potential Improvements

- Configurable reward amounts via YAML for each checklist (currently hardcoded in listeners).
- Shared utility for enumerating targets (biomes/monsters) with caching at startup to reduce repeated Registry traversal.
- Add persistence versioning/migrations for future format changes.
- Add tests for canonicalization and completion counting to avoid edge inconsistencies.
- Rate limit action bar/broadcasts if needed to avoid spam in crowded servers.
- Optional leaderboards summarizing per‑month and per‑year first counts.

## Summary

The Checklist system is now centralized around ChecklistService for consistent reward granting and persistence. BiomesService and MonstersService follow the same design pattern, making it straightforward to add new checklist types. Commands and listeners provide user visibility and automated tracking, while YAML files keep state across restarts. The system is resilient across Bukkit/Minecraft versions due to defensive API usage and provides clear administrative controls for maintaining firsts.

## 2025-08 Update

This section updates the Checklist analysis to reflect recent changes introduced up to 2025-08-21.

- Checklist interface contract:
  - There is now a minimal interface `com.spillhuset.furious.services.Checklist.Checklist` implemented by `ChecklistService` and concrete services.
  - Contract methods:
    - `void load()` and `void save()` for persistence.
    - `boolean isCompletionRewarded(UUID playerId)` and `void markCompletionRewarded(UUID playerId)` for idempotent completion payouts.
    - `boolean markPaidOnce(String marker)` generic once-only payout marker for arbitrary milestones; persisted across restarts.
    - `void grantCompletionReward(UUID playerId, double amount, String checklistName)` unified completion reward + message.
    - `default void clearAllFirsts()` no-op by default; services tracking firsts should override.

- Rewards persistence keys:
  - In addition to `rewards.completed`, services now persist `rewards.paid` (a list of opaque markers) used by `markPaidOnce(...)` to guarantee once-only rewards for various achievements (e.g., server-first for a specific key, your-first, 50%, etc.).
  - YAML example additions:
    -
      ```yaml
      rewards:
        completed:
          - "<uuid>"
        paid:
          - "player-first:Taming:<uuid>:minecraft:wolf"
          - "server-first:Taming:minecraft:wolf"
      ```

- New Taming checklist:
  - Service: `TamingService` (extends `ChecklistService`).
    - File: `plugins/Furious/taming.yml` with the same structure pattern as biomes/monsters (players/global/first/year/month/rewards.completed/rewards.paid).
    - Tracks per-player tamed tameable entities and global/firsts (server/year/month). Overrides `clearAllFirsts()` to clear all firsts and global taming.
  - Listener: `TamingTrackListener`
    - Handles `EntityTameEvent` to record tames, send action-bar feedback, broadcast server-firsts, and pay tiered rewards via the markers system.
    - Reward keys (read first from `taming.yml` under `rewards.amounts.*`, then fallback to `config.yml` under `rewards.*`, with defaults in code):
      - `rewards.your-first-Taming` (default 10)
      - `rewards.server-first-per-Taming` (default 1000)
      - `rewards.this-year-first-per-Taming` (default 500)
      - `rewards.this-month-first-per-Taming` (default 500)
      - `rewards.50%-Taming` (default 2000)
      - `rewards.complete-Taming` (default 10000) used when all tameable types are tamed; paid via `grantCompletionReward(...)`.
    - Marker patterns written into `rewards.paid`:
      - `player-first:Taming:<player-uuid>:<entity-key>`
      - `server-first:Taming:<entity-key>`
      - `year-first:Taming:<year>:<entity-key>`
      - `month-first:Taming:<yyyy-MM>:<entity-key>`
      - `50pct:Taming:<player-uuid>`
  - Command: `/checklist taming` (permission: `furious.checklist.taming`)
    - `/checklist taming` shows your tamed checklist; for each tameable shows ✔/✘ and firsts suffix (month/year/server) and totals.
    - `/checklist taming <player>` shows another player (requires elevated permission via can()).
    - `/checklist taming <tameable>` shows firsts for that type.
    - Admin maintenance:
      - `/checklist taming clear month`
      - `/checklist taming clear year`
      - `/checklist taming clear all confirm` (dangerous; also clears global taming)

- Cross-check with existing sections:
  - Where this document previously only listed Biomes and Monsters, consider those patterns to also apply to Taming (same firsts model, same reward/persistence helpers).
  - The Reward System section still applies; amounts for Taming are read from `config.yml` via `rewards.*` keys listed above.

- Minor clarifications:
  - Period handling is centralized in `ChecklistService.ensurePeriodCurrent()`; services call it before using firsts maps to ensure `currentYear`/`currentMonth` are valid.
  - Staff/ops actions are not counted towards global/firsts in listeners (`includeGlobal=false`).
