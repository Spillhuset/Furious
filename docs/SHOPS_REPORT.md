# Shops System Report

Date: 2025-08-24 16:02

This document provides an overview and technical report of the Shops feature implemented in the Furious plugin. It covers command usage, permissions, persistence format, user flows, constraints, and maintenance considerations.

## Scope and Goals
The Shops system enables operators to define shop areas and set prices for buying and selling items. Players can buy from and sell to these shops while standing in a claimed shop chunk. Shops optionally expose a teleport anchor using an ArmorStand marker that is visible only to operators.

The implementation follows the project’s Commands + Services pattern and uses YAML-based persistence.

## Key Concepts
- Shop: Named entity with an ID, optional single claimed chunk, global price toggles (buy/sell) with per-shop prices, optional spawn location and ArmorStand UUID (for teleport anchor/marker), and a type (PLAYER or GUILD) for metadata.
- ItemEntry: Per-material configuration within a shop, holding stock count, enable/disable flags and prices for buy and sell pathways.
- Claimed chunk: A shop can be placed inside a guild claim of type SAFE or WAR. Shops currently support a single claimed chunk per shop (by fields world/chunkX/chunkZ).

## Commands Overview
All base commands are registered under `/shops`.

Operator subcommands (default permissions op):
- `/shops create <name>` — create a shop record.
- `/shops delete <name>` — delete a shop; removes its marker if any.
- `/shops claim <shopName>` — claim the current chunk for the shop. Must be inside a SAFE or WAR guild territory.
- `/shops unclaim <shopName>` — unclaim the shop (clears claim fields; does not alter items/prices).
- `/shops type <shopName> <player|guild>` — set shop type metadata.
- `/shops setbuyprice <shopName> <price|'-'|0>` — set or disable the global buy price for the shop (`-` disables; `0` means free).
- `/shops setsellprice <shopName> <price|'-'|0>` — set or disable the global sell price for the shop (`-` disables; `0` means free).
- `/shops spawn <shopName>` — set the shop teleport anchor at your current location and create/move an ArmorStand marker. Marker visibility is op-only.
- `/shops teleport <shopName>` — teleport to the shop’s anchor.
- `/shops stock <shopName> <material> <amount>` — set absolute stock count for an item in the shop.
- `/shops restock <shopName> <material> <amount>` — increase stock for an item by amount.
- `/shops additem <shopName> <material> <stock> <buyPrice|'-'|0> <sellPrice|'-'|0>` — add or update a single material’s configuration.
- `/shops removeitem <shopName> <material>` — remove a material entry from the shop.

Player subcommands (default permissions true):
- `/shops list` — list all shops with basic info (type and guild location).
- `/shops listitem <shopName>` — list configured items for a shop (buy/sell status and prices, and stock). Non-ops must be standing in any shop area to use this.
- `/shops buy <material> <amount>` — purchase items while standing inside a shop-claimed chunk; price and stock are validated.
- `/shops sell <material> <amount>` — sell items to the shop while standing inside a shop-claimed chunk; price and inventory are validated.

Notes:
- In practice, “global” buy/sell prices exist on the Shop object, and also per-item entries have their own buy/sell prices and enabled flags. The effective price used for buy/sell is the per-item price from the ItemEntry (as implemented in ShopsService.buy/sell), not the global price fields.

## Permissions
Declared in `src/main/resources/plugin.yml` (as of this report):
- `furious.shops` — base permission (default: true)
- `furious.shops.claim` — claim chunks for a shop (default: op)
- `furious.shops.unclaim` — unclaim shop (default: op)
- `furious.shops.setbuyprice` — set shop buy price (default: op)
- `furious.shops.setsellprice` — set shop sell price (default: op)
- `furious.shops.create` — create shop (default: op)
- `furious.shops.delete` — delete shop (default: op)
- `furious.shops.spawn` — set shop spawn/marker (default: op)
- `furious.shops.teleport` — teleport to shop (default: op)
- `furious.shops.list` — list shops (default: true)
- `furious.shops.additem` — add/update item entry (default: op)
- `furious.shops.removeitem` — remove item entry (default: op)
- `furious.shops.listitem` — list items (default: true)
- `furious.shops.buy` — buy items (default: true)
- `furious.shops.sell` — sell items (default: true)
- `furious.shops.stock` — set absolute stock (default: op)
- `furious.shops.restock` — increase stock (default: op)
- `furious.shops.type` — set shop type metadata (default: op)

## User Flow Details
1. Admin setup
   - Stand in a guild claim of type SAFE or WAR.
   - Run `/shops create <name>`.
   - Run `/shops claim <name>` to associate the current chunk with the shop.
   - Configure prices and items using `setbuyprice`, `setsellprice`, `additem`, and stock commands.
   - Optionally set a teleport anchor using `/shops spawn <name>`.

2. Player browsing
   - Use `/shops list` to see available shops.
   - Use `/shops listitem <name>` to see items and prices (if you’re a non-op, you must be standing in a shop area).

3. Buying
   - Stand inside the shop’s claimed chunk.
   - Use `/shops buy <material> <amount>`.
   - The service checks: item exists, buy enabled, stock sufficient, and your wallet balance (if price > 0). Items are delivered to inventory; transaction is logged in WalletService. If inventory is full, the charge is refunded and the operation fails with feedback.

4. Selling
   - Stand inside the shop’s claimed chunk.
   - Use `/shops sell <material> <amount>`.
   - The service checks: item exists, sell enabled, sufficient items in your inventory, and then credits your wallet (if price > 0). Items are removed from your inventory and added to shop stock.

## Area Restrictions and Claiming Rules
- Area restriction: `/shops buy` and `/shops sell` require the player to be in the shop’s claimed chunk (ShopsService.getShopAt). `/shops listitem` enforces area for non-ops.
- Claiming: `/shops claim` validates the current chunk using GuildService: it must be inside a guild claim of type SAFE or WAR. Otherwise, an error is shown. Unclaim simply clears the claim from the shop.

## Teleport Anchor and Marker
- `/shops spawn` stores a spawn location for the shop and spawns or moves an ArmorStand there. The stand name is “Shop: <name>”.
- Visibility: For each viewer, ops see the stand while non-ops have it hidden via `showEntity/hideEntity`.
- `/shops teleport` sends the player to the stored spawn location. If the world is missing or spawn is unset, the command fails with feedback.
- If the ArmorStand entity disappears (chunk unload/entity removed), the next `ensureArmorStands()` call respawns/registers and re-applies visibility.

## Persistence
Data is stored in `plugins/Furious/shops.yml`.

Top-level structure:
- `shops` — map keyed by shopId UUID
  - `name` — shop name
  - `type` — `PLAYER` or `GUILD`
  - `claimed` — boolean
  - `world` — UUID of world (present if claimed)
  - `chunkX`/`chunkZ` — ints for claimed chunk coordinates
  - `buy.enabled` — boolean (shop-level)
  - `buy.price` — double
  - `sell.enabled` — boolean (shop-level)
  - `sell.price` — double
  - `spawn` — section (present if set)
    - `world` — UUID
    - `x`, `y`, `z` — doubles
    - `yaw`, `pitch` — doubles
    - `armorStand` — ArmorStand UUID
  - `items` — map keyed by material name
    - `<MATERIAL>.stock` — int
    - `<MATERIAL>.buy.enabled` — boolean
    - `<MATERIAL>.buy.price` — double
    - `<MATERIAL>.sell.enabled` — boolean
    - `<MATERIAL>.sell.price` — double

## Edge Cases and Validations
- Creating a shop with an existing name is rejected.
- Claiming outside a guild claim or in a non-SAFE/WAR guild type is rejected.
- `buy` rejects when item is not configured, buy disabled, insufficient stock, insufficient wallet balance, or player inventory lacks space.
- `sell` rejects when item not configured, sell disabled, or player lacks required items.
- Removing spawn ArmorStand externally: the service clears the spawn association when the registered removal callback fires and will persist that change.
- Teleport fails gracefully if spawn unset or world missing.

## Notes, Limitations, and Trade-offs
- Shops currently store a single claimed chunk (not a multi-claim list like Banks). If expanding to multiple chunks, model and persistence changes would be required.
- There are two layers of pricing: shop-level and per-item. The current buy/sell commands use per-item pricing; shop-level prices are stored and persisted but not used by the transaction logic. They can be seen as defaults or legacy fields.
- ArmorStand markers are visible only to ops by design.
- There is no global open/closed toggle for shops; access is governed by area restriction and per-item enable flags.

## Troubleshooting
- “Shop not found.” — Ensure the shop name exists. Use `/shops list`.
- “You must be inside a shop to buy/sell.” — Move into the claimed shop chunk.
- “Item not available/accepted.” — The material is not configured for buy/sell.
- “Buying/Selling disabled for this item.” — Enable via `additem` or set per-item prices accordingly.
- “Not enough stock.” — Ask an operator to restock.
- “Insufficient funds.” — Check your wallet balance.
- “Not enough inventory space.” — Free up slots.
- Teleport errors — Ensure spawn is set (`/shops spawn <name>`) and the world is loaded.

## Implementation Summary
- Service: `com.spillhuset.furious.services.ShopsService`
- Models: `com.spillhuset.furious.utils.Shop`, `com.spillhuset.furious.utils.ShopType`
- Command router: `com.spillhuset.furious.commands.ShopsCommand`
- Subcommands: `com.spillhuset.furious.commands.ShopsCommands.*` (Create, Delete, Spawn, Teleport, Claim, Unclaim, Type, SetSellPrice, SetBuyPrice, Stock, Restock, List, AddItem, RemoveItem, ListItem, Buy, Sell)
- Registration: in `Furious.onEnable()` (creates and loads ShopsService, registers shops command)
- Permissions: declared in `plugin.yml`

## Future Enhancements (Optional)
- Consider multi-claim support for shops similar to Banks.
- Add a command to toggle shop visibility/marker per viewer or globally.
- Add a global “open/closed” state per shop (with op bypass) if desired.
- Introduce per-guild or per-player pricing via ShopType or extensions.
- GUI-based shop interactions for better UX.
