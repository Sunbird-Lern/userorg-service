package org.sunbird.user.util;

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

  @Test
  public void appendAssociationTypeTest() {
    AssociationMechanism associationMechanism = new AssociationMechanism();
    associationMechanism.setAssociationType(1);
    associationMechanism.appendAssociationType(AssociationMechanism.SELF_DECLARATION);
    Assert.assertEquals(3, associationMechanism.getAssociationType());
  }

  @Test
  public void removeAssociationTypeTest() {
    AssociationMechanism associationMechanism = new AssociationMechanism();
    associationMechanism.setAssociationType(3);
    associationMechanism.removeAssociationType(AssociationMechanism.SELF_DECLARATION);
    Assert.assertEquals(1, associationMechanism.getAssociationType());
  }
}
