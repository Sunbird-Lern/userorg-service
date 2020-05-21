package org.sunbird.common.models.util;

import org.junit.Assert;
import org.junit.Test;

public class SlugTest {

  @Test
  public void createSlugWithNullValue() {
    String slug = Slug.makeSlug(null, true);
    Assert.assertEquals(null, slug);
  }

  @Test
  public void createSlug() {
    String val = "NTP@#Test";
    String slug = Slug.makeSlug(val, true);
    Assert.assertEquals("ntptest", slug);
  }

  @Test
  public void removeDuplicateChar() {
    String val = Slug.removeDuplicateChars("ntpntest");
    Assert.assertEquals("ntpes", val);
  }
}
