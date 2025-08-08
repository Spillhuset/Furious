package com.spillhuset.furious.entities;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Permission class.
 */
public class PermissionTest {

    /**
     * Tests the basic functionality of the Permission class.
     */
    @Test
    public void testBasicPermission() {
        Permission permission = new Permission("furious.test");

        assertTrue(permission.matches("furious.test"), "Permission should match itself");
        assertFalse(permission.matches("furious.other"), "Permission should not match different permission");
        assertFalse(permission.matches("furious.test.sub"), "Permission should not match sub-permission");
    }

    /**
     * Tests the wildcard matching at the end of permission nodes.
     */
    @Test
    public void testEndWildcardMatching() {
        Permission permission = new Permission("furious.test.*");

        assertTrue(permission.matches("furious.test.sub"), "Wildcard should match sub-permission");
        assertTrue(permission.matches("furious.test.another"), "Wildcard should match another sub-permission");
        assertFalse(permission.matches("furious.other.sub"), "Wildcard should not match different permission");
    }

    /**
     * Tests the enhanced wildcard matching with multi-level inheritance.
     */
    @Test
    public void testMultiLevelInheritance() {
        Permission permission = new Permission("furious.*");

        assertTrue(permission.matches("furious.test"), "Multi-level wildcard should match direct sub-permission");
        assertTrue(permission.matches("furious.test.sub"), "Multi-level wildcard should match nested sub-permission");
        assertTrue(permission.matches("furious.other.another"), "Multi-level wildcard should match any nested permission");
        assertFalse(permission.matches("other.test"), "Multi-level wildcard should not match different root permission");
    }

    /**
     * Tests the enhanced wildcard matching with middle wildcards.
     */
    @Test
    public void testMiddleWildcardMatching() {
        Permission permission = new Permission("furious.*.admin");

        assertTrue(permission.matches("furious.test.admin"), "Middle wildcard should match specific pattern");
        assertTrue(permission.matches("furious.other.admin"), "Middle wildcard should match another specific pattern");
        assertFalse(permission.matches("furious.test.user"), "Middle wildcard should not match different end pattern");
        assertFalse(permission.matches("other.test.admin"), "Middle wildcard should not match different root pattern");
    }

    /**
     * Tests the permission caching mechanism.
     */
    @Test
    public void testPermissionCaching() {
        // Clear the cache before testing
        Permission.clearCache();

        Permission permission = new Permission("furious.test.*");

        // First check should cache the result
        assertTrue(permission.matches("furious.test.sub"), "First check should match and cache result");

        // Second check should use the cached result
        assertTrue(permission.matches("furious.test.sub"), "Second check should use cached result");

        // Clear the cache
        Permission.clearCache();

        // Check should work after clearing cache
        assertTrue(permission.matches("furious.test.sub"), "Check should work after clearing cache");
    }

    /**
     * Tests negated permissions.
     */
    @Test
    public void testNegatedPermission() {
        Permission permission = new Permission("-furious.test");

        assertTrue(permission.isNegated(), "Permission should be negated");
        assertEquals("furious.test", permission.getNode(), "Node should not include negation symbol");

        Permission positivePermission = new Permission("furious.test");
        assertFalse(positivePermission.isNegated(), "Positive permission should not be negated");
    }

    /**
     * Tests permission equality.
     */
    @Test
    public void testPermissionEquality() {
        Permission permission1 = new Permission("furious.test");
        Permission permission2 = new Permission("furious.test");
        Permission permission3 = new Permission("furious.other");
        Permission permission4 = new Permission("furious.test", true);

        assertEquals(permission1, permission2, "Same permission nodes should be equal");
        assertNotEquals(permission1, permission3, "Different permission nodes should not be equal");
        assertNotEquals(permission1, permission4, "Same node but different negation should not be equal");
    }
}