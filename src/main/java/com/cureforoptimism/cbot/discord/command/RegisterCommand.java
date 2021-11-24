package com.cureforoptimism.cbot.discord.command;

import com.cureforoptimism.cbot.Constants;
import com.cureforoptimism.cbot.domain.Server;
import com.cureforoptimism.cbot.domain.Transaction;
import com.cureforoptimism.cbot.domain.TransactionType;
import com.cureforoptimism.cbot.domain.User;
import com.cureforoptimism.cbot.repository.ServerRepository;
import com.cureforoptimism.cbot.repository.TransactionRepository;
import com.cureforoptimism.cbot.repository.UserRepository;
import com.cureforoptimism.cbot.service.TransactionService;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@AllArgsConstructor
public class RegisterCommand implements CbotCommand {
  final UserRepository userRepository;
  final ServerRepository serverRepository;
  final TransactionRepository transactionRepository;
  final TransactionService transactionService; // Well, this is awkward

  @Override
  public String getName() {
    return "register";
  }

  @Override
  public Mono<Message> handle(MessageCreateEvent event, long userId, long guildId) {
    Message message = event.getMessage();

    final var userOptional = userRepository.findByDiscordId(userId);
    if (userOptional.isEmpty()) {
      Server server;

      final var serverOptional = serverRepository.findByDiscordId(guildId);
      if (serverOptional.isEmpty()) {
        server = serverRepository.save(Server.builder().discordId(guildId).build());
      } else {
        server = serverOptional.get();
      }

      final var userName = message.getUserData().username();
      final var discriminator = message.getUserData().discriminator();

      final User user =
          userRepository.save(
              User.builder()
                  .discordId(userId)
                  .userName(userName)
                  .discriminator(discriminator)
                  .server(server)
                  .build());

      // Initialize account with default value of USD
      transactionRepository.save(
          Transaction.builder()
              .user(user)
              .amount(Constants.DEFAULT_STARTING_USD)
              .purchasePrice(BigDecimal.ONE)
              .transactionType(TransactionType.INITIAL)
              .symbol("usd")
              .build());

      final BigDecimal usdValue = transactionService.getUsdValue(userId, guildId);
      final String formattedValue = String.format("$%.2f", usdValue);

      return event
          .getMessage()
          .getChannel()
          .flatMap(
              channel ->
                  channel.createMessage(
                      "You're now registered with a starting balance of "
                          + formattedValue
                          + ", "
                          + userName));
    } else {
      return event
          .getMessage()
          .getChannel()
          .flatMap(
              channel ->
                  channel.createMessage(
                      "You're already registered, " + userOptional.get().getUserName() + "!"));
    }
  }
}
