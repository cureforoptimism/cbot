package com.cureforoptimism.cbot.discord.command;

import com.cureforoptimism.cbot.domain.Server;
import com.cureforoptimism.cbot.domain.User;
import com.cureforoptimism.cbot.repository.ServerRepository;
import com.cureforoptimism.cbot.repository.UserRepository;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
public class RegisterCommand implements CbotCommand {
    final UserRepository userRepository;
    final ServerRepository serverRepository;

    @Override
    public String getName() {
        return "register";
    }

    @Override
    public Mono<Message> handle(MessageCreateEvent event) {
        Message message = event.getMessage();

        final var id = message.getUserData().id().asLong();

        final var userOptional = userRepository.findByDiscordId(id);
        if(userOptional.isEmpty()) {
            Server server;

            // If this is a DM or something, just ignore, for now.
            if(message.getGuildId().isEmpty()) {
                return Mono.empty();
            }

            final var serverId = message.getGuildId().get().asLong();
            final var serverOptional = serverRepository.findByDiscordId(serverId);
            if(serverOptional.isEmpty()) {
                server = serverRepository.save(Server.builder()
                                .discordId(serverId)
                        .build());
            } else {
                server = serverOptional.get();
            }

            final var userName = message.getUserData().username();
            final var discriminator = message.getUserData().discriminator();

            userRepository.save(User.builder()
                    .discordId(id)
                    .userName(userName)
                    .discriminator(discriminator)
                    .server(server)
                    .build());

            return event.getMessage().getChannel().flatMap(channel -> channel.createMessage("You're now registered with a starting balance of $20,000 USD, " + userName));
        } else {
            return event.getMessage().getChannel().flatMap(channel -> channel.createMessage("You're already registered, " + userOptional.get().getUserName() + "!"));
        }
    }
}
