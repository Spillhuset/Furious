# Player Guide: TOMBSTONES

This guide provides information on how to use the tombstone system as a player.

## Table of Contents
- [Overview](#overview)
- [Basic Usage](#basic-usage)
- [Permission Requirements](#permission-requirements)
- [Player Experience](#player-experience)
- [Examples](#examples)
- [Tips and Best Practices](#tips-and-best-practices)

## Overview

The tombstone system provides a secure way for players to recover their items after death. When a player dies, a tombstone is created at their death location, containing all their inventory items. Players can interact with their tombstones to retrieve their items, and tombstones automatically expire after a configurable time period.

## Basic Usage

The tombstone system works automatically when you die. There are no specific commands for players to use.

## Permission Requirements

The tombstone system works automatically for all players without requiring any special permissions.

## Player Experience

This section explains how the tombstone system works from a player's perspective.

### Death and Tombstone Creation

When you die:
1. A tombstone is automatically created at your death location
2. The tombstone contains all items from your inventory
3. You receive a message with the coordinates of your tombstone
4. The tombstone appears as a visible marker in the world (an armor stand)

### Retrieving Items

To retrieve items from a tombstone:
1. Find your tombstone at your death location
2. Right-click on the tombstone (the armor stand)
3. Your items will be transferred back to your inventory
4. If your inventory is full, remaining items will stay in the tombstone
5. Once all items are retrieved, the tombstone disappears

### Tombstone Expiration

Tombstones don't last forever:
- Tombstones expire after a configurable time period (default: 30 minutes)
- When a tombstone expires, it disappears along with any remaining items
- You should retrieve your items before the expiration time

### Tombstone Protection

Tombstones are protected:
- Only you can retrieve items from your tombstone
- Other players cannot destroy or interact with your tombstone
- This ensures your items remain safe until you can retrieve them

## Examples

### Player Death and Recovery

1. You die while mining:
   ```
   [Server] You died at X: 100, Y: 40, Z: -200. A tombstone has been created with your items.
   ```

2. You return to your death location and find your tombstone.

3. You right-click on the tombstone to retrieve your items:
   ```
   [Server] You have retrieved your items from the tombstone.
   ```

## Tips and Best Practices

1. **Remember Your Death Location**: Make note of the coordinates where you died so you can find your tombstone.

2. **Act Quickly**: Tombstones expire after a set time, so retrieve your items as soon as possible.

3. **Clear Inventory Space**: Before retrieving items from your tombstone, make sure you have enough inventory space.

4. **Use Ender Chest**: Store valuable items in your ender chest to avoid losing them on death.

5. **Safe Location Finding**: If your death location is in a dangerous area (like lava), the tombstone will be placed at the nearest safe location.

6. **Multiple Deaths**: If you die multiple times, each death creates a separate tombstone. Be sure to retrieve items from all your tombstones.

7. **Server Restarts**: Tombstones persist through server restarts, so you don't need to worry about losing your items if the server restarts.

8. **World Restrictions**: Tombstones work in all worlds unless specifically disabled in the configuration.

9. **Inventory Management**: If your inventory is full when retrieving items, make multiple trips or drop less valuable items to make room.