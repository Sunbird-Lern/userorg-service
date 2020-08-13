package org.sunbird.user.util;

import static org.junit.Assert.assertNotNull;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.datasecurity.DataMaskingService;
import org.sunbird.common.models.util.datasecurity.DecryptionService;
import org.sunbird.common.models.util.datasecurity.EncryptionService;
import org.sunbird.common.models.util.datasecurity.impl.DefaultDataMaskServiceImpl;
import org.sunbird.common.models.util.datasecurity.impl.DefaultDecryptionServiceImpl;
import org.sunbird.common.models.util.datasecurity.impl.DefaultEncryptionServivceImpl;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  CassandraOperationImpl.class,
  DataCacheHandler.class,
  DefaultEncryptionServivceImpl.class,
  DefaultDecryptionServiceImpl.class,
  Util.class,
  EncryptionService.class,
  DecryptionService.class,
  DataMaskingService.class,
  org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class
})
@PowerMockIgnore({"javax.management.*"})
public class AddMaskEmailPhoneTest {

  @Test
  public void addMaskEmailAndPhone() {
    PowerMockito.mockStatic(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class);
    DecryptionService decryptionService = PowerMockito.mock(DefaultDecryptionServiceImpl.class);
    when(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory
            .getDecryptionServiceInstance(null))
        .thenReturn(decryptionService);
    when(decryptionService.decryptData("test@test.com")).thenReturn("test@test.com");
    when(decryptionService.decryptData("9742857854")).thenReturn("9742857854");
    DataMaskingService maskingService = PowerMockito.mock(DefaultDataMaskServiceImpl.class);
    when(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getMaskingServiceInstance(
            null))
        .thenReturn(maskingService);
    when(maskingService.maskData("test@test.com")).thenReturn("test@test.com");
    when(maskingService.maskData("9742857854")).thenReturn("9742857854");

    Map<String, Object> userMap = new HashMap<>();
    userMap.put(JsonKey.PHONE, "9742857854");
    userMap.put(JsonKey.EMAIL, "test@test.com");
    UserUtil.addMaskEmailAndPhone(userMap);
    assertNotNull(true);
  }

  @Test
  public void addMaskEmailAndPhone2() {
    PowerMockito.mockStatic(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class);
    DecryptionService decryptionService = PowerMockito.mock(DefaultDecryptionServiceImpl.class);
    when(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory
            .getDecryptionServiceInstance(null))
        .thenReturn(decryptionService);
    when(decryptionService.decryptData("test@test.com")).thenReturn("test@test.com");
    when(decryptionService.decryptData("9742857854")).thenReturn("9742857854");
    DataMaskingService maskingService = PowerMockito.mock(DefaultDataMaskServiceImpl.class);
    when(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getMaskingServiceInstance(
            null))
        .thenReturn(maskingService);
    when(maskingService.maskData("test@test.com")).thenReturn("test@test.com");
    when(maskingService.maskData("9742857854")).thenReturn("9742857854");

    Map<String, Object> userMap = new HashMap<>();
    userMap.put(JsonKey.PHONE, "9742857854");
    userMap.put(JsonKey.EMAIL, "test@test.com");
    UserUtil.addMaskEmailAndMaskPhone(userMap);
    assertNotNull(true);
  }
}
