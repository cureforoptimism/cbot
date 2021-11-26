package com.cureforoptimism.cbot.discord.command;

import com.cureforoptimism.cbot.Constants;
import com.cureforoptimism.cbot.service.CoinGeckoService;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import java.math.BigDecimal;
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
  public String getDescription() {
    return "Gets current token value in USD. Usage: <token>. Example: `cbot price eth`";
  }

  @Override
  public Mono<Message> handle(MessageCreateEvent event, long userId, long guildId) {
    Message message = event.getMessage();
    String[] parts = message.getContent().split(" ");

    // TODO: May as well do multiple token fetches
    if (parts.length == 3) {
      String symbol = parts[2];
      BigDecimal value = coinGeckoService.getCurrentPrice(symbol);
      String displayValue = Constants.DECIMAL_FMT_DEFAULT.format(value);

      return message
          .getChannel()
          .flatMap(channel -> channel.createMessage(symbol.toUpperCase() + ": " + displayValue));
    }

    return Mono.empty();
  }
}
