package com.cureforoptimism.cbot;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public class Constants {
  public static final BigDecimal DEFAULT_STARTING_USD = new BigDecimal("20000.0");
  public static final long COIN_GECKO_CACHE_EXPIRY = 30 * 100; // 30 seconds
  public static final BigDecimal BUY_SELL_FEE_PCT = new BigDecimal("0.0035");
  public static final DecimalFormat DECIMAL_FMT_DEFAULT = new DecimalFormat("#.################");
  public static final DecimalFormat DECIMAL_FMT_TWO_PRECISION = new DecimalFormat("#.##");
}
