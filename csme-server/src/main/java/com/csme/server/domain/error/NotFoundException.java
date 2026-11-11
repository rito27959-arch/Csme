package com.csme.server.domain.error;

/** Ressource introuvable — y compris lorsqu'elle appartient à une autre entreprise (aucune fuite d'existence). */
public class NotFoundException extends DomainException {
    public NotFoundException(String what) {
        super("NOT_FOUND", what + " introuvable.");
    }
}
