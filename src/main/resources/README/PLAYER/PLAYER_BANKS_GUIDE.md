# Player Guide: BANKS

This guide provides information on how to use the `/banks` command and its features as a player.

## Table of Contents
- [Overview](#overview)
- [Basic Usage](#basic-usage)
- [Permission Requirements](#permission-requirements)
- [Account Management](#account-management)
- [Examples](#examples)
- [Tips and Best Practices](#tips-and-best-practices)

## Overview

The banks system allows you to store and manage your currency in bank accounts. Banks provide a secure way to store money, earn interest, and perform financial transactions. You can have accounts at different banks, each with their own interest rates and locations.

## Basic Usage

- `/banks` - Shows help information
- `/banks help` - Shows detailed help information
- `/banks balance` - Check your bank balance
- `/banks deposit <amount>` - Deposit money into your bank
- `/banks withdraw <amount>` - Withdraw money from your bank
- `/banks transfer <amount> <from> <to>` - Transfer money between accounts
- `/banks info` - View bank information at current location

## Permission Requirements

To use the banks system, you need these permissions:

- `furious.bank.balance` - Allows checking bank balance (default: true)
- `furious.bank.deposit` - Allows depositing to bank (default: true)
- `furious.bank.withdraw` - Allows withdrawing from bank (default: true)
- `furious.bank.transfer` - Allows transferring funds between bank accounts (default: true)
- `furious.bank.info` - Allows viewing bank information at current location (default: true)
- `furious.bank.createaccount` - Allows creating your own bank accounts (default: true)
- `furious.bank.deleteaccount` - Allows deleting your own bank accounts (default: true)

### Permission Inheritance
The server now uses a structured permission inheritance system:
- `furious.bank.*` grants all bank permissions
- `furious.bank.admin.*` grants all administrative bank permissions
- Higher-level permissions automatically grant related lower-level permissions

For example, if you have `furious.bank.*`, you automatically have all basic bank permissions.

## Account Management

These commands allow you to manage your bank accounts.

### Checking Balance
- `/banks balance` - Check your bank balance
  - Example: `/banks balance`
  - Alias: `/banks b`
  - Shows your balance at the current bank or all your bank accounts

### Depositing and Withdrawing
- `/banks deposit <amount>` - Deposit money into your bank
  - Example: `/banks deposit 100`
  - Alias: `/banks d 100`
  - Transfers money from your wallet to your bank account

- `/banks withdraw <amount>` - Withdraw money from your bank
  - Example: `/banks withdraw 50`
  - Alias: `/banks w 50`
  - Transfers money from your bank account to your wallet

### Transferring Funds
- `/banks transfer <amount> <from> <to>` - Transfer money between accounts
  - Example: `/banks transfer 100 CityBank FarmerBank`
  - Alias: `/banks t 100 CityBank FarmerBank`
  - Transfers money between your accounts at different banks

### Viewing Information
- `/banks info` - View bank information at current location
  - Example: `/banks info`
  - Alias: `/banks i`
  - Shows information about the bank at your current location

## Examples

### Basic Banking Operations

1. Check your balance at all banks:
   ```
   /banks balance
   ```

2. Deposit money into your account at the current bank:
   ```
   /banks deposit 500
   ```

3. Withdraw money from your account at the current bank:
   ```
   /banks withdraw 200
   ```

4. Transfer money between your accounts:
   ```
   /banks transfer 300 CityBank FarmerBank
   ```

5. Check information about the bank at your location:
   ```
   /banks info
   ```

## Tips and Best Practices

1. **Use Multiple Banks**: Different banks may have different interest rates or be located in different areas. Consider having accounts at multiple banks.

2. **Regular Deposits**: Make regular deposits to take advantage of interest rates and keep your money secure.

3. **Check Bank Information**: Use the info command to check details about a bank before opening an account.

4. **Interest Benefits**: Banks pay interest on your deposits at regular intervals, so keeping money in banks is more profitable than just in your wallet.

5. **Location Matters**: You typically need to be physically at a bank to deposit or withdraw from it, so remember where your banks are located.

6. **Transfer Between Banks**: If you have accounts at multiple banks, you can transfer funds between them without visiting both locations.

7. **Use Command Aliases**: For frequently used commands, remember the short aliases (b, d, w, t, i) for faster banking.

8. **Balance Distribution**: Consider spreading your wealth across multiple banks for security and to take advantage of different interest rates.

9. **Permission Awareness**: Your ability to perform bank operations depends on your permissions. The new permission system provides more granular control over what you can do.

10. **Account Management**: You can now create and delete your own bank accounts using the appropriate commands, provided you have the necessary permissions.

11. **Bank-Wallet Integration**: The bank system now integrates more seamlessly with the wallet system, allowing for smoother transactions between your wallet and bank accounts.

12. **Transaction Limits**: Your permission level may determine transaction limits for deposits, withdrawals, and transfers. Higher permission levels may allow for larger transactions.

13. **Economic System Integration**: The bank system is now part of a unified economic permission structure that includes wallet and shop systems, ensuring consistent behavior across all economic activities.