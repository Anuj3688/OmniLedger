package dev.fintech.omniledger.dto.response;

import dev.fintech.omniledger.model.enums.RejectionCode;
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
public class FileIngestionRejectionResponse {

    private UUID rejectionId;
    private UUID jobId;
    private Integer rowNumber;
    private String rawRecord;
    private RejectionCode rejectionCode;
    private String reason;
    private Instant createdAt;
}
