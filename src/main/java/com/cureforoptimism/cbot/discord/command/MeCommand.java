package com.cureforoptimism.cbot.discord.command;

import com.cureforoptimism.cbot.domain.User;
import com.cureforoptimism.cbot.service.UserService;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
public class MeCommand implements CbotCommand {
  private final UserService userService;

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

      String response = "Yeah, I know you, " + user.getUserName() + "#" + user.getDiscriminator();
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
