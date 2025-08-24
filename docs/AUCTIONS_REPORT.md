# Auctions System Report

Date: 2025-08-24 19:34

This document provides an overview and technical report of the Auctions feature implemented in the Furious plugin. It covers command usage, permissions, persistence format, flows, constraints, and maintenance considerations.

## Scope and Goals
The Auctions system enables players to list items for auction inside specific claimed chunks, place bids, and buy out items. Operators manage where auctions are allowed and can set a global open/closed state and a teleport anchor.

The implementation aims to be minimal, aligned with the project’s Commands + Services pattern, with YAML-based persistence and ArmorStand-based teleport anchors.

## Commands Overview

All base commands are registered under `/auctions`.

Operator subcommands (default permission op):
- `/auctions claim` — claims the current chunk for Auctions (must be inside SAFE or WAR guild territory).
- `/auctions unclaim` — unclaims the current chunk for Auctions.
- `/auctions open <true|false>` — sets the global Auctions state as open or closed.
- `/auctions spawn` — creates an invisible marker ArmorStand and stores the location as Auctions teleport anchor.
- `/auctions unspawn` — removes the stored Auctions teleport anchor and deletes the ArmorStand if present.
- `/auctions teleport` — teleports to the stored Auctions anchor.

Player subcommands (default true under furious.auctions):
- `/auctions set <name> <start> [buyout] [hours]` — lists the item in main hand for auction.
  - name: a unique, player-defined string; unique per auction (case-insensitive key)
  - start: starting bid (double)
  - buyout: optional buyout price (double)
  - hours: optional duration in hours (integer). If set, an end timestamp is stored and an expiration scheduler will return items to the owner once the time elapses.
- `/auctions list [playerName]` — lists auctions with details; optionally filter by owner name.
- `/auctions bid <name> <offer>` — places a bid on the named auction.
- `/auctions buyout <name>` — displays cost; requires confirmation.
- `/auctions buyout <name> confirm` — charges the buyer, pays the seller 90%, gives the buyer the item, and removes the auction.
- `/auctions cancel <name>` — owner-only; logically the same as buyout confirm with the owner as buyer (owner pays buyout, receives 90% back, item is returned, auction removed).

## Permissions
Declared in `plugin.yml`:
- `furious.auctions` — base permission (default: true)
- `furious.auctions.claim` — claim chunks for Auctions (default: op)
- `furious.auctions.unclaim` — unclaim chunks for Auctions (default: op)
- `furious.auctions.open` — open/close Auctions globally (default: op)
- `furious.auctions.spawn` — set or remove the Auctions teleport anchor (covers `/auctions spawn` and `/auctions unspawn`, default: op)
- `furious.auctions.teleport` — teleport to the Auctions anchor (default: op)

Player subcommands (set, list, bid, buyout, cancel) use the base `furious.auctions` permission.

## User Flow Details

1. Admin setup
   - Navigate to a SAFE or WAR guild-claimed chunk.
   - Run `/auctions claim` to enable Auctions in that chunk. Repeat for all desired chunks.
   - Optionally, run `/auctions spawn` at the preferred teleport point.
   - Ensure global Auctions are open with `/auctions open true`.

2. Player listing an item
   - Stand inside any Auctions-claimed chunk.
   - Hold the item stack in your main hand.
   - Run `/auctions set <name> <start> [buyout] [hours]`.
   - The entire main-hand stack is removed from the player and stored as the auction item.

3. Bidding
   - Stand inside an Auctions-claimed chunk.
   - Run `/auctions bid <name> <offer>` — offer must be strictly greater than current bid.

4. Buyout
   - Run `/auctions buyout <name>` to see the cost and confirmation hint.
   - Run `/auctions buyout <name> confirm` to complete:
     - Buyer is charged the full buyout price.
     - Seller receives 90% (10% fee applied).
     - Item is granted to buyer (or dropped at their feet if inventory is full).
     - Auction is removed.

5. Cancel (owner only)
   - Run `/auctions cancel <name>`:
     - Owner is charged the buyout.
     - Owner receives 90% back (effective 10% cancellation fee).
     - Item is returned (or dropped if inventory is full).
     - Auction is removed.

## Open/Closed Behavior
- The global open flag is enforced for `/auctions set`, `/auctions bid`, `/auctions buyout`, and `/auctions cancel` for non-ops.
- Operators (OP) can bypass the closed state for these checks as implemented.

## Teleport Anchor
- `/auctions spawn` places an invisible, named ArmorStand at the player’s location and stores the location and the ArmorStand UUID.
- `/auctions unspawn` removes the stored anchor and deletes the ArmorStand if present.
- `/auctions teleport` sends the player to the stored location.
- Setting a new spawn removes the previous ArmorStand if it still exists.
- Unclaiming chunks does not remove the spawn automatically; use `/auctions unspawn` to clear it when needed.

## Area Restrictions
- `/auctions set` and `/auctions bid` require the player to stand inside any claimed Auctions chunk.
- `/auctions list`, `/auctions buyout`, and `/auctions cancel` have no area restriction.

## Persistence
Data is stored in `plugins/Furious/auctions.yml`.

Top-level keys:
- `open` — boolean, whether Auctions are open globally.
- `claims` — set of claimed chunk entries with world UUID and chunk coordinates.
- `spawn` — location and armor stand UUID for teleport anchor.
- `auctions` — map of auction entries keyed by lowercased name.
- `returns` — pending item returns queued for offline owners, delivered on next login.

Auction entry fields:
- `name` — display name (original case preserved).
- `owner` — UUID of the player who created the auction.
- `item` — serialized ItemStack of the entire original main-hand stack.
- `start` — starting bid (double).
- `buyout` — optional buyout price (double).
- `end` — epoch milliseconds when the auction is set to end (auto-handled by a scheduler that returns items and notifies users).
- `current` — current bid value (double).
- `bidder` — optional UUID of the current highest bidder.

## Expiration Handling
- A scheduler runs every 60 seconds to auto-handle auction expiration based on the `end` timestamp.
- When an auction expires, the item is returned to the owner. If the owner is online, the item is delivered immediately; otherwise it is stored as a pending return and delivered automatically on the owner's next login.
- The current highest bidder (if any) is notified if they are online that the auction expired with no winner.

## Notes, Limitations, and Trade-offs
- Bids do not lock funds and therefore do not require refunds if auctions are canceled, bought out, or expire. This keeps the system simple, but it means bids are "soft" commitments.
- `/auctions set` removes the entire stack from the main hand. If a single item is intended, split the stack before listing.
- The open state is only enforced for set and bid. You can optionally extend enforcement to buyout/cancel.
- Teleport anchor is not auto-removed on unclaim.

## Troubleshooting
- “This chunk is not claimed by a guild.” — Ensure you are standing inside an existing guild claim.
- “Auctions must be in SAFE or WAR guild.” — Only SAFE or WAR type guild territories can host Auctions.
- “Auctions are currently closed.” — Ask an operator to open with `/auctions open true` or try again later.
- “An auction with that name already exists.” — Use a unique name; names are case-insensitive keys.
- “You cannot afford this buyout.” — Ensure sufficient balance in wallet.
- Teleport anchor missing — Run `/auctions spawn` again to set or reset anchor, then `/auctions teleport`.

## Admin Maintenance Tips
- To relocate Auctions areas, unclaim and claim chunks as needed. Multiple chunks can be claimed.
- If the marker ArmorStand is missing after a restart or chunk changes, re-run `/auctions spawn`.
- Consider scheduled maintenance: adding a task to auto-handle `end` timestamps.
- Economy records use WalletService logs (descriptive memo strings) for buyout and cancel actions.

## Implementation Summary
- Service: `com.spillhuset.furious.services.AuctionsService`
- Command router: `com.spillhuset.furious.commands.AuctionsCommand`
- Subcommands:
  - `com.spillhuset.furious.commands.AuctionsCommands.*` (ClaimCommand, UnclaimCommand, OpenCommand, SpawnCommand, UnspawnCommand, TeleportCommand, SetCommand, ListCommand, BuyoutCommand, CancelCommand, BidCommand)
- Registration: in `Furious.onEnable()` (creates and loads AuctionsService, registers auctions command; PlayerJoinListener delivers pending returns on login).
- Permissions: declared in `src/main/resources/plugin.yml`. 

## Future Enhancements (Optional)
- Admin commands to list all claimed chunks or unclaim all.
