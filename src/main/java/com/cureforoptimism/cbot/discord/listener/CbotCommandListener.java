package com.cureforoptimism.cbot.discord.listener;

import com.cureforoptimism.cbot.discord.command.CbotCommand;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import java.util.Collection;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class CbotCommandListener {
  private final Collection<CbotCommand> commands;

  public CbotCommandListener(ApplicationContext applicationContext) {
    commands = applicationContext.getBeansOfType(CbotCommand.class).values();
  }

  public Mono<Message> handle(MessageCreateEvent event) {
    String message = event.getMessage().getContent().toLowerCase();
    if (!message.startsWith("!cbot")) {
      return Mono.empty();
    }

    String[] parts = message.split(" ");
    if (parts.length >= 2) {
      String commandName = parts[1];

      // Convert our list to a flux that we can iterate through
      return Flux.fromIterable(commands)
          .filter(command -> command.getName().equals(commandName))
          .next()
          .flatMap(command -> command.handle(event));
    }

    return Mono.empty();
  }
}
