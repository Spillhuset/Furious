# Furious Plugin

A comprehensive Minecraft plugin for server administrators looking to enhance their Minecraft server with a variety of features. Furious provides teleportation, guild systems, home management, warps, locks, minigames, and more in a single plugin.

## Features

- **Teleportation System**: Request-based teleportation with accept/decline functionality
- **Guild System**: Create and manage guilds with chunk claiming, homes, and member management
- **Homes**: Personal teleport locations with customizable limits
- **Warps**: Server-wide teleport locations with optional costs and passwords
- **Locks**: Secure containers and doors with lock and key items
- **Minigames**: Built-in minigames including Hunger Games, Spleef, and Zombie Survival
- **Game World Management**: Tools for managing game worlds
- **Player Utilities**: Commands for healing and feeding players
- **Economy Integration**: Built-in currency system

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

For more details, see the [Security Report](SECURITY_REPORT.md).

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

### Economy Configuration

The plugin includes a built-in currency system that can be configured in the `wallet.yml` file:

- `currency.name` - The name of the currency (singular form)
- `currency.plural` - The plural form of the currency name
- `currency.symbol` - The symbol used for the currency
- `currency.material` - The Minecraft material used for physical currency items

## Commands

### Teleport Commands
- `/teleport` (aliases: `/tp`) - Teleport command for requesting and managing teleports
  - `/teleport request <player>` - Request to teleport to another player
  - `/teleport accept [player]` - Accept a teleport request
  - `/teleport decline [player]` - Decline a teleport request
  - `/teleport list` - List all pending teleport requests
  - `/teleport abort [player]` - Abort an outgoing teleport request
  - `/teleport deny` - Toggle auto-deny of teleport requests
  - `/teleport world <world>` - Configure world teleport settings
  - `/teleport coords <x> <y> <z> [world]` - Teleport to specific coordinates
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
  - `/guild join <guild>` - Join a guild you've been invited to
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
  - `/guild mobs <allow|deny>` - Control mob spawning in guild claimed chunks
  - `/guild homes` - Manage guild homes
  - Permission: `furious.guild.*` (includes all guild permissions)

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

### GameWorld Commands
- `/gameworld` (aliases: `/gw`) - Manage game worlds
  - `/gameworld tp` - Teleport to the GameWorld
  - `/gameworld save` - Save the GameWorld to GameBackup
  - Permission: `furious.gameworld.*` (includes all gameworld permissions)

### Homes Commands
- `/homes` (aliases: `/home`, `/h`) - Manage your homes
  - `/homes set <name>` - Set a home at your current location
  - `/homes move <name>` - Move an existing home to your current location
  - `/homes rename <old> <new>` - Rename a home
  - `/homes delete <name>` - Delete a home
  - `/homes list` - List all your homes
  - `/homes tp <name>` - Teleport to a home
  - `/homes buy` - Purchase additional home slots
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

### Locks Permissions
- `furious.locks.*` - Access to all locks commands
- `furious.locks.lock` - Create lock items (default: true)
- `furious.locks.unlock` - Create unlock items (default: true)
- `furious.locks.info` - Check lock ownership (default: true)
- `furious.locks.key` - Create key items (default: true)

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

### GameWorld Permissions
- `furious.gameworld.*` - Access to all game world commands
- `furious.gameworld.tp` - Teleport to the GameWorld (default: op)
- `furious.gameworld.save` - Save the GameWorld to GameBackup (default: op)

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
