package com.cureforoptimism.cbot.discord.command;

import com.cureforoptimism.cbot.domain.User;
import com.cureforoptimism.cbot.repository.UserRepository;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
public class MeCommand implements CbotCommand {
    private final UserRepository userRepository;

    @Override
    public String getName() {
        return "me";
    }

    @Override
    public Mono<Message> handle(MessageCreateEvent event) {
        final var userOptional = userRepository.findByDiscordId(event.getMessage().getUserData().id().asLong());
        if(userOptional.isPresent()) {
            User user = userOptional.get();
            String response = "Yeah, I know you, " + user.getUserName() + "#" + user.getDiscriminator();
            return event.getMessage().getChannel().flatMap(channel -> channel.createMessage(response));
        } else {
            return event.getMessage().getChannel().flatMap(channel -> channel.createMessage("You're not registered with me, yet, " + event.getMessage().getUserData().username() + ". To register, type !cbot register"));
        }
    }
}
