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
- `wallet.yml`: Economy system configuration
- `tombstones.yml`: Tombstone system settings
- `security_reviews.yml`: Security review configuration

### Database Configuration
The plugin supports multiple storage options for guild data:
- YAML: Stores guild data in the `guilds.yml` file (default)
- MySQL/MariaDB: Stores guild data in a MySQL or MariaDB database
- SQLite: Stores guild data in a SQLite database file

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
- `/wallet`: View your wallet balance
- `/wallet [player]`: View another player's wallet balance
- `/wallet pay <player> <amount>`: Pay another player

#### Admin Wallet Commands
- `/wallet give <player> <amount>`: Give money to a player
- `/wallet take <player> <amount>`: Take money from a player
- `/wallet set <player> <amount>`: Set a player's balance

#### Bank Commands
- `/bank balance`: Check your bank balance
- `/bank deposit <amount>`: Deposit money into your bank
- `/bank withdraw <amount>`: Withdraw money from your bank
- `/bank transfer <amount> <from> <to>`: Transfer money between accounts
- `/bank info`: View bank information at current location

#### Admin Bank Commands
- `/bank claim`: Claim chunks for banks
- `/bank unclaim`: Unclaim chunks from banks
- `/bank create <name>`: Create a new bank
- `/bank rename <old> <new>`: Rename a bank
- `/bank delete <name>`: Delete a bank
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
- `/warps warp <name>`: Teleport to a specific warp location
- `/warp <name>`: Alias for `/warps warp`

#### Admin Commands
- `/warps create <name>`: Create a new warp at your current location
- `/setwarp <name>`: Alias for `/warps create`
- `/warps delete <name>`: Delete an existing warp
- `/delwarp <name>`: Alias for `/warps delete`
- `/warps relocate <name>`: Move an existing warp to your current location
- `/warps cost <name> <amount>`: Set the cost to use a warp
- `/warps passwd <name> <password>`: Set a password for a warp
- `/warps rename <old> <new>`: Rename an existing warp
- `/warps link <name>`: Link a warp to a portal

### Home Commands

#### User Commands
- `/homes list`: List all your set home locations
- `/homes tp <name>`: Teleport to one of your homes
- `/home <name>`: Alias for `/homes tp`
- `/homes set <name>`: Set a new home at your current location
- `/sethome <name>`: Alias for `/homes set`
- `/homes delete <name>`: Delete one of your homes
- `/delhome <name>`: Alias for `/homes delete`
- `/homes move <name>`: Move an existing home to your current location
- `/homes rename <old> <new>`: Rename one of your homes
- `/homes buy`: Purchase additional home slots

### Guild Commands
- `/guild create <name>`: Create a new guild
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
- `/perm help`: Get help with permission commands

#### Role Management
- `/perm roles create <name>`: Create a new role
- `/perm roles delete <name>`: Delete a role
- `/perm roles list`: List all roles
- `/perm roles info <role>`: View role information
- `/perm roles set description <role> <description>`: Set role description
- `/perm roles add permission <role> <permission>`: Add permission to role
- `/perm roles remove permission <role> <permission>`: Remove permission from role
- `/perm roles add player <role> <player>`: Add role to player
- `/perm roles remove player <role> <player>`: Remove role from player

#### Player Management
- `/perm player list roles <player>`: List player's roles
- `/perm player add permission <player> <permission>`: Add permission to player
- `/perm player remove permission <player> <permission>`: Remove permission from player
- `/perm player list permissions <player>`: List player's permissions


### Minigame Commands

#### User Commands
- `/minigame list`: List available minigames
- `/minigame info <game>`: View information about a minigame
- `/minigame join <game>`: Join a minigame queue
- `/minigame leave`: Leave the current queue

#### Admin Commands
- `/minigame create <name> <type>`: Create a new minigame
- `/minigame disable <name>`: Disable a minigame
- `/minigame enable <name>`: Enable a minigame
- `/minigame start <name>`: Force start a minigame
- `/minigame stop <name>`: Force stop a minigame
- `/minigame edit <name>`: Edit a minigame
- `/minigame spawn <name> <type>`: Set spawn point for a minigame
- `/minigame save <name>`: Save minigame configuration
- `/minigame tp`: Teleport to the GameWorld

### Tombstone Commands
- `/tombstones purge`: Purge expired tombstones (admin)

### Utility Commands
- `/invsee <player>`: View player inventory
- `/endersee <player>`: View player enderchest
- `/heal [player]`: Heal player
- `/feed [player]`: Feed player

## Permissions

Furious uses a permission-based system for access control. Here are the main permission nodes:

### General Permissions
- `furious.*`: Gives access to all furious plugin permissions
- `furious.admin`: All administrative permissions (not defined in plugin.yml)
- `furious.mod`: All moderator permissions (not defined in plugin.yml)
- `furious.user`: Basic user permissions (not defined in plugin.yml)

### Economy Permissions

#### Wallet Permissions
- `furious.wallet`: Allows basic wallet operations (check balance, pay)
- `furious.wallet.admin`: Allows administrative wallet operations (give, take, set)
- `furious.wallet.*`: Gives access to all wallet commands

#### Bank Permissions
- `furious.bank.balance`: Allows checking bank balance
- `furious.bank.deposit`: Allows depositing to bank
- `furious.bank.withdraw`: Allows withdrawing from bank
- `furious.bank.transfer`: Allows transferring funds between bank accounts
- `furious.bank.info`: Allows viewing bank information at current location
- `furious.bank.claim`: Allows claiming chunks for banks
- `furious.bank.unclaim`: Allows unclaiming chunks from banks
- `furious.bank.create`: Allows creating banks
- `furious.bank.rename`: Allows renaming banks
- `furious.bank.delete`: Allows deleting banks
- `furious.bank.deleteaccount`: Allows deleting accounts from banks
- `furious.bank.createaccount`: Allows creating accounts in banks
- `furious.bank.editbalance`: Allows editing player balances in banks
- `furious.bank.editinterest`: Allows editing bank interest rates
- `furious.bank.admin`: Allows administrative bank operations
- `furious.bank.*`: Gives access to all bank commands

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
- `furious.teleport.*`: Gives access to all teleport commands

### Warp Permissions

#### User Permissions
- `furious.warps.warp`: Allows teleporting to warps
- `furious.warps.list`: Allows listing warps

#### Admin Permissions
- `furious.warps.create`: Allows creating warps
- `furious.warps.relocate`: Allows relocating warps
- `furious.warps.cost`: Allows setting warp costs
- `furious.warps.passwd`: Allows setting warp passwords
- `furious.warps.rename`: Allows renaming warps
- `furious.warps.delete`: Allows deleting warps
- `furious.warps.link`: Allows linking warps to portals
- `furious.warps.*`: Gives access to all warps commands

### Home Permissions

#### User Permissions
- `furious.homes.set`: Allows setting homes
- `furious.homes.move`: Allows moving homes
- `furious.homes.rename`: Allows renaming homes
- `furious.homes.delete`: Allows deleting homes
- `furious.homes.list`: Allows listing homes
- `furious.homes.tp`: Allows teleporting to homes
- `furious.homes.buy`: Allows purchasing additional home slots
- `furious.homes.limit.<number>`: Sets home limit to <number> (e.g., 1, 2, 3, 5, 10)
- `furious.homes`: Allows using `/homes world list` command to view world settings for homes
- `furious.homes.world`: Allows using `/homes world enable/disable [world]` commands to manage homes in specific worlds

#### Admin Permissions
- `furious.homes.admin`: Allows managing other players' homes
- `furious.homes.*`: Gives access to all homes commands

### Guild Permissions
- `furious.guild.create`: Allows creating guilds
- `furious.guild.invite`: Allows inviting players to guilds
- `furious.guild.join`: Allows joining guilds
- `furious.guild.leave`: Allows leaving guilds
- `furious.guild.info`: Allows viewing guild information
- `furious.guild.list`: Allows listing all guilds
- `furious.guild`: Allows using `/guild world list` command to view world settings for guilds
- `furious.guild.world`: Allows using `/guild world enable/disable [world]` commands to manage guilds in specific worlds
- `furious.guild.kick`: Allows kicking players from guilds
- `furious.guild.disband`: Allows disbanding guilds
- `furious.guild.transfer`: Allows transferring guild ownership
- `furious.guild.description`: Allows setting guild descriptions
- `furious.guild.claim`: Allows claiming chunks for a guild
- `furious.guild.claim.unmanned`: Allows claiming chunks for unmanned guilds (S_A_F_E, WARZONE, WILDLIFE)
- `furious.guild.unclaim`: Allows unclaiming chunks from a guild
- `furious.guild.unclaim.unmanned`: Allows unclaiming chunks from unmanned guilds
- `furious.guild.claims`: Allows viewing claimed chunks of a guild
- `furious.guild.mobs`: Allows controlling mob spawning in guild claimed chunks
- `furious.guild.homes`: Allows managing guild homes
- `furious.guild.homes.set`: Allows setting guild homes
- `furious.guild.homes.teleport`: Allows teleporting to guild homes
- `furious.guild.*`: Gives access to all guild commands

### Locks Permissions
- `furious.locks.lock`: Allows creating lock items
- `furious.locks.unlock`: Allows creating unlock items
- `furious.locks.info`: Allows checking lock ownership
- `furious.locks.key`: Allows creating key items
- `furious.locks`: Allows using `/locks world list` command to view world settings for locks
- `furious.locks.world`: Allows using `/locks world enable/disable [world]` commands to manage locks in specific worlds
- `furious.locks.*`: Gives access to all locks commands

### Security Permissions
- `furious.security.admin`: Allows managing security reviews and other security-related tasks
- `furious.security.*`: Gives access to all security commands

### Tombstone Permissions
- `furious.tombstones.admin`: Allows managing tombstones (purge, etc.)
- `furious.tombstones.*`: Gives access to all tombstone commands

### Permission Management
- `furious.permission.*`: Gives access to all permission management commands

### Minigame Permissions

#### User Permissions
- `furious.minigame.join`: Allows joining minigame queues
- `furious.minigame.leave`: Allows leaving minigame queues
- `furious.minigame.list`: Allows listing available minigames
- `furious.minigame.info`: Allows viewing minigame information

#### Admin Permissions
- `furious.minigame.create`: Allows creating minigames
- `furious.minigame.disable`: Allows disabling minigames
- `furious.minigame.enable`: Allows enabling minigames
- `furious.minigame.start`: Allows starting minigames
- `furious.minigame.stop`: Allows stopping minigames
- `furious.minigame.edit`: Allows editing minigames
- `furious.minigame.tp`: Allows teleporting to the GameWorld
- `furious.minigame.*`: Gives access to all minigame commands

### Utility Permissions

#### Inventory Viewing
- `furious.invsee`: Allows viewing other players' inventories
- `furious.endersee`: Allows viewing other players' enderchests
- `furious.endersee.offline`: Allows viewing offline players' enderchests

#### Healing and Feeding
- `furious.heal.self`: Allows healing yourself
- `furious.heal.others`: Allows healing other players
- `furious.heal.*`: Gives access to all heal commands
- `furious.feed.self`: Allows feeding yourself
- `furious.feed.others`: Allows feeding other players
- `furious.feed.*`: Gives access to all feed commands

#### Rate Limiting
- `furious.ratelimit.exempt`: Exempts a player from all rate limits

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
