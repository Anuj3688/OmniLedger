package dev.fintech.omniledger.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Corporate registration, tax, and credential details for merchant and business entities.
 * Supports domestic Indian entities (CIN, GSTIN) and foreign cross-border corporations (Country, Foreign Reg / EIN).
 * Shares primary key with Party via @MapsId.
 */
@Entity
@Table(name = "business_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessProfile {

    @Id
    @Column(name = "party_id")
    private UUID partyId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "party_id")
    private Party party;

    @Column(name = "legal_business_name", nullable = false, length = 150)
    private String legalBusinessName;

    @Column(name = "trade_name", length = 150)
    private String tradeName;

    @Column(name = "country_of_incorporation", nullable = false, length = 2)
    @Builder.Default
    private String countryOfIncorporation = "IN";

    @Column(name = "gstin", length = 15)
    private String gstin;

    @Column(name = "cin_number", length = 21)
    private String cinNumber;

    @Column(name = "corporate_pan", length = 255)
    private String corporatePan;

    @Column(name = "corporate_pan_hash", length = 64)
    private String corporatePanHash;

    @Column(name = "foreign_registration_number", length = 50)
    private String foreignRegistrationNumber;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone_number", length = 255)
    private String phoneNumber;
}
