package dev.fintech.omniledger.model.enums;

/**
 * Classification of legal and operational entities in the banking ecosystem.
 * Follows the BIAN (Banking Industry Architecture Network) Party archetype.
 */
public enum PartyType {
    INDIVIDUAL,   // Retail customer / natural person
    BUSINESS,     // Corporate merchant / enterprise legal entity
    GOVERNMENT,   // Statutory tax authority or municipal body
    PLATFORM      // OmniLedger system internal operator
}
