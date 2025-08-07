# Player Guide: TELEPORT

This guide provides information on how to use the `/teleport` command and its features as a player.

## Table of Contents
- [Overview](#overview)
- [Basic Usage](#basic-usage)
- [Permission Requirements](#permission-requirements)
- [Request System](#request-system)
- [Managing Requests](#managing-requests)
- [Command Aliases](#command-aliases)
- [Examples](#examples)
- [Tips and Best Practices](#tips-and-best-practices)

## Overview

The teleport system allows you to request teleportation to other players, accept or decline incoming requests, and manage your teleport preferences. The system is designed to be safe and user-friendly, with options to control your teleportation experience.

## Basic Usage

- `/teleport request <player>` - Request to teleport to a player
- `/teleport accept [player]` - Accept teleport request
- `/teleport decline [player]` - Decline teleport request
- `/teleport list [in|out]` - List teleport requests
- `/teleport abort` - Cancel your outgoing request or countdown
- `/teleport deny` - Toggle auto-decline of requests

## Permission Requirements

To use the teleport system, you need these permissions:

- `furious.teleport.request` - Allows requesting teleports to other players (default: true)
- `furious.teleport.accept` - Allows accepting teleport requests (default: true)
- `furious.teleport.decline` - Allows declining teleport requests (default: true)
- `furious.teleport.list` - Allows listing teleport requests (default: true)
- `furious.teleport.abort` - Allows aborting outgoing teleport requests (default: true)
- `furious.teleport.deny` - Allows toggling auto-deny of teleport requests (default: true)

### Permission Inheritance
The server now uses a structured permission inheritance system:
- `furious.teleport.*` grants all teleport permissions
- `furious.teleport.admin.*` grants all administrative teleport permissions
- Higher-level permissions automatically grant related lower-level permissions

For example, if you have `furious.teleport.*`, you automatically have all basic teleport permissions.

## Request System

The teleport request system allows you to request teleportation to other players.

### Requesting Teleportation
- `/teleport request <player>` - Request to teleport to a player
  - Example: `/teleport request Steve`
  - Sends a teleport request to the specified player
  - The target player must accept the request for the teleportation to occur
  - Requirements:
    - The target player must be online
    - You cannot request to teleport to yourself
    - The target player must not have auto-deny enabled for you
    - The target world must allow teleportation

### Accepting Requests
- `/teleport accept [player]` - Accept teleport request
  - Example: `/teleport accept Steve`
  - Accepts an incoming teleport request from the specified player
  - If no player is specified, accepts the most recent request

### Declining Requests
- `/teleport decline [player]` - Decline teleport request
  - Example: `/teleport decline Steve`
  - Declines an incoming teleport request from the specified player
  - If no player is specified, declines the most recent request

## Managing Requests

These commands allow you to manage your teleport requests and preferences.

### Listing Requests
- `/teleport list [in|out]` - List teleport requests
  - Example: `/teleport list in` - List incoming requests
  - Example: `/teleport list out` - List outgoing requests
  - If no direction is specified, lists both incoming and outgoing requests

### Aborting Requests
- `/teleport abort` - Cancel your outgoing request or countdown
  - Example: `/teleport abort`
  - Cancels any outgoing teleport request or teleportation countdown

### Auto-Deny Settings
- `/teleport deny` - Toggle auto-deny of requests
  - Example: `/teleport deny`
  - When enabled, automatically declines all incoming teleport requests

## Command Aliases

For convenience, you can use these shorter command aliases:

- `/tp` - Alias for `/teleport`
- `/tpa <player>` - Alias for `/teleport request <player>`
- `/tpaccept [player]` - Alias for `/teleport accept [player]`
- `/tpdecline [player]` - Alias for `/teleport decline [player]`

## Examples

### Basic Teleport Requests

1. Request to teleport to another player:
   ```
   /teleport request Alex
   ```
   or
   ```
   /tpa Alex
   ```

2. Accept a teleport request:
   ```
   /teleport accept Steve
   ```
   or
   ```
   /tpaccept Steve
   ```

3. Decline a teleport request:
   ```
   /teleport decline Steve
   ```
   or
   ```
   /tpdecline Steve
   ```

4. List all your teleport requests:
   ```
   /teleport list
   ```

5. Cancel your outgoing request:
   ```
   /teleport abort
   ```

6. Toggle auto-deny of teleport requests:
   ```
   /teleport deny
   ```

## Tips and Best Practices

1. **Communicate Before Requesting**: It's polite to ask in chat before sending a teleport request.

2. **Use Auto-Deny Wisely**: Enable auto-deny when you're busy or don't want to be disturbed, but remember to turn it off later.

3. **Check World Settings**: Some worlds may have teleportation disabled by server administrators. World teleport settings are now more configurable.

4. **Use Command Aliases**: For frequently used commands, remember the shorter aliases like `/tpa`, `/tpaccept`, and `/tpdecline`.

5. **Abort Requests When Needed**: If you change your mind after sending a request, use `/teleport abort` to cancel it.

6. **Specify Players for Multiple Requests**: If you have multiple incoming requests, specify the player name when accepting or declining.

7. **Safe Teleportation**: The plugin ensures teleportation is safe by finding a safe location near the destination.

8. **Minigame Restriction**: Teleport commands cannot be used while in a minigame.

9. **Unified Teleportation System**: The teleport system now integrates seamlessly with homes and warps systems, providing a consistent teleportation experience across all teleport types.

10. **Permission-Based Cooldowns**: Your teleport cooldown times are now based on your permission level. Higher permission levels may have shorter cooldowns or none at all.

11. **Permission-Based Costs**: Teleport costs (if enabled) are now determined by your permission level. Higher permission levels may have reduced costs or free teleportation.

12. **Consistent Restrictions**: Teleport restrictions (cooldowns, costs, etc.) are now consistent across all teleport types (player teleports, homes, warps), based on your permissions.

13. **Customizable Experience**: Server administrators can now customize how permissions affect your teleport behavior. If you need special teleport privileges, ask about permission options.