package dev.fintech.omniledger.exception;

import dev.fintech.omniledger.security.PiiMasker;

public class DuplicatePanException extends RuntimeException {

    public DuplicatePanException(String panNumber) {
        super("User with PAN number " + PiiMasker.maskPan(panNumber) + " already exists");
    }
}
