package com.cureforoptimism.cbot.discord.command;

import com.cureforoptimism.cbot.domain.exceptions.TransactionException;
import com.cureforoptimism.cbot.service.TransactionService;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Component
@AllArgsConstructor
public class SellCommand implements CbotCommand {
  private final TransactionService transactionService;

  @Override
  public String getName() {
    return "sell";
  }

  @Override
  public String getDescription() {
    return "Command to sell tokens to USD. Usage: <token> <amount>, or <amount> <token>. Example: `cbot sell eth 1.2` or `!cbot sell 1.2 eth`";
  }

  @Override
  @Transactional
  public Mono<Message> handle(MessageCreateEvent event, long userId, long guildId) {
    Message message = event.getMessage();
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
      // OK, let's allow either syntax...
      try {
        symbol = parts[3].toLowerCase().trim();
        amountStr = parts[2].toLowerCase().trim();

        amount = new BigDecimal(amountStr);
      } catch (NumberFormatException ex1) {
        return event
                .getMessage()
                .getChannel()
                .flatMap(
                        channel ->
                                channel.createMessage("Invalid amount. You gotsta use a proper number, yo"));
      }
    }

    try {
      final var tx = transactionService.sell(userId, guildId, symbol, amount);

      if (tx.isEmpty()) {
        // Should not happen
        return Mono.empty();
      }

      BigDecimal finalAmount = amount;
      String finalSymbol = symbol;
      return event
          .getMessage()
          .getChannel()
          .flatMap(
              channel ->
                  channel.createMessage(
                      "You have sold "
                          + finalAmount
                          + " of "
                          + finalSymbol
                          + " for $"
                          + tx.get().getAmount()
                          + " (and paid $"
                          + String.format("%.2f", tx.get().getFees())
                          + " fees). You have "
                          + String.format(
                              "%.2f", transactionService.getToken(userId, guildId, finalSymbol))
                          + " "
                          + finalSymbol
                          + " left."));
    } catch (TransactionException ex) {
      return event
          .getMessage()
          .getChannel()
          .flatMap(channel -> channel.createMessage("Failed to sell: " + ex.getMessage()));
    }
  }
}
