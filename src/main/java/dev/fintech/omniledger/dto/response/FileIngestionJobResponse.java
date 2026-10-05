package dev.fintech.omniledger.dto.response;

import dev.fintech.omniledger.model.enums.IngestionJobStatus;
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
public class FileIngestionJobResponse {

    private UUID jobId;
    private String fileName;
    private String fileChecksumSha256;
    private String sourceSystem;
    private Long fileSizeBytes;
    private Integer totalRecords;
    private Integer processedRecords;
    private Integer failedRecords;
    private IngestionJobStatus status;
    private String errorMessage;
    private Instant createdAt;
    private Instant updatedAt;
}
