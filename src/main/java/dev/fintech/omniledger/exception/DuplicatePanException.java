package dev.fintech.omniledger.exception;

/**
 * Thrown when attempting to register a user with a PAN number that already exists.
 */
public class DuplicatePanException extends RuntimeException {

    public DuplicatePanException(String panNumber) {
        super("User with PAN number " + panNumber + " already exists");
    }
}
