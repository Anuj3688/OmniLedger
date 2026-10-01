package dev.fintech.omniledger.exception;

import java.util.UUID;

/**
 * Thrown when a Party cannot be located by its UUID.
 */
public class PartyNotFoundException extends RuntimeException {

    public PartyNotFoundException(UUID partyId) {
        super("Party not found with id: " + partyId);
    }
}
