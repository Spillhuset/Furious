# Admin Guide: BANKS

This guide provides information on how to administer and configure the banks system.

## Table of Contents
- [Overview](#overview)
- [Administrative Permissions](#administrative-permissions)
- [Admin Commands](#admin-commands)
- [Bank Management](#bank-management)
- [Territory Management](#territory-management)
- [Account Management](#account-management)
- [Balance and Interest Management](#balance-and-interest-management)
- [Examples](#examples)
- [Best Practices](#best-practices)

## Overview

The banks system allows players to store and manage their currency in bank accounts. As an administrator, you can create and manage banks, control bank territories, and manage player accounts and interest rates.

## Administrative Permissions

To administer the banks system, you need these permissions:

### Bank Management Permissions
- `furious.bank.claim` - Allows claiming chunks for banks (default: op)
- `furious.bank.unclaim` - Allows unclaiming chunks from banks (default: op)
- `furious.bank.create` - Allows creating banks (default: op)
- `furious.bank.rename` - Allows renaming banks (default: op)
- `furious.bank.delete` - Allows deleting banks (default: op)

### Account Management Permissions
- `furious.bank.createaccount` - Allows creating your own account in banks (default: true)
- `furious.bank.createaccount.others` - Allows creating accounts for other players (default: op)
- `furious.bank.deleteaccount` - Allows deleting your own account from banks (default: true)
- `furious.bank.deleteaccount.others` - Allows deleting other players' accounts from banks (default: op)

### Balance Management Permissions
The general `furious.bank.editbalance` permission has been split into more granular permissions:

- `furious.bank.add` - Allows adding to account balances (default: op)
- `furious.bank.subtract` - Allows subtracting from account balances (default: op)
- `furious.bank.set` - Allows setting account balances to specific values (default: op)
- `furious.bank.add.others` - Allows adding to other players' account balances (default: op)
- `furious.bank.subtract.others` - Allows subtracting from other players' account balances (default: op)
- `furious.bank.set.others` - Allows setting other players' account balances (default: op)

For backward compatibility, the general permissions still work:
- `furious.bank.editbalance` - Grants all balance editing permissions for your own accounts
- `furious.bank.editbalance.others` - Grants all balance editing permissions for other players' accounts

### Interest Management Permissions
- `furious.bank.interest` - Allows editing bank interest rates (default: op)

### Permission Inheritance
The server now uses a structured permission inheritance system:

- `furious.bank.*` - Grants all bank permissions
- `furious.bank.admin.*` - Grants all administrative bank permissions
- Higher-level permissions automatically grant related lower-level permissions

For example, if you grant a player `furious.bank.admin.*`, they automatically have all administrative bank permissions like `furious.bank.add.others`, `furious.bank.set.others`, etc.

## Admin Commands

These commands are available for server administrators:

### Console Execution
Most admin commands can be executed from the console. When executing from the console, you must specify the bank name as the first parameter:

- Example: `/bank balance RubberBank Steve` (Check Steve's balance in RubberBank)
- Example: `/bank set RubberBank Steve 1000` (Set Steve's balance to 1000 in RubberBank)
- Example: `/bank interest RubberBank 0.05` (Set RubberBank's interest rate to 5%)

Commands that require a player's location (like `claim` and `unclaim`) can only be executed by players in-game.

### Bank Management
- `/banks create <n>` - Create a new bank at your current location
  - Example: `/banks create CityBank`
  - Creates a new bank with the specified name
  - Requirements:
    - You must have the `furious.bank.create` permission

- `/banks rename <old> <new>` - Rename a bank
  - Example: `/banks rename OldBank NewBank`
  - Changes the name of an existing bank
  - Requirements:
    - You must have the `furious.bank.rename` permission

- `/banks delete <n>` - Delete a bank
  - Example: `/banks delete FailedBank`
  - Permanently removes the bank and all its accounts
  - Requirements:
    - You must have the `furious.bank.delete` permission

### Territory Management
- `/banks claim` - Claim the current chunk for a bank
  - Example: `/banks claim`
  - Alias: `/banks c`
  - Claims the chunk you're standing in for the bank at that location
  - Requirements:
    - You must have the `furious.bank.claim` permission

- `/banks unclaim` - Unclaim the current chunk from a bank
  - Example: `/banks unclaim`
  - Alias: `/banks u`
  - Removes the bank's claim on the chunk you're standing in
  - Requirements:
    - You must have the `furious.bank.unclaim` permission

### Account Management
- `/banks createaccount` - Create an account for yourself in the current bank
  - Example: `/banks createaccount`
  - Creates a new account for yourself in the bank at your current location
  - Requirements:
    - You must have the `furious.bank.createaccount` permission

- `/banks createaccount <player>` - Create an account for another player
  - Example: `/banks createaccount Steve`
  - Creates a new account for the specified player in the bank at your current location
  - Requirements:
    - You must have the `furious.bank.createaccount.others` permission

- `/banks deleteaccount` - Delete your own account
  - Example: `/banks deleteaccount`
  - Permanently removes your account from the bank at your current location
  - Requirements:
    - You must have the `furious.bank.deleteaccount` permission

- `/banks deleteaccount <player>` - Delete another player's account
  - Example: `/banks deleteaccount Steve`
  - Permanently removes the player's account from the bank at your current location
  - Requirements:
    - You must have the `furious.bank.deleteaccount.others` permission

### Balance and Interest Management
- `/bank set <player> <amount>` - Set a player's balance (in-game, must be in bank chunk)
  - Example: `/bank set Steve 1000`
  - Console usage: `/bank set <bankName> <player> <amount>`
  - Example (console): `/bank set CityBank Steve 1000`
  - Sets the player's balance at the specified bank to the specified amount
  - Requirements:
    - For your own account: You must have the `furious.bank.set` permission
      - Legacy: The `furious.bank.editbalance` permission also works
    - For other players' accounts: You must have the `furious.bank.set.others` permission
      - Legacy: The `furious.bank.editbalance.others` permission also works

- `/bank add <player> <amount>` - Add to a player's balance (in-game, must be in bank chunk)
  - Example: `/bank add Steve 500`
  - Console usage: `/bank add <bankName> <player> <amount>`
  - Example (console): `/bank add CityBank Steve 500`
  - Adds the specified amount to the player's balance
  - Requirements:
    - For your own account: You must have the `furious.bank.add` permission
      - Legacy: The `furious.bank.editbalance` permission also works
    - For other players' accounts: You must have the `furious.bank.add.others` permission
      - Legacy: The `furious.bank.editbalance.others` permission also works

- `/bank sub <player> <amount>` - Subtract from a player's balance (in-game, must be in bank chunk)
  - Example: `/bank sub Steve 200`
  - Console usage: `/bank sub <bankName> <player> <amount>`
  - Example (console): `/bank sub CityBank Steve 200`
  - Subtracts the specified amount from the player's balance
  - Requirements:
    - For your own account: You must have the `furious.bank.subtract` permission
      - Legacy: The `furious.bank.editbalance` permission also works
    - For other players' accounts: You must have the `furious.bank.subtract.others` permission
      - Legacy: The `furious.bank.editbalance.others` permission also works

- `/bank interest <rate>` - Set a bank's interest rate (in-game, must be in bank chunk)
  - Example: `/bank interest 0.05`
  - Console usage: `/bank interest <bankName> <rate>`
  - Example (console): `/bank interest CityBank 0.05`
  - Sets the interest rate for the specified bank (e.g., 0.05 for 5%)
  - Requirements:
    - You must have the `furious.bank.interest` permission

## Examples

### Administrative Bank Management

1. Create a new bank:
   ```
   /banks create MinerBank
   ```
   (Execute this command at the location where you want the bank to be)

2. Rename a bank:
   ```
   /banks rename OldBankName NewBankName
   ```

3. Set the interest rate for a bank:
   ```
   /banks editinterest MinerBank 0.03
   ```
   (Sets a 3% interest rate)

4. Create an account for a player:
   ```
   /banks createaccount MinerBank Alex
   ```

5. Adjust a player's balance:
   ```
   /banks editbalance MinerBank Alex 5000
   ```

6. Delete a bank that's no longer needed:
   ```
   /banks delete ClosedBank
   ```

### Territory Management

1. Claim a chunk for a bank:
   ```
   /banks claim
   ```
   (Execute this command while standing in the chunk you want to claim)

2. Unclaim a chunk from a bank:
   ```
   /banks unclaim
   ```
   (Execute this command while standing in the chunk you want to unclaim)

## Best Practices

1. **Granular Permission Management**: Take advantage of the new granular admin permissions to assign specific administrative capabilities to different staff roles:
   - Junior staff might receive only `furious.bank.add` for helping players with deposits
   - Moderators could get `furious.bank.add.others` and `furious.bank.subtract.others` for managing player transactions
   - Senior staff might receive all balance management permissions
   - Only administrators should receive full access to all bank permissions including interest management

2. **Permission Inheritance Setup**: Configure your permission system to take advantage of the new inheritance structure:
   - Set up permission groups that inherit from `furious.bank.*` or `furious.bank.admin.*`
   - Use the inheritance system to simplify permission management and ensure consistency
   - Remember that granting a wildcard permission like `furious.bank.admin.*` grants all related permissions

3. **Bank-Wallet Integration**: Configure the bank system to work seamlessly with the wallet system:
   - Set up consistent permission-based rules across bank and wallet systems
   - Configure transaction limits based on permission levels
   - Create a unified economic experience across all financial systems

4. **Strategic Bank Placement**: Place banks in strategic locations like towns or market areas for player convenience.
   - Consider player traffic patterns when placing banks
   - Create banks near other economic facilities like shops and markets

5. **Balance Interest Rates**: Set interest rates that are rewarding for players but balanced for the server economy.
   - Consider different interest rates for different player ranks based on permissions
   - Monitor the economic impact of interest payments regularly

6. **Secure Bank Territories**: Claim chunks around banks to create secure banking districts.
   - Use the claim system to protect bank areas from griefing
   - Consider creating staff-only areas for administrative bank operations

7. **Document Bank Locations**: Keep track of bank locations and share this information with players.
   - Create an in-game map or guide to bank locations
   - Document which permissions are required for different bank operations

8. **Regular Interest Payments**: Configure the plugin to pay interest at regular intervals to encourage bank usage.
   - Set up automated interest payments based on server time
   - Consider permission-based interest rate bonuses for VIP players

9. **Bank Signage**: Place informative signs near banks to help players understand available services and interest rates.
   - Include information about required permissions for different operations
   - Update signs when permission requirements change

10. **Economic Balance**: Consider the overall economy when setting up banks and interest rates to prevent inflation.
    - Monitor the economic impact of the granular permission system
    - Adjust permission-based limits and rates as needed to maintain balance

11. **Multiple Banks**: Create different banks with different interest rates to give players options and create economic diversity.
    - Consider permission-based access to exclusive banks
    - Create banks with different features for different player ranks

12. **Bank Themes**: Consider creating themed banks for different areas or purposes (e.g., mining district bank, farming district bank).
    - Customize permission requirements for themed banks
    - Create special permissions for accessing themed bank features

13. **Regular Audits**: Periodically review bank accounts, balances, and permission assignments.
    - Check for permission inconsistencies across staff ranks
    - Verify that the granular permissions are working as intended
    - Monitor usage patterns of different bank commands to identify potential issues

14. **Account Management**: Use the granular permissions to create a tiered account management system.
    - Configure which staff roles can create, delete, and manage accounts
    - Set up permission-based account limits for different player ranks

15. **Transaction Limits**: Configure transaction limits based on permission levels.
    - Set different deposit, withdrawal, and transfer limits for different permission groups
    - Create VIP permissions that allow higher transaction limits