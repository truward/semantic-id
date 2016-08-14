package com.truward.semantic.id.exception;

/**
 * Base class for exceptions thrown during encoding/decoding IDs.
 *
 * @author Alexander Shabanov
 */
public abstract class IdCodingException extends RuntimeException {

  public IdCodingException() {
  }

  public IdCodingException(String message) {
    super(message);
  }

  public IdCodingException(String message, Throwable cause) {
    super(message, cause);
  }

  public IdCodingException(Throwable cause) {
    super(cause);
  }

  public IdCodingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
