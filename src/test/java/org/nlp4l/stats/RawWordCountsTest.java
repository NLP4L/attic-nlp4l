package org.nlp4l.stats;

import static org.junit.Assert.*;

import org.apache.lucene.util.BytesRef;
import org.junit.Test;

public class RawWordCountsTest {

  @Test
  public void testPrefixOfNull() throws Exception {
    assertFalse(RawWordCounts.prefixOf(new BytesRef("test"), null));
  }

  @Test
  public void testPrefixOf() throws Exception {
    assertTrue(RawWordCounts.prefixOf(new BytesRef("test"), new BytesRef("test")));
    assertFalse(RawWordCounts.prefixOf(new BytesRef("testLonger"), new BytesRef("test")));
    assertTrue(RawWordCounts.prefixOf(new BytesRef("test"), new BytesRef("testLonger")));
    assertFalse(RawWordCounts.prefixOf(new BytesRef("testY"), new BytesRef("testX")));
    assertTrue(RawWordCounts.prefixOf(new BytesRef(""), new BytesRef("testLonger")));
  }
}
