# Player Guide: SHOPS

This guide provides information on how to use the shops system as a player.

## Table of Contents
- [Overview](#overview)
- [Basic Usage](#basic-usage)
- [Permission Requirements](#permission-requirements)
- [Finding Shops](#finding-shops)
- [Buying Items](#buying-items)
- [Selling Items](#selling-items)
- [Understanding Dynamic Pricing](#understanding-dynamic-pricing)
- [Examples](#examples)
- [Tips and Best Practices](#tips-and-best-practices)

## Overview

The shops system allows you to buy and sell items at various shops throughout the world. Each shop has its own inventory, prices, and location. Shops implement a dynamic pricing system where prices change based on supply and demand.

## Basic Usage

- `/shops` - Shows help information
- `/shops help` - Shows detailed help information
- `/shops list` - Lists all available shops
- `/shops info` - Shows information about the shop at your current location
- `/shops buy <item> <amount>` - Buy items from a shop
- `/shops sell` - Sell the item in your hand to a shop
- `/shops tp <shop>` - Teleport to a shop

## Permission Requirements

To use the shops system, you need these permissions:

- `furious.shops.use` - Allows using the shops system (default: true)
- `furious.shops.list` - Allows listing all shops (default: true)
- `furious.shops.info` - Allows viewing shop information (default: true)
- `furious.shops.buy` - Allows buying items from shops (default: true)
- `furious.shops.sell` - Allows selling items to shops (default: true)
- `furious.shops.teleport` - Allows teleporting to shops (default: true)

## Finding Shops

There are several ways to find shops in the game:

1. **Shop List**: Use `/shops list` to see all available shops.
2. **Teleportation**: Use `/shops tp <shop>` to teleport directly to a shop.
3. **Shop Info**: When you're in a shop's territory, use `/shops info` to get information about that shop.

## Buying Items

To buy items from a shop:

1. Visit a shop (either by finding it in the world or teleporting to it).
2. Use `/shops info` to see what items are available and their prices.
3. Use `/shops buy <item> <amount>` to purchase items.
   - Example: `/shops buy diamond 5` to buy 5 diamonds.

The shop must have the item in stock, and you must have enough money in your wallet to complete the purchase.

## Selling Items

To sell items to a shop:

1. Visit a shop (either by finding it in the world or teleporting to it).
2. Hold the item you want to sell in your main hand.
3. Use `/shops sell` to sell the item.

The shop must be configured to buy the item you're trying to sell. You'll receive money in your wallet based on the shop's buying price for that item.

## Understanding Dynamic Pricing

Shops use a dynamic pricing system that adjusts prices based on supply and demand:

- **Price Increases**: When players buy a lot of an item, its price gradually increases.
- **Price Decreases**: When players sell a lot of an item to the shop, its price gradually decreases.
- **Price Limits**: Prices will always stay within 50-200% of the original base price.

This system creates a realistic economy where popular items become more expensive, while items that players are selling a lot become cheaper.

## Examples

### Basic Shopping Operations

1. Find available shops:
   ```
   /shops list
   ```

2. Teleport to a shop:
   ```
   /shops tp MiningShop
   ```

3. Check what items are available at the current shop:
   ```
   /shops info
   ```

4. Buy 10 iron ingots:
   ```
   /shops buy iron_ingot 10
   ```

5. Sell the diamond you're holding:
   ```
   /shops sell
   ```

## Tips and Best Practices

1. **Compare Prices**: Different shops may have different prices for the same items. Check multiple shops to find the best deals.

2. **Watch for Price Changes**: Due to the dynamic pricing system, prices change over time. An item that's expensive now might be cheaper later.

3. **Sell at the Right Time**: If many players are selling the same item, its price will drop. Try to sell items when they're in high demand.

4. **Buy in Bulk**: Some items might be worth buying in bulk when prices are low, to resell later when prices increase.

5. **Check Stock Levels**: Shops have limited stock. Popular items might sell out quickly.

6. **Use the Info Command**: Always use `/shops info` when visiting a new shop to see what items they buy and sell.

7. **Teleport Wisely**: The teleport command makes shopping convenient, but there might be a cooldown or cost associated with it.

8. **Hold Items Correctly**: When selling, make sure the item you want to sell is in your main hand.