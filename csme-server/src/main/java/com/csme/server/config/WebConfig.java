package com.csme.server.config;

import com.csme.server.application.AuditService;
import com.csme.server.application.SystemTransactions;
import com.csme.server.infrastructure.security.PermissionInterceptor;
import com.csme.shared.api.ApiRoutes;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
public class WebConfig implements WebMvcConfigurer {

    private final AuditService audit;
    private final SystemTransactions system;

    public WebConfig(AuditService audit, SystemTransactions system) {
        this.audit = audit;
        this.system = system;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new PermissionInterceptor(audit, system)).addPathPatterns(ApiRoutes.BASE + "/**");
    }
}
