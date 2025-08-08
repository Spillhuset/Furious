# furious - A Comprehensive Minecraft Server Plugin

![furious Plugin Banner](https://via.placeholder.com/800x200?text=furious+Plugin)

**Version:** 1.0-SNAPSHOT | **API Version:** 1.21

furious is a feature-rich Minecraft plugin designed to enhance server functionality with a wide range of utilities and gameplay features. It provides server administrators with powerful tools to manage their server while offering players an enhanced gameplay experience.

## Table of Contents
- [Features](#features)
- [Installation](#installation)
- [Configuration](#configuration)
- [Commands](#commands)
- [Permissions](#permissions)
- [API for Developers](#api-for-developers)
- [Contributing](#contributing)
- [Support](#support)
- [License](#license)

## Features

### Economy System
- **Wallet Management**: Personal player currency system with transaction history
- **Banking System**: Store and manage currency with configurable interest rates
- **Currency Items**: Physical representation of currency that can be traded or stored
- **Shop Integration**: Built-in support for player and admin shops

### Player Utilities
- **Teleportation**:
  - TPA requests with configurable cooldowns
  - Cross-world teleportation with permission controls
  - Ability to deny all teleport requests
  - Teleport queuing system to prevent conflicts
- **Homes System**:
  - Set and manage multiple home locations
  - Home limits based on permissions
  - Home sharing with other players
- **Inventory Management**:
  - View and manage player inventories and enderchests
  - Backup and restore player inventories
- **Player Commands**:
  - Heal and feed commands with configurable cooldowns
  - Player information lookup

### Security Features
- **Locks System**:
  - Secure containers, doors, and redstone components
  - Multiple lock types (private, public, guild)
  - Lock sharing with specific players
- **Permission Management**:
  - Granular permission control
  - Temporary permission grants
  - Permission inheritance
- **Security Review System**:
  - Scheduled security audits
  - Customizable security checks
  - Detailed reports
- **Audit Logging**:
  - Comprehensive logging of sensitive operations
  - Log filtering and search capabilities
  - Automatic log rotation

### Guild System
- **Guild Management**:
  - Create and manage player guilds with customizable roles
  - Guild hierarchies with owner, officers, and members
  - Guild invitations and join requests
- **Territory Control**:
  - Claim chunks for your guild
  - Protect guild territory from non-members
  - Configure mob spawning in guild territories
- **Guild Types**:
  - Different guild types (Safe, Unmanned, Open)
  - Customizable guild settings

### Minigames
- **Game Types**:
  - Hunger Games support
  - Zombie Survival mode
  - Expandable framework for custom games
- **Game Management**:
  - Custom container registry for minigames
  - Player queue system
  - Inventory preservation during games
- **Configuration**:
  - In-game minigame editor
  - Customizable spawn points
  - Game-specific settings

### World Management
- **Multi-world Support**:
  - Manage multiple worlds from a single interface
  - World-specific configurations
  - Enable/disable worlds
- **Teleportation**:
  - Cross-world teleportation with permission controls
  - World spawn management
  - Safe teleportation handling

### Tombstone System
- **Death Management**:
  - Secure player items on death in a protected tombstone
  - Configurable tombstone timeout
  - Owner-only access option
- **Item Recovery**:
  - Easy retrieval system for lost items
  - Death location tracking
  - Automatic item return on timeout (configurable)

### Combat Management
- **PvP Utilities**:
  - Combat tracking system
  - Configurable PvP zones
- **Anti-Abuse**:
  - Combat logging prevention
  - Punishment system for combat loggers

## Installation

1. Download the latest release JAR file from the [releases page](https://github.com/yourusername/furious/releases)
2. Place the JAR file in your server's `plugins` folder
3. Restart your server
4. Configure the plugin using the generated configuration files in the `plugins/furious` directory

### Requirements
- Minecraft Server: Paper/Spigot 1.21+
- Java 17 or higher
- Minimum 512MB of RAM allocated to the plugin
- WorldEdit plugin (automatically downloaded in test environment)

## Configuration

furious generates several configuration files in the `plugins/furious` directory:

### Main Configuration
- `config.yml`: Main configuration file with the following sections:
  - General plugin settings (debug mode, metrics, language)
  - Rate limiting for commands
  - Tombstone settings
  - Database settings
  - Default configuration files list

### Feature-specific Configurations
- `teleport.yml`: Teleportation settings and saved locations
- `guilds.yml`: Guild system configuration and guild data (when using YAML storage)
- `homes.yml`: Home locations and settings
- `locks.yml`: Locks system configuration and lock data
- `minigames.yml`: Minigame configurations and statistics
- `warps.yml`: Warp locations and settings
- `wallet.yml`: Economy system configuration including:
  - Currency name, plural form, and symbol
  - Currency format for display
  - Currency material for physical representation
  - When using YAML storage, player wallet balances are stored in `wallets.yml`
- `tombstones.yml`: Tombstone system settings
- `security_reviews.yml`: Security review configuration

### Database Configuration
The plugin supports multiple storage options for guild and wallet data:
- YAML: Stores data in the respective YAML files (default)
  - Guild data in `guilds.yml`
  - Wallet data in `wallets.yml`
- MySQL/MariaDB: Stores data in a MySQL or MariaDB database
- SQLite: Stores data in a SQLite database file

To configure the database storage, edit the `database` section in `config.yml`:

```yaml
# Database settings
database:
  # Storage type: YAML, MYSQL, MARIADB, or SQLITE
  storage-type: "YAML"

  # MySQL/MariaDB settings (only used if storage-type is MYSQL or MARIADB)
  mysql:
    host: "localhost"
    port: 3306
    database: "furious"
    username: "furious"
    password: "password"
    use-ssl: false

  # SQLite settings (only used if storage-type is SQLITE)
  sqlite:
    # Path to the SQLite database file (relative to plugin folder)
    file: "database.db"

  # Connection pool settings (HikariCP)
  connection-pool:
    maximum-pool-size: 10
    minimum-idle: 5
    maximum-lifetime: 1800000
    connection-timeout: 5000
    idle-timeout: 600000
```

### Example Configuration
```yaml
# Rate limiting settings
rate-limits:
  # Commands that are exempt from rate limiting
  exempt-commands:
    - "help"
    - "spawn"

  # Command-specific rate limits
  commands:
    teleport:
      max-uses: 10
      time-window-seconds: 60
```

## Commands

### Economy Commands

#### Wallet Commands
- `/wallet`: Check your wallet balance
- `/wallet pay <player> <amount>`: Pay scraps to another player (requires `furious.wallet.pay` permission)

#### Admin Wallet Commands
- `/wallet balance <player>`: Check a player's wallet balance (requires `furious.wallet.balance.others` permission or OP)
- `/wallet add <player> <amount>`: Add scraps to a player (requires `furious.wallet.add` permission or OP)
- `/wallet sub <player> <amount>`: Subtract scraps from a player (requires `furious.wallet.sub` permission or OP)
- `/wallet set <player> <amount>`: Set a player's wallet balance (requires `furious.wallet.set` permission or OP)

#### Bank Commands
- `/bank balance`: Check your bank balance
- `/bank deposit <amount>`: Deposit money into your bank
- `/bank withdraw <amount>`: Withdraw money from your bank
- `/bank transfer <amount> <from> <to>`: Transfer money between accounts
- `/bank info`: View bank information at current location

#### Admin Bank Commands
- `/bank claim`: Claim chunks for banks
- `/bank unclaim`: Unclaim chunks from banks
- `/bank create <n>`: Create a new bank
- `/bank rename <old> <new>`: Rename a bank
- `/bank delete <n>`: Delete a bank
- `/bank deleteaccount <bank> <player>`: Delete an account from a bank
- `/bank createaccount <bank> <player>`: Create an account in a bank
- `/bank editbalance <bank> <player> <amount>`: Edit a player's balance
- `/bank editinterest <bank> <rate>`: Edit bank interest rate
- `/bank help`: Get help with bank commands

### Teleport Commands

#### User Commands
- `/teleport request <player>`: Send teleport request to another player
- `/tpa <player>`: Alias for `/teleport request`
- `/teleport accept [player]`: Accept an incoming teleport request
- `/tpaccept [player]`: Alias for `/teleport accept`
- `/teleport decline [player]`: Decline an incoming teleport request
- `/tpdecline [player]`: Alias for `/teleport decline`
- `/teleport list`: List all pending teleport requests
- `/teleport abort`: Cancel your outgoing teleport request
- `/teleport deny`: Toggle auto-deny of teleport requests

#### Admin Commands
- `/teleport <player>`: Teleport directly to a player without sending request
- `/teleport <player> <player>`: Teleport one player to another
- `/teleport <x> <y> <z> [world]`: Teleport to specific coordinates
- `/teleport world`: Configure world teleport settings

### Warp Commands

#### User Commands
- `/warps list`: List all available warps
- `/warps warp <n>`: Teleport to a specific warp location
- `/warp <n>`: Alias for `/warps warp`

#### Admin Commands
- `/warps create <n>`: Create a new warp at your current location
- `/setwarp <n>`: Alias for `/warps create`
- `/warps delete <n>`: Delete an existing warp
- `/delwarp <n>`: Alias for `/warps delete`
- `/warps relocate <n>`: Move an existing warp to your current location
- `/warps cost <n> <amount>`: Set the cost to use a warp
- `/warps passwd <n> <password>`: Set a password for a warp
- `/warps rename <old> <new>`: Rename an existing warp
- `/warps link <n>`: Link a warp to a portal

### Home Commands

#### User Commands
- `/homes list`: List all your set home locations
- `/homes tp <n>`: Teleport to one of your homes
- `/home <n>`: Alias for `/homes tp`
- `/homes set <n>`: Set a new home at your current location
- `/sethome <n>`: Alias for `/homes set`
- `/homes delete <n>`: Delete one of your homes
- `/delhome <n>`: Alias for `/homes delete`
- `/homes move <n>`: Move an existing home to your current location
- `/homes rename <old> <new>`: Rename one of your homes
- `/homes buy`: Purchase additional home slots

### Guild Commands
- `/guild create <n>`: Create a new guild
- `/guild info [guild]`: View guild information
- `/guild list`: List all guilds
- `/guild invite <player>`: Invite player to guild
- `/guild join <guild>`: Accept guild invitation
- `/guild leave`: Leave current guild
- `/guild kick <player>`: Kick player from guild
- `/guild disband`: Disband your guild
- `/guild transfer <player>`: Transfer guild ownership
- `/guild description <text>`: Set guild description
- `/guild claim`: Claim current chunk for guild
- `/guild claim SAFE`: (Admin) Claim all chunks between WorldEdit selection points for SAFE guild
- `/guild unclaim`: Unclaim current chunk
- `/guild claims`: View claimed chunks
- `/guild mobs`: Toggle mob spawning in guild territory
- `/guild homes`: Manage guild homes

### Security Commands
- `/locks lock <type>`: Create a lock of specified type
- `/locks unlock`: Remove a lock
- `/locks info`: Check lock ownership
- `/locks key`: Create a key for a lock
- `/locks world`: Manage locks world settings
- `/security status`: View security status
- `/security review`: Manage security reviews
- `/security help`: Get help with security commands

#### Permission Management
- `/permissions`: Manage permissions, roles, and player permissions
- `/permissions help`: Get help with permission commands
- `/permissions roles`: Manage roles (create, delete, list, etc.)
- `/permissions player`: Manage player permissions

For a comprehensive guide on using the permissions system, see the [Admin Permissions Guide](ADMIN/ADMIN_PERMISSIONS_GUIDE.md). Players can refer to the [Player Permissions Guide](PLAYER/PLAYER_PERMISSIONS_GUIDE.md) to understand how permissions affect them.


### Minigame Commands

#### User Commands
- `/minigame list`: List available minigames
- `/minigame info <game>`: View information about a minigame
- `/minigame join <game>`: Join a minigame queue
- `/minigame leave`: Leave the current queue

#### Admin Commands
- `/minigame create <n> <type>`: Create a new minigame
- `/minigame disable <n>`: Disable a minigame
- `/minigame enable <n>`: Enable a minigame
- `/minigame start <n>`: Force start a minigame
- `/minigame stop <n>`: Force stop a minigame
- `/minigame edit <n>`: Edit a minigame
- `/minigame spawn <n> <type>`: Set spawn point for a minigame
- `/minigame save <n>`: Save minigame configuration
- `/minigame tp`: Teleport to the GameWorld

### Tombstone Commands
- `/tombstones purge`: Purge expired tombstones (admin)

### Utility Commands
- `/invsee <player>`: View player inventory
- `/endersee <player>`: View player enderchest
- `/heal [player]`: Heal player
- `/feed [player]`: Feed player

## Permissions

Furious uses a comprehensive permission-based system for access control. The system features a structured permission inheritance model where higher-level permissions automatically grant related lower-level permissions.

### Permission Inheritance System

The permission system follows these principles:

1. **Wildcard Permissions**: Permissions ending with `.*` grant all child permissions
   - Example: `furious.guild.*` grants all guild-related permissions
   - Example: `furious.bank.admin.*` grants all administrative bank permissions

2. **Hierarchical Structure**: Permissions are organized in a hierarchical structure
   - Top level: Feature category (e.g., `furious.guild`, `furious.bank`)
   - Second level: Operation type (e.g., `furious.guild.admin`, `furious.bank.add`)
   - Third level: Target specification (e.g., `furious.bank.add.others`)

3. **Permission-Based Features**: Many features now have behavior that varies based on permission level
   - Cooldowns may be shorter or non-existent for higher permission levels
   - Costs may be reduced or waived for certain permission levels
   - Limits (homes, guild claims, etc.) are often determined by specific permissions

Here are the main permission nodes:

### General Permissions
- `furious.*`: Gives access to all furious plugin permissions
- `furious.admin`: All administrative permissions (not defined in plugin.yml)
- `furious.mod`: All moderator permissions (not defined in plugin.yml)
- `furious.user`: Basic user permissions (not defined in plugin.yml)

### Economy Permissions

#### Wallet Permissions
- `furious.wallet`: Allows checking your own wallet balance
- `furious.wallet.pay`: Allows paying scraps to other players
- `furious.wallet.balance.others`: Allows checking other players' wallet balances
- `furious.wallet.add`: Allows adding scraps to players' wallets
- `furious.wallet.sub`: Allows subtracting scraps from players' wallets
- `furious.wallet.set`: Allows setting players' wallet balances
- `furious.wallet.bypass.cost`: Allows bypassing costs for wallet-integrated features
- `furious.wallet.bypass.limit`: Allows bypassing transaction limits

##### Wallet Permission Inheritance
- `furious.wallet.*`: Grants all wallet permissions
- `furious.wallet.admin.*`: Grants all administrative wallet permissions (balance.others, add, sub, set)

##### Wallet Feature Limitations
- `furious.wallet.limit.X`: Sets maximum transaction amount to X
- `furious.wallet.cooldown.X`: Sets transaction cooldown to X seconds

#### Bank Permissions

##### Basic Bank Permissions
- `furious.bank.balance`: Allows checking bank balance
- `furious.bank.deposit`: Allows depositing to bank
- `furious.bank.withdraw`: Allows withdrawing from bank
- `furious.bank.transfer`: Allows transferring funds between bank accounts
- `furious.bank.info`: Allows viewing bank information at current location
- `furious.bank.createaccount`: Allows creating your own account in banks
- `furious.bank.deleteaccount`: Allows deleting your own account from banks

##### Bank Management Permissions
- `furious.bank.claim`: Allows claiming chunks for banks
- `furious.bank.unclaim`: Allows unclaiming chunks from banks
- `furious.bank.create`: Allows creating banks
- `furious.bank.rename`: Allows renaming banks
- `furious.bank.delete`: Allows deleting banks

##### Granular Balance Management Permissions
- `furious.bank.add`: Allows adding to account balances
- `furious.bank.subtract`: Allows subtracting from account balances
- `furious.bank.set`: Allows setting account balances to specific values
- `furious.bank.add.others`: Allows adding to other players' account balances
- `furious.bank.subtract.others`: Allows subtracting from other players' account balances
- `furious.bank.set.others`: Allows setting other players' account balances

##### Other Administrative Permissions
- `furious.bank.createaccount.others`: Allows creating accounts for other players
- `furious.bank.deleteaccount.others`: Allows deleting other players' accounts from banks
- `furious.bank.interest`: Allows editing bank interest rates
- `furious.bank.withdraw.auto`: Allows automatic withdrawals from bank when making purchases

##### Legacy Permissions
- `furious.bank.editbalance`: Grants all balance editing permissions for your own accounts
- `furious.bank.editbalance.others`: Grants all balance editing permissions for other players' accounts
- `furious.bank.admin`: Allows administrative bank operations

##### Bank Permission Inheritance
- `furious.bank.*`: Grants all bank permissions
- `furious.bank.admin.*`: Grants all administrative bank permissions
- Higher-level permissions automatically grant related lower-level permissions

##### Bank Feature Limitations
- `furious.bank.accounts.limit.X`: Sets maximum number of accounts to X
- `furious.bank.interest.rate.X`: Sets interest rate multiplier to X
- `furious.bank.transfer.limit.X`: Sets maximum transfer amount to X

### Teleport Permissions

#### User Permissions
- `furious.teleport.request`: Allows requesting teleports to other players
- `furious.teleport.accept`: Allows accepting teleport requests
- `furious.teleport.decline`: Allows declining teleport requests
- `furious.teleport.list`: Allows listing teleport requests
- `furious.teleport.abort`: Allows aborting outgoing teleport requests
- `furious.teleport.deny`: Allows toggling auto-deny of teleport requests

#### Admin Permissions
- `furious.teleport.worldconfig`: Allows configuring world teleport settings
- `furious.teleport.coords`: Allows using coordinate teleport commands
- `furious.teleport.force`: Allows using force teleport commands
- `furious.teleport.worlds`: Allows viewing world teleport settings
- `furious.teleport.worldspawn`: Allows teleporting to world spawn
- `furious.teleport.setworldspawn`: Allows setting world spawn location

#### Special Permissions
- `furious.teleport.admin`: Allows bypassing teleport queue, effects, costs, and cooldowns

#### Teleport Permission Inheritance
- `furious.teleport.*`: Grants all teleport permissions
- `furious.teleport.admin.*`: Grants all administrative teleport permissions
- Higher-level permissions automatically grant related lower-level permissions

#### Teleport Feature Limitations
- `furious.teleport.cooldown.X`: Sets teleport cooldown to X seconds
- `furious.teleport.cost.X`: Sets teleport cost to X
- `furious.teleport.bypass.cooldown`: Allows bypassing teleport cooldowns
- `furious.teleport.bypass.cost`: Allows teleporting without paying costs

### Warp Permissions

#### User Permissions
- `furious.warps.warp`: Allows teleporting to warps
- `furious.warps.list`: Allows listing warps
- `furious.warps.unlink`: Allows removing warp portals (if you have permission to create them)

#### Admin Permissions
- `furious.warps.create`: Allows creating warps
- `furious.warps.delete`: Allows deleting warps
- `furious.warps.relocate`: Allows relocating warps
- `furious.warps.rename`: Allows renaming warps
- `furious.warps.cost`: Allows setting warp costs
- `furious.warps.passwd`: Allows setting warp passwords
- `furious.warps.link`: Allows linking warps to portals
- `furious.warps.visibility`: Allows toggling warp visibility

#### Granular Admin Permissions
- `furious.warps.create.others`: Allows creating warps for other players
- `furious.warps.delete.others`: Allows deleting other players' warps
- `furious.warps.relocate.others`: Allows relocating other players' warps
- `furious.warps.rename.others`: Allows renaming other players' warps
- `furious.warps.cost.others`: Allows setting costs on other players' warps
- `furious.warps.passwd.others`: Allows setting passwords on other players' warps

#### Special Permissions
- `furious.teleport.admin`: Allows bypassing teleport queue, effects, costs, and password requirements for warps

#### Warp Permission Inheritance
- `furious.warps.*`: Grants all warps permissions
- `furious.warps.admin.*`: Grants all administrative warps permissions
- Higher-level permissions automatically grant related lower-level permissions

#### Warp Feature Limitations
- `furious.warps.limit.X`: Sets maximum number of warps to X
- `furious.warps.cooldown.X`: Sets warp cooldown to X seconds
- `furious.warps.cost.multiplier.X`: Sets warp cost multiplier to X

### Home Permissions

#### User Permissions
- `furious.homes.set`: Allows setting homes
- `furious.homes.move`: Allows moving homes
- `furious.homes.rename`: Allows renaming homes
- `furious.homes.delete`: Allows deleting homes
- `furious.homes.list`: Allows listing homes
- `furious.homes.tp`: Allows teleporting to homes
- `furious.homes.buy`: Allows purchasing additional home slots

#### World Management Permissions
- `furious.homes`: Allows using `/homes world list` command to view world settings for homes
- `furious.homes.world`: Allows using `/homes world enable/disable [world]` commands to manage homes in specific worlds

#### Granular Admin Permissions
- `furious.homes.set.others`: Allows setting homes for other players
- `furious.homes.delete.others`: Allows deleting other players' homes
- `furious.homes.move.others`: Allows moving other players' homes
- `furious.homes.rename.others`: Allows renaming other players' homes
- `furious.homes.tp.others`: Allows teleporting to other players' homes
- `furious.homes.list.others`: Allows listing other players' homes

#### Legacy Admin Permissions
- `furious.homes.admin`: Allows managing other players' homes (grants all granular admin permissions)

#### Home Permission Inheritance
- `furious.homes.*`: Grants all homes permissions and unlimited homes
- `furious.homes.*.others`: Grants all administrative homes permissions
- Higher-level permissions automatically grant related lower-level permissions

#### Home Limit Permissions
- `furious.homes.limit.1`: Sets home limit to 1 (default: false)
- `furious.homes.limit.2`: Sets home limit to 2 (default: false)
- `furious.homes.limit.3`: Sets home limit to 3 (default: true)
- `furious.homes.limit.5`: Sets home limit to 5 (default: false)
- `furious.homes.limit.10`: Sets home limit to 10 (default: false)
- `furious.homes.limit.custom`: Custom home limit set by server administrators

### Guild Permissions

#### Basic Guild Permissions
- `furious.guild.create`: Allows creating guilds
- `furious.guild.info`: Allows viewing guild information
- `furious.guild.list`: Allows listing all guilds

#### Member Management Permissions
- `furious.guild.invite`: Allows inviting players to guilds
- `furious.guild.join`: Allows joining guilds
- `furious.guild.leave`: Allows leaving guilds
- `furious.guild.kick`: Allows kicking players from guilds
- `furious.guild.transfer`: Allows transferring guild ownership

#### Territory Management Permissions
- `furious.guild.claim`: Allows claiming chunks for a guild
- `furious.guild.unclaim`: Allows unclaiming chunks from a guild
- `furious.guild.claims`: Allows viewing claimed chunks of a guild
- `furious.guild.mobs`: Allows controlling mob spawning in guild claimed chunks

#### Guild Homes Permissions
- `furious.guild.homes`: Allows managing guild homes
- `furious.guild.homes.set`: Allows setting guild homes
- `furious.guild.homes.teleport`: Allows teleporting to guild homes

#### World Management Permissions
- `furious.guild`: Allows using `/guild world list` command to view world settings for guilds
- `furious.guild.world`: Allows using `/guild world enable/disable [world]` commands to manage guilds in specific worlds

#### Guild Settings Permissions
- `furious.guild.description`: Allows setting guild descriptions

#### Granular Admin Permissions
- `furious.guild.admin.transfer`: Allows administrative guild ownership transfers
- `furious.guild.admin.unclaim`: Allows administrative unclaiming of guild land
- `furious.guild.admin.homes`: Allows administrative management of guild homes
- `furious.guild.admin.disband`: Allows administrative disbanding of guilds
- `furious.guild.admin.info`: Allows viewing detailed administrative information about guilds

#### Special Admin Permissions
- `furious.guild.claim.unmanned`: Allows claiming chunks for unmanned guilds (S_A_F_E, WARZONE, WILDLIFE)
- `furious.guild.unclaim.unmanned`: Allows unclaiming chunks from unmanned guilds

#### Legacy Admin Permissions
- `furious.guild.admin`: Allows administrative guild operations (grants all granular admin permissions)

#### Guild Permission Inheritance
- `furious.guild.*`: Grants all guild permissions
- `furious.guild.admin.*`: Grants all administrative guild permissions
- `furious.guild.officer.*`: Grants all officer-level permissions
- Higher-level permissions automatically grant related lower-level permissions

#### Guild Role-Permission Integration
- Guild roles (Owner, Officer, Member) have corresponding permission sets
- Permission checks consider both the player's permissions and their role within the guild
- Higher roles automatically include permissions from lower roles

### Locks Permissions

#### Basic Lock Management Permissions
- `furious.locks.lock`: Allows creating lock items (default: true)
- `furious.locks.unlock`: Allows creating unlock items (default: true)
- `furious.locks.info`: Allows checking lock ownership (default: true)
- `furious.locks.key`: Allows creating key items (default: true)

#### World Management Permissions
- `furious.locks`: Allows using `/locks world list` command to view world settings for locks
- `furious.locks.world`: Allows managing locks world settings (default: op)

#### Granular Admin Permissions
- `furious.locks.lock.others`: Allows creating locks for other players (default: op)
- `furious.locks.unlock.others`: Allows unlocking other players' locks (default: op)
- `furious.locks.key.others`: Allows creating keys for other players' locks (default: op)
- `furious.locks.bypass`: Allows bypassing lock restrictions (default: op)

#### Economic Integration
- `furious.wallet.bypass.cost`: Allows creating locks without paying the cost
- Players with different permission levels may have different lock costs

#### Locks Permission Inheritance
- `furious.locks.*`: Grants all locks permissions
- `furious.locks.admin.*`: Grants all administrative locks permissions
- Higher-level permissions automatically grant related lower-level permissions

### Security Permissions

#### Basic Security Permissions
- `furious.security.status`: Allows viewing security status (default: op)
- `furious.security.review`: Allows managing security reviews (default: op)
- `furious.security.help`: Allows accessing security help information (default: op)

#### Admin Security Permissions
- `furious.security.admin`: Allows managing security reviews and other security-related tasks (default: op)

#### Security Permission Inheritance
- `furious.security.*`: Grants all security permissions
- Higher-level permissions automatically grant related lower-level permissions

### Tombstone Permissions

#### Basic Tombstone Management
- `furious.tombstones.locate`: Allows locating your own tombstones (default: true)
- `furious.tombstones.info`: Allows viewing information about your own tombstones (default: true)
- `furious.tombstones.extend`: Allows extending the expiration time of your own tombstones (default: true)
- `furious.tombstones.teleport`: Allows teleporting to your own tombstones (default: true)

#### Granular Admin Permissions
- `furious.tombstones.purge`: Allows purging all tombstones (default: op)
- `furious.tombstones.purge.others`: Allows purging other players' tombstones (default: op)
- `furious.tombstones.access.others`: Allows accessing other players' tombstones (default: op)
- `furious.tombstones.extend.others`: Allows extending the expiration time of other players' tombstones (default: op)
- `furious.tombstones.teleport.others`: Allows teleporting to other players' tombstones (default: op)

#### Legacy Admin Permissions
- `furious.tombstones.admin`: Allows all tombstone administration (default: op)

#### Tombstone Permission Inheritance
- `furious.tombstones.*`: Grants all tombstone permissions
- `furious.tombstones.admin.*`: Grants all administrative tombstone permissions
- Higher-level permissions automatically grant related lower-level permissions

#### Player Experience Customization
- `furious.tombstones.extended`: Provides longer tombstone expiration times
- `furious.tombstones.secure`: Makes tombstones accessible only to the owner
- `furious.tombstones.notify`: Sends notifications when tombstones are about to expire

### Permission Management

#### Basic Permission Management
- `furious.permission.roles.list`: Allows listing all roles (default: op)
- `furious.permission.roles.info`: Allows viewing detailed information about roles (default: op)
- `furious.permission.player.list`: Allows listing player permissions and roles (default: op)

#### Advanced Permission Management
- `furious.permission.roles.create`: Allows creating new roles (default: op)
- `furious.permission.roles.delete`: Allows deleting roles (default: op)
- `furious.permission.roles.set`: Allows modifying role properties (default: op)
- `furious.permission.roles.add`: Allows adding permissions or players to roles (default: op)
- `furious.permission.roles.remove`: Allows removing permissions or players from roles (default: op)
- `furious.permission.player.add`: Allows adding permissions directly to players (default: op)
- `furious.permission.player.remove`: Allows removing permissions from players (default: op)

#### Permission Management Inheritance
- `furious.permission.*`: Grants all permission management permissions (default: op)
- Higher-level permissions automatically grant related lower-level permissions

### Minigame Permissions

#### User Permissions
- `furious.minigame.join`: Allows joining minigame queues (default: true)
- `furious.minigame.leave`: Allows leaving minigame queues (default: true)
- `furious.minigame.list`: Allows listing available minigames (default: true)
- `furious.minigame.info`: Allows viewing minigame information (default: true)

#### Admin Permissions
- `furious.minigame.create`: Allows creating minigames (default: op)
- `furious.minigame.disable`: Allows disabling minigames (default: op)
- `furious.minigame.enable`: Allows enabling minigames (default: op)
- `furious.minigame.start`: Allows starting minigames (default: op)
- `furious.minigame.stop`: Allows stopping minigames (default: op)
- `furious.minigame.edit`: Allows editing minigames (default: op)
- `furious.minigame.tp`: Allows teleporting to the GameWorld (default: op)

#### Minigame Permission Inheritance
- `furious.minigame.*`: Grants all minigame permissions
- `furious.minigame.admin.*`: Grants all administrative minigame permissions
- Higher-level permissions automatically grant related lower-level permissions

### Utility Permissions

#### Inventory Viewing
- `furious.invsee`: Allows viewing other players' inventories (default: op)
- `furious.invsee.edit`: Allows editing other players' inventories (default: op)
- `furious.endersee`: Allows viewing other players' enderchests (default: op)
- `furious.endersee.edit`: Allows editing other players' enderchests (default: op)
- `furious.endersee.offline`: Allows viewing offline players' enderchests (default: op)

#### Healing and Feeding
- `furious.heal.self`: Allows healing yourself (default: op)
- `furious.heal.others`: Allows healing other players (default: op)
- `furious.heal.cooldown.X`: Sets heal cooldown to X seconds
- `furious.heal.bypass.cooldown`: Allows bypassing heal cooldowns (default: op)
- `furious.feed.self`: Allows feeding yourself (default: op)
- `furious.feed.others`: Allows feeding other players (default: op)
- `furious.feed.cooldown.X`: Sets feed cooldown to X seconds
- `furious.feed.bypass.cooldown`: Allows bypassing feed cooldowns (default: op)

#### Utility Permission Inheritance
- `furious.heal.*`: Grants all heal permissions
- `furious.feed.*`: Grants all feed permissions
- `furious.inventory.*`: Grants all inventory viewing permissions
- Higher-level permissions automatically grant related lower-level permissions

#### Rate Limiting
- `furious.ratelimit.exempt`: Exempts a player from all rate limits (default: op)
- `furious.ratelimit.exempt.X`: Exempts a player from rate limits for command X

## API for Developers

furious provides a comprehensive API for developers who want to integrate with or extend the plugin's functionality.

### Maven Dependency
```xml
<dependency>
    <groupId>com.spillhuset</groupId>
    <artifactId>furious</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
</dependency>
```

### Basic Usage Example
```java
import com.spillhuset.furious.Furious;
import com.spillhuset.furious.managers.GuildManager;

public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        if (getServer().getPluginManager().getPlugin("furious") != null) {
            GuildManager guildManager = Furious.getInstance().getGuildManager();
            // Use the guild manager
        }
    }
}
```

For detailed API documentation, please visit our [Wiki](https://github.com/yourusername/furious/wiki/API).

## Contributing

We welcome contributions to the furious plugin! Here's how you can help:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

Please make sure to update tests as appropriate and adhere to the existing coding style.

### Development Setup

1. Clone the repository
2. Import the project into your IDE as a Gradle project
3. Run `./gradlew build` to build the project
4. Run `./gradlew runServer` to start a test server with the plugin

## Support

For issues, feature requests, or general support:

- Create an issue on the [GitHub repository](https://github.com/yourusername/furious/issues)
- Join our [Discord server](https://discord.gg/yourdiscord) for community support
- Check out the [Wiki](https://github.com/yourusername/furious/wiki) for documentation

## License

This project is licensed under the [MIT License](LICENSE) - see the LICENSE file for details.

---

Made with ❤️ by the furious Team