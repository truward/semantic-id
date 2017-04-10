package com.truward.semantic.id.util;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;

/**
 * Simple case insensitive *AND* padless Base32 encoding/decoding utility class.
 * <p>
 * Padless feature allows creating smaller encoding sequence as it doesn't require encoder to append padding
 * characters.
 * For example, classic Base32 yields <code>IFBEGRCFIY======</code> for a string <code>ABCDEF</code> and this string
 * shows that there can be up to 6 of padding characters (see also RFC4648).
 * </p>
 * <p>
 * This encoder ignores padding and as an ugly consequence of this encoded Base32 strings cannot be appended safely
 * unless encoded bytes length is divisible by 5. This is acceptable though if appending encoded sequences is not
 * required which is usually the case for things like encoding IDs or arrays for storing in the database.
 * If appending streams is required, given streams shall be decoded first, decoded byte sequence appended and then
 * encoded as a whole again.
 * </p>
 *
 * @author Alexander Shabanov
 */
@ParametersAreNonnullByDefault
public final class PadlessBase32 {
  private PadlessBase32() {} // hidden

  /**
   * 5 is how much bits does element in this base occupy (32 is 2^5).
   */
  public static final int BASE_BITS = 5;

  /**
   * Mask to apply to byte sequence to yield a number between 0 and base maximum.
   */
  private static final int BASE_MASK = (1 << BASE_BITS) - 1;

  /**
   * Count of digits in long encoded in base32 which is equal to <pre>ceil(64 / 5) == 13</pre>.
   */
  public static final int ENCODED_LONG_MAX_SIZE = (Long.SIZE + BASE_BITS - 1) / BASE_BITS;

  /**
   * Appends long and converts it into Base32 to the given string builder.
   *
   * @param builder String builder, to append number to
   * @param num Number to append
   */
  public static void appendLong(StringBuilder builder, long num) {
    if (num == 0) {
      builder.append((char) CHARS[0]);
      return;
    }

    while (num != 0) {
      // extract next 32-bit digit, 31 = 11111b
      final int digitChar = (int) (num & BASE_MASK);
      // move to the next digit
      num = num >>> BASE_BITS;

      // NOTE: this is different from usual num-to-str conversion style for simplicity
      builder.append((char) CHARS[digitChar]);
    }
  }

  /**
   * Decodes long number from the given sequence starting from the given startPos and ending with the character,
   * preceding endPos.
   *
   * @param seq Sequence, to decode long number from
   * @param startPos Start position
   * @param endPos End position
   * @return Decoded long number
   * @throws NumberFormatException On numeric
   */
  public static long decodeLong(CharSequence seq, int startPos, int endPos) throws NumberFormatException {
    long result = 0;

    // NOTE: start from last and go to first, see also encodeLong method
    for (int i = endPos - 1; i >= startPos; --i) {
      result = (result << BASE_BITS) + ((long) getBaseCharCode(seq, i));
    }

    return result;
  }

  // TODO: simplify
  public static void appendBytes(StringBuilder builder, byte[] body) {
    final int bodyBits = Byte.SIZE * body.length;
    final int fullBase32ElemCount = bodyBits / BASE_BITS;
    final int partialBase32ElemBits = bodyBits % BASE_BITS;

    for (int i = 0; i < fullBase32ElemCount; ++i) {
      final int startPosBit = i * BASE_BITS;
      final int startPosByte = startPosBit / Byte.SIZE;
      final int offsetBitPos = startPosBit % Byte.SIZE;
      final int endBitPos = offsetBitPos + BASE_BITS;
      int elem = ((int) body[startPosByte]) & 0xff;

      int base32ElemIndex = (elem >>> offsetBitPos) & BASE_MASK; // base32 element is completely within this byte
      if (endBitPos > Byte.SIZE) {
        final int tailBitCount = (endBitPos - Byte.SIZE);
        base32ElemIndex |= (body[startPosByte + 1] & ((1 << tailBitCount) - 1)) << (BASE_BITS - tailBitCount);
      }
      builder.append((char) CHARS[base32ElemIndex]);
    }

    if (partialBase32ElemBits > 0) {
      final int lastElem = ((int) body[body.length - 1]) & 0xff;
      final int base32ElemIndex = lastElem >>> (Byte.SIZE - partialBase32ElemBits);
      builder.append((char) CHARS[base32ElemIndex]);
    }
  }

  // TODO: simplify
  public static byte[] decodeBytes(CharSequence seq, int startPos, int endPos) {
    if (startPos < 0) {
      throw new IllegalArgumentException("startPos");
    }

    if (endPos <= startPos) {
      throw new IllegalArgumentException("endPos");
    }

    final int length = endPos - startPos;
    final int byteCount = (length * BASE_BITS) / Byte.SIZE;
    final byte[] result = new byte[byteCount];

    int tailBits = 0;
    int tailBitsCount = 0;
    int resultBitsOffset = 0;

    for (int charPos = startPos, resultPos = 0; (resultPos < byteCount) && (charPos < endPos);) {
      if (tailBitsCount > 0) {
        // apply the remainder of previously processed digit
        result[resultPos] |= tailBits;
        resultBitsOffset = tailBitsCount;
        tailBitsCount = 0;
        ++charPos;
        continue;
      }

      // no tail bits, fetch new char element and decode it into base32 number
      final int base32Digit = getBaseCharCode(seq, charPos);
      int nextBitOffset = resultBitsOffset + BASE_BITS;
      int nextResultPos = resultPos;

      if (nextBitOffset > Byte.SIZE) {
        // this entire digit doesn't fits into current byte, apply only part of it
        final int headBitCount = (Byte.SIZE - resultBitsOffset);
        tailBits = base32Digit >> headBitCount;
        tailBitsCount = nextBitOffset - Byte.SIZE;
        ++nextResultPos;
        nextBitOffset = 0;
      } else if (nextBitOffset == Byte.SIZE) {
        // proceed to the next byte and next base32 element (bit bounds matched)
        ++nextResultPos;
        ++charPos;
        nextBitOffset = 0;
      } else {
        // this current character fits the entire byte and there still enough place in result byte to put something else
        ++charPos;
      }

      result[resultPos] |= (base32Digit << resultBitsOffset);
      resultPos = nextResultPos;
      resultBitsOffset = nextBitOffset;
    }

    return result;
  }

  //
  // Private
  //

  private static int getBaseCharCode(CharSequence seq, int pos) {
    final char ch = seq.charAt(pos);
    if (ch < CHAR_TO_INT_MAP.length) {
      final int digit = CHAR_TO_INT_MAP[ch];
      if (digit >= 0) {
        return digit;
      }
    }
    throw new IllegalArgumentException("Illegal character with code=" + ((int) ch) + " at position=" + pos);
  }

  /**
   * Characters, that form Base32 encoding-decoding table.
   * Excluded characters are selected to avoid similarity to the other ones,
   * such as 'l' which is visually similar to '1', 'o' which is similar to '0', 'u' which is similar to 'v'.
   */
  private static final byte[] CHARS = {
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
  private static final byte[] CHAR_TO_INT_MAP = new byte[128];

  // Static initializer block for {@link #BASE32_CHAR_TO_INT_MAP}
  static {
    Arrays.fill(CHAR_TO_INT_MAP, (byte) -1);
    for (int charIndex = 0; charIndex < CHARS.length; ++charIndex) {
      final char ch = (char) CHARS[charIndex];
      CHAR_TO_INT_MAP[Character.toLowerCase(ch)] = (byte) charIndex;
      CHAR_TO_INT_MAP[Character.toUpperCase(ch)] = (byte) charIndex;
    }
  }
}
