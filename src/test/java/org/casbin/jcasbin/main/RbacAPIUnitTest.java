// Copyright 2018 The casbin Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.casbin.jcasbin.main;

import org.casbin.jcasbin.util.Util;
import org.junit.Test;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static java.util.Arrays.asList;
import static org.casbin.jcasbin.main.TestUtil.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class RbacAPIUnitTest {
    @Test
    public void testRoleAPI() {
        Enforcer e = new Enforcer("examples/rbac_model.conf", "examples/rbac_policy.csv");

        testGetRoles(e, "alice", asList("data2_admin"));
        testGetRoles(e, "bob", asList());
        testGetRoles(e, "data2_admin", asList());
        testGetRoles(e, "non_exist", asList());

        testHasRole(e, "alice", "data1_admin", false);
        testHasRole(e, "alice", "data2_admin", true);

        e.addRoleForUser("alice", "data1_admin");

        testGetRoles(e, "alice", asList("data1_admin", "data2_admin"));
        testGetRoles(e, "bob", asList());
        testGetRoles(e, "data2_admin", asList());

        e.deleteRoleForUser("alice", "data1_admin");

        testGetRoles(e, "alice", asList("data2_admin"));
        testGetRoles(e, "bob", asList());
        testGetRoles(e, "data2_admin", asList());

        e.deleteRolesForUser("alice");

        testGetRoles(e, "alice", asList());
        testGetRoles(e, "bob", asList());
        testGetRoles(e, "data2_admin", asList());

        e.addRoleForUser("alice", "data1_admin");
        e.deleteUser("alice");

        testGetRoles(e, "alice", asList());
        testGetRoles(e, "bob", asList());
        testGetRoles(e, "data2_admin", asList());

        e.addRoleForUser("alice", "data2_admin");

        testEnforce(e, "alice", "data1", "read", true);
        testEnforce(e, "alice", "data1", "write", false);
        testEnforce(e, "alice", "data2", "read", true);
        testEnforce(e, "alice", "data2", "write", true);
        testEnforce(e, "bob", "data1", "read", false);
        testEnforce(e, "bob", "data1", "write", false);
        testEnforce(e, "bob", "data2", "read", false);
        testEnforce(e, "bob", "data2", "write", true);

        e.deleteRole("data2_admin");

        testEnforce(e, "alice", "data1", "read", true);
        testEnforce(e, "alice", "data1", "write", false);
        testEnforce(e, "alice", "data2", "read", false);
        testEnforce(e, "alice", "data2", "write", false);
        testEnforce(e, "bob", "data1", "read", false);
        testEnforce(e, "bob", "data1", "write", false);
        testEnforce(e, "bob", "data2", "read", false);
        testEnforce(e, "bob", "data2", "write", true);
    }

    @Test
    public void testGFunctionCache() {
        Enforcer e = new Enforcer("examples/rbac_model.conf", "examples/rbac_policy.csv");

        testEnforce(e, "alice", "data2", "read", true);
        e.removeGroupingPolicy(Arrays.asList("alice","data2_admin"));
        // Ensure that the gFunction cache is different for each enforce
        testEnforce(e, "alice", "data2", "read", false);
    }

    @Test
    public void testPermissionAPI() {
        Enforcer e = new Enforcer("examples/basic_without_resources_model.conf", "examples/basic_without_resources_policy.csv");

        testEnforceWithoutUsers(e, "alice", "read", true);
        testEnforceWithoutUsers(e, "alice", "write", false);
        testEnforceWithoutUsers(e, "bob", "read", false);
        testEnforceWithoutUsers(e, "bob", "write", true);

        testGetPermissions(e, "alice", asList(asList("alice", "read")));
        testGetPermissions(e, "bob", asList(asList("bob", "write")));

        testHasPermission(e, "alice", asList("read"), true);
        testHasPermission(e, "alice", asList("write"), false);
        testHasPermission(e, "bob", asList("read"), false);
        testHasPermission(e, "bob", asList("write"), true);

        e.deletePermission("read");

        testEnforceWithoutUsers(e, "alice", "read", false);
        testEnforceWithoutUsers(e, "alice", "write", false);
        testEnforceWithoutUsers(e, "bob", "read", false);
        testEnforceWithoutUsers(e, "bob", "write", true);

        e.addPermissionForUser("bob", "read");

        testEnforceWithoutUsers(e, "alice", "read", false);
        testEnforceWithoutUsers(e, "alice", "write", false);
        testEnforceWithoutUsers(e, "bob", "read", true);
        testEnforceWithoutUsers(e, "bob", "write", true);

        e.deletePermissionForUser("bob", "read");

        testEnforceWithoutUsers(e, "alice", "read", false);
        testEnforceWithoutUsers(e, "alice", "write", false);
        testEnforceWithoutUsers(e, "bob", "read", false);
        testEnforceWithoutUsers(e, "bob", "write", true);

        e.deletePermissionsForUser("bob");

        testEnforceWithoutUsers(e, "alice", "read", false);
        testEnforceWithoutUsers(e, "alice", "write", false);
        testEnforceWithoutUsers(e, "bob", "read", false);
        testEnforceWithoutUsers(e, "bob", "write", false);
    }

    @Test
    public void testImplicitRoleAPI() {
        Enforcer e = new Enforcer("examples/rbac_model.conf", "examples/rbac_with_hierarchy_policy.csv");
        assertEquals(e.getImplicitRolesForUser("alice"), asList("admin", "data1_admin", "data2_admin"));
    }

    @Test
    public void testImplicitPermissionAPI() {
        Enforcer e = new Enforcer("examples/rbac_model.conf", "examples/rbac_with_hierarchy_policy.csv");
        assertEquals(
                e.getImplicitPermissionsForUser("alice"),
                asList(
                        asList("alice", "data1", "read"),
                        asList("data1_admin", "data1", "read"),
                        asList("data1_admin", "data1", "write"),
                        asList("data2_admin", "data2", "read"),
                        asList("data2_admin", "data2", "write")
                )
        );

    }

    private void testGetImplicitUsersForRole(Enforcer e, String name, List<String> res) {
        List<String> myRes = e.getImplicitUsersForRole(name);
        Comparator<String> comparator = String::compareTo;
        myRes.sort(comparator);
        res.sort(comparator);

        if (!Util.arrayEquals(res, myRes)) {
            fail("Implicit users for : " + name + ": " + myRes + ", supposed to be " + res);
        }
    }

    @Test
    public void testImplicitUsersForRole() {
        Enforcer e = new Enforcer("examples/rbac_with_pattern_model.conf", "examples/rbac_with_pattern_policy.csv");

        testGetImplicitUsersForRole(e, "book_admin", asList("alice"));
        testGetImplicitUsersForRole(e, "pen_admin", asList("cathy", "bob"));

        testGetImplicitUsersForRole(e, "book_group", asList("/book/*", "/book/:id", "/book2/{id}"));
        testGetImplicitUsersForRole(e, "pen_group", asList("/pen/:id", "/pen2/{id}"));
    }
}
