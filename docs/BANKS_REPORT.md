# Banks System Report

Date: 2025-08-24 19:41

This document provides an overview and technical report of the Banks feature implemented in the Furious plugin. It covers command usage, permissions, persistence format, flows, constraints, and maintenance considerations.

## Scope and Goals
The Banks system provides bank locations inside selected guild-claimed chunks where players can create accounts and move funds between their Wallet and a per-bank account balance. Operators manage bank instances, their locations, visibility markers, and operational status (open/closed).

The implementation follows the project’s Commands + Services pattern, uses YAML-based persistence, and optionally uses an ArmorStand marker for teleport anchoring and visibility (op-only).

## Key Concepts
- Bank: Named entity with an ID, a type (PLAYER or GUILD), 0+ claimed chunks, interest rate field (no automatic accrual), optional ArmorStand marker, and an open/closed flag.
- Account: A mapping of player UUID to per-bank balance. A player can hold multiple accounts across different banks.
- Claimed chunk: Banks must be placed inside guild claims of type SAFE or WAR. Banks can claim multiple chunks.

## Commands Overview
All base commands are registered under `/banks`.

Admin/operator subcommands:
- `/banks create <name>` — create a new bank.
- `/banks rename <oldName> <newName>` — rename an existing bank.
- `/banks delete <name>` — delete a bank; also removes player accounts for that bank.
- `/banks claim <bankName>` — claim the current chunk for the bank (must be in SAFE or WAR guild territory). Adds an additional claim; multiple claims are supported.
- `/banks unclaim <bankName>` — unclaim all chunks for the bank; removes any ArmorStand marker.
- `/banks type <bankName> <player|guild>` — set the bank type metadata.
- `/banks setinterest <bankName> <percent>` — set the interest field (>= 0). Scheduled daily accrual applies this percentage as compound interest per day to account balances.
- `/banks open <bankName> <true|false>` — mark the bank as open or closed. Closed banks deny player operations (ops bypass).
- `/banks spawn <bankName>` — spawn/update an ArmorStand marker at the caller’s location (must stand inside one of the bank’s claimed chunks). Used as teleport anchor; existing marker is removed before respawn.
- `/banks teleport <bankName> [player]` — teleport self or a target player to the bank’s anchor (or center of first claimed chunk if no anchor). Op-only.

Player subcommands:
- `/banks list [bankName]` — show info for all banks or a specific bank (type, guild(s), interest, open/closed).
- `/banks createaccount [bankName]` — create an account at the bank. If no name is provided, the bank is inferred from the current chunk. Must be inside a bank-claimed chunk and the bank must be open (ops bypass closed).
- `/banks deleteaccount [bankName]` — delete your account at the bank (requires zero balance). If no name is provided, inferred from current chunk. Must be inside the bank’s chunk and bank must be open (ops bypass closed).
- `/banks deposit <bankName> <amount>` or `/banks deposit <amount>` — move funds from wallet to bank account. If only an amount is provided, the bank is inferred from the current chunk. Must be inside the bank’s chunk and bank must be open (ops bypass closed).
- `/banks withdraw <bankName> <amount>` or `/banks withdraw <amount>` — move funds from bank account to wallet. If only an amount is provided, the bank is inferred from the current chunk. Must be inside the bank’s chunk and bank must be open (ops bypass closed).
- `/banks balances [player]` — list balances per bank for yourself, or for a target player with `.others`.
- `/banks summary [player]` — show number of bank accounts and the total balance for yourself, or for a target player with `.others`. 

## Permissions
Declared in `src/main/resources/plugin.yml` (as of this report):
- `furious.banks` — base permission (default: true)
- `furious.banks.create` — create bank (default: op)
- `furious.banks.rename` — rename bank (default: op)
- `furious.banks.delete` — delete bank (default: op)
- `furious.banks.claim` — claim chunks (default: op)
- `furious.banks.unclaim` — unclaim (default: op)
- `furious.banks.setinterest` — set interest (default: op)
- `furious.banks.type` — set bank type (default: op)
- `furious.banks.createaccount` — create account (default: true)
- `furious.banks.deposit` — deposit (default: true)
- `furious.banks.withdraw` — withdraw (default: true)
- `furious.banks.deleteaccount` — delete account (default: true)
- `furious.banks.list` — list banks (default: true)

Notes:
- Explicit permissions are declared: `furious.banks.open`, `furious.banks.spawn`, `furious.banks.teleport`, and `furious.banks.teleport.others`. The `/banks teleport` command remains op-only in code regardless of permissions.

## User Flow Details
1. Admin setup
   - Stand inside a guild claim of type SAFE or WAR.
   - Run `/banks create <name>` to create the bank.
   - Run `/banks claim <name>` at each desired chunk to associate one or more chunks with this bank.
   - Optionally run `/banks spawn <name>` while standing inside one of the bank’s claimed chunks to place a marker ArmorStand and set the teleport anchor.
   - Optionally set open state `/banks open <name> true` and metadata like type `/banks type <name> player|guild` and interest `/banks setinterest <name> <percent>`.

2. Player creates an account
   - Stand inside the bank’s claimed chunk.
   - Use `/banks createaccount` (or `/banks createaccount <name>`).
   - The bank must be open; ops bypass this check.

3. Deposit/Withdraw
   - Stand inside the bank’s claimed chunk.
   - Use `/banks deposit <amount>` (or `/banks deposit <name> <amount>`) to move funds from wallet to account. Wallet is debited via WalletService.
   - Use `/banks withdraw <amount>` (or `/banks withdraw <name> <amount>`) to move funds from account to wallet. Account balance is checked and updated.

4. Delete account
   - Stand inside the bank’s claimed chunk.
   - Use `/banks deleteaccount` (or `/banks deleteaccount <name>`). Balance must be zero.

5. Teleport
   - Ops can run `/banks teleport <name> [player]` to send themselves or others to the bank’s anchor (ArmorStand) if present; otherwise to the center of the first claimed chunk.

## Area and State Restrictions
- Area: `createaccount`, `deleteaccount`, `deposit`, and `withdraw` require the player to be located inside one of the bank’s claimed chunks (`getBankAt(player.getLocation())`).
- State: If a bank is closed, these actions are denied for regular players; ops bypass.
- Claiming: `/banks claim` validates that the chunk is inside a claimed guild territory of type SAFE or WAR (`canClaimHere`).

## ArmorStand Marker and Visibility
- `/banks spawn` uses `ArmorStandManager.create()` to place a marker named "Bank: <name>" and stores the ArmorStand UUID in the bank.
- Visibility is restricted to ops: for each viewer, ops see the marker, non-ops have it hidden (`showEntity/hideEntity`).
- `ensureArmorStands()` is called at startup to recreate missing markers at the center of the first claimed chunk and to update name/visibility for existing ones.
- `/banks unclaim` removes any existing ArmorStand and clears claims.

## Persistence
Data is stored in `plugins/Furious/banks.yml`.

Top-level keys:
- `banks` — map of bankId -> bank fields:
  - `name` — bank name
  - `interest` — double (>= 0)
  - `type` — string: `PLAYER` or `GUILD`
  - `open` — boolean
  - Legacy single-claim fields:
    - `world` — world UUID
    - `chunkX` — int
    - `chunkZ` — int
  - Multi-claim list (preferred):
    - `claims.<index>.world` — world UUID
    - `claims.<index>.chunkX` — int
    - `claims.<index>.chunkZ` — int
  - `armorStand` — optional ArmorStand UUID for teleport anchor
- `accounts` — playerId -> bankId -> balance (double)

Notes:
- On save, both the legacy first-claim fields and the `claims` list are written when claims exist, ensuring backward compatibility.

## Edge Cases and Errors
- Attempting to create a bank with an existing name is rejected.
- Claiming a chunk already claimed by the same bank is rejected.
- Creating/deleting accounts, deposits, and withdrawals outside bank-claimed chunks are rejected.
- Deleting an account with a non-zero balance is rejected.
- Deposits validate wallet balance; withdrawals validate account balance.
- Teleport fails gracefully if the world is not loaded or no claims/anchor exist, with user feedback.

## Notes, Limitations, and Trade-offs
- Interest accrues daily via a scheduler. The `interest` field is interpreted as percent per day and compounds when full day(s) have elapsed since the last accrual per bank. Accrual runs hourly to catch up missed days.
- The ArmorStand marker is visible only to ops by design.
- `/banks teleport` is op-only in code; even with permissions set, non-ops are blocked.

## Troubleshooting
- “This chunk is not claimed by a guild.” — You must stand inside a guild claim.
- “Bank claims must be inside SAFE or WAR guild.” — Only those guild types can host banks.
- “This bank is not claimed yet.” — Admins must claim a chunk for the bank first.
- “You must be at the bank's claimed chunk to do this.” — Move inside a chunk claimed by the bank.
- “This bank is currently closed.” — Ask an operator to open it with `/banks open <name> true`.
- “Insufficient wallet/bank balance.” — Ensure you have enough funds in the respective source.
- Teleport anchor missing — Re-run `/banks spawn <name>` and use `/banks teleport <name>` (op-only).

## Implementation Summary
- Service: `com.spillhuset.furious.services.BanksService`
- Models: `com.spillhuset.furious.utils.Bank`, `com.spillhuset.furious.utils.BankAccount`, `com.spillhuset.furious.utils.BankType`
- Command router: `com.spillhuset.furious.commands.BanksCommand`
- Subcommands: `com.spillhuset.furious.commands.BanksCommands.*` (Create, Rename, Delete, Claim, Unclaim, SetInterest, Type, List, CreateAccount, Deposit, Withdraw, DeleteAccount, Spawn, Teleport, Open, Balances, Summary, Audit)
- Registration: in `Furious.onEnable()` (creates and loads BanksService, registers banks command, starts interest scheduler)
- Permissions: declared in `plugin.yml` (including `furious.banks.open`, `furious.banks.spawn`, `furious.banks.teleport(.others)`, `furious.banks.balances(.others)`, `furious.banks.summary(.others)`, and `furious.banks.audit`)

## Future Enhancements (Optional)
- Configurable interest schedule (cron-like or specific time of day) and per-bank overrides.
- Optional non-op visibility of bank markers or per-player toggles.
