package dev.fintech.omniledger.service.routing;

import dev.fintech.omniledger.model.Account;
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
     * If the account is a sharded master, randomly chooses one of its child shards.
     * Otherwise returns the original account ID untouched.
     */
    public UUID resolveDestinationAccount(UUID targetAccountId) {
        Account target = accountRepository.findById(targetAccountId).orElse(null);
        if (target == null || !target.isSharded()) {
            return targetAccountId;
        }

        List<Account> shards = accountRepository.findByParentAccountId(targetAccountId);
        if (shards.isEmpty()) {
            log.warn("Account {} is marked as sharded but has no child shards; falling back to parent account", targetAccountId);
            return targetAccountId;
        }

        int selectedIndex = ThreadLocalRandom.current().nextInt(shards.size());
        UUID selectedShardId = shards.get(selectedIndex).getId();
        log.debug("Routed transfer for sharded account {} to shard {} ({}/{})",
                targetAccountId, selectedShardId, selectedIndex + 1, shards.size());
        return selectedShardId;
    }
}
