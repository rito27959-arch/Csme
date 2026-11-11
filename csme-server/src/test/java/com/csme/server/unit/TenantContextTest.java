package com.csme.server.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.csme.server.tenancy.TenantContext;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TenantContextTest {

    @Test
    void scopesAreNestedAndRestored() {
        UUID company = UUID.randomUUID();
        assertThat(TenantContext.current()).isEmpty();
        TenantContext.asCompany(company, () -> {
            assertThat(TenantContext.requireCompanyId()).isEqualTo(company);
            TenantContext.asSystem(() -> {
                assertThat(TenantContext.isSystem()).isTrue();
                assertThat(TenantContext.companyId()).isEmpty();
                return null;
            });
            assertThat(TenantContext.requireCompanyId()).isEqualTo(company);
            return null;
        });
        assertThat(TenantContext.current()).isEmpty();
    }

    @Test
    void requireCompanyFailsOutsideCompanyScope() {
        assertThatThrownBy(TenantContext::requireCompanyId).isInstanceOf(IllegalStateException.class);
        TenantContext.asSystem(() -> {
            assertThatThrownBy(TenantContext::requireCompanyId).isInstanceOf(IllegalStateException.class);
            return null;
        });
    }
}
