package com.cureforoptimism.cbot.discord.command;

import com.cureforoptimism.cbot.Constants;
import com.cureforoptimism.cbot.domain.exceptions.TransactionException;
import com.cureforoptimism.cbot.service.TransactionService;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import java.math.BigDecimal;
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
      final var tx = transactionService.buy(userId, guildId, symbol, amount);

      if (tx.isPresent()) {
        BigDecimal total = tx.get().getPurchasePrice().multiply(amount);
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
                            + " for $"
                            + Constants.DECIMAL_FMT_DEFAULT.format(tx.get().getPurchasePrice())
                            + " per token, totalling $"
                            + Constants.DECIMAL_FMT_TWO_PRECISION.format(total)
                            + " ($"
                            + Constants.DECIMAL_FMT_TWO_PRECISION.format(tx.get().getFees())
                            + " fees). You have $"
                            + Constants.DECIMAL_FMT_TWO_PRECISION.format(
                                transactionService.getUsdValue(
                                    message.getUserData().id().asLong(),
                                    message.getGuildId().get().asLong()))
                            + " USD left."));
      }
    } catch (TransactionException ex) {
      return event
          .getMessage()
          .getChannel()
          .flatMap(channel -> channel.createMessage("Failed to buy: " + ex.getMessage()));
    }

    return Mono.empty();
  }
}
