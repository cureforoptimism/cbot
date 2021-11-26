package com.cureforoptimism.cbot.discord.command;

import com.cureforoptimism.cbot.Constants;
import com.cureforoptimism.cbot.domain.Transaction;
import com.cureforoptimism.cbot.domain.User;
import com.cureforoptimism.cbot.service.TransactionService;
import com.cureforoptimism.cbot.service.UserService;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.Set;

@Component
@AllArgsConstructor
public class LedgerCommand implements CbotCommand {
  final TransactionService transactionService;
  final UserService userService;

  @Override
  public String getName() {
    return "ledger";
  }

  @Override
  public String getDescription() {
    return "Shows your transaction history";
  }

  @Override
  public Mono<Message> handle(MessageCreateEvent event, long userId, long guildId) {
    Set<Transaction> transactions = transactionService.getAllTransactions(userId, guildId);
    Optional<User> userOptional = userService.findByDiscordIdAndServerId(userId, guildId);
    if (userOptional.isEmpty()) {
      return Mono.empty();
    }

    User user = userOptional.get();

    final var embed =
        EmbedCreateSpec.builder()
            .author("cbot", null, null)
            .title(user.getUserName() + "@" + user.getDiscriminator());

    StringBuilder description = new StringBuilder();
    description.append("```");
    for (Transaction transaction : transactions) {
      description
          .append(transaction.getTransactionType())
          .append(" | ")
          .append(Constants.DECIMAL_FMT_DEFAULT.format(transaction.getAmount()))
          .append(" | ")
          .append(Constants.DECIMAL_FMT_DEFAULT.format(transaction.getPurchasePrice()))
          .append(" | ")
          .append(transaction.getSymbol().toUpperCase())
          .append("\n");
    }
    description.append("```");

    embed.description(description.toString());

    return event.getMessage().getChannel().flatMap(channel -> channel.createMessage(embed.build()));
  }
}
