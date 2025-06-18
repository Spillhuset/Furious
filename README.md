# Furious Plugin

A comprehensive Minecraft plugin for server administrators looking to enhance their Minecraft server with a variety of features. Furious provides teleportation, guild systems, home management, warps, locks, minigames, economy, and more in a single plugin.

**Current Version:** 1.0-SNAPSHOT

## Features

- **Teleportation System**: Request-based teleportation with accept/decline functionality and world-specific settings
- **Guild System**: Create and manage guilds with chunk claiming, homes, and member management
- **Homes**: Personal teleport locations with customizable limits and purchase options
- **Warps**: Server-wide teleport locations with optional costs and passwords
- **Locks**: Secure containers and doors with lock and key items
- **Minigames**: Built-in minigames including Hunger Games, Spleef, and Zombie Survival
- **Game World Management**: Tools for managing game worlds
- **Player Utilities**: Commands for healing and feeding players
- **Economy Integration**: Built-in currency system with player wallets, transactions, and integration with other features (homes, warps)
- **Banking System**: Store currency in banks, transfer between players, and claim bank territories
- **Combat System**: Tracks player combat status with visual indicators and notifications. Players enter combat when they are damaged by or damage another entity, and exit combat after 10 seconds or when they die.
- **Tombstones**: When players die, their inventory, armor, and wallet are stored in a tombstone (represented by an armor stand) that can be retrieved within a configurable time limit. Players can right-click the tombstone to access their items. Tombstones are indestructible except by player punches and will disappear when empty or when the time limit expires. Admin players don't get tombstones when they die, and tombstones won't be created for players in minigames.
- **Security Features**: 
  - **Audit Logging**: Detailed logging of sensitive operations like inventory viewing, teleportation, and administrative actions
  - **Rate Limiting**: Prevents command spam and abuse by limiting how frequently commands can be used
  - **Permission Management**: Comprehensive permission system for fine-grained access control

## Requirements

- Minecraft Server 1.21+
- Java 17 or higher

## Installation

1. Download the latest version of the Furious plugin JAR file
2. Place the JAR file in your server's `plugins` folder
3. Restart your server
4. Configure the plugin using the generated configuration files in the `plugins/furious` directory

## Security

The plugin has undergone a security audit and implements best practices for permission management. Recent security updates include:

- Added proper permission checks for inventory viewing commands
- Disabled unsafe offline player data access
- Added self-checks in inventory commands to prevent confusion
- Implemented secure file operations and input validation

The security report was recently updated (June 15, 2023) with additional recommendations, most of which have been implemented:

- Enhanced input sanitization for user-provided inputs ✓ (Implemented)
- Secure configuration handling with encryption for sensitive data
- Comprehensive permission hierarchy review ✓ (Implemented)
- Regular security reviews with automated scheduling and notifications ✓ (Implemented)
- Rate limiting for commands to prevent abuse ✓ (Implemented)

### Input Sanitization

The input sanitization has been significantly improved with a dedicated utility class that validates player names, Minecraft selectors, and checks for potentially dangerous input patterns.

### Audit Logging

A comprehensive audit logging system has been implemented to track sensitive operations. This helps server administrators monitor who is using powerful commands and when. All logs include timestamps, player information (name, UUID, IP address), and detailed operation information.

Operations that are logged include:
- Inventory and enderchest viewing
- Teleport commands
- Warp usage
- Administrative actions
- Failed access attempts and permission violations

### Rate Limiting

To prevent command spam and abuse, a rate limiting system has been implemented. This system tracks command usage by players and enforces limits on how frequently commands can be used. Rate limits can be configured for each command, and players with the appropriate permission can be exempted from these limits.

### Security Reviews

A comprehensive security review process has been implemented to ensure that the plugin remains secure as it evolves. The SecurityReviewManager automatically tracks when security reviews are conducted and when the next review is due. Server administrators with the `furious.security.admin` permission will be notified when a security review is due, both in the server log and in-game.

The following commands are available for managing security reviews:

- `/security status`: Show the status of security reviews (last review date, next review date, etc.)
- `/security review complete [notes]`: Mark a security review as completed with optional notes
- `/security review interval <days>`: Set the interval between security reviews in days
- `/security help`: Show help for security commands

For more details, see the [Security Report](SECURITY_REPORT.md) and the [Security Review Process](SECURITY_REVIEW_PROCESS.md).

## Configuration

After installing the plugin, the following configuration files will be generated in the `plugins/furious` directory:

- `config.yml` - Main configuration file for general plugin settings
- `guilds.yml` - Configuration for guild settings and data
- `homes.yml` - Configuration for homes settings and data
- `locks.yml` - Configuration for locks settings and data
- `minigames.yml` - Configuration for minigames settings and data
- `teleport.yml` - Configuration for teleport settings and data
- `warps.yml` - Configuration for warps settings and data
- `wallet.yml` - Configuration for economy settings and data
- `banks.yml` - Configuration for bank settings and data
- `tombstones.yml` - Configuration for tombstone settings and data

### Economy Configuration

The plugin includes a built-in currency system that can be configured in the `wallet.yml` file:

- `currency.name` - The name of the currency (singular form)
- `currency.plural` - The plural form of the currency name
- `currency.symbol` - The symbol used for the currency
- `currency.material` - The Minecraft material used for physical currency items

### Banking Configuration

The plugin includes a banking system that allows players to store their currency safely. The banking system is configured in the `banks.yml` file:

- New players automatically receive an account in the default bank (RubberBank) with 100S
- Players can deposit, withdraw, transfer between banks, and transfer between players
- Banks can claim chunks to establish their physical presence in the world
- Players can view bank information at their current location

### Tombstone Configuration

The plugin includes a tombstone system that can be configured in the `config.yml` file under the `tombstones` section:

- `tombstones.timeout-seconds` - Time in seconds before a tombstone expires (default: 1800, which is 30 minutes)
- `tombstones.owner-only-access` - If true, only the owner of the tombstone can access it (default: true)

### Security Configuration

#### Audit Logging

The plugin includes a comprehensive audit logging system that logs sensitive operations to the server log. This helps server administrators track who is using powerful commands and when. Audit logs include:

- Inventory and enderchest viewing operations
- Teleport operations
- Warp usage
- Administrative actions
- Admin actions on other players' homes (set, move, delete, rename)
- Failed access attempts
- Security review operations

Audit logs include timestamps, player information (name, UUID, IP address), and detailed operation information.

#### Rate Limiting

The plugin includes a rate limiting system to prevent command spam and abuse. Rate limits can be configured in the `config.yml` file under the `rate-limits` section:

- `rate-limits.exempt-commands` - List of commands that are exempt from rate limiting
- `rate-limits.commands.<command>.max-uses` - Maximum number of times a command can be used within the time window
- `rate-limits.commands.<command>.time-window-seconds` - Time window in seconds for the rate limit

Default rate limits are applied if not specified in the configuration:
- `invsee`: 5 uses per 60 seconds
- `endersee`: 5 uses per 60 seconds
- `teleport`: 10 uses per 60 seconds
- `guild`: 15 uses per 60 seconds
- `locks`: 15 uses per 60 seconds
- `minigame`: 10 uses per 60 seconds
- `homes`: 10 uses per 60 seconds
- `warps`: 10 uses per 60 seconds

Players with the `furious.ratelimit.exempt` permission are exempt from all rate limits.

#### Security Reviews

The plugin includes a security review management system that tracks when security reviews are conducted and when the next review is due. Security review configuration is stored in the `security_reviews.yml` file:

- `last-review` - Timestamp of the last security review
- `next-review` - Timestamp of the next scheduled security review
- `review-interval-days` - Interval between security reviews in days (default: 90)
- `reviews` - List of completed security reviews with details

The security review interval can be configured using the `/security review interval <days>` command. Server administrators with the `furious.security.admin` permission will be notified when a security review is due.

## Commands

### Teleport Commands
- `/teleport` (aliases: `/tp`) - Teleport command for requesting and managing teleports
  - `/teleport request <player>` (alias: `/tpa <player>`) - Request to teleport to another player
  - `/teleport accept [player]` (alias: `/tpaccept [player]`) - Accept a teleport request
  - `/teleport decline [player]` (alias: `/tpdecline [player]`) - Decline a teleport request
  - `/teleport list` - List all pending teleport requests
  - `/teleport abort [player]` - Abort an outgoing teleport request
  - `/teleport deny` - Toggle auto-deny of teleport requests
  - `/teleport world` - Configure world teleport settings
    - `/teleport world enable <world>` - Enable teleportation in a world
    - `/teleport world disable <world>` - Disable teleportation in a world
  - `/teleport worlds` - List all worlds and their teleportation status
  - `/teleport coords <x> <y> <z> [world]` - Teleport to specific coordinates
  - `/teleport coords <player> <x> <y> <z> [world]` - Teleport a player to specific coordinates
  - `/teleport force <player>` - Force teleport yourself to a player
  - `/teleport force <source> <destination>` - Force teleport a player to another player
  - `/teleport worldspawn <player> [world]` - Teleport a player to a world's spawn location
  - `/teleport setworldspawn [world]` - Set the spawn location of a world to your current position
  - Permission: `furious.teleport.*` (includes all teleport permissions)

### Inventory Commands
- `/invsee <player>` - View another player's inventory
  - Permission: `furious.invsee` (default: op)
- `/endersee <player>` - View another player's enderchest
  - Permission: `furious.endersee` (default: op)
  - Permission for offline players: `furious.endersee.offline` (default: op)

### Guild Commands
- `/guild` (aliases: `/g`) - Guild command for creating and managing guilds
  - `/guild create <name>` - Create a new guild
  - `/guild invite <player>` - Invite a player to your guild
  - `/guild join <guild>` - Join a guild you've been invited to or request to join
  - `/guild leave` - Leave your current guild
  - `/guild info [guild]` - View information about a guild
  - `/guild list` - List all guilds
  - `/guild kick <player>` - Kick a player from your guild
  - `/guild disband` - Disband your guild
  - `/guild transfer <player>` - Transfer guild ownership
  - `/guild description <text>` - Set guild description
  - `/guild claim` - Claim a chunk for your guild
  - `/guild unclaim` - Unclaim a chunk from your guild
  - `/guild claims` - View claimed chunks of your guild
  - `/guild set mobs <allow|deny>` - Control mob spawning in guild claimed chunks
  - `/guild set open <true|false>` - Set whether the guild is open for anyone to join
  - `/guild accept [player]` - Accept a join request or invitation
  - `/guild decline [player]` - Decline a join request or invitation
  - `/guild homes` - Manage guild homes
  - `/guild world` - Manage guild world settings (admin only)
    - `/guild world list` - Show all worlds and their guild settings
    - `/guild world disable <world>` - Disable guilds in a world
    - `/guild world enable <world>` - Enable guilds in a world
  - Admin commands (require `furious.guild.admin` permission):
    - `/guild claim <SAFE|WAR|WILD>` - Claim a chunk for an unmanned guild
    - `/guild unclaim <SAFE|WAR|WILD>` - Unclaim a chunk from an unmanned guild
    - `/guild home <guild> [home]` - Teleport to another guild's home
    - `/guild homes <guild> [home]` - Alternative way to teleport to another guild's home
    - `/guild transfer <guild> <player>` - Transfer ownership of any guild
    - `/guild disband <guild>` - Disband any guild
    - `/guild kick <guild> <player>` - Kick a player from any guild
  - Permission: `furious.guild.*` (includes all guild permissions)
  - Permission: `furious.guild.admin` (includes all admin guild permissions)

### Player Status Commands
- `/heal [player1] [player2]...` - Heals yourself or given players to maximum
  - Permission: `heal.self` (for self), `heal.others` (for others)
- `/feed` (aliases: `/food`) - Feeds yourself or given players to maximum
  - Permission: `feed.self` (for self), `feed.others` (for others)

### Locks Commands
- `/locks` - Manage locks for blocks
  - `/locks lock` - Create lock items
  - `/locks unlock` - Create unlock items
  - `/locks info` - Check lock ownership
  - `/locks key` - Create key items
  - `/locks world` - Manage locks world settings
    - `/locks world list` - Show all worlds and their locks settings
    - `/locks world disable <world>` - Disable locks in a world
    - `/locks world enable <world>` - Enable locks in a world
  - Permission: `furious.locks.*` (includes all locks permissions)

### Minigame Commands
- `/minigame` (aliases: `/mg`) - Manage minigames
  - `/minigame join <game>` - Join a minigame queue
  - `/minigame leave [game]` - Leave a minigame queue
  - `/minigame list` - List available minigames
  - `/minigame info <game>` - View minigame information
  - `/minigame create <name> <type> <min> [map]` - Create a new minigame
  - `/minigame edit <game>` - Edit a minigame's settings
  - `/minigame disable <game>` - Disable a minigame
  - `/minigame enable <game>` - Enable a minigame
  - `/minigame start <game>` - Start a minigame
  - `/minigame stop <game>` - Stop a minigame
  - `/minigame spawn <game> <type> [index]` - Set spawn points for a minigame
  - `/minigame teleport <game>` - Teleport to a minigame's world
  - `/minigame save <game>` - Save a minigame's configuration
  - Permission: `furious.minigame.*` (includes all minigame permissions)
  - Available minigame types: Hunger Games (hungergame), Spleef (spleef), Zombie Survival (zombiesurvival)


### Homes Commands
- `/homes` (aliases: `/home`, `/h`) - Manage your homes
  - `/homes set <name>` - Set a home at your current location
  - `/homes move <name>` - Move an existing home to your current location
  - `/homes rename <old> <new>` - Rename a home
  - `/homes delete <name>` - Delete a home
  - `/homes list` - List all your homes
  - `/homes tp <name>` - Teleport to a home
  - `/homes buy` - Purchase additional home slots
  - `/homes world` - Manage homes world settings
    - `/homes world list` - Show all worlds and their homes settings
    - `/homes world disable <world>` - Disable homes in a world
    - `/homes world enable <world>` - Enable homes in a world
  - Admin commands (require `furious.homes.admin` permission):
    - `/homes set <player> <name>` - Set a home for another player
    - `/homes move <player> <name>` - Move an existing home for another player
    - `/homes rename <player> <oldname> <newname>` - Rename a home for another player
    - `/homes delete <player> <name>` - Delete a home for another player
    - `/homes list <player>` - List all homes for another player
    - `/homes tp <player> <name>` - Teleport to another player's home
  - Permission: `furious.homes.*` (includes all homes permissions)
  - Home limits: Permissions from `furious.homes.limit.1` to `furious.homes.limit.10`

### Warps Commands
- `/warps` (aliases: `/warp`) - Manage and use warps
  - `/warps create <name>` - Create a warp at your current location
  - `/warps relocate <name>` - Move an existing warp to your current location
  - `/warps cost <name> <amount>` - Set the cost to use a warp
  - `/warps passwd <name> <password>` - Set a password for a warp
  - `/warps rename <old> <new>` - Rename a warp
  - `/warps delete <name>` - Delete a warp
  - `/warps link <name>` - Link a warp to a portal
  - `/warps warp <name> [password]` - Teleport to a warp
  - `/warps list` - List all available warps
  - Permission: `furious.warps.*` (includes all warps permissions)

### Tombstone Commands
- `/tombstones` - Manage tombstones
  - `/tombstones purge` - Remove all tombstones
  - Permission: `furious.tombstones.admin` (default: op)

### Security Commands
- `/security` - Manage security reviews and other security-related tasks
  - `/security status` - Show the status of security reviews
  - `/security review complete [notes]` - Mark a security review as completed
  - `/security review interval <days>` - Set the interval between security reviews
  - `/security help` - Show help for security commands
  - Permission: `furious.security.admin` (default: op)

### Bank Commands
- Currency in the game is called "Scraps" (abbreviated as "S")
- New players start with 50S in their wallet and 100S in their RubberBank account
- `/bank` (aliases: `/b`) - Manage your bank account
  - `/bank balance` (alias: `/bank b`) - View your wallet and bank balances
  - `/bank deposit <amount>` (alias: `/bank d <amount>`) - Deposit from wallet to bank
  - `/bank withdraw <amount>` (alias: `/bank w <amount>`) - Withdraw from bank to wallet
  - `/bank transfer <player> <amount>` (alias: `/bank t <player> <amount>`) - Transfer from your bank to another player's bank
  - `/bank claim [bank]` (alias: `/bank c [bank]`) - Claim the current chunk for a bank (default: RubberBank)
  - `/bank unclaim` (alias: `/bank u`) - Unclaim the current chunk from a bank
  - `/bank info` (alias: `/bank i`) - Show information about the bank at your location
  - `/bank help` (alias: `/bank h`) - Show help for bank commands
  - Permission: `furious.bank.*` (includes all bank permissions)

#### Bank Administration Commands
- `/bank create <bankName>` - Create a new bank with the specified name
  - Permission: `furious.bank.create` (default: op)
- `/bank rename <oldName> <newName>` - Rename a bank
  - Permission: `furious.bank.rename` (default: op)
- `/bank delete <bankName>` - Delete a bank (cannot delete the default RubberBank)
  - Permission: `furious.bank.delete` (default: op)
- `/bank createaccount <bankName> <player> [initialBalance]` - Create an account for a player in a bank
  - Permission: `furious.bank.createaccount` (default: op)
- `/bank deleteaccount <bankName> <player>` - Delete a player's account from a bank
  - Permission: `furious.bank.deleteaccount` (default: op)
- `/bank editbalance <bankName> <player> <newBalance>` - Set the balance of a player's account in a bank
  - Permission: `furious.bank.editbalance` (default: op)
- `/bank editinterest <bankName> <interestRate>` - Set the interest rate of a bank (e.g., 0.05 for 5%)
  - Interest is applied after 2 day-cycles
  - After interest is applied, new random interest rates (between 0.00 and 1.00) are set for each bank
  - The editInterest command can override the random interest rates
  - Permission: `furious.bank.editinterest` (default: op)

## Permissions

### Teleport Permissions
- `furious.teleport.*` - Access to all teleport commands
- `furious.teleport.request` - Request teleports to other players (default: true)
- `furious.teleport.accept` - Accept teleport requests (default: true)
- `furious.teleport.decline` - Decline teleport requests (default: true)
- `furious.teleport.list` - List teleport requests (default: true)
- `furious.teleport.abort` - Abort outgoing teleport requests (default: true)
- `furious.teleport.deny` - Toggle auto-deny of teleport requests (default: true)
- `furious.teleport.worldconfig` - Configure world teleport settings (default: op)
- `furious.teleport.coords` - Use coordinate teleport commands (default: op)
- `furious.teleport.force` - Use force teleport commands (default: op)
- `furious.teleport.worldspawn` - Teleport players to world spawn points (default: op)
- `furious.teleport.setworldspawn` - Set world spawn points (default: op)

### Guild Permissions
- `furious.guild.*` - Access to all guild commands
- `furious.guild.create` - Create guilds (default: true)
- `furious.guild.invite` - Invite players to guilds (default: true)
- `furious.guild.join` - Join guilds (default: true)
- `furious.guild.leave` - Leave guilds (default: true)
- `furious.guild.info` - View guild information (default: true)
- `furious.guild.list` - List all guilds (default: true)
- `furious.guild.kick` - Kick players from guilds (default: true)
- `furious.guild.disband` - Disband guilds (default: true)
- `furious.guild.transfer` - Transfer guild ownership (default: true)
- `furious.guild.description` - Set guild descriptions (default: true)
- `furious.guild.claim` - Claim chunks for a guild (default: true)
- `furious.guild.claim.unmanned` - Claim chunks for unmanned guilds (default: op)
- `furious.guild.unclaim` - Unclaim chunks from a guild (default: true)
- `furious.guild.unclaim.unmanned` - Unclaim chunks from unmanned guilds (default: op)
- `furious.guild.claims` - View claimed chunks of a guild (default: true)
- `furious.guild.mobs` - Control mob spawning in guild claimed chunks (default: true)
- `furious.guild.homes` - Manage guild homes (default: true)
- `furious.guild.homes.set` - Set guild homes (default: true)
- `furious.guild.homes.teleport` - Teleport to guild homes (default: true)
- `furious.guild.world` - Manage guild world settings (default: op)

### Locks Permissions
- `furious.locks.*` - Access to all locks commands
- `furious.locks.lock` - Create lock items (default: true)
- `furious.locks.unlock` - Create unlock items (default: true)
- `furious.locks.info` - Check lock ownership (default: true)
- `furious.locks.key` - Create key items (default: true)
- `furious.locks.world` - Manage locks world settings (default: op)

### Minigame Permissions
- `furious.minigame.*` - Access to all minigame commands
- `furious.minigame.join` - Join minigame queues (default: true)
- `furious.minigame.leave` - Leave minigame queues (default: true)
- `furious.minigame.list` - List available minigames (default: true)
- `furious.minigame.info` - View minigame information (default: true)
- `furious.minigame.create` - Create minigames (default: op)
- `furious.minigame.disable` - Disable minigames (default: op)
- `furious.minigame.enable` - Enable minigames (default: op)
- `furious.minigame.start` - Start minigames (default: op)
- `furious.minigame.stop` - Stop minigames (default: op)
- `furious.minigame.edit` - Edit minigames (default: op)
- `furious.minigame.tp` - Teleport to the GameWorld (default: op)


### Homes Permissions
- `furious.homes.*` - Access to all homes commands
- `furious.homes.set` - Set homes (default: true)
- `furious.homes.move` - Move homes (default: true)
- `furious.homes.rename` - Rename homes (default: true)
- `furious.homes.delete` - Delete homes (default: true)
- `furious.homes.list` - List homes (default: true)
- `furious.homes.tp` - Teleport to homes (default: true)
- `furious.homes.buy` - Purchase additional home slots (default: true)
- `furious.homes.admin` - Manage other players' homes (default: op)
- `furious.homes.world` - Manage homes world settings (default: op)
- `furious.homes.limit.*` - Home limit permissions
  - `furious.homes.limit.1` - Sets home limit to 1 (default: false)
  - `furious.homes.limit.2` - Sets home limit to 2 (default: false)
  - `furious.homes.limit.3` - Sets home limit to 3 (default: true)
  - `furious.homes.limit.5` - Sets home limit to 5 (default: false)
  - `furious.homes.limit.10` - Sets home limit to 10 (default: false)

### Warps Permissions
- `furious.warps.*` - Access to all warps commands
- `furious.warps.create` - Create warps (default: op)
- `furious.warps.relocate` - Relocate warps (default: op)
- `furious.warps.cost` - Set warp costs (default: op)
- `furious.warps.passwd` - Set warp passwords (default: op)
- `furious.warps.rename` - Rename warps (default: op)
- `furious.warps.delete` - Delete warps (default: op)
- `furious.warps.link` - Link warps to portals (default: op)
- `furious.warps.warp` - Teleport to warps (default: true)
- `furious.warps.list` - List warps (default: true)

### Player Status Permissions
- `heal.self` - Heal yourself (default: op)
- `heal.others` - Heal other players (default: op)
- `feed.self` - Feed yourself (default: op)
- `feed.others` - Feed other players (default: op)

### Tombstone Permissions
- `furious.tombstones.*` - Access to all tombstone commands
- `furious.tombstones.admin` - Allows managing tombstones (purge, etc.) (default: op)

### Security Permissions
- `furious.security.*` - Access to all security commands
  - `furious.security.admin` - Allows managing security reviews and other security-related tasks (default: op)
- `furious.ratelimit.exempt` - Exempts a player from all rate limits (default: op)

### Bank Permissions
- `furious.bank.*` - Access to all bank commands
- `furious.bank.balance` - Check bank balance (default: true)
- `furious.bank.deposit` - Deposit to bank (default: true)
- `furious.bank.withdraw` - Withdraw from bank (default: true)
- `furious.bank.transfer` - Transfer funds between bank accounts (default: true)
- `furious.bank.claim` - Claim chunks for banks (default: op)
- `furious.bank.unclaim` - Unclaim chunks from banks (default: op)
- `furious.bank.info` - View bank information at current location (default: true)
- `furious.bank.admin` - Administrative bank operations (default: op)

## Support and Contributing

### Reporting Issues

If you encounter any issues or bugs while using the Furious plugin, please report them through the issue tracker. When reporting issues, please include:

- A clear description of the problem
- Steps to reproduce the issue
- Server version and plugin version
- Any relevant error messages or logs

### Contributing

Contributions to the Furious plugin are welcome! If you'd like to contribute:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### License

This project is licensed under the MIT License - see the LICENSE file for details.
