# Player Guide: WALLET

This guide provides information on how to use the `/wallet` command and its features as a player.

## Table of Contents
- [Overview](#overview)
- [Basic Usage](#basic-usage)
- [Permission Requirements](#permission-requirements)
- [Basic Wallet Operations](#basic-wallet-operations)
- [Player Transactions](#player-transactions)
- [Examples](#examples)
- [Tips and Best Practices](#tips-and-best-practices)

## Overview

The wallet system provides a personal currency storage for each player. You can check your balance and pay other players using a currency called "scraps" which can be used for various transactions on the server.

## Basic Usage

- `/wallet` - Check your wallet balance
- `/wallet pay <player> <amount>` - Pay scraps to another player

## Permission Requirements

To use the wallet system, you need these permissions:

- `furious.wallet` - Allows checking wallet balance (default: true)
- `furious.wallet.pay` - Allows paying scraps to other players (default: true)

The server uses a permission inheritance system where higher-level permissions automatically grant lower-level permissions. For example, if you have `furious.wallet.*`, you automatically have all wallet-related permissions.

## Basic Wallet Operations

These commands allow you to manage your personal wallet.

### Checking Balance
- `/wallet` - Check your wallet balance
  - Example: `/wallet`
  - Shows your current wallet balance in scraps

## Player Transactions

These commands allow you to transfer scraps between wallets.

### Paying Other Players
- `/wallet pay <player> <amount>` - Pay scraps to another player
  - Example: `/wallet pay Steve 100`
  - Transfers the specified amount from your wallet to the target player's wallet
  - Both you and the recipient will receive a confirmation message
  - Requirements:
    - You must have sufficient funds in your wallet
    - The target player must be online
    - You cannot pay yourself

## Examples

### Basic Wallet Usage

1. Check your wallet balance:
   ```
   /wallet
   ```

2. Pay another player:
   ```
   /wallet pay Alex 250
   ```
   This transfers 250 scraps from your wallet to Alex's wallet.

## Tips and Best Practices

1. **Check Your Balance**: Always check your balance before making payments to ensure you have sufficient funds.

2. **Verify Recipients**: Double-check player names when making payments to avoid sending scraps to the wrong person.

3. **Record Transactions**: For significant transactions, consider keeping a record of the payment details.

4. **Tab Completion**: Use tab completion to quickly select player names when making payments.

5. **Payment Confirmations**: Both the sender and recipient receive confirmation messages for all transactions, helping to prevent disputes.

6. **Transaction Limits**: Your permission level determines your transaction limits. Higher permission levels allow for larger transactions. Contact server staff if you need your limits increased.

7. **Wallet Integration**: The wallet system now fully integrates with other economic systems:
   - **Bank Integration**: Seamlessly transfer funds between your wallet and bank accounts
   - **Shop Integration**: Use your wallet for shop transactions with permission-based verification
   - **Warp Costs**: Pay for warps directly from your wallet based on your permissions
   - **Home Purchases**: Buy additional home slots using your wallet

8. **Economic Permissions**: Your economic activities across wallet, bank, and shop systems are governed by a unified permission structure, ensuring consistent behavior.

9. **Currency Format**: The wallet system automatically formats currency amounts for readability (e.g., 1,000 instead of 1000).