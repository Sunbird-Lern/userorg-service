package util;

import org.junit.Assert;
import org.junit.Test;

public class ActorsTest {
  @Test
  public void test() {
    for (ACTORS actors : ACTORS.values()) {
      Assert.assertNotNull(actors.getActorClass());
      Assert.assertNotNull(actors.getActorName());
    }
  }
}
