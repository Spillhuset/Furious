# Bank Permissions Analysis

## Current Permissions

| Command | Current Permission | Issue |
|---------|-------------------|-------|
| SpawnBankSubCommand | furious.bank.spawn | None (follows lowercase pattern) |
| BalanceSubCommand | furious.bank.balance | None (follows lowercase pattern) |
| CreateAccountSubCommand | furious.bank.createaccount | None (follows lowercase pattern) |
| CreateAccountSubCommand (for others) | furious.bank.createaccount.others | None (follows granular permission pattern) |
| DeleteAccountSubCommand | furious.bank.deleteaccount | None (follows lowercase pattern) |
| DeleteAccountSubCommand (for others) | furious.bank.deleteaccount.others | None (follows granular permission pattern) |
| AddBalanceSubCommand | furious.bank.editbalance | Shared permission for all balance editing |
| AddBalanceSubCommand (for others) | furious.bank.editbalance.others | None (follows granular permission pattern) |
| SubBalanceSubCommand | furious.bank.editbalance | Shared permission for all balance editing |
| SubBalanceSubCommand (for others) | furious.bank.editbalance.others | None (follows granular permission pattern) |
| SetBalanceSubCommand | furious.bank.editbalance | Shared permission for all balance editing |
| SetBalanceSubCommand (for others) | furious.bank.editbalance.others | None (follows granular permission pattern) |
| InterestSubCommand | furious.bank.interest | None (follows lowercase pattern) |

## Permission Structure Analysis

The bank permission system uses a consistent pattern for most commands:

1. **Command-specific Permissions**: Most commands use the pattern `furious.bank.[command]` (e.g., `furious.bank.balance`, `furious.bank.deposit`)
2. **Granular Permissions**: Operations on other players' accounts use the pattern `furious.bank.[command].others` (e.g., `furious.bank.createaccount.others`)
3. **Shared Permissions**: Some related commands share permissions (e.g., all balance editing commands use `furious.bank.editbalance`)
4. **Player Permissions**: Basic permissions like `furious.bank.balance` and `furious.bank.deposit` are granted to all players by default
5. **Admin Permissions**: Administrative permissions like `furious.bank.createbank` and `furious.bank.deletebank` are restricted to operators by default

This structure provides a balance between granularity and simplicity, allowing server administrators to assign permissions based on player roles.

## Permission Categories

The bank permission system can be categorized as follows:

1. **Basic Player Permissions**:
   - `furious.bank.balance` - Allows checking own account balance
   - `furious.bank.deposit` - Allows depositing scraps into own account
   - `furious.bank.withdraw` - Allows withdrawing scraps from own account
   - `furious.bank.transfer` - Allows transferring scraps between own accounts
   - `furious.bank.createaccount` - Allows creating own accounts
   - `furious.bank.deleteaccount` - Allows deleting own accounts

2. **Administrative Permissions**:
   - `furious.bank.createbank` - Allows creating banks
   - `furious.bank.deletebank` - Allows deleting banks
   - `furious.bank.rename` - Allows renaming banks
   - `furious.bank.spawn` - Allows spawning bank NPCs
   - `furious.bank.interest` - Allows managing interest rates
   - `furious.bank.claim` - Allows claiming banks
   - `furious.bank.unclaim` - Allows unclaiming banks

3. **Granular Admin Permissions**:
   - `furious.bank.createaccount.others` - Allows creating accounts for other players
   - `furious.bank.deleteaccount.others` - Allows deleting accounts for other players
   - `furious.bank.editbalance.others` - Allows editing other players' account balances

## Implemented Permission Changes

| Command | Old Permission | New Permission | Reason |
|---------|-------------------|------------------------|--------|
| CreateAccountSubCommand | furious.bank.createAccount | furious.bank.createaccount | Standardized to lowercase format |
| InterestSubCommand | furious.bank.editinterest | furious.bank.interest | Matched permission name to command name |
| CreateAccountSubCommand (for others) | N/A | furious.bank.createaccount.others | Added granular permission for creating other players' accounts |
| DeleteAccountSubCommand (for others) | N/A | furious.bank.deleteaccount.others | Added granular permission for deleting other players' accounts |
| AddBalanceSubCommand (for others) | N/A | furious.bank.editbalance.others | Added granular permission for editing other players' balances |
| SubBalanceSubCommand (for others) | N/A | furious.bank.editbalance.others | Added granular permission for editing other players' balances |
| SetBalanceSubCommand (for others) | N/A | furious.bank.editbalance.others | Added granular permission for editing other players' balances |

## Commands with Correct Permissions

| Command | Current Permission | Notes |
|---------|-------------------|-------|
| SpawnBankSubCommand | furious.bank.spawn | Follows lowercase pattern |
| BalanceSubCommand | furious.bank.balance | Follows lowercase pattern |
| AddBalanceSubCommand | furious.bank.editbalance | For self-editing, shared permission for all balance editing |
| AddBalanceSubCommand | furious.bank.editbalance.others | For editing other players' balances |
| SubBalanceSubCommand | furious.bank.editbalance | For self-editing, shared permission for all balance editing |
| SubBalanceSubCommand | furious.bank.editbalance.others | For editing other players' balances |
| SetBalanceSubCommand | furious.bank.editbalance | For self-editing, shared permission for all balance editing |
| SetBalanceSubCommand | furious.bank.editbalance.others | For editing other players' balances |
| ClaimSubCommand | furious.bank.claim | Follows lowercase pattern |
| CreateAccountSubCommand | furious.bank.createaccount | For creating own account, follows lowercase pattern |
| CreateAccountSubCommand | furious.bank.createaccount.others | For creating other players' accounts |
| CreateBankSubCommand | furious.bank.create | Follows lowercase pattern |
| DeleteAccountSubCommand | furious.bank.deleteaccount | For deleting own account, follows lowercase pattern |
| DeleteAccountSubCommand | furious.bank.deleteaccount.others | For deleting other players' accounts |
| DeleteBankSubCommand | furious.bank.delete | Follows lowercase pattern |
| DepositSubCommand | furious.bank.deposit | Follows lowercase pattern |
| HelpSubCommand | null | No permission required (accessible to all players) |
| InfoSubCommand | furious.bank.info | Follows lowercase pattern |
| InterestSubCommand | furious.bank.interest | Follows lowercase pattern |
| ListSubCommand | furious.bank.list | Follows lowercase pattern |
| RenameBankSubCommand | furious.bank.rename | Follows lowercase pattern |
| TeleportBankSubCommand | furious.bank.teleport | Follows lowercase pattern, also uses furious.bank.teleport.bypass for queue skipping |
| TransferSubCommand | furious.bank.transfer | Follows lowercase pattern |
| UnclaimSubCommand | furious.bank.unclaim | Follows lowercase pattern |
| WithdrawSubCommand | furious.bank.withdraw | Follows lowercase pattern |

## Implementation Status

The following implementation steps have been completed:

1. Standardized all permission names to lowercase format ✓
2. Updated InterestSubCommand to use the specific permission `furious.bank.interest` instead of `furious.bank.editinterest` ✓
3. Added granular permissions for operations on other players' accounts ✓
4. Updated all permission checks in code to use the correct permissions ✓
5. Ensured all permissions are properly documented in the plugin.yml file ✓

## Implemented Future Considerations

The following improvements have been implemented:

1. **More Granular Permissions**: ✓
   - Split the shared `furious.bank.editbalance` permission into separate permissions:
     - `furious.bank.add` - For adding to account balances
     - `furious.bank.subtract` - For subtracting from account balances
     - `furious.bank.set` - For setting account balances to specific values
   - Maintained backward compatibility with the general `furious.bank.editbalance` permission
   - Updated all commands to check for the specific permission first, then fall back to the general permission

2. **Permission Inheritance**: ✓
   - Implemented a structured permission inheritance system:
     - `furious.bank.*` grants all bank permissions
     - `furious.bank.admin.*` grants all administrative bank permissions
     - Higher-level permissions automatically grant related lower-level permissions
     - For example, `furious.bank.admin.*` grants all `.others` permissions

3. **Default Permission Configuration**: ✓
   - Adjusted default permission assignments:
     - Basic permissions (`furious.bank.balance`, `furious.bank.deposit`, etc.) granted to all players
     - Account management permissions granted to trusted players
     - Administrative permissions granted only to server operators
     - Added configuration options for account limits based on permission levels

4. **Permission Documentation**: ✓
   - Created comprehensive permission documentation in the ADMIN_BANK_GUIDE.md file
   - Added detailed explanations of each permission and its intended use
   - Included examples of permission configurations for different server roles
   - Documented the relationship between bank permissions and wallet permissions

## Permission Configuration

Server administrators should ensure:

1. All permission plugins are updated with the correct permissions
2. The wildcard permission `furious.bank.*` includes all the specific bank permissions
3. Players and staff are assigned appropriate permissions based on their roles
4. Granular permissions for operations on other players' accounts are carefully assigned to trusted staff members