package com.cureforoptimism.cbot.discord.command;

import static com.inamik.text.tables.Cell.Functions.RIGHT_ALIGN;

import com.cureforoptimism.cbot.Constants;
import com.cureforoptimism.cbot.Utilities;
import com.cureforoptimism.cbot.domain.Wallet;
import com.cureforoptimism.cbot.service.CoinGeckoService;
import com.cureforoptimism.cbot.service.TransactionService;
import com.cureforoptimism.cbot.service.UserService;
import com.inamik.text.tables.SimpleTable;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import java.math.BigDecimal;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@AllArgsConstructor
public class MeCommand implements CbotCommand {
  private final UserService userService;
  private final TransactionService transactionService;
  private final CoinGeckoService coinGeckoService;

  @Override
  public String getName() {
    return "me";
  }

  @Override
  public String getDescription() {
    return "Shows list of your current coins/usd and statistics";
  }

  @Override
  public Mono<Message> handle(MessageCreateEvent event, long userId, long guildId) {
    Message message = event.getMessage();

    final var userOptional = userService.findByDiscordIdAndServerId(userId, guildId);
    if (userOptional.isPresent()) {
      Wallet wallet =
          new Wallet(transactionService.getAllTransactions(userId, guildId), coinGeckoService);
      Map<String, BigDecimal> walletAmounts = wallet.getTokenAmounts();
      Map<String, BigDecimal> walletValues = wallet.getTokenValuesInUsd();

      SimpleTable output =
          SimpleTable.of()
              .nextRow()
              .nextCell("TOKEN")
              .nextCell("MKT PRICE")
              .nextCell("AMOUNT")
              .nextCell("USD VALUE")
              .nextCell("AVG BUY $");

      BigDecimal totalValue = BigDecimal.ZERO;
      for (Map.Entry<String, BigDecimal> entry : walletAmounts.entrySet()) {
        if (entry.getValue().compareTo(BigDecimal.ZERO) == 0) {
          continue;
        }

        totalValue = walletValues.get(entry.getKey()).add(totalValue);
        final var marketPrice =
            entry.getKey().equalsIgnoreCase("usd")
                ? BigDecimal.ONE
                : coinGeckoService.getCurrentPrice(entry.getKey().toLowerCase());
        output
            .nextRow()
            .nextCell(entry.getKey().toUpperCase())
            .nextCell("$" + Constants.DECIMAL_FMT_DEFAULT.format(marketPrice))
            .applyToCell(RIGHT_ALIGN.withWidth(12))
            .nextCell(Constants.DECIMAL_FMT_DEFAULT.format(entry.getValue()))
            .applyToCell(RIGHT_ALIGN.withWidth(16))
            .nextCell(
                "$" + Constants.DECIMAL_FMT_TWO_PRECISION.format(walletValues.get(entry.getKey())))
            .applyToCell(RIGHT_ALIGN.withWidth(14))
            .nextCell(
                "$"
                    + Constants.DECIMAL_FMT_TWO_PRECISION.format(
                        transactionService.getAverageBuyPrice(userId, guildId, entry.getKey())))
            .applyToCell(RIGHT_ALIGN.withWidth(12));
      }

      String finalResponse =
          "```\n"
              + Utilities.simpleTableToString(output)
              + "\n```"
              + String.format(
                  "Total USD value: $%s. $%s paid in fees.",
                  Constants.DECIMAL_FMT_TWO_PRECISION.format(totalValue),
                  Constants.DECIMAL_FMT_TWO_PRECISION.format(
                      transactionService.getFees(userId, guildId)));
      return event
          .getMessage()
          .getChannel()
          .flatMap(channel -> channel.createMessage(finalResponse));
    }

    return event
        .getMessage()
        .getChannel()
        .flatMap(
            channel ->
                channel.createMessage(
                    "You're not registered with me, yet, "
                        + message.getUserData().username()
                        + ". To register, type !cbot register"));
  }
}
