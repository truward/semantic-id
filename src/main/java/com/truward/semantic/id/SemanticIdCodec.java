package com.truward.semantic.id;

import com.truward.semantic.id.exception.IdParsingException;
import com.truward.semantic.id.util.SimpleBase32;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.UUID;

import static com.truward.semantic.id.util.SimpleBase32.appendLong;

/**
 * Default implementation of {@link IdCodec} that provides semantic ID encoding.
 *
 * @author Alexander Shabanov
 */
@ParametersAreNonnullByDefault
public abstract class SemanticIdCodec implements IdCodec {

  /**
   * Separator for most and least significant bits in UUID keys.
   */
  private static final char UUID_BITS_SEPARATOR = '-';

  /**
   * Count of digits in UUID encoded in base32 which is two longs + 1,
   * where 1 counts as a placeholder character that separates UUID most and least significant bits.
   */
  private static final int BASE32_ENCODED_UUID_MAX_SIZE = SimpleBase32.ENCODED_LONG_MAX_SIZE * 2 + 1;

  /**
   * Max size of encoded byte array.
   */
  private static final int BASE32_ENCODED_BYTES_MAX_SIZE = MAX_BYTES_ID_SIZE * SimpleBase32.BASE_BITS;


  private final String serviceName;

  private SemanticIdCodec(String serviceName) {
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
  public static SemanticIdCodec forService(String serviceName) {
    return new SemanticIdCodecWithServiceName(serviceName);
  }

  /**
   * Creates new instance of codec, associated with a given entity name.
   *
   * @param entityName Entity name, associated with this encoders, e.g. <pre>user</pre> or <pre>item</pre>.
   * @return New instance of codec, as1ociated with given entity name
   */
  public final SemanticIdCodec withEntityName(String entityName) {
    return new SemanticIdCodecWithServiceAndEntityName(getServiceName(), entityName);
  }

  /**
   * @return Service name plus version, associated with this encoder, e.g. <pre>foo1</pre>
   */
  public final String getServiceName() {
    return serviceName;
  }

  /**
   * @return Entity name, associated with this encoders, e.g. <pre>user</pre> or <pre>item</pre>.
   */
  public abstract String getEntityName();

  @Override
  public final String encodeLong(long id) {
    final StringBuilder builder = appendPrefix(new StringBuilder(
        getPrefixLength() + SimpleBase32.ENCODED_LONG_MAX_SIZE));
    appendLong(builder, id);
    return builder.toString();
  }

  @Override
  public final long decodeLong(String semanticId) throws IdParsingException {
    checkCanDecodeId(semanticId, SimpleBase32.ENCODED_LONG_MAX_SIZE);

    try {
      return SimpleBase32.decodeLong(semanticId, getPrefixLength(), semanticId.length());
    } catch (IllegalArgumentException e) {
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
  public boolean canDecode(String semanticId) {
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

    return semanticId.charAt(serviceName.length()) == '.' &&
        isIdBodyValid(semanticId, prefixLength);
  }

  @Override
  public String encodeBytes(byte[] id) {
    if (id.length == 0) {
      throw new IllegalArgumentException("ID is empty");
    }

    if (id.length > MAX_BYTES_ID_SIZE) {
      throw new IllegalArgumentException("ID is too big");
    }

    final StringBuilder builder = appendPrefix(new StringBuilder(
        getPrefixLength() + (id.length + 1) * SimpleBase32.BASE_BITS));
    SimpleBase32.appendBytes(builder, id);
    return builder.toString();
  }

  @Override
  public byte[] decodeBytes(String semanticId, int expectedByteIdSize) throws IdParsingException {
    checkCanDecodeId(semanticId, BASE32_ENCODED_BYTES_MAX_SIZE);
    return SimpleBase32.decodeBytes(semanticId, getPrefixLength(), semanticId.length());
  }

  @Override
  public String encodeUUID(UUID id) {
    final StringBuilder builder = appendPrefix(new StringBuilder(
        getPrefixLength() + BASE32_ENCODED_UUID_MAX_SIZE));
    appendLong(builder, id.getMostSignificantBits());
    builder.append(UUID_BITS_SEPARATOR);
    appendLong(builder, id.getLeastSignificantBits());

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

      final long mostSigBits = SimpleBase32.decodeLong(semanticId, idBodyStart, midLen);
      final long leastSigBits = SimpleBase32.decodeLong(semanticId, midLen + 1, semanticId.length());

      return new UUID(mostSigBits, leastSigBits);
    } catch (NumberFormatException e) {
      throw new IdParsingException("semanticId is malformed", e);
    }
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

  //
  // Protected
  //

  protected int getPrefixLength() {
    return getServiceName().length() + 1;
  }

  protected StringBuilder appendPrefix(StringBuilder builder) {
    return builder.append(getServiceName()).append('.');
  }

  //
  // Private
  //

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

  final boolean isIdBodyValid(CharSequence semanticId, int prefixLength) {
    if (semanticId.length() <= prefixLength) {
      return false;
    }

    final int len = semanticId.length();
    for (int i = prefixLength; i < len; ++i) {
      final char ch = semanticId.charAt(i);
      if (ch == '.') {
        return false; // disallow dots in ID
      }
    }

    return true;
  }

  private static final class SemanticIdCodecWithServiceName extends SemanticIdCodec {
    SemanticIdCodecWithServiceName(String serviceName) {
      super(serviceName);
    }

    @Override
    public String getEntityName() {
      return "";
    }
  }

  private static final class SemanticIdCodecWithServiceAndEntityName extends SemanticIdCodec {
    private final String entityName;

    SemanticIdCodecWithServiceAndEntityName(String serviceName, String entityName) {
      super(serviceName);
      this.entityName = Objects.requireNonNull(entityName, "entityName").toLowerCase();
    }

    @Override
    public String getEntityName() {
      return entityName;
    }

    @Override
    protected int getPrefixLength() {
      return getServiceName().length() + entityName.length() + 2;
    }

    @Override
    protected StringBuilder appendPrefix(StringBuilder builder) {
      return super.appendPrefix(builder).append(getEntityName()).append('.');
    }

    @Override
    public boolean canDecode(String semanticId) {
      if (!super.canDecode(semanticId)) {
        return false;
      }

      final int prefixLength = getPrefixLength();
      for (int i = 0; i < getEntityName().length(); ++i) {
        final char otherCh = Character.toLowerCase(semanticId.charAt(getServiceName().length() + 1 + i));
        if (otherCh != getEntityName().charAt(i)) {
          return false;
        }
      }
      return semanticId.charAt(prefixLength - 1) == '.' && isIdBodyValid(semanticId, prefixLength);
    }
  }
}
