# Admin Guide: SHOPS

This guide provides information on how to administer and configure the shops system.

## Table of Contents
- [Overview](#overview)
- [Administrative Permissions](#administrative-permissions)
- [Admin Commands](#admin-commands)
- [Shop Management](#shop-management)
- [Territory Management](#territory-management)
- [Item Management](#item-management)
- [Price and Stock Management](#price-and-stock-management)
- [Examples](#examples)
- [Best Practices](#best-practices)

## Overview

The shops system allows players to buy and sell items at various shops throughout the world. As an administrator, you can create and manage shops, control shop territories, and manage item availability, prices, and stock levels. The system includes a dynamic pricing feature that adjusts prices based on supply and demand.

## Administrative Permissions

To administer the shops system, you need these permissions:

### Basic Shop Management Permissions
- `furious.shops.create` - Allows creating shops (default: op)
- `furious.shops.delete` - Allows deleting shops (default: op)
- `furious.shops.claim` - Allows claiming chunks for shops (default: op)
- `furious.shops.unclaim` - Allows unclaiming chunks from shops (default: op)
- `furious.shops.setspawn` - Allows setting shop spawn points (default: op)

### Item Management Permissions
- `furious.shops.additem` - Allows adding items to shops (default: op)
- `furious.shops.removeitem` - Allows removing items from shops (default: op)
- `furious.shops.setprice` - Allows setting item prices (default: op)
- `furious.shops.setstock` - Allows setting item stock levels (default: op)
- `furious.shops.restock` - Allows restocking shops (default: op)
- `furious.shops.togglebuy` - Allows toggling whether items can be bought from shops (default: op)
- `furious.shops.togglesell` - Allows toggling whether items can be sold to shops (default: op)

### Granular Admin Permissions
- `furious.shops.create.others` - Allows creating shops for other players (default: op)
- `furious.shops.delete.others` - Allows deleting other players' shops (default: op)
- `furious.shops.manage.others` - Allows managing other players' shops (default: op)
- `furious.shops.price.others` - Allows setting prices in other players' shops (default: op)
- `furious.shops.stock.others` - Allows managing stock in other players' shops (default: op)

### Legacy Permission
- `furious.shops.admin` - Allows access to all shop administration commands (default: op)

### Permission Inheritance
The server now uses a structured permission inheritance system:

- `furious.shops.*` - Grants all shops permissions
- `furious.shops.admin.*` - Grants all administrative shops permissions
- Higher-level permissions automatically grant related lower-level permissions

For example, if you grant a player `furious.shops.admin.*`, they automatically have all administrative shops permissions like `furious.shops.create.others`, `furious.shops.delete.others`, etc.

### Economic Integration
The shops system integrates with the wallet and bank permission systems:

- Players with `furious.wallet.bypass.cost` can make purchases without sufficient funds
- Players with `furious.bank.withdraw.auto` can automatically withdraw funds from their bank when making purchases
- Shop administrators with `furious.shops.price.dynamic` can configure dynamic pricing based on supply and demand

## Admin Commands

These commands are available for server administrators:

### Shop Management
- `/shops create <name>` - Create a new shop
  - Example: `/shops create MiningShop`
  - Creates a new shop with the specified name
  - Requirements:
    - You must have the `furious.shops.create` permission

- `/shops delete <name>` - Delete a shop
  - Example: `/shops delete OldShop`
  - Warns about deleting the shop and its associations
  - Requirements:
    - You must have the `furious.shops.delete` permission

- `/shops delete confirm` - Confirm shop deletion
  - Example: `/shops delete confirm`
  - Permanently removes the shop and all its associations
  - Must be used after `/shops delete <name>` command
  - Requirements:
    - You must have the `furious.shops.delete` permission

- `/shops spawn` - Set the spawn point for the shop at your current location
  - Example: `/shops spawn`
  - Sets the shop's spawn point to your current location
  - Requirements:
    - You must have the `furious.shops.setspawn` permission
    - You must be standing in a shop's claimed chunk

### Territory Management
- `/shops claim <name>` - Claim the current chunk for a shop
  - Example: `/shops claim MiningShop`
  - Claims the chunk you're standing in for the specified shop
  - Requirements:
    - You must have the `furious.shops.claim` permission

- `/shops unclaim` - Unclaim the current chunk from a shop
  - Example: `/shops unclaim`
  - Removes the shop's claim on the chunk you're standing in
  - Requirements:
    - You must have the `furious.shops.unclaim` permission
    - You must be standing in a shop's claimed chunk

### Item Management
- `/shops add <item>` - Add an item to the shop at your current location
  - Example: `/shops add diamond`
  - Adds the specified item to the shop's inventory
  - Requirements:
    - You must have the `furious.shops.additem` permission
    - You must be standing in a shop's claimed chunk

- `/shops remove <item>` - Remove an item from the shop at your current location
  - Example: `/shops remove coal`
  - Removes the specified item from the shop's inventory
  - Requirements:
    - You must have the `furious.shops.removeitem` permission
    - You must be standing in a shop's claimed chunk

- `/shops toggle buy <item>` - Toggle whether an item can be bought from the shop at your current location
  - Example: `/shops toggle buy diamond`
  - Toggles whether players can buy the specified item from the shop
  - Requirements:
    - You must have the `furious.shops.togglebuy` permission
    - You must be standing in a shop's claimed chunk

- `/shops toggle sell <item>` - Toggle whether an item can be sold to the shop at your current location
  - Example: `/shops toggle sell diamond`
  - Toggles whether players can sell the specified item to the shop
  - Requirements:
    - You must have the `furious.shops.togglesell` permission
    - You must be standing in a shop's claimed chunk

### Price and Stock Management
- `/shops price <item> <price>` - Set the price of an item in the shop at your current location
  - Example: `/shops price diamond 100`
  - Sets the buying price of the specified item in the shop
  - Requirements:
    - You must have the `furious.shops.setprice` permission
    - You must be standing in a shop's claimed chunk

- `/shops stock <item> <amount>` - Set the stock level of an item in the shop at your current location
  - Example: `/shops stock diamond 50`
  - Sets the stock level of the specified item in the shop
  - Requirements:
    - You must have the `furious.shops.setstock` permission
    - You must be standing in a shop's claimed chunk

- `/shops restock` - Restock all items in the shop at your current location
  - Example: `/shops restock`
  - Restocks all items in the shop to their maximum levels
  - Also resets the dynamic pricing system to base prices
  - Requirements:
    - You must have the `furious.shops.restock` permission
    - You must be standing in a shop's claimed chunk

## Examples

### Setting Up a New Shop

1. Create a new shop:
   ```
   /shops create MiningShop
   ```

2. Claim chunks for the shop:
   ```
   /shops claim MiningShop
   ```
   (Execute this command in each chunk you want to claim for the shop)

3. Set the shop's spawn point:
   ```
   /shops spawn
   ```
   (Execute this command at the location where you want players to teleport to, while standing in a claimed chunk)

4. Add items to the shop:
   ```
   /shops add diamond
   /shops add iron_ingot
   /shops add gold_ingot
   ```
   (Execute these commands while standing in a claimed chunk)

5. Set prices for the items:
   ```
   /shops price diamond 100
   /shops price iron_ingot 10
   /shops price gold_ingot 50
   ```
   (Execute these commands while standing in a claimed chunk)

6. Set stock levels:
   ```
   /shops stock diamond 20
   /shops stock iron_ingot 64
   /shops stock gold_ingot 32
   ```
   (Execute these commands while standing in a claimed chunk)

### Managing an Existing Shop

1. Toggle buying/selling options:
   ```
   /shops toggle buy diamond
   /shops toggle sell coal
   ```
   (Execute these commands while standing in a claimed chunk)

2. Restock a shop after heavy player activity:
   ```
   /shops restock
   ```
   (Execute this command while standing in a claimed chunk)

3. Adjust prices manually (overrides dynamic pricing):
   ```
   /shops price diamond 150
   ```
   (Execute this command while standing in a claimed chunk)

4. Remove an item that shouldn't be sold anymore:
   ```
   /shops remove coal
   ```
   (Execute this command while standing in a claimed chunk)

5. Delete a shop that's no longer needed:
   ```
   /shops delete OldShop
   /shops delete confirm
   ```

## Best Practices

1. **Strategic Shop Placement**: Place shops in strategic locations like towns or resource-rich areas for player convenience.

2. **Balanced Pricing**: Set initial prices that are fair and balanced for the server economy. The dynamic pricing system will adjust based on player activity.

3. **Themed Shops**: Create themed shops for different purposes (e.g., mining shop, farming shop, combat shop) to organize items logically.

4. **Regular Restocking**: Periodically restock popular shops to ensure players can always find what they need.

5. **Economic Balance**: Monitor the dynamic pricing system to ensure it's creating a balanced economy. Adjust base prices if necessary.

6. **Shop Territories**: Claim enough chunks around shops to create proper shopping districts, but avoid claiming too much territory.

7. **Item Curation**: Carefully select which items to add to each shop. Not every shop needs to sell everything.

8. **Buy/Sell Toggles**: Use the toggle commands to control which items can be bought from or sold to shops, creating specialized buying or selling shops.

9. **Price Differentiation**: Set different base prices for the same items in different shops to create regional economies.

10. **Documentation**: Keep track of shop locations, items, and prices for administrative purposes.

11. **Confirmation for Deletion**: Always use the two-step deletion process to avoid accidentally deleting shops. The first command warns about deletion, and the second command confirms it.