package com.cureforoptimism.cbot.service;

import com.cureforoptimism.cbot.domain.Transaction;
import com.cureforoptimism.cbot.repository.TransactionRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;

    public Double getUsdValue(Long userId, Long serverId) {
        // TODO: Use current token values, mapped to USD. Not just USD.
        final var transactions = transactionRepository.findByUser_DiscordIdEqualsAndUser_Server_DiscordId(userId, serverId);
        Double usdValue = 0.0d;
        for(Transaction transaction : transactions) {
            if(transaction.getSymbol().equalsIgnoreCase("usd")) {
                usdValue += transaction.getAmount();
            }
        }

        return usdValue;
    }
}
