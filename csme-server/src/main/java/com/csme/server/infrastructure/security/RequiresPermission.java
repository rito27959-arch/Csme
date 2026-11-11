package com.csme.server.infrastructure.security;

import com.csme.shared.security.Action;
import com.csme.shared.security.ErpModule;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Point d'entrée réservé aux utilisateurs disposant de la permission {@code module:action}. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {
    ErpModule module();

    Action action();
}
