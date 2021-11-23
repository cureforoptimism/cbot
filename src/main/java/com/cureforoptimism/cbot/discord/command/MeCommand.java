package com.cureforoptimism.cbot.discord.command;

import com.cureforoptimism.cbot.domain.User;
import com.cureforoptimism.cbot.service.TransactionService;
import com.cureforoptimism.cbot.service.UserService;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@AllArgsConstructor
public class MeCommand implements CbotCommand {
  private final UserService userService;
  private final TransactionService transactionService;

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

      String chanMessage = "You have... ";
      Double totalValue = 0.0d;
      Map<String, Double> coinValues = transactionService.getAllTokenValues(user.getDiscordId(), message.getGuildId().get().asLong());
      for(Map.Entry<String, Double> entry : coinValues.entrySet()) {
        chanMessage += entry.getValue() + " " + entry.getKey() + ", ";
      }

      String response = "Yeah, I know you, " + user.getUserName() + "#" + user.getDiscriminator() + ". You currently have: " + chanMessage;
      return event.getMessage().getChannel().flatMap(channel -> channel.createMessage(response));
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
