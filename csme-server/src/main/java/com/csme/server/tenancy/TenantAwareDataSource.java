package com.csme.server.tenancy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * Applique la portée du {@link TenantContext} à chaque connexion empruntée au pool :
 * {@code app.company_id} et {@code app.bypass_rls} sont lus par les politiques RLS de PostgreSQL.
 * Les valeurs sont réinitialisées avant le retour de la connexion au pool.
 */
public class TenantAwareDataSource extends DelegatingDataSource {

    private static final Logger log = LoggerFactory.getLogger(TenantAwareDataSource.class);
    private static final String APPLY_SQL =
            "select set_config('app.company_id', ?, false), set_config('app.bypass_rls', ?, false)";

    public TenantAwareDataSource(DataSource target) {
        super(target);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return prepare(super.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return prepare(super.getConnection(username, password));
    }

    private Connection prepare(Connection connection) throws SQLException {
        TenantScope scope = TenantContext.current().orElse(null);
        String companyId = scope instanceof TenantScope.Company c ? c.companyId().toString() : "";
        String bypass = scope instanceof TenantScope.System ? "on" : "";
        try {
            apply(connection, companyId, bypass);
        } catch (SQLException e) {
            connection.close();
            throw e;
        }
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] {Connection.class},
                new ResettingHandler(connection));
    }

    private static void apply(Connection connection, String companyId, String bypass) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(APPLY_SQL)) {
            ps.setString(1, companyId);
            ps.setString(2, bypass);
            ps.execute();
        }
    }

    /** Intercepte {@code close()} pour effacer la portée avant de rendre la connexion au pool. */
    private static final class ResettingHandler implements InvocationHandler {
        private final Connection target;

        private ResettingHandler(Connection target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if ("close".equals(name) && (args == null || args.length == 0)) {
                try {
                    if (!target.isClosed()) {
                        apply(target, "", "");
                    }
                } catch (SQLException e) {
                    // Chaque emprunt réapplique la portée : un échec ici ne peut pas fuir de données.
                    log.warn("Réinitialisation de la portée impossible avant retour au pool : {}", e.getMessage());
                }
                target.close();
                return null;
            }
            if ("unwrap".equals(name) && args != null && args[0] instanceof Class<?> c && c.isInstance(proxy)) {
                return proxy;
            }
            if ("equals".equals(name)) {
                return proxy == args[0];
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            }
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }
}
