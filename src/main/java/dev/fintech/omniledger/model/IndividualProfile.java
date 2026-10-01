package dev.fintech.omniledger.model;

import dev.fintech.omniledger.model.enums.ResidentialStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.time.LocalDate;
import java.util.UUID;

/**
 * KYC and personal identity profile for natural persons (retail customers).
 * Tracks residential status under FEMA (Indian Resident vs. NRI).
 * Shares primary key with Party via @MapsId.
 */
@Entity
@Table(name = "individual_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndividualProfile {

    @Id
    @Column(name = "party_id")
    private UUID partyId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "party_id")
    private Party party;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "pan_number", nullable = false, unique = true, length = 10)
    private String panNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "residential_status", nullable = false, length = 30)
    @Builder.Default
    private ResidentialStatus residentialStatus = ResidentialStatus.RESIDENT_INDIAN;

    @Column(name = "country_of_residence", nullable = false, length = 2)
    @Builder.Default
    private String countryOfResidence = "IN";

    @Column(name = "email", length = 120, unique = true)
    private String email;

    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
}
