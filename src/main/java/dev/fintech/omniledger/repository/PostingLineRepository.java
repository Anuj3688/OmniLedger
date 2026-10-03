package dev.fintech.omniledger.repository;

import dev.fintech.omniledger.model.PostingLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostingLineRepository extends JpaRepository<PostingLine, UUID> {

    List<PostingLine> findByJournalEntryId(UUID journalEntryId);

    List<PostingLine> findByAccount_Id(UUID accountId);

    List<PostingLine> findByAccount_IdOrderByIdAsc(UUID accountId);
}
