package com.cureforoptimism.cbot.application;

import com.cureforoptimism.cbot.discord.listener.CbotCommandListener;
import com.cureforoptimism.cbot.repository.ServerRepository;
import com.cureforoptimism.cbot.repository.UserRepository;
import com.cureforoptimism.cbot.service.CoinGeckoService;
import com.cureforoptimism.cbot.service.TokenService;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@AllArgsConstructor
public class DiscordBot implements ApplicationRunner {
  final ApplicationContext context;
  final CoinGeckoService coinGeckoService;
  final TokenService tokenService;
  final UserRepository userRepository;
  final ServerRepository serverRepository;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    CbotCommandListener cbotCommandListener = new CbotCommandListener(context);

    DiscordClient.create(tokenService.getDiscordToken())
        .withGateway(
            gatewayClient ->
                gatewayClient.on(MessageCreateEvent.class, cbotCommandListener::handle))
        .block();
  }
}
