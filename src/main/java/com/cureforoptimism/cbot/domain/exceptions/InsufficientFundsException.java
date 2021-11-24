package com.cureforoptimism.cbot.domain.exceptions;

public class InsufficientFundsException extends TransactionException {
  public InsufficientFundsException() {
    super("insufficient funds");
  }
}
