# Wallet Permissions Analysis

## Current Permissions

| Command | Current Permission | Issue |
|---------|-------------------|-------|
| BalanceSubCommand | furious.wallet | None (follows general wallet permission pattern) |
| BalanceSubCommand (for others) | furious.wallet.balance.others | None (follows granular permission pattern) |
| PaySubCommand | furious.wallet.pay | None (follows specific command permission pattern) |
| AddSubCommand | furious.wallet.add | None (follows granular permission pattern) |
| SubSubCommand | furious.wallet.sub | None (follows granular permission pattern) |
| SetSubCommand | furious.wallet.set | None (follows granular permission pattern) |
| HelpSubCommand | furious.wallet | None (follows general wallet permission pattern) |

## Permission Structure Analysis

The wallet permission system now uses a granular structure similar to the bank permission system:

1. **Basic Permission**: `furious.wallet` - Used for basic commands like checking your own balance and help
2. **Player-specific Permission**: `furious.wallet.pay` - Used for player-to-player transactions
3. **Granular Admin Permissions**:
   - `furious.wallet.balance.others` - For checking other players' balances
   - `furious.wallet.add` - For adding to players' wallets
   - `furious.wallet.sub` - For subtracting from players' wallets
   - `furious.wallet.set` - For setting players' wallet balances

This granular approach aligns with the bank permission system, providing more specific control over administrative actions.

## Permission Categories

The wallet permission system can be categorized as follows:

1. **Basic Player Permissions**:
   - `furious.wallet` - Allows checking own balance and accessing help

2. **Player-to-Player Permissions**:
   - `furious.wallet.pay` - Allows sending scraps to other players

3. **Administrative Permissions**:
   - `furious.wallet.balance.others` - Allows checking other players' balances
   - `furious.wallet.add` - Allows adding scraps to players' wallets
   - `furious.wallet.sub` - Allows removing scraps from players' wallets
   - `furious.wallet.set` - Allows setting players' wallet balances to specific values

## Implemented Permission Changes

The following changes have been implemented to align with the bank permission system's more granular approach:

| Command | Old Permission | New Permission | Reason |
|---------|-------------------|------------------------|--------|
| BalanceSubCommand (for others) | furious.wallet.admin | furious.wallet.balance.others | More granular permission for checking other players' balances |
| AddSubCommand | furious.wallet.admin | furious.wallet.add | More specific permission for adding to wallets |
| SubSubCommand | furious.wallet.admin | furious.wallet.sub | More specific permission for subtracting from wallets |
| SetSubCommand | furious.wallet.admin | furious.wallet.set | More specific permission for setting wallet balances |

## Commands with Correct Permissions

| Command | Current Permission | Notes |
|---------|-------------------|-------|
| BalanceSubCommand | furious.wallet | For checking own balance, available to all players |
| BalanceSubCommand | furious.wallet.balance.others | For checking other players' balances, restricted to staff |
| PaySubCommand | furious.wallet.pay | For player-to-player transactions, available to all players |
| AddSubCommand | furious.wallet.add | For adding to wallets, restricted to staff |
| SubSubCommand | furious.wallet.sub | For subtracting from wallets, restricted to staff |
| SetSubCommand | furious.wallet.set | For setting wallet balances, restricted to staff |
| HelpSubCommand | furious.wallet | For accessing help information, available to all players |

## Implementation Status

The following implementation steps have been completed:

1. Removed all references to the legacy `furious.wallet.admin` permission ✓
2. Updated all commands to use their specific granular permissions ✓
3. Updated documentation to reflect the new permission structure ✓

## Implemented Future Considerations

The following improvements have been implemented:

1. **Permission Inheritance**: ✓
   - Implemented a structured permission inheritance system:
     - `furious.wallet.*` grants all wallet permissions
     - `furious.wallet.admin.*` grants all administrative wallet permissions
     - Higher-level permissions automatically grant related lower-level permissions
     - For example, `furious.wallet.admin.*` grants `furious.wallet.balance.others`, `furious.wallet.add`, etc.

2. **Default Permission Configuration**: ✓
   - Adjusted default permission assignments:
     - Basic permissions (`furious.wallet`, `furious.wallet.pay`) granted to all players
     - Administrative permissions (`furious.wallet.add`, `furious.wallet.sub`, etc.) granted only to server operators
     - Added configuration options for transaction limits based on permission levels

3. **Permission Documentation**: ✓
   - Created comprehensive permission documentation in the ADMIN_WALLET_GUIDE.md file
   - Added detailed explanations of each permission and its intended use
   - Included examples of permission configurations for different server roles

4. **Integration with Other Systems**: ✓
   - Enhanced integration between wallet permissions and other economic systems:
     - Added permission-based integration with the bank system for automatic transfers
     - Implemented shop system integration with wallet permissions for transaction verification
     - Created a unified economic permission structure across wallet, bank, and shop systems
     - Added configuration options to customize how permissions interact across systems

## Permission Configuration

Server administrators should ensure:

1. All permission plugins are updated with the new granular permissions
2. The wildcard permission `furious.wallet.*` includes all the new granular permissions
3. Players and staff are assigned appropriate permissions based on their roles