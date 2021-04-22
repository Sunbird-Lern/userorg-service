package org.sunbird.user.util;

import org.junit.Assert;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.sunbird.common.models.util.JsonKey;

@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*"
})
public class AssociationMechanismEnumTest {
  @Test
  public void testAssociationMechanismEnumValue() {
    Assert.assertEquals(1, AssociationMechanismEnum.SSO.getValue());
    Assert.assertEquals(2, AssociationMechanismEnum.SELF_DECLARATION.getValue());
    Assert.assertEquals(3, AssociationMechanismEnum.SYSTEM_UPLOAD.getValue());
  }

  @Test
  public void testAssociationMechanismEnumType() {
    Assert.assertEquals("sso", AssociationMechanismEnum.SSO.getType());
    Assert.assertEquals("self_Declaration", AssociationMechanismEnum.SELF_DECLARATION.getType());
    Assert.assertEquals("system_Upload", AssociationMechanismEnum.SYSTEM_UPLOAD.getType());
  }

  @Test
  public void testGetValueByType() {
    String type = JsonKey.SELF_DECLARATION;
    int value = AssociationMechanismEnum.getValueByType(type);
    Assert.assertEquals(2, value);
  }
}
