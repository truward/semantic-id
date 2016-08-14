package com.truward.semantic.id.test;

import com.truward.semantic.id.IdCodec;
import com.truward.semantic.id.SemanticIdCodec;
import com.truward.semantic.id.exception.IdParsingException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Alexander Shabanov
 */
public class IdCodecTest {

  @Test
  public void shouldSetNames() {
    final SemanticIdCodec codec = SemanticIdCodec.forService("foo1").withEntityName("user");
    assertEquals("foo1", codec.getServiceName());
    assertEquals("user", codec.getEntityName());
  }

  @Test
  public void shouldEncodeAndDecodeIdWithSingleServiceName() {
    final IdCodec codec = SemanticIdCodec.forService("foo1");
    assertEquals("foo1.1", codec.encodeLong(1));
    assertEquals(1, codec.decodeLong("foo1.1"));
    checkTestLongs(codec);
    assertDecodeLongFails(codec, Arrays.asList("foo1.", "foo1.-", "foo1.012345678901234"));
  }

  @Test
  public void shouldEncodeAndDecodeIdWithServiceAndEntityName() {
    final IdCodec codec = SemanticIdCodec.forService("foo1").withEntityName("user");
    assertEquals("foo1.user.1", codec.encodeLong(1));
    assertEquals(1, codec.decodeLong("foo1.user.1"));
    checkTestLongs(codec);
    assertDecodeLongFails(codec, Arrays.asList("foo1.user.", "foo1.user.-", "foo1.user.12345678901234"));
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

  private static void checkTestLongs(IdCodec codec) {
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
  }
}
