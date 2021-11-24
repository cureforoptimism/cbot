package com.cureforoptimism.cbot.discord.command;

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
  public Mono<Message> handle(MessageCreateEvent event) {
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

    final var tx =
        transactionService.sell(
            message.getUserData().id().asLong(),
            message.getGuildId().get().asLong(),
            symbol,
            amount);

    return tx.map(
            transaction ->
                event
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
                                    + transaction.getAmount()
                                    + ". You have "
                                    + String.format(
                                        "%.2f",
                                        transactionService.getToken(
                                            message.getUserData().id().asLong(),
                                            message.getGuildId().get().asLong(),
                                            symbol))
                                    + " "
                                    + symbol
                                    + " left.")))
        .orElseGet(
            () ->
                event
                    .getMessage()
                    .getChannel()
                    .flatMap(
                        channel ->
                            channel.createMessage(
                                "Failed to sell. I'll tell you why as soon as CFO makes the next commit.")));
  }
}
