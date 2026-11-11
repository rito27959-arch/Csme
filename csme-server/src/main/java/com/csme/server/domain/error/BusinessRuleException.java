package com.csme.server.domain.error;

/** Opération refusée par une règle métier (ex. se désactiver soi-même). */
public class BusinessRuleException extends DomainException {
    public BusinessRuleException(String code, String message) {
        super(code, message);
    }
}
