# Permission System Testing Guide

## Table of Contents
- [Introduction](#introduction)
- [Manual Testing Procedures](#manual-testing-procedures)
  - [Basic Permission Testing](#basic-permission-testing)
  - [Inheritance Testing](#inheritance-testing)
  - [Cross-Feature Integration Testing](#cross-feature-integration-testing)
  - [Edge Case Testing](#edge-case-testing)
- [Automated Testing Approaches](#automated-testing-approaches)
  - [Unit Testing](#unit-testing)
  - [Integration Testing](#integration-testing)
  - [Load Testing](#load-testing)
- [Test Scenarios](#test-scenarios)
  - [Basic Permission Scenarios](#basic-permission-scenarios)
  - [Inheritance Scenarios](#inheritance-scenarios)
  - [Edge Case Scenarios](#edge-case-scenarios)
  - [Integration Scenarios](#integration-scenarios)
- [Testing Checklist](#testing-checklist)

## Introduction

This guide provides comprehensive testing procedures for the furious plugin's permission system. It includes manual testing procedures for administrators, automated testing approaches for developers, and specific test scenarios to ensure the permission system works correctly in all situations.

Testing the permission system is crucial to ensure that:
1. Basic permissions work as expected
2. Permission inheritance works correctly
3. Legacy permissions are properly supported for backward compatibility
4. Cross-feature integrations function properly
5. Edge cases are handled gracefully

## Manual Testing Procedures

### Basic Permission Testing

1. **Individual Permission Testing**
   - Create a test user with no permissions
   - Grant a specific permission (e.g., `furious.wallet.pay`)
   - Verify the user can perform the associated action
   - Revoke the permission and verify the user can no longer perform the action
   - Repeat for each basic permission

2. **Default Permission Testing**
   - Create a new user with no explicit permissions
   - Verify the user has access to all permissions marked as "default: true"
   - Verify the user does not have access to permissions marked as "default: op"

3. **Operator Permission Testing**
   - Grant a user operator status
   - Verify the user has access to all permissions, including those marked as "default: op"
   - Revoke operator status and verify permissions are properly restricted

### Inheritance Testing

1. **Wildcard Permission Testing**
   - Grant a user a wildcard permission (e.g., `furious.bank.*`)
   - Verify the user has access to all permissions under that category (e.g., `furious.bank.balance`, `furious.bank.deposit`, etc.)
   - Revoke the wildcard permission and verify access is properly restricted

2. **Admin Wildcard Testing**
   - Grant a user an admin wildcard permission (e.g., `furious.bank.admin.*`)
   - Verify the user has access to all administrative permissions under that category (e.g., `furious.bank.add.others`, `furious.bank.subtract.others`, etc.)
   - Verify the user does not have access to non-administrative permissions
   - Revoke the admin wildcard permission and verify access is properly restricted

3. **Legacy Permission Testing**
   - Grant a user a legacy permission (e.g., `furious.homes.admin`)
   - Verify the user has access to all granular permissions that the legacy permission should grant (e.g., `furious.homes.set.others`, `furious.homes.delete.others`, etc.)
   - Revoke the legacy permission and verify access is properly restricted

### Cross-Feature Integration Testing

1. **Economic Integration Testing**
   - Grant a user `furious.wallet.bypass.cost`
   - Verify the user can bypass costs in all integrated features (warps, homes, locks, etc.)
   - Revoke the permission and verify costs are properly enforced

2. **Teleport Integration Testing**
   - Grant a user `furious.teleport.admin`
   - Verify the user can bypass teleport restrictions in all teleportation systems (teleport, homes, warps)
   - Revoke the permission and verify restrictions are properly enforced

3. **Guild Role Integration Testing**
   - Create a guild with a user as owner
   - Verify the user has all guild owner permissions
   - Demote the user to officer and verify permissions are adjusted accordingly
   - Demote the user to member and verify permissions are further restricted

### Edge Case Testing

1. **Permission Conflict Testing**
   - Grant a user both a permission and its negated form (e.g., `furious.bank.deposit` and `-furious.bank.deposit`)
   - Verify the negated permission takes precedence
   - Test with wildcard permissions and their negated specific forms

2. **Permission Hierarchy Testing**
   - Grant a user both a specific permission and its parent wildcard (e.g., `furious.bank.deposit` and `furious.bank.*`)
   - Verify the user has access to the specific permission
   - Revoke the specific permission but keep the wildcard
   - Verify the user still has access through the wildcard

3. **Multi-Level Inheritance Testing**
   - Test if `furious.*` grants access to all permissions (note: current implementation may not support this)
   - Test if `furious.bank.*` grants access to `furious.bank.add.others`
   - Document any limitations in the current implementation

## Automated Testing Approaches

### Unit Testing

1. **Permission Class Testing**
   - Create unit tests for the `Permission` class
   - Test the `matches()` method with various permission patterns
   - Test wildcard matching functionality
   - Test negated permissions

2. **Role Class Testing**
   - Create unit tests for the `Role` class
   - Test the `hasPermission()` method with various permission scenarios
   - Test adding and removing permissions from roles

3. **PermissionManager Testing**
   - Create unit tests for the `PermissionManager` class
   - Test the `hasPermission()` method with various scenarios
   - Test role assignment and permission inheritance

### Integration Testing

1. **Command Handler Testing**
   - Create integration tests for command handlers
   - Test permission checks in command execution
   - Verify commands respect both legacy and granular permissions

2. **Cross-Feature Testing**
   - Create integration tests for features that interact with each other
   - Test permission-based restrictions across features
   - Verify consistent behavior across related systems

### Load Testing

1. **Performance Testing**
   - Test permission checking performance with a large number of permissions
   - Measure the impact of wildcard permissions on performance
   - Identify any performance bottlenecks in the permission system

2. **Scalability Testing**
   - Test the permission system with a large number of users
   - Test with complex permission hierarchies
   - Identify any scalability issues

## Test Scenarios

### Basic Permission Scenarios

1. **Wallet Permission Scenario**
   - User has `furious.wallet.pay`
   - User should be able to pay other players
   - User should not be able to add scraps to other players' wallets

2. **Bank Permission Scenario**
   - User has `furious.bank.deposit` and `furious.bank.withdraw`
   - User should be able to deposit and withdraw from their own account
   - User should not be able to deposit or withdraw from other players' accounts

3. **Teleport Permission Scenario**
   - User has `furious.teleport.request` and `furious.teleport.accept`
   - User should be able to request teleports and accept incoming requests
   - User should not be able to force teleport other players

### Inheritance Scenarios

1. **Wallet Inheritance Scenario**
   - User has `furious.wallet.*`
   - User should have all wallet permissions, including `furious.wallet.pay`, `furious.wallet.balance`, etc.
   - User should also have administrative permissions like `furious.wallet.add` and `furious.wallet.set`

2. **Bank Admin Inheritance Scenario**
   - User has `furious.bank.admin.*`
   - User should have all bank administrative permissions, including `furious.bank.add.others`, `furious.bank.subtract.others`, etc.
   - User should not necessarily have basic bank permissions like `furious.bank.deposit`

3. **Legacy Permission Scenario**
   - User has `furious.homes.admin`
   - User should have all granular home administrative permissions, including `furious.homes.set.others`, `furious.homes.delete.others`, etc.

### Edge Case Scenarios

1. **Negated Permission Scenario**
   - User has `furious.bank.*` and `-furious.bank.deposit`
   - User should have all bank permissions except deposit
   - The negated permission should take precedence over the wildcard

2. **Specific vs. Wildcard Scenario**
   - User has both `furious.teleport.force` and `furious.teleport.*`
   - Revoking `furious.teleport.force` should not affect access if the wildcard remains
   - The user should still be able to force teleport through the wildcard permission

3. **Multi-Level Inheritance Scenario**
   - Test if `furious.*` grants access to `furious.bank.deposit`
   - Test if `furious.bank.*` grants access to `furious.bank.add.others`
   - Document any limitations in the current implementation

### Integration Scenarios

1. **Wallet-Bank Integration Scenario**
   - User has `furious.bank.withdraw.auto`
   - User should be able to make purchases that automatically withdraw from their bank when their wallet has insufficient funds
   - Without this permission, purchases should fail if the wallet has insufficient funds

2. **Teleport-Warps Integration Scenario**
   - User has `furious.teleport.admin`
   - User should be able to bypass teleport cooldowns, costs, and password requirements for warps
   - Without this permission, user should be subject to normal restrictions

3. **Guild Role-Permission Scenario**
   - User is a guild owner but lacks `furious.guild.kick`
   - Test whether guild role or server permission takes precedence
   - Document the behavior for administrator reference

## Testing Checklist

Use this checklist to ensure all aspects of the permission system have been tested:

- [ ] Basic permissions work as expected
- [ ] Default permissions are correctly assigned
- [ ] Operator status grants all permissions
- [ ] Wildcard permissions grant all child permissions
- [ ] Admin wildcard permissions grant all administrative permissions
- [ ] Legacy permissions grant appropriate granular permissions
- [ ] Cross-feature integrations work correctly
- [ ] Guild roles interact properly with permissions
- [ ] Permission conflicts are resolved correctly
- [ ] Permission hierarchy is respected
- [ ] Multi-level inheritance limitations are documented
- [ ] Performance is acceptable with many permissions
- [ ] System scales well with many users
- [ ] All test scenarios pass successfully

By thoroughly testing the permission system using these procedures and scenarios, you can ensure that it works correctly in all situations and provides a consistent experience for both players and administrators.