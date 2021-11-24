package com.cureforoptimism.cbot.discord.command;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

public interface CbotCommand {
  String getName();

  Mono<Message> handle(MessageCreateEvent event, long userId, long guildId);
}
