package dev.fintech.omniledger.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalAccountMappingResponse {

    private UUID mappingId;
    private String externalSystem;
    private String externalAccountId;
    private UUID accountId;
    private Instant createdAt;
}
