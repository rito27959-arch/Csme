package com.csme.server.application;

import com.csme.server.tenancy.TenantContext;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Seul point d'entrée vers la portée système (contournement RLS). Chaque appel est une transaction
 * distincte, ouverte après le passage en portée système pour que la connexion soit bien configurée.
 * À utiliser uniquement pour la connexion et les opérations du super administrateur.
 */
@Component
public class SystemTransactions {

    private final TransactionTemplate template;

    public SystemTransactions(PlatformTransactionManager transactionManager) {
        this.template = new TransactionTemplate(transactionManager);
    }

    public <T> T call(Supplier<T> work) {
        return TenantContext.asSystem(() -> template.execute(status -> work.get()));
    }

    public void run(Runnable work) {
        call(() -> {
            work.run();
            return null;
        });
    }
}
