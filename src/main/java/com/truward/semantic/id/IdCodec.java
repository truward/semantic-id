package com.truward.semantic.id;

import com.truward.semantic.id.exception.IdParsingException;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

/**
 * Abstraction for encoding and decoding IDs.
 *
 * @author Alexander Shabanov
 */
@ParametersAreNonnullByDefault
public interface IdCodec {
  /**
   * Max size of encoded key byte array.
   */
  int MAX_BYTES_ID_SIZE = 256;

  /**
   * Returns true, if ID can be decoded with this codec.
   *
   * @param semanticId ID to be decoded
   * @return true, if ID can be decoded with this codec
   */
  boolean canDecode(String semanticId);

  /**
   * Encodes long into opaque string representation which later can be decoded with {@link #decodeLong(String)} method.
   *
   * @param id ID, that should be encoded to semantic ID
   * @return Encoded ID
   */
  String encodeLong(long id);

  /**
   * Decodes opaque semantic ID, previously encoded with {@link #encodeLong(long)} method.
   *
   * @param semanticId Previously encoded semantic ID, character case may vary
   * @return Associated long ID
   * @throws IdParsingException Parsing exception
   */
  long decodeLong(String semanticId) throws IdParsingException;

  /**
   * Encodes byte values into opaque string representation which later can be decoded with {@link #decodeBytes(String)}
   * method.
   *
   * @param id ID, that should be encoded to semantic ID
   * @return Encoded ID
   */
  String encodeBytes(byte[] id);

  /**
   * Decodes opaque semantic ID, previously encoded with {@link #encodeBytes(byte[])} method.
   *
   * @param semanticId Previously encoded semantic ID, character case may vary
   * @return Associated bytes ID
   * @throws IdParsingException Parsing exception
   */
  default byte[] decodeBytes(String semanticId) throws IdParsingException {
    return decodeBytes(semanticId, -1);
  }

  /**
   * Decodes opaque semantic ID, previously encoded with {@link #encodeBytes(byte[])} method.
   *
   * @param semanticId Previously encoded semantic ID, character case may vary
   * @param expectedByteIdSize Expected length of the bytes body, may be negative which indicates that decoded
   *                           byte array may be of variadic length not exceeding {@link #MAX_BYTES_ID_SIZE}
   * @return Associated bytes ID
   * @throws IdParsingException Parsing exception
   */
  byte[] decodeBytes(String semanticId, int expectedByteIdSize) throws IdParsingException;

  /**
   * Encodes byte values into opaque string representation which later can be decoded with {@link #decodeUUID(String)}}
   * method.
   *
   * @param id ID, that should be encoded to semantic ID
   * @return Encoded ID
   */
  String encodeUUID(UUID id);

  /**
   * Decodes opaque semantic ID, previously encoded with {@link #encodeUUID(UUID)}} method.
   *
   * @param semanticId Previously encoded semantic ID, character case may vary
   * @return Associated UUID
   * @throws IdParsingException Parsing exception
   */
  UUID decodeUUID(String semanticId) throws IdParsingException;
}
