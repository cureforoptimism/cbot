package com.cureforoptimism.cbot.discord.command;

import com.cureforoptimism.cbot.Constants;
import com.cureforoptimism.cbot.domain.Server;
import com.cureforoptimism.cbot.domain.Transaction;
import com.cureforoptimism.cbot.domain.User;
import com.cureforoptimism.cbot.repository.ServerRepository;
import com.cureforoptimism.cbot.repository.TransactionRepository;
import com.cureforoptimism.cbot.repository.UserRepository;
import com.cureforoptimism.cbot.service.TransactionService;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
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

            final User user = userRepository.save(User.builder()
                    .discordId(id)
                    .userName(userName)
                    .discriminator(discriminator)
                    .server(server)
                    .build());

            // Initialize account with default value of USD
            transactionRepository.save(Transaction.builder()
                            .user(user)
                            .amount(Constants.DEFAULT_STARTING_USD)
                            .purchasePrice(1.0d)
                            .symbol("USD")
                    .build());

            final Double usdValue = transactionService.getUsdValue(id, serverId);
            final String formattedValue = String.format("$%.2f", usdValue);

            return event.getMessage().getChannel().flatMap(channel -> channel.createMessage("You're now registered with a starting balance of " + formattedValue + ", " + userName));
        } else {
            return event.getMessage().getChannel().flatMap(channel -> channel.createMessage("You're already registered, " + userOptional.get().getUserName() + "!"));
        }
    }
}
