# Comprehensive Permission Reference Guide

## Table of Contents
- [Introduction](#introduction)
- [Permission Inheritance System](#permission-inheritance-system)
- [Enhanced Wildcard Matching](#enhanced-wildcard-matching)
- [Permission Caching](#permission-caching)
- [GUI-Based Permission Management](#gui-based-permission-management)
- [Permission Presets](#permission-presets)
- [Economy Permissions](#economy-permissions)
  - [Wallet Permissions](#wallet-permissions)
  - [Bank Permissions](#bank-permissions)
  - [Shop Permissions](#shop-permissions)
- [Teleportation Permissions](#teleportation-permissions)
  - [Teleport Permissions](#teleport-permissions)
  - [Homes Permissions](#homes-permissions)
  - [Warps Permissions](#warps-permissions)
- [Guild Permissions](#guild-permissions)
- [Security Permissions](#security-permissions)
  - [Locks Permissions](#locks-permissions)
  - [Tombstone Permissions](#tombstone-permissions)
- [Utility Permissions](#utility-permissions)
  - [Inventory Management](#inventory-management)
  - [Player Commands](#player-commands)
  - [Rate Limiting](#rate-limiting)
- [Permission Management](#permission-management)
- [Common Server Setups](#common-server-setups)
  - [Basic Server](#basic-server)
  - [Economy-Focused Server](#economy-focused-server)
  - [RPG Server](#rpg-server)
  - [Survival Server](#survival-server)
- [Migration Guide](#migration-guide)

## Introduction

This document provides a comprehensive reference for all permissions in the furious plugin. It includes information about permission inheritance, default values, and recommended configurations for different server setups.

Permissions in furious follow a consistent naming pattern:
- `furious.<feature>.<action>` - Basic permission for a specific action
- `furious.<feature>.<action>.others` - Permission to perform an action on other players
- `furious.<feature>.admin.<action>` - Administrative permission for a specific action
- `furious.<feature>.*` - Grants all permissions for a specific feature
- `furious.<feature>.admin.*` - Grants all administrative permissions for a specific feature

## Permission Inheritance System

The furious plugin uses a structured permission inheritance system where higher-level permissions automatically grant related lower-level permissions. This system follows these principles:

1. **Wildcard Permissions**: Permissions ending with `.*` grant all child permissions
   - Example: `furious.guild.*` grants all guild-related permissions
   - Example: `furious.bank.admin.*` grants all administrative bank permissions

2. **Hierarchical Structure**: Permissions are organized in a hierarchical structure
   - Top level: Feature category (e.g., `furious.guild`, `furious.bank`)
   - Second level: Operation type (e.g., `furious.guild.admin`, `furious.bank.add`)
   - Third level: Target specification (e.g., `furious.bank.add.others`)

3. **Automatic Inheritance**: Higher-level permissions automatically grant lower-level permissions
   - If a player has `furious.homes.*.others`, they automatically have all permissions like `furious.homes.set.others`, `furious.homes.delete.others`, etc.
   - If a player has `furious.teleport.admin.*`, they automatically have permissions like `furious.teleport.worldconfig`, `furious.teleport.coords`, etc.

4. **Granular Control**: The system allows for precise control over permissions
   - You can grant specific permissions without granting entire categories
   - You can create custom permission sets for different staff roles

**Technical Note**: The permission system now supports enhanced wildcard matching with multi-level inheritance and middle wildcards. This means that permissions like "furious.*" will match all permissions that start with "furious.", including "furious.bank.balance". Additionally, wildcards can appear anywhere in the permission node, allowing patterns like "furious.*.admin" to match permissions like "furious.bank.admin" and "furious.guild.admin".

## Enhanced Wildcard Matching

The permission system now features enhanced wildcard matching capabilities that provide more flexibility and power in permission management.

### How Enhanced Wildcard Matching Works

1. **Multi-Level Inheritance**: The enhanced system supports multi-level inheritance for wildcard permissions
   - A permission like `furious.*` now grants all permissions that start with "furious."
   - This works for any level of the permission hierarchy

2. **Middle Wildcards**: Wildcards can now appear anywhere in the permission node, not just at the end
   - Example: `furious.*.admin` matches permissions like `furious.bank.admin` and `furious.guild.admin`
   - Example: `furious.homes.*.others` matches all permissions for managing other players' homes

3. **Pattern Matching**: The system uses sophisticated pattern matching for permissions
   - Permissions are converted to regex patterns for flexible matching
   - The system handles complex permission structures efficiently

### Examples of Enhanced Wildcard Matching

1. **Feature-Wide Administrative Access**:
   - `furious.*.admin` grants admin permissions across all features
   - This matches `furious.bank.admin`, `furious.guild.admin`, `furious.homes.admin`, etc.

2. **Cross-Feature Operations**:
   - `furious.*.*.others` grants all permissions for operations on other players
   - This matches `furious.homes.set.others`, `furious.bank.balance.others`, etc.

3. **Specific Feature Wildcards**:
   - `furious.homes.*` grants all home-related permissions
   - `furious.bank.*` grants all bank-related permissions

## Permission Caching

To improve performance, especially on servers with many players and permissions, the system now includes a permission caching mechanism.

### How Permission Caching Works

1. **Result Caching**: The system caches the results of permission checks
   - When a permission is checked, the result is stored in memory
   - Subsequent checks for the same permission use the cached result
   - This significantly reduces CPU usage for permission checks

2. **Cache Invalidation**: The cache is automatically invalidated when permissions change
   - When permissions are added or removed, the cache is cleared
   - This ensures that permission changes take effect immediately

3. **Configurable Caching**: The caching system can be configured based on server needs
   - Cache size limits prevent memory issues
   - Cache expiration can be set to balance performance and freshness

### Benefits of Permission Caching

1. **Improved Performance**: Faster permission checks, especially for frequently checked permissions
2. **Reduced Server Load**: Less CPU usage for permission operations
3. **Better Scalability**: More efficient handling of large permission sets and many players

### Managing the Permission Cache

- The cache is automatically managed by the system
- To manually clear the cache (rarely needed):
  ```
  /permissions cache clear
  ```

## GUI-Based Permission Management

A new graphical user interface (GUI) has been added to make permission management more intuitive and user-friendly.

### Accessing the Permission GUI

To open the permission management GUI:
```
/permissions gui
```

Requirements:
- You must have the `furious.permission.*` permission

### GUI Features

The permission management GUI includes several sections:

1. **Browse Permissions**: Browse and edit all permissions in the system
   - View permissions by category
   - Toggle permissions on/off with a click
   - Search for specific permissions

2. **Search Permissions**: Search for specific permissions
   - Find permissions by name
   - Filter by category or feature

3. **Permission Presets**: Apply predefined permission sets
   - View available presets
   - Apply presets to players or roles

4. **Player Permissions**: Manage permissions for specific players
   - View a player's current permissions
   - Add or remove permissions
   - Assign roles to players

5. **Role Editor**: Create and manage permission roles
   - Create new roles
   - Edit role permissions
   - Assign roles to players

6. **Inheritance Visualization**: Visualize permission inheritance relationships
   - See how permissions are inherited
   - Understand the permission hierarchy

### Using the Permission GUI

1. **Browsing Permissions**:
   - Click on "Browse Permissions" in the main menu
   - Navigate through pages of permissions
   - Click on a permission to toggle it

2. **Applying Presets**:
   - Click on "Permission Presets" in the main menu
   - Select a preset to view its permissions
   - Click "Apply Preset" and enter a player or role name

3. **Managing Player Permissions**:
   - Click on "Player Permissions" in the main menu
   - Enter a player name to view their permissions
   - Add or remove permissions as needed

## Permission Presets

The system now includes predefined permission presets that can be applied to players or roles, making permission setup faster and more consistent.

### Available Permission Presets

1. **Admin Preset**: Full access to all server features and administrative tools
   - Includes permissions like `furious.*` and `furious.admin.*`
   - Typically assigned to server administrators only

2. **Moderator Preset**: Tools for helping manage the server and players
   - Includes permissions to view other players' information and manage basic server functions
   - Examples: `furious.teleport.admin`, `furious.homes.*.others`, `furious.guild.admin.*`

3. **Builder Preset**: Special permissions for world building and construction
   - Includes permissions for teleportation and world management
   - Examples: `furious.teleport.coords`, `furious.warps.create`

4. **VIP Preset**: Enhanced gameplay features for donators or special players
   - Includes bypass permissions for cooldowns and costs
   - Examples: `furious.teleport.bypass.cooldown`, `furious.homes.set.extra.5`

5. **Economy Preset**: Focused on economic activities and transactions
   - Includes permissions for bank and wallet management
   - Examples: `furious.bank.*`, `furious.wallet.*`

6. **Survival Preset**: Basic gameplay permissions for regular survival players
   - Includes essential teleportation, homes, and guild features
   - Examples: `furious.teleport.*`, `furious.homes.*`, `furious.guild.*`

### Using Permission Presets

#### Via GUI

1. Open the permission GUI:
   ```
   /permissions gui
   ```

2. Click on "Permission Presets"

3. Select a preset and click "Apply Preset"

4. Enter the player or role name to apply the preset

#### Via Commands

Apply a preset to a player:
```
/permissions preset apply <preset_name> player <player_name>
```

Apply a preset to a role:
```
/permissions preset apply <preset_name> role <role_name>
```

List available presets:
```
/permissions preset list
```

View preset details:
```
/permissions preset info <preset_name>
```

## Economy Permissions

### Wallet Permissions

#### Basic Wallet Permissions
- `furious.wallet` - Allows checking your own wallet balance (default: true)
- `furious.wallet.pay` - Allows paying scraps to other players (default: true)

#### Administrative Wallet Permissions
- `furious.wallet.balance.others` - Allows checking other players' wallet balances (default: op)
- `furious.wallet.add` - Allows adding scraps to players' wallets (default: op)
- `furious.wallet.sub` - Allows subtracting scraps from players' wallets (default: op)
- `furious.wallet.set` - Allows setting players' wallet balances (default: op)

#### Special Wallet Permissions
- `furious.wallet.bypass.cost` - Allows bypassing costs for wallet-integrated features (default: op)
- `furious.wallet.bypass.limit` - Allows bypassing transaction limits (default: op)

#### Wallet Permission Inheritance
- `furious.wallet.*` - Grants all wallet permissions
- `furious.wallet.admin.*` - Grants all administrative wallet permissions (balance.others, add, sub, set)

#### Wallet Feature Limitations
- `furious.wallet.limit.X` - Sets maximum transaction amount to X
- `furious.wallet.cooldown.X` - Sets transaction cooldown to X seconds

### Bank Permissions

#### Basic Bank Permissions
- `furious.bank.balance` - Allows checking bank balance (default: true)
- `furious.bank.deposit` - Allows depositing to bank (default: true)
- `furious.bank.withdraw` - Allows withdrawing from bank (default: true)
- `furious.bank.transfer` - Allows transferring funds between bank accounts (default: true)
- `furious.bank.info` - Allows viewing bank information at current location (default: true)
- `furious.bank.createaccount` - Allows creating your own account in banks (default: true)
- `furious.bank.deleteaccount` - Allows deleting your own account from banks (default: true)

#### Bank Management Permissions
- `furious.bank.claim` - Allows claiming chunks for banks (default: op)
- `furious.bank.unclaim` - Allows unclaiming chunks from banks (default: op)
- `furious.bank.create` - Allows creating banks (default: op)
- `furious.bank.rename` - Allows renaming banks (default: op)
- `furious.bank.delete` - Allows deleting banks (default: op)

#### Granular Balance Management Permissions
- `furious.bank.add` - Allows adding to account balances (default: op)
- `furious.bank.subtract` - Allows subtracting from account balances (default: op)
- `furious.bank.set` - Allows setting account balances to specific values (default: op)
- `furious.bank.add.others` - Allows adding to other players' account balances (default: op)
- `furious.bank.subtract.others` - Allows subtracting from other players' account balances (default: op)
- `furious.bank.set.others` - Allows setting other players' account balances (default: op)

#### Other Administrative Permissions
- `furious.bank.createaccount.others` - Allows creating accounts for other players (default: op)
- `furious.bank.deleteaccount.others` - Allows deleting other players' accounts from banks (default: op)
- `furious.bank.interest` - Allows editing bank interest rates (default: op)
- `furious.bank.withdraw.auto` - Allows automatic withdrawals from bank when making purchases (default: op)

#### Legacy Permissions
- `furious.bank.editbalance` - Grants all balance editing permissions for your own accounts
- `furious.bank.editbalance.others` - Grants all balance editing permissions for other players' accounts
- `furious.bank.admin` - Allows administrative bank operations

#### Bank Permission Inheritance
- `furious.bank.*` - Grants all bank permissions
- `furious.bank.admin.*` - Grants all administrative bank permissions

#### Bank Feature Limitations
- `furious.bank.accounts.limit.X` - Sets maximum number of accounts to X
- `furious.bank.interest.rate.X` - Sets interest rate multiplier to X
- `furious.bank.transfer.limit.X` - Sets maximum transfer amount to X

### Shop Permissions

#### Basic Shop Permissions
- `furious.shops.use` - Allows using shops (default: true)
- `furious.shops.sell` - Allows selling items to shops (default: true)
- `furious.shops.buy` - Allows buying items from shops (default: true)

#### Shop Management Permissions
- `furious.shops.create` - Allows creating shops (default: op)
- `furious.shops.delete` - Allows deleting shops (default: op)
- `furious.shops.claim` - Allows claiming chunks for shops (default: op)
- `furious.shops.unclaim` - Allows unclaiming chunks from shops (default: op)
- `furious.shops.setspawn` - Allows setting shop spawn points (default: op)

#### Item Management Permissions
- `furious.shops.additem` - Allows adding items to shops (default: op)
- `furious.shops.removeitem` - Allows removing items from shops (default: op)
- `furious.shops.setprice` - Allows setting item prices (default: op)
- `furious.shops.setstock` - Allows setting item stock levels (default: op)
- `furious.shops.restock` - Allows restocking shops (default: op)
- `furious.shops.togglebuy` - Allows toggling whether items can be bought from shops (default: op)
- `furious.shops.togglesell` - Allows toggling whether items can be sold to shops (default: op)

#### Granular Admin Permissions
- `furious.shops.create.others` - Allows creating shops for other players (default: op)
- `furious.shops.delete.others` - Allows deleting other players' shops (default: op)
- `furious.shops.manage.others` - Allows managing other players' shops (default: op)
- `furious.shops.price.others` - Allows setting prices in other players' shops (default: op)
- `furious.shops.stock.others` - Allows managing stock in other players' shops (default: op)

#### Legacy Permissions
- `furious.shops.admin` - Allows access to all shop administration commands (default: op)

#### Shop Permission Inheritance
- `furious.shops.*` - Grants all shops permissions
- `furious.shops.admin.*` - Grants all administrative shops permissions

## Teleportation Permissions

### Teleport Permissions

#### User Permissions
- `furious.teleport.request` - Allows requesting teleports to other players (default: true)
- `furious.teleport.accept` - Allows accepting teleport requests (default: true)
- `furious.teleport.decline` - Allows declining teleport requests (default: true)
- `furious.teleport.list` - Allows listing teleport requests (default: true)
- `furious.teleport.abort` - Allows aborting outgoing teleport requests (default: true)
- `furious.teleport.deny` - Allows toggling auto-deny of teleport requests (default: true)

#### Admin Permissions
- `furious.teleport.worldconfig` - Allows configuring world teleport settings (default: op)
- `furious.teleport.coords` - Allows using coordinate teleport commands (default: op)
- `furious.teleport.force` - Allows using force teleport commands (default: op)
- `furious.teleport.worlds` - Allows viewing world teleport settings (default: op)
- `furious.teleport.worldspawn` - Allows teleporting to world spawn (default: op)
- `furious.teleport.setworldspawn` - Allows setting world spawn location (default: op)

#### Special Permissions
- `furious.teleport.admin` - Allows bypassing teleport queue, effects, costs, and cooldowns (default: op)

#### Teleport Permission Inheritance
- `furious.teleport.*` - Grants all teleport permissions
- `furious.teleport.admin.*` - Grants all administrative teleport permissions

#### Teleport Feature Limitations
- `furious.teleport.cooldown.X` - Sets teleport cooldown to X seconds
- `furious.teleport.cost.X` - Sets teleport cost to X
- `furious.teleport.bypass.cooldown` - Allows bypassing teleport cooldowns
- `furious.teleport.bypass.cost` - Allows teleporting without paying costs

### Homes Permissions

#### Basic Homes Permissions
- `furious.homes.set` - Allows setting homes (default: true)
- `furious.homes.move` - Allows moving homes (default: true)
- `furious.homes.rename` - Allows renaming homes (default: true)
- `furious.homes.delete` - Allows deleting homes (default: true)
- `furious.homes.list` - Allows listing homes (default: true)
- `furious.homes.tp` - Allows teleporting to homes (default: true)
- `furious.homes.buy` - Allows purchasing additional home slots (default: true)

#### World Management Permissions
- `furious.homes.world` - Allows managing world settings for homes (default: op)

#### Granular Admin Permissions
- `furious.homes.set.others` - Allows setting homes for other players (default: op)
- `furious.homes.delete.others` - Allows deleting other players' homes (default: op)
- `furious.homes.move.others` - Allows moving other players' homes (default: op)
- `furious.homes.rename.others` - Allows renaming other players' homes (default: op)
- `furious.homes.tp.others` - Allows teleporting to other players' homes (default: op)
- `furious.homes.list.others` - Allows listing other players' homes (default: op)

#### Legacy Admin Permissions
- `furious.homes.admin` - Allows managing other players' homes (grants all granular admin permissions)

#### Home Permission Inheritance
- `furious.homes.*` - Grants all homes permissions and unlimited homes
- `furious.homes.*.others` - Grants all administrative homes permissions

#### Home Limit Permissions
- `furious.homes.limit.1` - Sets home limit to 1 (default: false)
- `furious.homes.limit.2` - Sets home limit to 2 (default: false)
- `furious.homes.limit.3` - Sets home limit to 3 (default: true)
- `furious.homes.limit.5` - Sets home limit to 5 (default: false)
- `furious.homes.limit.10` - Sets home limit to 10 (default: false)
- `furious.homes.limit.custom` - Custom home limit set by server administrators

### Warps Permissions

#### User Permissions
- `furious.warps.warp` - Allows teleporting to warps (default: true)
- `furious.warps.list` - Allows listing warps (default: true)
- `furious.warps.unlink` - Allows removing warp portals (if you have permission to create them)

#### Admin Permissions
- `furious.warps.create` - Allows creating warps (default: op)
- `furious.warps.delete` - Allows deleting warps (default: op)
- `furious.warps.relocate` - Allows relocating warps (default: op)
- `furious.warps.rename` - Allows renaming warps (default: op)
- `furious.warps.cost` - Allows setting warp costs (default: op)
- `furious.warps.passwd` - Allows setting warp passwords (default: op)
- `furious.warps.link` - Allows linking warps to portals (default: op)
- `furious.warps.visibility` - Allows toggling warp visibility (default: op)

#### Granular Admin Permissions
- `furious.warps.create.others` - Allows creating warps for other players (default: op)
- `furious.warps.delete.others` - Allows deleting other players' warps (default: op)
- `furious.warps.relocate.others` - Allows relocating other players' warps (default: op)
- `furious.warps.rename.others` - Allows renaming other players' warps (default: op)
- `furious.warps.cost.others` - Allows setting costs on other players' warps (default: op)
- `furious.warps.passwd.others` - Allows setting passwords on other players' warps (default: op)

#### Special Permissions
- `furious.teleport.admin` - Allows bypassing teleport queue, effects, costs, and password requirements for warps

#### Warp Permission Inheritance
- `furious.warps.*` - Grants all warps permissions
- `furious.warps.admin.*` - Grants all administrative warps permissions

#### Warp Feature Limitations
- `furious.warps.limit.X` - Sets maximum number of warps to X
- `furious.warps.cooldown.X` - Sets warp cooldown to X seconds
- `furious.warps.cost.multiplier.X` - Sets warp cost multiplier to X

## Guild Permissions

#### Basic Guild Permissions
- `furious.guild.create` - Allows creating guilds (default: true)
- `furious.guild.info` - Allows viewing guild information (default: true)
- `furious.guild.list` - Allows listing all guilds (default: true)

#### Member Management Permissions
- `furious.guild.invite` - Allows inviting players to guilds (default: true)
- `furious.guild.join` - Allows joining guilds (default: true)
- `furious.guild.leave` - Allows leaving guilds (default: true)
- `furious.guild.kick` - Allows kicking players from guilds (default: true)
- `furious.guild.transfer` - Allows transferring guild ownership (default: true)

#### Territory Management Permissions
- `furious.guild.claim` - Allows claiming chunks for a guild (default: true)
- `furious.guild.unclaim` - Allows unclaiming chunks from a guild (default: true)
- `furious.guild.claims` - Allows viewing claimed chunks of a guild (default: true)
- `furious.guild.mobs` - Allows controlling mob spawning in guild claimed chunks (default: true)

#### Guild Homes Permissions
- `furious.guild.homes` - Allows managing guild homes (default: true)
- `furious.guild.homes.set` - Allows setting guild homes (default: true)
- `furious.guild.homes.teleport` - Allows teleporting to guild homes (default: true)

#### World Management Permissions
- `furious.guild.world` - Allows managing world settings for guilds (default: op)

#### Guild Settings Permissions
- `furious.guild.description` - Allows setting guild descriptions (default: true)

#### Granular Admin Permissions
- `furious.guild.admin.transfer` - Allows administrative guild ownership transfers (default: op)
- `furious.guild.admin.unclaim` - Allows administrative unclaiming of guild land (default: op)
- `furious.guild.admin.homes` - Allows administrative management of guild homes (default: op)
- `furious.guild.admin.disband` - Allows administrative disbanding of guilds (default: op)
- `furious.guild.admin.info` - Allows viewing detailed administrative information about guilds (default: op)

#### Special Admin Permissions
- `furious.guild.claim.unmanned` - Allows claiming chunks for unmanned guilds (S_A_F_E, WARZONE, WILDLIFE) (default: op)
- `furious.guild.unclaim.unmanned` - Allows unclaiming chunks from unmanned guilds (default: op)

#### Legacy Admin Permissions
- `furious.guild.admin` - Allows administrative guild operations (grants all granular admin permissions)

#### Guild Permission Inheritance
- `furious.guild.*` - Grants all guild permissions
- `furious.guild.admin.*` - Grants all administrative guild permissions
- `furious.guild.officer.*` - Grants all officer-level permissions

#### Guild Role-Permission Integration
- Guild roles (Owner, Officer, Member) have corresponding permission sets
- Permission checks consider both the player's permissions and their role within the guild
- Higher roles automatically include permissions from lower roles

## Security Permissions

### Locks Permissions

#### Basic Lock Management Permissions
- `furious.locks.lock` - Allows creating lock items (default: true)
- `furious.locks.unlock` - Allows creating unlock items (default: true)
- `furious.locks.info` - Allows checking lock ownership (default: true)
- `furious.locks.key` - Allows creating key items (default: true)

#### World Management Permissions
- `furious.locks.world` - Allows managing locks world settings (default: op)

#### Granular Admin Permissions
- `furious.locks.lock.others` - Allows creating locks for other players (default: op)
- `furious.locks.unlock.others` - Allows unlocking other players' locks (default: op)
- `furious.locks.key.others` - Allows creating keys for other players' locks (default: op)
- `furious.locks.bypass` - Allows bypassing lock restrictions (default: op)

#### Economic Integration
- `furious.wallet.bypass.cost` - Allows creating locks without paying the cost

#### Locks Permission Inheritance
- `furious.locks.*` - Grants all locks permissions
- `furious.locks.admin.*` - Grants all administrative locks permissions

### Tombstone Permissions

#### Basic Tombstone Management
- `furious.tombstones.locate` - Allows locating your own tombstones (default: true)
- `furious.tombstones.info` - Allows viewing information about your own tombstones (default: true)
- `furious.tombstones.extend` - Allows extending the expiration time of your own tombstones (default: true)
- `furious.tombstones.teleport` - Allows teleporting to your own tombstones (default: true)

#### Granular Admin Permissions
- `furious.tombstones.purge` - Allows purging all tombstones (default: op)
- `furious.tombstones.purge.others` - Allows purging other players' tombstones (default: op)
- `furious.tombstones.access.others` - Allows accessing other players' tombstones (default: op)
- `furious.tombstones.extend.others` - Allows extending the expiration time of other players' tombstones (default: op)
- `furious.tombstones.teleport.others` - Allows teleporting to other players' tombstones (default: op)

#### Legacy Admin Permissions
- `furious.tombstones.admin` - Allows all tombstone administration (default: op)

#### Tombstone Permission Inheritance
- `furious.tombstones.*` - Grants all tombstone permissions
- `furious.tombstones.admin.*` - Grants all administrative tombstone permissions

#### Player Experience Customization
- `furious.tombstones.extended` - Provides longer tombstone expiration times
- `furious.tombstones.secure` - Makes tombstones accessible only to the owner
- `furious.tombstones.notify` - Sends notifications when tombstones are about to expire

## Utility Permissions

### Inventory Management

#### Inventory Viewing
- `furious.invsee` - Allows viewing other players' inventories (default: op)
- `furious.invsee.edit` - Allows editing other players' inventories (default: op)
- `furious.endersee` - Allows viewing other players' enderchests (default: op)
- `furious.endersee.edit` - Allows editing other players' enderchests (default: op)
- `furious.endersee.offline` - Allows viewing offline players' enderchests (default: op)

#### Utility Permission Inheritance
- `furious.inventory.*` - Grants all inventory viewing permissions

### Player Commands

#### Healing and Feeding
- `furious.heal.self` - Allows healing yourself (default: op)
- `furious.heal.others` - Allows healing other players (default: op)
- `furious.heal.cooldown.X` - Sets heal cooldown to X seconds
- `furious.heal.bypass.cooldown` - Allows bypassing heal cooldowns (default: op)
- `furious.feed.self` - Allows feeding yourself (default: op)
- `furious.feed.others` - Allows feeding other players (default: op)
- `furious.feed.cooldown.X` - Sets feed cooldown to X seconds
- `furious.feed.bypass.cooldown` - Allows bypassing feed cooldowns (default: op)

#### Utility Permission Inheritance
- `furious.heal.*` - Grants all heal permissions
- `furious.feed.*` - Grants all feed permissions

### Rate Limiting

#### Rate Limiting Permissions
- `furious.ratelimit.exempt` - Exempts a player from all rate limits (default: op)
- `furious.ratelimit.exempt.X` - Exempts a player from rate limits for command X

## Permission Management

#### Basic Permission Management
- `furious.permission.roles.list` - Allows listing all roles (default: op)
- `furious.permission.roles.info` - Allows viewing detailed information about roles (default: op)
- `furious.permission.player.list` - Allows listing player permissions and roles (default: op)

#### Advanced Permission Management
- `furious.permission.roles.create` - Allows creating new roles (default: op)
- `furious.permission.roles.delete` - Allows deleting roles (default: op)
- `furious.permission.roles.set` - Allows modifying role properties (default: op)
- `furious.permission.roles.add` - Allows adding permissions or players to roles (default: op)
- `furious.permission.roles.remove` - Allows removing permissions or players from roles (default: op)
- `furious.permission.player.add` - Allows adding permissions directly to players (default: op)
- `furious.permission.player.remove` - Allows removing permissions from players (default: op)

#### Permission Management Inheritance
- `furious.permission.*` - Grants all permission management permissions (default: op)

## Common Server Setups

### Basic Server

For a basic server with minimal staff, you might want to use the following permission setup:

#### Players
- `furious.wallet` - Allow checking wallet balance
- `furious.wallet.pay` - Allow paying other players
- `furious.bank.balance` - Allow checking bank balance
- `furious.bank.deposit` - Allow depositing to bank
- `furious.bank.withdraw` - Allow withdrawing from bank
- `furious.teleport.request` - Allow requesting teleports
- `furious.teleport.accept` - Allow accepting teleports
- `furious.teleport.decline` - Allow declining teleports
- `furious.homes.set` - Allow setting homes
- `furious.homes.tp` - Allow teleporting to homes
- `furious.homes.limit.3` - Limit to 3 homes
- `furious.warps.warp` - Allow using warps
- `furious.warps.list` - Allow listing warps
- `furious.guild.create` - Allow creating guilds
- `furious.guild.join` - Allow joining guilds
- `furious.locks.lock` - Allow creating locks

#### Moderators
- `furious.teleport.force` - Allow forcing teleports
- `furious.invsee` - Allow viewing inventories
- `furious.heal.others` - Allow healing others
- `furious.feed.others` - Allow feeding others
- `furious.homes.tp.others` - Allow teleporting to others' homes
- `furious.homes.limit.5` - Limit to 5 homes

#### Administrators
- `furious.wallet.admin.*` - All wallet admin permissions
- `furious.bank.admin.*` - All bank admin permissions
- `furious.teleport.admin.*` - All teleport admin permissions
- `furious.homes.admin.*` - All homes admin permissions
- `furious.warps.admin.*` - All warps admin permissions
- `furious.guild.admin.*` - All guild admin permissions
- `furious.locks.admin.*` - All locks admin permissions
- `furious.tombstones.admin.*` - All tombstones admin permissions
- `furious.permission.*` - All permission management permissions

### Economy-Focused Server

For a server focused on economy, you might want to use the following permission setup:

#### Players
- `furious.wallet` - Allow checking wallet balance
- `furious.wallet.pay` - Allow paying other players
- `furious.bank.balance` - Allow checking bank balance
- `furious.bank.deposit` - Allow depositing to bank
- `furious.bank.withdraw` - Allow withdrawing from bank
- `furious.bank.transfer` - Allow transferring between accounts
- `furious.shops.use` - Allow using shops
- `furious.shops.sell` - Allow selling to shops
- `furious.shops.buy` - Allow buying from shops

#### VIP Players
- `furious.bank.accounts.limit.5` - Allow up to 5 bank accounts
- `furious.wallet.limit.1000` - Allow transactions up to 1000 scraps
- `furious.bank.interest.rate.1.5` - 50% higher interest rate

#### Shop Owners
- `furious.shops.create` - Allow creating shops
- `furious.shops.additem` - Allow adding items to shops
- `furious.shops.setprice` - Allow setting prices in shops
- `furious.shops.setstock` - Allow setting stock levels in shops

#### Administrators
- `furious.wallet.admin.*` - All wallet admin permissions
- `furious.bank.admin.*` - All bank admin permissions
- `furious.shops.admin.*` - All shop admin permissions

### RPG Server

For an RPG server with guilds and custom teleportation, you might want to use the following permission setup:

#### Players
- `furious.guild.create` - Allow creating guilds
- `furious.guild.join` - Allow joining guilds
- `furious.guild.leave` - Allow leaving guilds
- `furious.guild.invite` - Allow inviting to guilds
- `furious.guild.claim` - Allow claiming territory
- `furious.homes.set` - Allow setting homes
- `furious.homes.tp` - Allow teleporting to homes
- `furious.homes.limit.2` - Limit to 2 homes
- `furious.warps.warp` - Allow using warps
- `furious.warps.list` - Allow listing warps

#### Guild Leaders
- `furious.guild.kick` - Allow kicking from guild
- `furious.guild.transfer` - Allow transferring guild ownership
- `furious.guild.mobs` - Allow controlling mob spawning
- `furious.guild.homes.set` - Allow setting guild homes

#### VIP Players
- `furious.homes.limit.5` - Limit to 5 homes
- `furious.teleport.cooldown.30` - 30-second teleport cooldown (instead of default 60)

#### Administrators
- `furious.guild.admin.*` - All guild admin permissions
- `furious.homes.admin.*` - All homes admin permissions
- `furious.warps.admin.*` - All warps admin permissions
- `furious.teleport.admin.*` - All teleport admin permissions

### Survival Server

For a survival server with minimal teleportation and economy, you might want to use the following permission setup:

#### Players
- `furious.wallet` - Allow checking wallet balance
- `furious.wallet.pay` - Allow paying other players
- `furious.homes.set` - Allow setting homes
- `furious.homes.tp` - Allow teleporting to homes
- `furious.homes.limit.1` - Limit to 1 home
- `furious.locks.lock` - Allow creating locks
- `furious.tombstones.locate` - Allow locating tombstones
- `furious.tombstones.teleport` - Allow teleporting to tombstones

#### Trusted Players
- `furious.homes.limit.2` - Limit to 2 homes
- `furious.teleport.request` - Allow requesting teleports
- `furious.teleport.accept` - Allow accepting teleports
- `furious.teleport.cooldown.300` - 5-minute teleport cooldown

#### Moderators
- `furious.teleport.force` - Allow forcing teleports
- `furious.invsee` - Allow viewing inventories
- `furious.heal.others` - Allow healing others
- `furious.feed.others` - Allow feeding others
- `furious.homes.tp.others` - Allow teleporting to others' homes
- `furious.tombstones.access.others` - Allow accessing others' tombstones

#### Administrators
- `furious.teleport.admin.*` - All teleport admin permissions
- `furious.homes.admin.*` - All homes admin permissions
- `furious.locks.admin.*` - All locks admin permissions
- `furious.tombstones.admin.*` - All tombstones admin permissions

## Migration Guide

If you're updating from an older version of the plugin, you'll need to update your permission configurations to use the new granular permissions. Here's a guide to help you migrate:

### Wallet Permissions

Old Permission | New Permission(s)
---------------|----------------
`furious.wallet.admin` | `furious.wallet.balance.others`, `furious.wallet.add`, `furious.wallet.sub`, `furious.wallet.set`

### Bank Permissions

Old Permission | New Permission(s)
---------------|----------------
`furious.bank.editbalance` | `furious.bank.add`, `furious.bank.subtract`, `furious.bank.set`
`furious.bank.editbalance.others` | `furious.bank.add.others`, `furious.bank.subtract.others`, `furious.bank.set.others`
`furious.bank.editinterest` | `furious.bank.interest`

### Guild Permissions

Old Permission | New Permission(s)
---------------|----------------
`furious.guild.admin` | `furious.guild.admin.transfer`, `furious.guild.admin.unclaim`, `furious.guild.admin.homes`, `furious.guild.admin.disband`, `furious.guild.admin.info`

### Homes Permissions

Old Permission | New Permission(s)
---------------|----------------
`furious.homes.admin` | `furious.homes.set.others`, `furious.homes.delete.others`, `furious.homes.move.others`, `furious.homes.rename.others`, `furious.homes.tp.others`, `furious.homes.list.others`

### Warps Permissions

Old Permission | New Permission(s)
---------------|----------------
`furious.warps.admin` | `furious.warps.create.others`, `furious.warps.delete.others`, `furious.warps.relocate.others`, `furious.warps.rename.others`, `furious.warps.cost.others`, `furious.warps.passwd.others`

### Tombstone Permissions

Old Permission | New Permission(s)
---------------|----------------
`furious.tombstones.admin` | `furious.tombstones.purge`, `furious.tombstones.purge.others`, `furious.tombstones.access.others`, `furious.tombstones.extend.others`, `furious.tombstones.teleport.others`

### Using Permission Inheritance

To simplify permission management, you can use the new permission inheritance system. For example, instead of granting all the individual administrative permissions for a feature, you can grant the wildcard permission:

- `furious.wallet.admin.*` instead of `furious.wallet.balance.others`, `furious.wallet.add`, etc.
- `furious.bank.admin.*` instead of `furious.bank.add.others`, `furious.bank.subtract.others`, etc.
- `furious.guild.admin.*` instead of `furious.guild.admin.transfer`, `furious.guild.admin.unclaim`, etc.

This makes permission management much simpler while still allowing for granular control when needed.