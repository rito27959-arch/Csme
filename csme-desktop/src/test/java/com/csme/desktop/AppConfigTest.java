package com.csme.desktop;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.csme.desktop.config.AppConfig;
import java.net.URI;
import org.junit.jupiter.api.Test;

class AppConfigTest {

    @Test
    void httpsRequiredExceptForLocalMachine() throws Exception {
        var m = AppConfig.class.getDeclaredMethod("requireSecure", URI.class);
        m.setAccessible(true);
        assertDoesNotThrow(() -> m.invoke(null, URI.create("https://erp.csme.cm")));
        assertDoesNotThrow(() -> m.invoke(null, URI.create("http://localhost:8080")));
        assertDoesNotThrow(() -> m.invoke(null, URI.create("http://127.0.0.1:8080")));
        var e = assertThrows(java.lang.reflect.InvocationTargetException.class,
                () -> m.invoke(null, URI.create("http://erp.csme.cm")));
        org.junit.jupiter.api.Assertions.assertInstanceOf(IllegalStateException.class, e.getCause());
    }
}
