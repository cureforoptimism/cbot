package com.cureforoptimism.cbot.discord.command;

import com.cureforoptimism.cbot.domain.User;
import com.cureforoptimism.cbot.domain.Wallet;
import com.cureforoptimism.cbot.service.CoinGeckoService;
import com.cureforoptimism.cbot.service.TransactionService;
import com.cureforoptimism.cbot.service.UserService;
import com.inamik.text.tables.GridTable;
import com.inamik.text.tables.SimpleTable;
import com.inamik.text.tables.grid.Border;
import com.inamik.text.tables.grid.Util;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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
  @Transactional
  public Mono<Message> handle(MessageCreateEvent event) {
    Message message = event.getMessage();

    if (message.getGuildId().isEmpty()) {
      return Mono.empty();
    }

    final var userOptional =
        userService.findByDiscordIdAndServerId(
            message.getUserData().id().asLong(), message.getGuildId().get().asLong());
    if (userOptional.isPresent()) {
      User user = userOptional.get();

      Wallet wallet =
          new Wallet(
              transactionService.getAllTransactions(
                  user.getDiscordId(), message.getGuildId().get().asLong()),
              coinGeckoService);
      Map<String, BigDecimal> walletAmounts = wallet.getTokenAmounts();
      Map<String, BigDecimal> walletValues = wallet.getTokenValuesInUsd();

      SimpleTable output =
          SimpleTable.of().nextRow().nextCell("TOKEN").nextCell("AMOUNT").nextCell("USD VALUE");

      BigDecimal totalValue = BigDecimal.ONE;
      for (Map.Entry<String, BigDecimal> entry : walletAmounts.entrySet()) {
        if (entry.getValue().compareTo(BigDecimal.ZERO) == 0) {
          continue;
        }

        totalValue = walletValues.get(entry.getKey()).add(totalValue);
        output
            .nextRow()
            .nextCell(entry.getKey().toUpperCase())
            .nextCell(String.format("%.5f", entry.getValue()))
            .nextCell("$" + String.format("%.2f", walletValues.get(entry.getKey())));
      }

      GridTable gridTable = output.toGrid();
      gridTable = Border.of(Border.Chars.of('+', '-', '|')).apply(gridTable);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      PrintStream printStream = new PrintStream(baos);
      Util.print(gridTable, printStream);

      String response;
      response = baos.toString(StandardCharsets.UTF_8);

      String finalResponse =
          "```\n" + response + "\n```" + String.format("Total USD value: $%.2f", totalValue);
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
