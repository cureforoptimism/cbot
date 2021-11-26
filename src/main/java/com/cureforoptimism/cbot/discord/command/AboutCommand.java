package com.cureforoptimism.cbot.discord.command;

import com.cureforoptimism.cbot.Constants;
import com.cureforoptimism.cbot.service.CoinGeckoService;
import com.litesoftwares.coingecko.domain.Coins.CoinFullData;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class AboutCommand implements CbotCommand {
  final Document.OutputSettings outputSettings;
  final CoinGeckoService coinGeckoService;

  public AboutCommand(CoinGeckoService coinGeckoService) {
    this.coinGeckoService = coinGeckoService;
    this.outputSettings = new Document.OutputSettings();
    outputSettings.prettyPrint(false);
  }

  @Override
  public String getName() {
    return "about";
  }

  @Override
  public String getDescription() {
    return "Shows some boring-ass details about coins, but you do you, boo. Usage <token>. Example: `!cbot about eth`";
  }

  @Override
  public Mono<Message> handle(MessageCreateEvent event, long userId, long guildId) {
    Message message = event.getMessage();
    String[] parts = message.getContent().split(" ");

    if (parts.length == 3) {
      String symbol = parts[2];

      final CoinFullData coinFullData = coinGeckoService.getFullCoinData(symbol);
      final String description =
          "$"
              + coinFullData.getTickers().get(0).getLast()
              + "\n24h "
              + Constants.DECIMAL_FMT_TWO_PRECISION.format(
                  coinFullData.getMarketData().getPriceChangePercentage24h())
              + "% ($"
              + Constants.DECIMAL_FMT_TWO_PRECISION.format(
                  coinFullData.getMarketData().getPriceChange24h())
              + ")";

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
                          .description(
                              Jsoup.clean(
                                  coinFullData.getDescription().get("en"),
                                  "",
                                  Safelist.none(),
                                  outputSettings))
                          .build()));
    }

    return Mono.empty();
  }
}
