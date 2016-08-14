package com.truward.semantic.id;

import com.truward.semantic.id.exception.IdParsingException;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Objects;

/**
 * Default implementation of {@link IdCodec} that provides semantic ID encoding.
 *
 * @author Alexander Shabanov
 */
public abstract class SemanticIdCodec implements IdCodec {

  /**
   * Characters, that form Base32 encoding-decoding table.
   * Excluded characters are selected to avoid similarity to the other ones,
   * such as 'l' which is visually similar to '1', 'o' which is similar to '0', 'u' which is similar to 'v'.
   */
  private static final byte[] BASE32_CHARS = {
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'j', 'k',
    'm', 'n', 'p', 'q', 'r', 's', 't', 'v', 'w', 'x',
    'y', 'z'
  };

  /**
   * A map of characters to integers, where index matches ASCII character and value is
   * either negative - which designates invalid character
   * or zero or positive - which designates numeric value, associated with the corresponding character digit.
   */
  private static final byte[] BASE32_CHAR_TO_INT_MAP = new byte[128];

  /**
   * Count of digits in long encoded in base32 which is equal to <pre>ceil(64 / 5) == 13</pre>.
   */
  private static final int BASE32_ENCODED_LONG_MAX_SIZE = 13;

  // Static initializer block for {@link #BASE32_CHAR_TO_INT_MAP}
  static {
    Arrays.fill(BASE32_CHAR_TO_INT_MAP, (byte) -1);
    for (int charIndex = 0; charIndex < BASE32_CHARS.length; ++charIndex) {
      final char ch = (char) BASE32_CHARS[charIndex];
      BASE32_CHAR_TO_INT_MAP[Character.toLowerCase(ch)] = (byte) charIndex;
      BASE32_CHAR_TO_INT_MAP[Character.toUpperCase(ch)] = (byte) charIndex;
    }
  }

  private final String serviceName;

  private SemanticIdCodec(@Nonnull String serviceName) {
    Objects.requireNonNull(serviceName, "serviceName");
    if (serviceName.isEmpty()) {
      throw new IllegalArgumentException("serviceName cannot be empty");
    }
    this.serviceName = serviceName.toLowerCase();
  }

  /**
   * Creates new instance of codec for given service.
   *
   * @param serviceName Service name, the newly created codec should be associated with
   * @return New instance of codec, associated with given service name
   */
  @Nonnull
  public static SemanticIdCodec forService(@Nonnull String serviceName) {
    return new SemanticIdCodecWithServiceName(serviceName);
  }

  /**
   * Creates new instance of codec, associated with a given entity name.
   *
   * @param entityName Entity name, associated with this encoders, e.g. <pre>user</pre> or <pre>item</pre>.
   * @return New instance of codec, associated with given entity name
   */
  @Nonnull
  public final SemanticIdCodec withEntityName(@Nonnull String entityName) {
    return new SemanticIdCodecWithServiceAndEntityName(getServiceName(), entityName);
  }

  /**
   * @return Service name plus version, associated with this encoder, e.g. <pre>foo1</pre>
   */
  @Nonnull
  public final String getServiceName() {
    return serviceName;
  }

  /**
   * @return Entity name, associated with this encoders, e.g. <pre>user</pre> or <pre>item</pre>.
   */
  @Nonnull
  public abstract String getEntityName();

  @Nonnull
  @Override
  public final String encodeLong(long id) {
    final StringBuilder builder = appendPrefix(new StringBuilder(getPrefixLength() + BASE32_ENCODED_LONG_MAX_SIZE));
    appendLong(builder, id);
    return builder.toString();
  }

  @Override
  public final long decodeLong(@Nonnull String semanticId) throws IdParsingException {
    if (semanticId.length() > getPrefixLength() + BASE32_ENCODED_LONG_MAX_SIZE) {
      throw new IdParsingException("Given semanticId is too big");
    }

    if (!startsWithPrefix(semanticId)) {
      // construct message to avoid reallocations
      final StringBuilder exMsgBuilder = new StringBuilder(getPrefixLength() + semanticId.length() + 100);
      exMsgBuilder.append("semanticId=").append(semanticId).append(" does not start with prefix=");
      appendPrefix(exMsgBuilder);

      throw new IdParsingException(exMsgBuilder.toString());
    }

    try {
      return decodeLong(semanticId, getPrefixLength(), semanticId.length());
    } catch (NumberFormatException e) {
      throw new IdParsingException("id is malformed", e);
    }
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SemanticIdCodec)) return false;

    final SemanticIdCodec that = (SemanticIdCodec) o;

    return serviceName.equals(that.serviceName) && getEntityName().equals(that.getEntityName());

  }

  @Override
  public final int hashCode() {
    int result = getServiceName().hashCode();
    result = 31 * result + getEntityName().hashCode();
    return result;
  }

  @Override
  public final String toString() {
    return "SemanticIdCodec{" +
        "serviceName='" + getServiceName() + '\'' +
        ", entityName='" + getEntityName() + '\'' +
        '}';
  }

  // VisibleForTesting
  @SuppressWarnings("WeakerAccess")
  public static long decodeLong(@Nonnull CharSequence seq, int startPos, int endPos) throws NumberFormatException {
    long result = 0;

    // NOTE: start from last and go to first, see also encodeLong method
    for (int i = endPos - 1; i >= startPos; --i) {
      final char ch = seq.charAt(i);
      if (ch < BASE32_CHAR_TO_INT_MAP.length) {
        final int digit = BASE32_CHAR_TO_INT_MAP[ch];
        if (digit >= 0) {
          result = (result << 5) + ((long) digit);
          continue;
        }
      }
      throw new NumberFormatException("Illegal character at " + i + " in ID=" + seq);
    }

    return result;
  }

  // VisibleForTesting
  @SuppressWarnings("WeakerAccess")
  public static void appendLong(@Nonnull StringBuilder builder, long num) {
    if (num == 0) {
      builder.append((char) BASE32_CHARS[0]);
      return;
    }

    while (num != 0) {
      // extract next 32-bit digit, 31 = 11111b
      final int digitChar = (int) (num & 31);
      // move to the next digit
      num = num >>> 5;

      // NOTE: this is different from usual num-to-str conversion style for simplicity
      builder.append((char) BASE32_CHARS[digitChar]);
    }
  }

  //
  // Protected
  //

  protected int getPrefixLength() {
    return getServiceName().length() + 1;
  }

  @Nonnull
  protected StringBuilder appendPrefix(@Nonnull StringBuilder builder) {
    return builder.append(getServiceName()).append('.');
  }

  protected boolean startsWithPrefix(@Nonnull String semanticId) {
    final int prefixLength = getPrefixLength();
    if (semanticId.length() <= prefixLength) {
      return false;
    }

    // match serviceName plus dot
    for (int i = 0; i < serviceName.length(); ++i) {
      final char otherCh = Character.toLowerCase(semanticId.charAt(i));
      if (otherCh != serviceName.charAt(i)) {
        return false;
      }
    }
    return semanticId.charAt(serviceName.length()) == '.';
  }

  //
  // Private
  //

  private static final class SemanticIdCodecWithServiceName extends SemanticIdCodec {
    SemanticIdCodecWithServiceName(@Nonnull String serviceName) {
      super(serviceName);
    }

    @Nonnull
    @Override
    public String getEntityName() {
      return "";
    }
  }

  private static final class SemanticIdCodecWithServiceAndEntityName extends SemanticIdCodec {
    private final String entityName;

    SemanticIdCodecWithServiceAndEntityName(@Nonnull String serviceName, @Nonnull String entityName) {
      super(serviceName);
      this.entityName = Objects.requireNonNull(entityName, "entityName").toLowerCase();
    }

    @Nonnull
    @Override
    public String getEntityName() {
      return entityName;
    }

    @Override
    protected int getPrefixLength() {
      return getServiceName().length() + entityName.length() + 2;
    }

    @Nonnull
    @Override
    protected StringBuilder appendPrefix(@Nonnull StringBuilder builder) {
      return super.appendPrefix(builder).append(getEntityName()).append('.');
    }

    @Override
    protected boolean startsWithPrefix(@Nonnull String semanticId) {
      if (!super.startsWithPrefix(semanticId)) {
        return false;
      }

      final int prefixLength = getPrefixLength();
      for (int i = 0; i < getEntityName().length(); ++i) {
        final char otherCh = Character.toLowerCase(semanticId.charAt(getServiceName().length() + 1 + i));
        if (otherCh != getEntityName().charAt(i)) {
          return false;
        }
      }
      return semanticId.charAt(prefixLength - 1) == '.';
    }
  }
}
