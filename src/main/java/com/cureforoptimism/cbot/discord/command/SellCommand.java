package com.cureforoptimism.cbot.discord.command;

import com.cureforoptimism.cbot.domain.exceptions.TransactionException;
import com.cureforoptimism.cbot.service.TransactionService;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
public class SellCommand implements CbotCommand {
  private final TransactionService transactionService;

  @Override
  public String getName() {
    return "sell";
  }

  @Override
  @Transactional
  public Mono<Message> handle(MessageCreateEvent event, long userId, long guildId) {
    Message message = event.getMessage();

    if (message.getGuildId().isEmpty()) {
      return Mono.empty();
    }

    String[] parts = message.getContent().split(" ");
    if (parts.length != 4) {
      return Mono.empty();
    }

    String symbol = parts[2].toLowerCase().trim();
    String amountStr = parts[3].toLowerCase().trim();
    BigDecimal amount;

    try {
      amount = new BigDecimal(amountStr);
    } catch (NumberFormatException ex) {
      return event
          .getMessage()
          .getChannel()
          .flatMap(
              channel ->
                  channel.createMessage("Invalid amount. You gotsta use a proper number, yo"));
    }

    try {
      final var tx = transactionService.sell(userId, guildId, symbol, amount);

      if (tx.isEmpty()) {
        // Should not happen
        return Mono.empty();
      }

      return event
          .getMessage()
          .getChannel()
          .flatMap(
              channel ->
                  channel.createMessage(
                      "You have sold "
                          + amount
                          + " of "
                          + symbol
                          + " for $"
                          + tx.get().getAmount()
                          + " (and paid $"
                          + String.format("%.2f", tx.get().getFees())
                          + " fees). You have "
                          + String.format(
                              "%.2f", transactionService.getToken(userId, guildId, symbol))
                          + " "
                          + symbol
                          + " left."));
    } catch (TransactionException ex) {
      return event
          .getMessage()
          .getChannel()
          .flatMap(channel -> channel.createMessage("Failed to sell: " + ex.getMessage()));
    }
  }
}
