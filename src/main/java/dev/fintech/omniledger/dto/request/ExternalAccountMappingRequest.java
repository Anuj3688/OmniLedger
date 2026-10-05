package dev.fintech.omniledger.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalAccountMappingRequest {

    @NotBlank(message = "External system is required")
    private String externalSystem;

    @NotBlank(message = "External account ID is required")
    private String externalAccountId;

    @NotNull(message = "Account ID is required")
    private UUID accountId;
}
