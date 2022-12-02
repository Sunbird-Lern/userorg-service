package org.sunbird.util;

import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cloud.storage.BaseStorageService;
import org.sunbird.cloud.storage.factory.StorageServiceFactory;
import scala.Option;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*"
})
@PrepareForTest({StorageServiceFactory.class, CloudStorageUtil.class, PropertiesCache.class})
public class CloudStorageUtilTest {

  private static final String SIGNED_URL = "singedUrl";
  private static final String UPLOAD_URL = "uploadUrl";

  @Before
  public void initTest() {
    PowerMockito.mockStatic(PropertiesCache.class);
    PropertiesCache propertiesCache = mock(PropertiesCache.class);
    when(PropertiesCache.getInstance()).thenReturn(propertiesCache);
    PowerMockito.when(propertiesCache.getProperty(Mockito.anyString())).thenReturn("anyString");

    BaseStorageService service = mock(BaseStorageService.class);
    mockStatic(StorageServiceFactory.class);

    try {
      when(StorageServiceFactory.class, "getStorageService", Mockito.any()).thenReturn(service);

      when(service.upload(
              Mockito.anyString(),
              Mockito.anyString(),
              Mockito.anyString(),
              Mockito.any(Option.class),
              Mockito.any(Option.class),
              Mockito.any(Option.class),
              Mockito.any(Option.class)))
          .thenReturn(UPLOAD_URL);

      when(service.getSignedURL(
              Mockito.anyString(),
              Mockito.anyString(),
              Mockito.any(Option.class),
              Mockito.any(Option.class)))
          .thenReturn(SIGNED_URL);

    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  // @Ignore
  public void testUploadSuccess() {
    String result =
        CloudStorageUtil.upload("azure", "container", "key", "/file/path");
    assertTrue(UPLOAD_URL.equals(result));
  }

  @Test
  @Ignore
  public void testGetSignedUrlSuccess() {
    String signedUrl = CloudStorageUtil.getSignedUrl("azure", "container", "key");
    assertTrue(SIGNED_URL.equals(signedUrl));
  }
}
