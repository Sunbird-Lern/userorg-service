/** */
package org.sunbird.common.models.util.datasecurity.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;
import org.sunbird.common.models.util.datasecurity.OneWayHashing;

/** @author Manzarul */
public class OnWayhashingTest {
  public static String data = "test1234$5";

  @Test
  public void validateDataHashingSuccess() {
    String encryptval = OneWayHashing.encryptVal("test1234$5");
    Assert.assertNotEquals(encryptval.length(), 0);
    assertEquals(encryptval, OneWayHashing.encryptVal(data));
  }

  @Test
  public void validateDataHashingFailure() {
    assertEquals(OneWayHashing.encryptVal(null).length(), 0);
  }

  @Test
  public void validateDataHashingWithEmptyKey() {
    Assert.assertNotEquals((OneWayHashing.encryptVal("")).length(), 0);
  }
}
