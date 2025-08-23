# Furious Plugin — Player Guide

Welcome to Furious! This guide helps players understand the main features and how to use the commands you have access to by default. If your server has different permissions, some commands may be restricted.

Tip: Typing a base command without arguments usually shows a list of subcommands you can use. For example: `/homes`.

## Economy: Wallet
- Check balance: `/wallet balance`
- Pay another player: `/wallet pay <player> <amount>`

Notes:
- Some servers may allow you to also view others’ balances (permission: `furious.wallet.balance.others`), but that is typically for staff.

## Homes
Set and manage personal homes for easy teleporting.
- Set a home at your location: `/homes set <name>`
- Teleport to a home: `/homes teleport <name>` (alias might be `/homes tp <name>` if provided by the server)
- List your homes: `/homes list`
- Rename a home: `/homes rename <old> <new>`
- Move a home to your current location: `/homes move <name>`
- Remove a home: `/homes remove <name>`
- Buy an extra home slot (if server uses limits/costs): `/homes buy`

Notes:
- Operators can manage other users’ homes; regular players won’t have those permissions.

## Guilds (Clans)
Form and manage a guild, claim land, and collaborate with members.
- Create a guild: `/guild create <name>`
- Rename your guild: `/guild rename <newName>`
- Delete your guild: `/guild delete`
- Invite a player: `/guild invite <player>`
- Accept an invite: `/guild accept`
- Decline an invite: `/guild decline`
- Kick a member: `/guild kick <player>`
- Promote a member: `/guild promote <player>`
- Demote a member: `/guild demote <player>`
- Claim land: `/guild claim`
- Unclaim land (current): `/guild unclaim`
- View nearby guild claims map: `/guild connectivity`
- Unclaim all (staff-only on most servers): `/guild unclaims`
- Make guild open/closed: `/guild setopen <true|false>`
- Request to join an open guild: `/guild join <name>`
- Show info about a guild: `/guild info [name]`
- List guilds: `/guild list`
- Check world rules for guilds: `/guild worlds`

Notes:
- Some features like mass unclaim and cross-guild moderation are limited to staff.

## Warps
Public or protected warp points across the server.
- Teleport to a warp: `/warps teleport <name>`
- List available warps: `/warps list`

Notes:
- Creating/renaming/moving warps are typically staff-only.

## Banks
Manage bank accounts where supported by the server.
- Create a personal account: `/banks createaccount <name>`
- Deposit money: `/banks deposit <account> <amount>`
- Withdraw money: `/banks withdraw <account> <amount>`
- Delete your account: `/banks deleteaccount <name>`
- List banks or accounts: `/banks list`

Notes:
- Creating/claiming banks and setting interest are staff-only.

## Shops
Trade items with server shops where available.
- List shops: `/shops list`
- See items a shop trades: `/shops listitem <shop>`
- Buy from a shop: `/shops buy <shop> <item> <amount>`
- Sell to a shop: `/shops sell <shop> <item> <amount>`

Notes:
- Managing shops (create/delete/prices/stock) is for staff or shop owners when enabled by the server.

## Player-to-Player Teleport Requests
Request and manage teleports between players.
- Request teleport to someone: `/teleport request <player>`
- Accept someone’s request: `/teleport accept`
- Decline a request: `/teleport decline`
- Cancel your outgoing request: `/teleport cancel`
- Deny all future requests (toggle): `/teleport deny`

## Locks
Lock simple blocks/containers if enabled by the server.
- Lock a block you’re looking at or interacting with: `/locks lock`
- Unlock it: `/locks unlock`

Notes:
- Keys and world rules may apply depending on server settings.

## Tombstones (Death Chests)
Players don’t usually manage tombstones directly. Staff may clear old tombstones; yours will appear on death if enabled by the server.

## Permissions Summary (what most players have)
Servers can change these, but by default you typically have:
- `furious.wallet`, `furious.wallet.balance`, `furious.wallet.pay`
- `furious.homes` including: `set`, `remove`, `teleport`, `list`, `move`, `rename`, `buy`
- `furious.guild` core actions like `create`, `rename`, `delete`, `invite`, `accept`, `decline`, `kick`, `promote`, `demote`, `claim`, `unclaim`, `setopen`, `join`
- `furious.warps.teleport` (warp travel) and possibly `furious.warps` access
- `furious.banks` basic: `createaccount`, `deposit`, `withdraw`, `deleteaccount`
- `furious.shops` basic: `list`, `listitem`, `buy`, `sell`
- `furious.teleport` request suite: `request`, `accept`, `decline`, `cancel`, `deny`
- `furious.locks`

If you can’t use a command you see in this guide, ask a server operator to confirm your permissions.
