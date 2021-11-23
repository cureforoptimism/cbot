package com.cureforoptimism.cbot.discord.command;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class BuyCommand implements CbotCommand {
    @Override
    public String getName() {
        return "buy";
    }

    @Override
    public Mono<Message> handle(MessageCreateEvent event) {
        // TODO: Implement
        return Mono.empty();
    }
}
