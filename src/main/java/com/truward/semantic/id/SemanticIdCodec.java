package com.truward.semantic.id;

import com.truward.semantic.id.exception.IdParsingException;
import com.truward.semantic.id.util.PadlessBase32;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

import static com.truward.semantic.id.util.PadlessBase32.appendLong;
import static com.truward.semantic.id.util.PadlessBase32.getEncodedSize;

/**
 * Default implementation of {@link IdCodec} that provides semantic ID encoding.
 *
 * @author Alexander Shabanov
 */
@ParametersAreNonnullByDefault
public final class SemanticIdCodec implements IdCodec {

  /**
   * Separator for most and least significant bits in UUID keys.
   */
  private static final char UUID_BITS_SEPARATOR = '$';

  /**
   * Separator between ID components
   */
  private static final char PREFIX_SEPARATOR = '-';

  /**
   * Count of digits in UUID encoded in base32 which is two longs + 1,
   * where 1 counts as a placeholder character that separates UUID most and least significant bits.
   */
  private static final int BASE32_ENCODED_UUID_MAX_SIZE = PadlessBase32.ENCODED_LONG_MAX_SIZE * 2 + 1;

  /**
   * Max size of encoded byte array.
   */
  private static final int BASE32_ENCODED_BYTES_MAX_SIZE = MAX_BYTES_ID_SIZE * PadlessBase32.BASE_BITS;


  private final List<String> names;

  private SemanticIdCodec(String... names) {
    //noinspection ConstantConditions
    if (names == null) {
      throw new IllegalArgumentException("serviceNames");
    }

    if (names.length == 0) {
      this.names = Collections.emptyList();
      return;
    }

    final String[] namesCopy = new String[names.length];
    //noinspection ManualArrayCopy
    for (int i = 0; i < names.length; ++i) {
      namesCopy[i] = names[i]; // retain casing
    }
    this.names = names.length > 1 ? Arrays.asList(namesCopy) : Collections.singletonList(namesCopy[0]);
  }

  /**
   * Creates new instance of codec for given service.
   *
   * @param names Prefix names, the newly created codec should be associated with
   * @return New instance of codec, associated with given service name
   */
  public static SemanticIdCodec forPrefixNames(String... names) {
    return new SemanticIdCodec(names);
  }

  /**
   * @return Service names plus version, associated with this encoder, e.g. <pre>foo1</pre>
   */
  public final List<String> getPrefixNames() {
    return names;
  }

  @Override
  public final String encodeLong(long id) {
    final int capacity = getPrefixLength() + PadlessBase32.ENCODED_LONG_MAX_SIZE;
    final StringBuilder builder = appendPrefix(new StringBuilder(capacity));
    appendLong(builder, id);
    assert capacity == builder.capacity();
    return builder.toString();
  }

  @Override
  public final long decodeLong(String semanticId) throws IdParsingException {
    checkCanDecodeId(semanticId, PadlessBase32.ENCODED_LONG_MAX_SIZE);

    try {
      return PadlessBase32.decodeLong(semanticId, getPrefixLength(), semanticId.length());
    } catch (IllegalArgumentException e) {
      throw new IdParsingException("id is malformed", e);
    }
  }

  @Override
  public boolean canDecode(String semanticId) {
    final int prefixLength = getPrefixLength();
    if (semanticId.length() <= prefixLength) {
      return false;
    }

    // match serviceName plus dot
    int pos = 0;
    for (int k = 0; k < getPrefixNames().size(); ++k) {
      final String prefixName = getPrefixNames().get(k);
      for (int i = 0; i < prefixName.length(); ++i, ++pos) {
        final char otherCh = Character.toLowerCase(semanticId.charAt(pos));
        if (otherCh != Character.toLowerCase(prefixName.charAt(i))) {
          return false;
        }
      }

      // match trailing dot
      if (semanticId.charAt(pos) != PREFIX_SEPARATOR) {
        return false;
      }
      ++pos;
    }

    return isIdBodyValid(semanticId, pos);
  }

  @Override
  public String encodeBytes(byte[] id) {
    if (id.length == 0) {
      throw new IllegalArgumentException("ID is empty");
    }

    if (id.length > MAX_BYTES_ID_SIZE) {
      throw new IllegalArgumentException("ID is too big");
    }

    final int capacity = getPrefixLength() + getEncodedSize(id.length);
    final StringBuilder builder = appendPrefix(new StringBuilder(capacity));
    PadlessBase32.appendBytes(builder, id);
    assert capacity == builder.capacity(); // Test only: make sure that capacity is still the same
    return builder.toString();
  }

  @Override
  public String encodeRandomBytes(Random random, int byteSize) {
    if (byteSize <= 0) {
      throw new IllegalArgumentException("byteSize");
    }

    final int encodedSize = getEncodedSize(byteSize);
    final int capacity = getPrefixLength() + encodedSize;
    final StringBuilder builder = appendPrefix(new StringBuilder(capacity));
    for (int i = 0; i < encodedSize; ++i) {
      final char base32Char = PadlessBase32.getBaseChar(random.nextInt(PadlessBase32.BASE));
      builder.append(base32Char);
    }
    assert capacity == builder.capacity();
    return builder.toString();
  }

  @Override
  public byte[] decodeBytes(String semanticId, int expectedByteIdSize) throws IdParsingException {
    if (expectedByteIdSize == 0) {
      throw new IllegalArgumentException("expectedByteIdSize");
    }

    checkCanDecodeId(semanticId, expectedByteIdSize > 0 ? expectedByteIdSize : BASE32_ENCODED_BYTES_MAX_SIZE);
    return PadlessBase32.decodeBytes(semanticId, getPrefixLength(), semanticId.length());
  }

  @Override
  public String encodeUUID(UUID id) {
    final int capacity = getPrefixLength() + BASE32_ENCODED_UUID_MAX_SIZE;
    final StringBuilder builder = appendPrefix(new StringBuilder(capacity));
    appendLong(builder, id.getMostSignificantBits());
    builder.append(UUID_BITS_SEPARATOR);
    appendLong(builder, id.getLeastSignificantBits());

    assert capacity == builder.capacity();

    return builder.toString();
  }

  @Override
  public UUID decodeUUID(String semanticId) throws IdParsingException {
    checkCanDecodeId(semanticId, BASE32_ENCODED_UUID_MAX_SIZE);

    try {
      final int idBodyStart = getPrefixLength();
      final int midLen = semanticId.indexOf(UUID_BITS_SEPARATOR, idBodyStart + 1);
      if (midLen < 0) {
        throw new IdParsingException("There is no separator for most/least UUID bits");
      }

      final long mostSigBits = PadlessBase32.decodeLong(semanticId, idBodyStart, midLen);
      final long leastSigBits = PadlessBase32.decodeLong(semanticId, midLen + 1, semanticId.length());

      return new UUID(mostSigBits, leastSigBits);
    } catch (NumberFormatException e) {
      throw new IdParsingException("semanticId is malformed", e);
    }
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SemanticIdCodec)) return false;

    final SemanticIdCodec that = (SemanticIdCodec) o;

    return getPrefixNames().equals(that.getPrefixNames());

  }

  @Override
  public final int hashCode() {
    return getPrefixNames().hashCode();
  }

  @Override
  public final String toString() {
    return appendPrefix(new StringBuilder(100)).append("<codec>").toString();
  }

  //
  // Private
  //

  private int getPrefixLength() {
    int result = 0;
    for (int i = 0; i < getPrefixNames().size(); ++i) {
      result = result + getPrefixNames().get(i).length() + 1;
    }
    return result;
  }

  private StringBuilder appendPrefix(StringBuilder builder) {
    for (int i = 0; i < getPrefixNames().size(); ++i) {
      builder.append(getPrefixNames().get(i)).append(PREFIX_SEPARATOR);
    }
    return builder;
  }

  private void checkCanDecodeId(String semanticId, int maxLength) throws IdParsingException {
    if (semanticId.length() > getPrefixLength() + maxLength) {
      throw new IdParsingException("Given semanticId is too big");
    }

    if (!canDecode(semanticId)) {
      // construct message to avoid reallocations
      final StringBuilder exMsgBuilder = new StringBuilder(getPrefixLength() + semanticId.length() + 30);
      exMsgBuilder.append("semanticId=").append(semanticId).append(" does not start with prefix=");
      appendPrefix(exMsgBuilder);

      throw new IdParsingException(exMsgBuilder.toString());
    }
  }

  private boolean isIdBodyValid(CharSequence semanticId, int prefixLength) {
    if (semanticId.length() <= prefixLength) {
      return false;
    }

    final int len = semanticId.length();
    for (int i = prefixLength; i < len; ++i) {
      final char ch = semanticId.charAt(i);
      if (ch == PREFIX_SEPARATOR) {
        return false; // disallow prefix separators in ID
      }
    }

    return true;
  }
}
