package com.csme.shared.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PermissionTest {

    @Test
    void roundTripsThroughAuthorityString() {
        for (ErpModule module : ErpModule.values()) {
            for (Action action : Action.values()) {
                Permission p = Permission.of(module, action);
                assertEquals(p, Permission.parse(p.authority()));
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ":", "USERS", "USERS:", ":CREATE", "USERS:CREATE:X", "users:create", "NOPE:VIEW"})
    void rejectsMalformedAuthorities(String raw) {
        assertThrows(IllegalArgumentException.class, () -> Permission.parse(raw));
    }

    @Test
    void generalManagerHasEveryPermission() {
        var gm = DefaultRoles.all().stream()
                .filter(t -> t.name().equals(DefaultRoles.GENERAL_MANAGER))
                .findFirst().orElseThrow();
        assertEquals(ErpModule.values().length * Action.values().length, gm.permissions().size());
    }

    @Test
    void commercialCannotDeleteSales() {
        var commercial = DefaultRoles.all().stream()
                .filter(t -> t.name().equals("Commercial"))
                .findFirst().orElseThrow();
        assertTrue(commercial.permissions().contains(Permission.of(ErpModule.SALES, Action.CREATE)));
        assertFalse(commercial.permissions().contains(Permission.of(ErpModule.SALES, Action.DELETE)));
    }

    @Test
    void defaultRoleNamesAreUnique() {
        Set<String> names = new HashSet<>();
        DefaultRoles.all().forEach(t -> assertTrue(names.add(t.name()), "Rôle en double : " + t.name()));
    }
}
