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
 * Sovereign tax authority and municipal department details for automated tax settlement.
 * Shares primary key with Party via @MapsId.
 */
@Entity
@Table(name = "government_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GovernmentProfile {

    @Id
    @Column(name = "party_id")
    private UUID partyId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "party_id")
    private Party party;

    @Column(name = "department_name", nullable = false, length = 150)
    private String departmentName;

    @Column(name = "jurisdiction", nullable = false, length = 50)
    private String jurisdiction;

    @Column(name = "nodal_agency_code", nullable = false, unique = true, length = 50)
    private String nodalAgencyCode;

    @Column(name = "tax_category_code", nullable = false, length = 20)
    private String taxCategoryCode;
}
