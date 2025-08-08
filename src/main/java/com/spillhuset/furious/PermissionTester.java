package com.spillhuset.furious;

import com.spillhuset.furious.entities.Permission;

/**
 * A simple class to test the Permission class functionality.
 * This class provides a main method that tests the enhanced wildcard matching
 * and permission caching features.
 */
public class PermissionTester {

    /**
     * Main method to test Permission class functionality.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        System.out.println("Testing Permission class functionality...");

        // Test basic permission matching
        testBasicPermission();

        // Test end wildcard matching
        testEndWildcardMatching();

        // Test multi-level inheritance
        testMultiLevelInheritance();

        // Test middle wildcard matching
        testMiddleWildcardMatching();

        // Test permission caching
        testPermissionCaching();

        System.out.println("All tests completed!");
    }

    /**
     * Tests the basic functionality of the Permission class.
     */
    private static void testBasicPermission() {
        System.out.println("\nTesting basic permission matching...");

        Permission permission = new Permission("furious.test");

        boolean test1 = permission.matches("furious.test");
        boolean test2 = permission.matches("furious.other");
        boolean test3 = permission.matches("furious.test.sub");

        System.out.println("Permission 'furious.test' matches 'furious.test': " + test1 + " (expected: true)");
        System.out.println("Permission 'furious.test' matches 'furious.other': " + test2 + " (expected: false)");
        System.out.println("Permission 'furious.test' matches 'furious.test.sub': " + test3 + " (expected: false)");
    }

    /**
     * Tests the wildcard matching at the end of permission nodes.
     */
    private static void testEndWildcardMatching() {
        System.out.println("\nTesting end wildcard matching...");

        Permission permission = new Permission("furious.test.*");

        boolean test1 = permission.matches("furious.test.sub");
        boolean test2 = permission.matches("furious.test.another");
        boolean test3 = permission.matches("furious.other.sub");

        System.out.println("Permission 'furious.test.*' matches 'furious.test.sub': " + test1 + " (expected: true)");
        System.out.println("Permission 'furious.test.*' matches 'furious.test.another': " + test2 + " (expected: true)");
        System.out.println("Permission 'furious.test.*' matches 'furious.other.sub': " + test3 + " (expected: false)");
    }

    /**
     * Tests the enhanced wildcard matching with multi-level inheritance.
     */
    private static void testMultiLevelInheritance() {
        System.out.println("\nTesting multi-level inheritance...");

        Permission permission = new Permission("furious.*");

        boolean test1 = permission.matches("furious.test");
        boolean test2 = permission.matches("furious.test.sub");
        boolean test3 = permission.matches("furious.other.another");
        boolean test4 = permission.matches("other.test");

        System.out.println("Permission 'furious.*' matches 'furious.test': " + test1 + " (expected: true)");
        System.out.println("Permission 'furious.*' matches 'furious.test.sub': " + test2 + " (expected: true)");
        System.out.println("Permission 'furious.*' matches 'furious.other.another': " + test3 + " (expected: true)");
        System.out.println("Permission 'furious.*' matches 'other.test': " + test4 + " (expected: false)");
    }

    /**
     * Tests the enhanced wildcard matching with middle wildcards.
     */
    private static void testMiddleWildcardMatching() {
        System.out.println("\nTesting middle wildcard matching...");

        Permission permission = new Permission("furious.*.admin");

        boolean test1 = permission.matches("furious.test.admin");
        boolean test2 = permission.matches("furious.other.admin");
        boolean test3 = permission.matches("furious.test.user");
        boolean test4 = permission.matches("other.test.admin");

        System.out.println("Permission 'furious.*.admin' matches 'furious.test.admin': " + test1 + " (expected: true)");
        System.out.println("Permission 'furious.*.admin' matches 'furious.other.admin': " + test2 + " (expected: true)");
        System.out.println("Permission 'furious.*.admin' matches 'furious.test.user': " + test3 + " (expected: false)");
        System.out.println("Permission 'furious.*.admin' matches 'other.test.admin': " + test4 + " (expected: false)");
    }

    /**
     * Tests the permission caching mechanism.
     */
    private static void testPermissionCaching() {
        System.out.println("\nTesting permission caching...");

        // Clear the cache before testing
        Permission.clearCache();

        Permission permission = new Permission("furious.test.*");

        // First check should cache the result
        long startTime1 = System.nanoTime();
        boolean test1 = permission.matches("furious.test.sub");
        long endTime1 = System.nanoTime();
        long duration1 = (endTime1 - startTime1);

        // Second check should use the cached result (faster)
        long startTime2 = System.nanoTime();
        boolean test2 = permission.matches("furious.test.sub");
        long endTime2 = System.nanoTime();
        long duration2 = (endTime2 - startTime2);

        // Clear the cache
        Permission.clearCache();

        // Third check should not use the cached result (slower)
        long startTime3 = System.nanoTime();
        boolean test3 = permission.matches("furious.test.sub");
        long endTime3 = System.nanoTime();
        long duration3 = (endTime3 - startTime3);

        System.out.println("First check result: " + test1 + " (expected: true)");
        System.out.println("First check duration: " + duration1 + " ns");
        System.out.println("Second check result: " + test2 + " (expected: true)");
        System.out.println("Second check duration: " + duration2 + " ns");
        System.out.println("Third check result: " + test3 + " (expected: true)");
        System.out.println("Third check duration: " + duration3 + " ns");

        if (duration2 < duration1) {
            System.out.println("Caching is working! Second check was faster than first check.");
        } else {
            System.out.println("Warning: Second check was not faster than first check. Caching might not be working properly.");
        }

        if (duration3 > duration2) {
            System.out.println("Cache clearing is working! Third check was slower than second check.");
        } else {
            System.out.println("Warning: Third check was not slower than second check. Cache clearing might not be working properly.");
        }
    }
}