package com.truward.semantic.id.test;

import com.truward.semantic.id.IdCodec;
import com.truward.semantic.id.SemanticIdCodec;
import com.truward.semantic.id.exception.IdParsingException;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * @author Alexander Shabanov
 */
public final class IdCodecTest {

  @Test
  public void shouldSetNames() {
    assertEquals(Arrays.asList("foo1", "user"), SemanticIdCodec.forPrefixNames("foo1", "user").getPrefixNames());
    assertEquals(Arrays.asList("foo2", "user"), SemanticIdCodec.forPrefixNames("Foo2", "uSeR").getPrefixNames());
    assertEquals(Collections.singletonList("foo3"), SemanticIdCodec.forPrefixNames("foo3").getPrefixNames());
    assertEquals(Collections.emptyList(), SemanticIdCodec.forPrefixNames().getPrefixNames());
  }

  @Test
  public void shouldBeAbleToDecodeIdWithServiceName() {
    final SemanticIdCodec codec1 = SemanticIdCodec.forPrefixNames("f1");
    final SemanticIdCodec codec2 = SemanticIdCodec.forPrefixNames("f2");

    assertTrue(codec1.canDecode("f1.abc"));
    assertTrue(codec2.canDecode("f2.abc"));

    assertFalse(codec1.canDecode("f2.abc"));
    assertFalse(codec2.canDecode("f1.abc"));

    assertFalse(codec1.canDecode("foo1.user.abc"));
    assertFalse(codec2.canDecode("foo2.user.abc"));
    assertFalse(codec1.canDecode("foo2.user.abc"));
    assertFalse(codec2.canDecode("foo1.user.abc"));
  }

  @Test
  public void shouldBeAbleToDecodeIdWithServiceNameAndEntityName() {
    final SemanticIdCodec codec1 = SemanticIdCodec.forPrefixNames("foo1", "user");
    final SemanticIdCodec codec2 = SemanticIdCodec.forPrefixNames("foo2", "user");

    assertTrue(codec1.canDecode("foo1.user.abc"));
    assertTrue(codec2.canDecode("foo2.user.abc"));

    assertFalse(codec1.canDecode("f1.abc"));
    assertFalse(codec2.canDecode("f2.abc"));
    assertFalse(codec1.canDecode("foo2.user.abc"));
    assertFalse(codec2.canDecode("foo1.user.abc"));
  }

  @Test
  public void shouldEncodeAndDecodeIdWithSingleServiceName() {
    final IdCodec codec = SemanticIdCodec.forPrefixNames("foo1");
    assertEquals("foo1.1", codec.encodeLong(1));
    assertTrue(codec.canDecode("foo1.1"));
    assertTrue(codec.canDecode("foo1.2"));
    assertEquals(1, codec.decodeLong("foo1.1"));
    assertFalse(codec.canDecode("foo2.1"));
    checkTestValues(codec);
    assertDecodeLongFails(codec, Arrays.asList("foo1.", "foo1.-", "foo1.012345678901234", "foo2.1"));
  }

  @Test
  public void shouldEncodeAndDecodeIdWithServiceAndEntityName() {
    final IdCodec codec = SemanticIdCodec.forPrefixNames("foo1", "account");
    assertEquals("foo1.account.1", codec.encodeLong(1));
    assertEquals(1, codec.decodeLong("foo1.account.1"));
    checkTestValues(codec);
    assertDecodeLongFails(codec, Arrays.asList("foo1.account.", "foo1.account.-", "foo1.account.12345678901234",
        "foo2.account.1", "foo1.book.1"));
  }

  @Test
  public void shouldEncodeVariadicLengthByteArrayIds() {
    final IdCodec codec = SemanticIdCodec.forPrefixNames("book1");
    final List<byte[]> ids = Arrays.asList(
        new byte[] { 0 },
        new byte[] { 1 },
        new byte[] { -1 },
        new byte[] { Byte.MAX_VALUE },
        new byte[] { Byte.MIN_VALUE },
        new byte[] { 0, 0 },
        new byte[] { 1, Byte.MAX_VALUE },
        new byte[] { Byte.MIN_VALUE, -1 },
        new byte[] { Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE },
        new byte[] { Byte.MAX_VALUE, Byte.MAX_VALUE, Byte.MAX_VALUE },
        new byte[] { 1, 1, 1, 1, 1 },
        new byte[] { 1, -1, 127, 63, -67 }
    );

    for (final byte[] id : ids) {
      final String strId = codec.encodeBytes(id);
      assertArrayEquals(id, codec.decodeBytes(strId));
    }

    // check encode/decode of the other arrays
    final Random random = new Random(34287562345L);
    for (int size = 1; size < IdCodec.MAX_BYTES_ID_SIZE; ++size) {
      final byte[] id = new byte[size];
      for (int tries = 0; tries < 10; ++tries) {
        for (int i = 0; i < size; ++i) {
          random.nextBytes(id);
        }

        final String strId = codec.encodeBytes(id);
        assertArrayEquals(id, codec.decodeBytes(strId));
      }
    }
  }

  @Test
  public void shouldDecodeIdWithEmptyPrefix() {
    assertEquals(21L, SemanticIdCodec.forPrefixNames().decodeLong("n"));
  }

  @Test(expected = IdParsingException.class)
  public void shouldDisallowInvalidIds() {
    SemanticIdCodec.forPrefixNames().decodeLong("!");
  }

  private static void assertDecodeLongFails(IdCodec codec, List<String> malformedIds) {
    for (final String malformedId : malformedIds) {
      try {
        codec.decodeLong(malformedId);
        fail("malformedId=" + malformedId + " is successfully parsed, but it shouldn't");
      } catch (IdParsingException e) {
        assertTrue(e.getMessage().length() > 0);
      }
    }
  }

  private static void checkTestValues(IdCodec codec) {
    final List<Long> testLongs = new ArrayList<>();
    testLongs.addAll(Arrays.asList(0L, 1L, 1000L, Long.MAX_VALUE, Long.MIN_VALUE, -1L, -1000L));
    final Random random = new Random(1248946974651564L);
    for (int i = 0; i < 10000; ++i) {
      testLongs.add(random.nextLong());
    }

    for (final long val : testLongs) {
      final String semanticId = codec.encodeLong(val);
      final long otherVal = codec.decodeLong(semanticId);
      final long otherVal1 = codec.decodeLong(semanticId.toUpperCase());
      final long otherVal2 = codec.decodeLong(semanticId.toLowerCase());

      assertEquals("Converted value does not match original one for " + codec, val, otherVal);
      assertEquals("[Uppercase] Converted value does not match original one for " + codec, val, otherVal1);
      assertEquals("[Lowercase] Converted value does not match original one for " + codec, val, otherVal2);
    }

    final List<UUID> testUuids = new ArrayList<>();
    testUuids.addAll(Arrays.asList(new UUID(0L, 0L), new UUID(Long.MAX_VALUE, Long.MAX_VALUE)));
    for (int i = 0; i < 1000; ++i) {
      testUuids.add(new UUID(random.nextLong(), random.nextLong()));
    }

    for (final UUID val : testUuids) {
      final String semanticId = codec.encodeUUID(val);
      final UUID otherVal = codec.decodeUUID(semanticId);
      final UUID otherVal1 = codec.decodeUUID(semanticId.toUpperCase());
      final UUID otherVal2 = codec.decodeUUID(semanticId.toLowerCase());

      assertEquals("Converted value does not match original one for " + codec, val, otherVal);
      assertEquals("[Uppercase] Converted value does not match original one for " + codec, val, otherVal1);
      assertEquals("[Lowercase] Converted value does not match original one for " + codec, val, otherVal2);
    }
  }
}
