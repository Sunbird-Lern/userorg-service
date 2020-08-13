package org.sunbird.user.util;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.datasecurity.DecryptionService;
import org.sunbird.common.models.util.datasecurity.EncryptionService;
import org.sunbird.common.models.util.datasecurity.impl.DefaultDecryptionServiceImpl;
import org.sunbird.common.models.util.datasecurity.impl.DefaultEncryptionServivceImpl;
import org.sunbird.common.responsecode.ResponseCode;
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
  org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class
})
@PowerMockIgnore({"javax.management.*"})
public class UserUtilEncryptionDecryptionTest {

  @Test
  public void getEncryptedDataFailure() throws Exception {
    PowerMockito.mockStatic(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class);
    EncryptionService encryptionService = PowerMockito.mock(DefaultEncryptionServivceImpl.class);
    when(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory
            .getEncryptionServiceInstance(null))
        .thenReturn(encryptionService);
    when(encryptionService.encryptData(Mockito.anyString()))
        .thenThrow(
            new ProjectCommonException(
                ResponseCode.userDataEncryptionError.getErrorCode(),
                ResponseCode.userDataEncryptionError.getErrorMessage(),
                ResponseCode.SERVER_ERROR.getResponseCode()));
    try {
      String res = UserUtil.getEncryptedData("test@test.com");
    } catch (ProjectCommonException e) {
      assertEquals(e.getResponseCode(), 500);
    }
  }

  @Test
  public void getDecryptedDataFailure() throws Exception {
    PowerMockito.mockStatic(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class);
    DecryptionService decryptionService = PowerMockito.mock(DefaultDecryptionServiceImpl.class);
    when(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory
            .getDecryptionServiceInstance(null))
        .thenReturn(decryptionService);
    when(decryptionService.decryptData(Mockito.anyString()))
        .thenThrow(
            new ProjectCommonException(
                ResponseCode.userDataEncryptionError.getErrorCode(),
                ResponseCode.userDataEncryptionError.getErrorMessage(),
                ResponseCode.SERVER_ERROR.getResponseCode()));
    try {
      String res = UserUtil.getDecryptedData("test@test.com");
    } catch (ProjectCommonException e) {
      assertEquals(e.getResponseCode(), 500);
    }
  }
}
