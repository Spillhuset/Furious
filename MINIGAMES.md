# Furious Minigames System

This document provides a comprehensive overview of the minigame system in the Furious plugin, including available minigame types, commands, configuration options, and gameplay mechanics.

## Table of Contents

1. [Overview](#overview)
2. [Minigame Types](#minigame-types)
   - [Hunger Games](#hunger-games)
   - [Zombie Survival](#zombie-survival)
   - [Spleef](#spleef)
3. [Commands](#commands)
   - [Player Commands](#player-commands)
   - [Admin Commands](#admin-commands)
4. [Configuration](#configuration)
   - [Global Settings](#global-settings)
   - [Minigame-Specific Settings](#minigame-specific-settings)
5. [Gameplay Mechanics](#gameplay-mechanics)
   - [Joining and Leaving](#joining-and-leaving)
   - [Game States](#game-states)
   - [Editing Minigames](#editing-minigames)

## Overview

The Furious minigame system provides a flexible framework for creating, managing, and playing various minigames on your server. The system supports both built-in minigame types and configurable minigames that can be customized by server administrators.

Key features of the minigame system include:
- Multiple minigame types with unique gameplay mechanics
- Queue system for players to join games
- Configurable settings for each minigame
- Admin commands for creating and managing minigames
- Player inventory and location preservation
- Dedicated game worlds for each minigame instance

## Minigame Types

The system currently supports three types of minigames:

### Hunger Games

A PvP survival game inspired by the popular franchise where players fight to be the last one standing.

**Mechanics:**
- Players start with basic equipment (wooden sword and bread)
- Initial grace period (30 seconds) where PvP is disabled
- Containers (chests) throughout the map contain random loot
- Containers are restocked periodically
- Players are eliminated when killed and become spectators
- Last player standing wins
- Game ends after 10 minutes if no winner is determined

**Special Features:**
- Countdown phase before game starts
- Grace period announcements
- Time remaining announcements
- Player elimination announcements
- Winner announcement

### Zombie Survival

A PvE survival game where players fight against waves of zombies.

**Mechanics:**
- Players can choose difficulty level (affects starting equipment):
  - EASY: Diamond armor and sword
  - NORMAL: Iron armor and sword
  - HARD: Leather armor and wooden sword
  - INSANE: No armor or weapons
- Zombies spawn at regular intervals near random players
- Spawn rate increases over time
- Players who die become spectators
- Game ends when all players are eliminated
- Success is measured by how many zombies were spawned/defeated

**Special Features:**
- Difficulty selection via signs
- Countdown phase before game starts
- Zombie spawn announcements
- Player elimination announcements
- End-game summary showing total zombies spawned

### Spleef

A competitive minigame where players break blocks beneath other players to make them fall.

**Mechanics:**
- Players compete on a floor made of snow blocks
- Players use shovels to break blocks beneath opponents
- Last player remaining on the platform wins
- Game ends after 5 minutes if no winner is determined

**Note:** Spleef is defined in the system but may not have a complete implementation.

## Commands

### Player Commands

| Command | Description | Permission | Usage |
|---------|-------------|------------|-------|
| `/minigame join <game>` | Join a minigame queue | furious.minigame.join | `/minigame join hungergame` |
| `/minigame leave` | Leave the current minigame queue | furious.minigame.leave | `/minigame leave` |
| `/minigame list` | List available minigames | furious.minigame.list | `/minigame list` |
| `/minigame info <game>` | Get information about a minigame | furious.minigame.info | `/minigame info hungergame` |

### Admin Commands

| Command | Description | Permission | Usage |
|---------|-------------|------------|-------|
| `/minigame create <name> <type> <min> [map]` | Create a new minigame | furious.minigame.create | `/minigame create mygame hungergame 2 mygameworld` |
| `/minigame disable <game>` | Disable the queue for a minigame | furious.minigame.disable | `/minigame disable mygame` |
| `/minigame enable <game>` | Enable the queue for a minigame | furious.minigame.enable | `/minigame enable mygame` |
| `/minigame start <game>` | Force start a minigame | furious.minigame.start | `/minigame start mygame` |
| `/minigame stop <game>` | Force stop a minigame | furious.minigame.stop | `/minigame stop mygame` |
| `/minigame edit <game>` | Enter edit mode for a minigame | furious.minigame.edit | `/minigame edit mygame` |
| `/minigame spawn <num>` | Set a spawn point while in edit mode | furious.minigame.edit | `/minigame spawn 1` |
| `/minigame save` | Save changes made in edit mode | furious.minigame.edit | `/minigame save` |

## Configuration

The minigame system is configured through the `minigames.yml` file.

### Global Settings

```yaml
# Default minimum number of players required to start a minigame
default-min-players: 2

# Default maximum number of players allowed in a minigame
default-max-players: 16

# Default countdown time (in seconds) before a minigame starts
default-countdown: 30

# Default game duration (in seconds, 0 for unlimited)
default-duration: 600

# If true, players will be teleported back to their previous location after a minigame ends
teleport-back: true

# If true, players' inventories will be saved and restored after a minigame ends
save-inventory: true

# If true, players' health and hunger will be restored after a minigame ends
restore-health: true

# List of worlds where minigames cannot be played
disabled-worlds: []

# Message shown when minigame functionality is attempted in a disabled world
disabled-world-message: "&cMinigames cannot be played in this world!"
```

### Minigame-Specific Settings

```yaml
minigames:
  hunger_games:
    min-players: 4
    max-players: 24
    countdown: 60
    duration: 1200
    grace-period: 30
    chest-refill-interval: 300

  spleef:
    min-players: 2
    max-players: 16
    countdown: 30
    duration: 300
    floor-material: SNOW_BLOCK

  zombie_survival:
    min-players: 2
    max-players: 8
    countdown: 30
    duration: 600
    waves: 10
    zombies-per-wave: 5
```

## Gameplay Mechanics

### Joining and Leaving

1. Players join a minigame queue using `/minigame join <game>`
2. When enough players join (reaching the minimum player count), a countdown begins
3. After the countdown, players are teleported to the game world and the game starts
4. Players' inventories and locations are saved before joining the game
5. When the game ends, players are teleported back to their original locations and their inventories are restored

### Game States

Minigames can be in one of the following states:

1. **DISABLED**: Minigame is disabled and no one can join
2. **READY**: Game is ready to be joined
3. **QUEUE**: Someone has joined the queue, waiting to reach minimum players
4. **COUNTDOWN**: Minimum number of players reached, final countdown till game starts
5. **STARTED**: Game has started, players are playing
6. **FINAL**: Game is in its final stage

### Editing Minigames

Administrators can edit minigames using the following process:

1. Enter edit mode with `/minigame edit <game>`
2. Set spawn points using one of these methods:
   - Place carpet blocks where you want spawn points to be
   - Use the command `/minigame spawn <num>` to set spawn points manually
3. Save changes with `/minigame save` (this will convert carpet blocks to spawn points)
4. Exit edit mode automatically after saving

When using carpet blocks for spawn points:
- Each carpet block will become a spawn point
- The number of carpet blocks determines the maximum number of players
- Carpet blocks are removed when saving
- Existing spawn points are shown as carpet blocks when entering edit mode

When in edit mode, a scoreboard is displayed showing:
- The minigame being edited
- The number of spawn points set
- The minimum number of players required
- The current state of the minigame