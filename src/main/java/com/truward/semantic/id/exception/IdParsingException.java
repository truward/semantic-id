package com.truward.semantic.id.exception;

/**
 * Exception thrown on failure to parse a given semantic ID.
 *
 * @author Alexander Shabanov
 */
public class IdParsingException extends IdCodingException {

  public IdParsingException(String message) {
    super(message);
  }

  public IdParsingException(String message, Throwable cause) {
    super(message, cause);
  }
}
