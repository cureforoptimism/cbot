package com.cureforoptimism.cbot.repository;

import com.cureforoptimism.cbot.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    // There's no way this is efficient
    Set<Transaction> findByUser_DiscordIdEqualsAndUser_Server_DiscordId(Long discordId, Long serverDiscordId);
}
