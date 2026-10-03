package dev.fintech.omniledger.service.routing;

import dev.fintech.omniledger.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * High-throughput routing component for hot accounts.
 * If a target account is sharded, routes incoming credits across child shards
 * to eliminate row-level locking bottlenecks.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountShardRouter {

    private final AccountRepository accountRepository;

    /**
     * Resolves the physical destination account ID.
     * Uses ID projections exclusively to avoid loading Account entities into Hibernate's
     * persistence context before pessimistic row locks are acquired.
     */
    public UUID resolveDestinationAccount(UUID targetAccountId) {
        Boolean isSharded = accountRepository.isAccountSharded(targetAccountId).orElse(false);
        if (!Boolean.TRUE.equals(isSharded)) {
            return targetAccountId;
        }

        List<UUID> shardIds = accountRepository.findShardIdsByParentAccountId(targetAccountId);
        if (shardIds.isEmpty()) {
            log.warn("Account {} is marked as sharded but has no child shards; falling back to parent account", targetAccountId);
            return targetAccountId;
        }

        int selectedIndex = ThreadLocalRandom.current().nextInt(shardIds.size());
        UUID selectedShardId = shardIds.get(selectedIndex);
        log.debug("Routed transfer for sharded account {} to shard {} ({}/{})",
                targetAccountId, selectedShardId, selectedIndex + 1, shardIds.size());
        return selectedShardId;
    }
}
