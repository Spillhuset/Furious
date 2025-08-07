# Admin Guide: WALLET

This guide provides information on how to administer and configure the wallet system.

## Table of Contents
- [Overview](#overview)
- [Administrative Permissions](#administrative-permissions)
- [Admin Commands](#admin-commands)
- [Adding Scraps](#adding-scraps)
- [Subtracting Scraps](#subtracting-scraps)
- [Setting Balance](#setting-balance)
- [Examples](#examples)
- [Best Practices](#best-practices)

## Overview

The wallet system provides a personal currency storage for each player using a currency called "scraps". As an administrator, you can manage player wallets by adding, subtracting, or setting wallet balances.

## Administrative Permissions

To administer the wallet system, you need one or more of these permissions:

- `furious.wallet.balance.others` - Allows checking other players' wallet balances (default: op)
- `furious.wallet.add` - Allows adding scraps to players' wallets (default: op)
- `furious.wallet.sub` - Allows subtracting scraps from players' wallets (default: op)
- `furious.wallet.set` - Allows setting players' wallet balances (default: op)

### Permission Inheritance
The server now uses a structured permission inheritance system:
- `furious.wallet.*` - Grants all wallet permissions
- `furious.wallet.admin.*` - Grants all administrative wallet permissions
- Higher-level permissions automatically grant related lower-level permissions

For example, if you grant a player `furious.wallet.admin.*`, they automatically have all administrative wallet permissions like `furious.wallet.balance.others`, `furious.wallet.add`, etc.

### Permission Configuration
You can configure transaction limits based on permission levels in the config.yml file:
- Set maximum transaction amounts for different permission levels
- Configure which permissions can bypass transaction limits
- Define custom permission-based economic rules

## Admin Commands

These commands are available for server administrators:

### Checking Balance
- `/wallet balance <player>` - Check a player's wallet balance
  - Example: `/wallet balance Steve`
  - Shows the current balance of the target player's wallet
  - Requirements:
    - You must have the `furious.wallet.balance.others` permission
    - The target player must exist in the system

### Adding Scraps
- `/wallet add <player> <amount>` - Add scraps to a player's wallet
  - Example: `/wallet add Steve 500`
  - Adds the specified amount to the target player's wallet
  - Both you and the target player will receive a confirmation message
  - Requirements:
    - You must have the `furious.wallet.add` permission
    - The target player must be online
    - The amount must be greater than 0

### Subtracting Scraps
- `/wallet sub <player> <amount>` - Subtract scraps from a player's wallet
  - Example: `/wallet sub Steve 200`
  - Removes the specified amount from the target player's wallet
  - Both you and the target player will receive a confirmation message
  - Requirements:
    - You must have the `furious.wallet.sub` permission
    - The target player must be online
    - The target player must have sufficient funds
    - The amount must be greater than 0

### Setting Balance
- `/wallet set <player> <amount>` - Set a player's wallet balance
  - Example: `/wallet set Steve 1000`
  - Sets the target player's wallet balance to the specified amount
  - Both you and the target player will receive a confirmation message
  - Requirements:
    - You must have the `furious.wallet.set` permission
    - The target player must be online
    - The amount cannot be negative

## Examples

### Administrative Operations

1. Add scraps to a player's wallet:
   ```
   /wallet add Steve 1000
   ```
   This adds 1000 scraps to Steve's wallet.

2. Subtract scraps from a player's wallet:
   ```
   /wallet sub Steve 500
   ```
   This removes 500 scraps from Steve's wallet.

3. Set a player's wallet balance:
   ```
   /wallet set Steve 2000
   ```
   This sets Steve's wallet balance to exactly 2000 scraps.

## Best Practices

1. **Administrative Responsibility**: Use wallet management commands responsibly and maintain transparency when modifying player balances.

2. **Audit Logs**: All administrative wallet operations are logged in the server's audit logs for security and accountability. Review these logs periodically.

3. **Economy Balance**: Consider the overall economy when adding or removing large amounts of currency from the system. Adding too much currency can lead to inflation.

4. **Consistent Policies**: Establish clear policies for when and why administrators should modify player wallets to ensure fairness.

5. **Documentation**: Keep records of significant administrative wallet operations, especially for events or server-wide economic adjustments.

6. **Player Communication**: Inform players when making changes to their wallets, especially for server-wide economic adjustments.

7. **Regular Audits**: Periodically review the distribution of wealth on your server to ensure a healthy economy.

8. **Starting Balance**: Consider setting a standard starting balance for new players to help them get started.

9. **Economic Events**: Use wallet commands to create server-wide economic events, such as bonuses during special events.

10. **Permission Inheritance Setup**: Configure your permission system to take advantage of the new inheritance structure. This simplifies permission management and ensures consistency.

11. **Transaction Limits Configuration**: Set appropriate transaction limits for different permission levels to prevent abuse while allowing flexibility for trusted players.

12. **Enhanced Economic Integration**: The wallet system now fully integrates with other economic systems:
    - Configure bank-wallet integration for seamless transfers
    - Set up shop system integration with wallet permissions for transaction verification
    - Implement warp cost integration with wallet permissions
    - Configure home slot purchases using the wallet system

13. **Unified Economic Permissions**: Maintain consistency across wallet, bank, and shop permissions to create a cohesive economic experience.

14. **Permission-Based Features**: Use the new granular permissions to create tiered access to economic features based on player rank or status.

15. **Economic Monitoring Tools**: Utilize the enhanced integration between systems to monitor economic activity across all aspects of your server.