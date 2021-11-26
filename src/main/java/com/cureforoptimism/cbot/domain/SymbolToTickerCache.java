package com.cureforoptimism.cbot.domain;

import java.util.Date;
import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@NoArgsConstructor
@AllArgsConstructor
public class SymbolToTickerCache {
  @Id String symbol;

  @Getter String ticker;

  @CreationTimestamp
  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "create_date")
  // May as well have a created column, just in case I ever want to expire this.
  private Date created;

  public SymbolToTickerCache(String symbol, String ticker) {
    this.ticker = ticker;
    this.symbol = symbol;
  }
}
