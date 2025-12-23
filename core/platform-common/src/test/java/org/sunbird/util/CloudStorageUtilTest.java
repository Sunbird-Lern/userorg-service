package org.sunbird.util;

import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
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
  private static final String PUT_SIGNED_URL = "gcpSignedUrl";

  @Before
  public void initTest() {
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

      when(service.getPutSignedURL(
              Mockito.anyString(),
              Mockito.anyString(),
              Mockito.any(Option.class),
              Mockito.any(Option.class),
              Mockito.any(Option.class),
              Mockito.any(Option.class)))
          .thenReturn(PUT_SIGNED_URL);

      when(service.getSignedURLV2(
              Mockito.eq("azurecontainer"),
              Mockito.anyString(),
              Mockito.any(Option.class),
              Mockito.any(Option.class),
              Mockito.any(Option.class),
              Mockito.any(Option.class)))
          .thenReturn(SIGNED_URL);

      when(service.getSignedURLV2(
              Mockito.eq("gcpcontainer"),
              Mockito.anyString(),
              Mockito.any(Option.class),
              Mockito.any(Option.class),
              Mockito.any(Option.class),
              Mockito.any(Option.class)))
          .thenReturn(PUT_SIGNED_URL);

      when(service.getSignedURLV2(
              Mockito.eq("awscontainer"),
              Mockito.anyString(),
              Mockito.any(Option.class),
              Mockito.any(Option.class),
              Mockito.any(Option.class),
              Mockito.any(Option.class)))
          .thenReturn(SIGNED_URL);

    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testUploadSuccess() {
    String result = CloudStorageUtil.upload("azure", "container", "key", "/file/path");
    assertTrue(UPLOAD_URL.equals(result));
  }

  @Test
  public void testGetSignedUrlAZURESuccess() {
    String signedUrl = CloudStorageUtil.getSignedUrl("azure", "azurecontainer", "key");
    assertTrue(SIGNED_URL.equals(signedUrl));
  }

  @Test
  public void testGetSignedUrlGCPSuccess() {
    String signedUrl = CloudStorageUtil.getSignedUrl("gcloud", "gcpcontainer", "key");
    assertTrue(PUT_SIGNED_URL.equals(signedUrl));
  }

  @Test
  public void testGetSignedUrlAWSSuccess() {
    String signedUrl = CloudStorageUtil.getSignedUrl("aws", "awscontainer", "key");
    assertTrue(SIGNED_URL.equals(signedUrl));
  }

  @Test
  public void testDeleteFileAWSSuccess() {
    CloudStorageUtil.deleteFile("aws", "awscontainer", "key");
  }
}
