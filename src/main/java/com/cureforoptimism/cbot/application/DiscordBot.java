package com.cureforoptimism.cbot.application;

import com.cureforoptimism.cbot.discord.listener.CbotCommandListener;
import com.cureforoptimism.cbot.service.TokenService;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@AllArgsConstructor
public class DiscordBot implements ApplicationRunner {
  final ApplicationContext context;
  final TokenService tokenService;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    CbotCommandListener cbotCommandListener = new CbotCommandListener(context);

    DiscordClient.create(tokenService.getDiscordToken())
        .gateway()
        .setInitialPresence(p -> ClientPresence.online(ClientActivity.playing("c0d3z...")))
        .withGateway(
            gatewayClient -> {
              final Publisher<?> messageEvent =
                  gatewayClient.on(MessageCreateEvent.class, cbotCommandListener::handle);

              return Mono.when(messageEvent);
            })
        .block();
  }
}
