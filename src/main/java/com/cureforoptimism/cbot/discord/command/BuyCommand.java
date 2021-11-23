package com.cureforoptimism.cbot.discord.command;

import com.cureforoptimism.cbot.service.TransactionService;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
public class BuyCommand implements CbotCommand {
  private final TransactionService transactionService;

  @Override
  public String getName() {
    return "buy";
  }

  @Override
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
    double amount;

    try {
      amount = Double.parseDouble(amountStr);
    } catch (NumberFormatException ex) {
      return event
          .getMessage()
          .getChannel()
          .flatMap(
              channel ->
                  channel.createMessage("Invalid amount. You gotsta use a proper number, yo"));
    }

    final var tx =
        transactionService.buy(
            message.getUserData().id().asLong(),
            message.getGuildId().get().asLong(),
            symbol,
            amount);
    if (tx.isPresent()) {
      String purchasePrice = String.format("%.6f", tx.get().getPurchasePrice());
      double total = tx.get().getPurchasePrice() * amount;
      return event
          .getMessage()
          .getChannel()
          .flatMap(
              channel ->
                  channel.createMessage(
                      "You have bought "
                          + amount
                          + " of "
                          + symbol
                          + " for "
                          + purchasePrice
                          + " per token, totalling "
                          + total
                          + ". You have "
                          + transactionService.getUsdValue(
                              message.getUserData().id().asLong(),
                              message.getGuildId().get().asLong())
                          + " USD left."));
    } else {
      return event
          .getMessage()
          .getChannel()
          .flatMap(
              channel ->
                  channel.createMessage(
                      "Failed to purchase. I'll tell you why as soon as CFO makes the next commit."));
    }
  }
}
