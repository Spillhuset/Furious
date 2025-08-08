package com.spillhuset.furious.utils;

import com.spillhuset.furious.entities.Permission;

/**
 * A utility class to test the enhanced Permission class functionality.
 * This class provides a simple way to verify that the wildcard matching works correctly.
 */
public class PermissionTester {

    /**
     * Main method to run the permission tests.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        System.out.println("Testing Enhanced Permission Class");
        System.out.println("================================");

        testExactMatching();
        testEndWildcardMatching();
        testBeginningWildcardMatching();
        testMiddleWildcardMatching();
        testMultipleWildcards();
        testWildcardOnly();
        testNegatedPermissions();

        System.out.println("All tests completed!");
    }

    /**
     * Test exact permission matching.
     */
    private static void testExactMatching() {
        System.out.println("\nTesting Exact Matching:");
        Permission permission = new Permission("furious.bank.deposit");

        // Exact match should work
        testMatch(permission, "furious.bank.deposit", true);

        // Different permissions should not match
        testMatch(permission, "furious.bank.withdraw", false);
        testMatch(permission, "furious.wallet.deposit", false);
        testMatch(permission, "furious.bank.deposit.others", false);
    }

    /**
     * Test wildcard matching at the end of permission nodes.
     */
    private static void testEndWildcardMatching() {
        System.out.println("\nTesting End Wildcard Matching:");
        Permission permission = new Permission("furious.bank.*");

        // Should match all bank permissions
        testMatch(permission, "furious.bank.deposit", true);
        testMatch(permission, "furious.bank.withdraw", true);
        testMatch(permission, "furious.bank.transfer", true);

        // Should not match other categories
        testMatch(permission, "furious.wallet.deposit", false);
        testMatch(permission, "furious.guild.create", false);

        // Should not match parent category
        testMatch(permission, "furious", false);
    }

    /**
     * Test wildcard matching at the beginning of permission nodes.
     */
    private static void testBeginningWildcardMatching() {
        System.out.println("\nTesting Beginning Wildcard Matching:");
        Permission permission = new Permission("*.admin");

        // Should match all admin permissions
        testMatch(permission, "furious.bank.admin", true);
        testMatch(permission, "furious.guild.admin", true);
        testMatch(permission, "furious.wallet.admin", true);

        // Should not match non-admin permissions
        testMatch(permission, "furious.bank.deposit", false);
        testMatch(permission, "furious.guild.create", false);
    }

    /**
     * Test wildcard matching in the middle of permission nodes.
     */
    private static void testMiddleWildcardMatching() {
        System.out.println("\nTesting Middle Wildcard Matching:");
        Permission permission = new Permission("furious.*.create");

        // Should match all create permissions across categories
        testMatch(permission, "furious.bank.create", true);
        testMatch(permission, "furious.guild.create", true);
        testMatch(permission, "furious.warp.create", true);

        // Should not match other operations
        testMatch(permission, "furious.bank.deposit", false);
        testMatch(permission, "furious.guild.delete", false);
    }

    /**
     * Test multiple wildcards in a single permission node.
     */
    private static void testMultipleWildcards() {
        System.out.println("\nTesting Multiple Wildcards:");
        Permission permission = new Permission("furious.*.*.others");

        // Should match all .others permissions with two levels
        testMatch(permission, "furious.bank.deposit.others", true);
        testMatch(permission, "furious.guild.invite.others", true);
        testMatch(permission, "furious.homes.set.others", true);

        // Should not match permissions without .others
        testMatch(permission, "furious.bank.deposit", false);
        testMatch(permission, "furious.guild.invite", false);

        // Should not match permissions with wrong number of levels
        testMatch(permission, "furious.admin.others", false);
        testMatch(permission, "furious.bank.admin.delete.others", false);
    }

    /**
     * Test wildcard-only permission.
     */
    private static void testWildcardOnly() {
        System.out.println("\nTesting Wildcard-Only Permission:");
        Permission permission = new Permission("*");

        // Should match everything
        testMatch(permission, "furious", true);
        testMatch(permission, "furious.bank.deposit", true);
        testMatch(permission, "furious.guild.invite.others", true);
        testMatch(permission, "anything.at.all", true);
    }

    /**
     * Test negated permissions.
     */
    private static void testNegatedPermissions() {
        System.out.println("\nTesting Negated Permissions:");
        Permission permission = new Permission("-furious.bank.*");

        // Should be negated
        System.out.println("Is negated: " + permission.isNegated() + " (expected: true)");

        // Matching should still work the same
        testMatch(permission, "furious.bank.deposit", true);
        testMatch(permission, "furious.bank.withdraw", true);
        testMatch(permission, "furious.wallet.deposit", false);
    }

    /**
     * Helper method to test if a permission matches a node and print the result.
     *
     * @param permission The permission to test
     * @param node The permission node to check against
     * @param expected The expected result
     */
    private static void testMatch(Permission permission, String node, boolean expected) {
        boolean result = permission.matches(node);
        System.out.println("Permission '" + permission + "' matches '" + node + "': " +
                          result + " (expected: " + expected + ")" +
                          (result == expected ? " ✓" : " ✗"));

        if (result != expected) {
            System.out.println("  ERROR: Test failed!");
        }
    }
}