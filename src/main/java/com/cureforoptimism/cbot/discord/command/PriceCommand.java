package com.cureforoptimism.cbot.discord.command;

import com.cureforoptimism.cbot.service.CoinGeckoService;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
public class PriceCommand implements CbotCommand {
    final CoinGeckoService coinGeckoService;

    @Override
    public String getName() {
        return "price";
    }

    @Override
    public Mono<Message> handle(MessageCreateEvent event) {
        Message message = event.getMessage();
        String[] parts = message.getContent().split(" ");

        // TODO: May as well do multiple token fetches
        if(parts.length == 3) {
            String symbol = parts[2].toLowerCase();
            Double value = coinGeckoService.getCurrentPrice(symbol);
            String displayValue = String.format("$%.6f", value);

            return message.getChannel().flatMap(channel -> channel.createMessage(symbol + ": " + displayValue));
        }

        return Mono.empty();
    }
}
