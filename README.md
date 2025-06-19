# Furious - A Comprehensive Minecraft Server Plugin

Furious is a feature-rich Minecraft plugin designed to enhance server functionality with a wide range of utilities and gameplay features.

## Features

### Economy System
- **Wallet Management**: Personal player currency system
- **Banking System**: Store and manage currency with interest rates
- **Currency Items**: Physical representation of currency

### Player Utilities
- **Teleportation**: TPA requests, teleport commands, and warps
- **Homes System**: Set and manage multiple home locations
- **Inventory Management**: View and manage player inventories and enderchests
- **Player Commands**: Heal and feed commands for player maintenance

### Security Features
- **Locks System**: Secure containers, doors, and redstone components
- **Permission Management**: Granular permission control
- **Security Review System**: Scheduled security audits
- **Audit Logging**: Comprehensive logging of sensitive operations

### Guild System
- Create and manage player guilds
- Guild permissions and hierarchy
- Guild-based features and benefits

### Minigames
- Hunger Games support
- Custom container registry for minigames
- Minigame management utilities

### World Management
- Multi-world support
- World-specific configurations
- Cross-world teleportation

### Tombstone System
- Secure player items on death
- Retrieval system for lost items

### Combat Management
- PvP utilities
- Combat logging prevention

## Installation

1. Download the latest release JAR file from the releases page
2. Place the JAR file in your server's `plugins` folder
3. Restart your server
4. Configure the plugin using the generated configuration files in the `plugins/furious` directory

## Configuration

Furious generates several configuration files:
- `config.yml`: Main configuration file
- `guilds.yml`: Guild system configuration
- `locks.yml`: Locks system configuration
- `warps.yml`: Warps system configuration
- `security_reviews.yml`: Security review configuration

## Commands

### Economy Commands
- `/wallet [player]`: View wallet balance
- `/bank`: Access banking features

### Teleportation Commands
- `/tpa <player>`: Send teleport request
- `/tpaccept`: Accept teleport request
- `/tpdecline`: Decline teleport request
- `/teleport <player>`: Teleport to player (admin)
- `/warps`: List available warps
- `/homes`: Manage home locations

### Guild Commands
- `/guild create <name>`: Create a new guild
- `/guild invite <player>`: Invite player to guild
- `/guild leave`: Leave current guild

### Security Commands
- `/locks`: Manage locks
- `/security review`: Manage security reviews
- `/perm`: Manage permissions

### Utility Commands
- `/invsee <player>`: View player inventory
- `/endersee <player>`: View player enderchest
- `/heal [player]`: Heal player
- `/feed [player]`: Feed player

## Permissions

Furious uses a permission-based system for access control:
- `furious.teleport.*`: All teleport permissions
- `furious.guild.*`: All guild permissions
- `furious.locks.*`: All lock permissions
- `furious.security.admin`: Security administration
- `furious.wallet.admin`: Wallet administration

## Support

For issues, feature requests, or general support, please create an issue on the GitHub repository.

## License

This project is licensed under the [MIT License](LICENSE).