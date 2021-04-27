package org.sunbird.user.service;

import org.junit.Assert;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;

@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*"
})
public class AssociationMechanismTest {
  @Test
  public void associationMechanismTest() {
    Assert.assertEquals(1, AssociationMechanism.SSO);
    Assert.assertEquals(2, AssociationMechanism.SELF_DECLARATION);
    Assert.assertEquals(4, AssociationMechanism.SYSTEM_UPLOAD);
  }

  @Test
  public void isAssociationTypeTest() {
    AssociationMechanism associationMechanism = new AssociationMechanism();
    associationMechanism.setAssociationType(1);
    associationMechanism.setAssociationName("SSO");
    String name = associationMechanism.getAssociationName();
    Assert.assertEquals("SSO", name);
    Assert.assertTrue(associationMechanism.isAssociationType(AssociationMechanism.SSO));
  }
}
