package com.cureforoptimism.cbot.discord.command;

import com.cureforoptimism.cbot.Constants;
import com.cureforoptimism.cbot.service.CoinGeckoService;
import com.litesoftwares.coingecko.domain.Coins.CoinFullData;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class PriceCommand implements CbotCommand {
  final Document.OutputSettings outputSettings;
  final CoinGeckoService coinGeckoService;

  public PriceCommand(CoinGeckoService coinGeckoService) {
    this.coinGeckoService = coinGeckoService;
    this.outputSettings = new Document.OutputSettings();
    outputSettings.prettyPrint(false);
  }

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

      final CoinFullData coinFullData = coinGeckoService.getFullCoinData(symbol);
      final String description =
          "$"
              + coinGeckoService.getReliableTicker(coinFullData.getTickers()).getLast()
              + "\n24h "
              + Constants.DECIMAL_FMT_TWO_PRECISION.format(
                  coinFullData.getMarketData().getPriceChangePercentage24h())
              + "% ($"
              + Constants.DECIMAL_FMT_TWO_PRECISION.format(
                  coinFullData.getMarketData().getPriceChange24h())
              + ")"
              + "\n7d  "
              + Constants.DECIMAL_FMT_TWO_PRECISION.format(
                  coinFullData.getMarketData().getPriceChangePercentage7d())
              + "%";
      return message
          .getChannel()
          .flatMap(
              channel ->
                  channel.createMessage(
                      EmbedCreateSpec.builder()
                          .author(
                              coinFullData.getName()
                                  + " - Rank #"
                                  + coinFullData.getMarketData().getMarketCapRank(),
                              null,
                              coinFullData.getImage().getSmall())
                          .title(description)
                          .build()));
    }

    return Mono.empty();
  }
}
