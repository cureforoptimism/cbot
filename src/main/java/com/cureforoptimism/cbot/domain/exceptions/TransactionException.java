package com.cureforoptimism.cbot.domain.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class TransactionException extends Exception {
  @Getter final String message;
}
