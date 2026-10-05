package dev.fintech.omniledger.exception;

public class ExternalAccountMappingNotFoundException extends RuntimeException {
    public ExternalAccountMappingNotFoundException(String externalSystem, String externalAccountId) {
        super("External account mapping not found for system '" + externalSystem + "' and external account '" + externalAccountId + "'");
    }
}
