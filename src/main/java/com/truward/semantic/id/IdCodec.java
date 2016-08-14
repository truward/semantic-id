package com.truward.semantic.id;

import com.truward.semantic.id.exception.IdParsingException;

import javax.annotation.Nonnull;

/**
 * Abstraction for encoding and decoding IDs.
 *
 * @author Alexander Shabanov
 */
public interface IdCodec {

  /**
   * Encodes long into opaque string representation which later can be decoded with {@link #decodeLong(String)} method.
   *
   * @param id ID, that should be encoded to semantic ID
   * @return Encoded ID
   */
  @Nonnull
  String encodeLong(long id);

  /**
   * Decodes opaque semantic ID, previously encoded with {@link #encodeLong(long)} method.
   *
   * @param semanticId Previously encoded semantic ID
   * @return Associated long ID
   * @throws IdParsingException ID
   */
  long decodeLong(@Nonnull String semanticId) throws IdParsingException;
}
