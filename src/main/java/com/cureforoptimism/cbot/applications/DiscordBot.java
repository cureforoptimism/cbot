package com.cureforoptimism.cbot.applications;

import com.cureforoptimism.cbot.services.TokenService;
import com.cureforoptimism.cbot.services.CoinGeckoService;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@AllArgsConstructor
public class DiscordBot implements ApplicationRunner {
  final CoinGeckoService coinGeckoService;
  final TokenService tokenService;

  @Override
  public void run(ApplicationArguments args) throws Exception {
    DiscordClient.create(tokenService.getDiscordToken())
        .withGateway(
            client ->
                client.on(
                    MessageCreateEvent.class,
                    event -> {
                      Message message = event.getMessage();

                      String[] parts = message.getContent().split(" ");
                      if (parts.length > 0 && parts[0].equalsIgnoreCase("!cbot")) {
                        if (parts.length == 2) {
                          String symbol = parts[1].toLowerCase();
                          Double value = coinGeckoService.getCurrentPrice(symbol);
                          String displayValue = String.format("%.6f", value);
                          return message
                              .getChannel()
                              .flatMap(
                                  channel -> channel.createMessage(symbol + ": " + displayValue));
                        }
                      }

                      return Mono.empty();
                    }))
        .block();
  }
}
