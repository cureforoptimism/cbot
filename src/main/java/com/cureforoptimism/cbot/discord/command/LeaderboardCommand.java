package com.cureforoptimism.cbot.discord.command;

import com.cureforoptimism.cbot.Constants;
import com.cureforoptimism.cbot.domain.User;
import com.cureforoptimism.cbot.domain.Wallet;
import com.cureforoptimism.cbot.repository.ServerRepository;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
public class LeaderboardCommand implements CbotCommand {
  private final UserService userService;
  private final ServerRepository serverRepository;
  private final TransactionService transactionService;
  private final CoinGeckoService coinGeckoService;

  @Override
  public String getName() {
    return "leaderboard";
  }

  @Override
  public String getDescription() {
    return "Shows the leaderboard for the current server";
  }

  @Override
  public Mono<Message> handle(MessageCreateEvent event, long userId, long guildId) {
    Set<User> users = userService.findByServerId(guildId);

    final Map<User, Wallet> wallets = new ConcurrentHashMap<>();
    users.forEach(
        u -> {
          Wallet wallet =
              new Wallet(
                  transactionService.getAllTransactions(u.getDiscordId(), guildId),
                  coinGeckoService);
          wallets.put(u, wallet);
        });

    Map<String, BigDecimal> unsorted = new TreeMap<>();
    for (Map.Entry<User, Wallet> entry : wallets.entrySet()) {
      for (BigDecimal value : entry.getValue().getTokenValuesInUsd().values()) {
        if (!unsorted.containsKey(entry.getKey().getUserName())) {
          unsorted.put(entry.getKey().getUserName(), BigDecimal.ZERO);
        }

        unsorted.put(
            entry.getKey().getUserName(), unsorted.get(entry.getKey().getUserName()).add(value));
      }
    }

    final var sorted =
        unsorted.entrySet().stream()
            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

    SimpleTable output =
        SimpleTable.of().nextRow().nextCell("POSITION").nextCell("USER").nextCell("USD VALUE");

    int x = 1;
    for (Map.Entry<String, BigDecimal> entry : sorted.entrySet()) {
      output
          .nextRow()
          .nextCell("#" + x++)
          .nextCell(entry.getKey())
          .nextCell(Constants.DECIMAL_FMT_TWO_PRECISION.format(entry.getValue()));
    }

    GridTable gridTable = output.toGrid();
    gridTable = Border.of(Border.Chars.of('+', '-', '|')).apply(gridTable);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream printStream = new PrintStream(baos);
    Util.print(gridTable, printStream);

    String response = baos.toString(StandardCharsets.UTF_8);
    String finalResponse = "```\n" + response + "\n```";
    return event.getMessage().getChannel().flatMap(channel -> channel.createMessage(finalResponse));
  }
}
