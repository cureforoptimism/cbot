package com.cureforoptimism.cbot.discord.command;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import java.util.Collection;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class HelpCommand implements CbotCommand {
  final ApplicationContext context;
  final Collection<CbotCommand> commands;

  public HelpCommand(ApplicationContext context) {
    this.context = context;
    this.commands = context.getBeansOfType(CbotCommand.class).values();
  }

  @Override
  public String getName() {
    return "help";
  }

  @Override
  public String getDescription() {
    return "This help screen";
  }

  @Override
  public Mono<Message> handle(MessageCreateEvent event, long userId, long guildId) {
    StringBuilder message = new StringBuilder("Available commands:\n\n");

    for (CbotCommand command : commands) {
      message
          .append('`')
          .append(command.getName())
          .append('`')
          .append(" - ")
          .append(command.getDescription())
          .append("\n");
    }

    return event
        .getMessage()
        .getChannel()
        .flatMap(channel -> channel.createMessage(message.toString()));
  }
}
