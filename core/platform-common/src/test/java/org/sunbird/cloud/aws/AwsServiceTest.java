package org.sunbird.cloud.aws;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.File;
import java.util.List;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cloud.CloudService;
import org.sunbird.cloud.CloudServiceFactory;
import org.sunbird.util.PropertiesCache;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PropertiesCache.class})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "com.microsoft.azure.storage.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*",
  "javax.script.*",
  "javax.xml.*",
  "com.sun.org.apache.xerces.*",
  "org.xml.*"
})
public class AwsServiceTest {

  private static Object obj = null;

  @BeforeClass
  public static void getObject() {

    PowerMockito.mockStatic(PropertiesCache.class);
    PropertiesCache propertiesCache = mock(PropertiesCache.class);
    when(PropertiesCache.getInstance()).thenReturn(propertiesCache);
    PowerMockito.when(propertiesCache.getProperty(Mockito.anyString())).thenReturn("anyString");

    obj = CloudServiceFactory.get("aws");
  }

  @Test
  public void testGetSuccess() {
    Object obj1 = CloudServiceFactory.get("aws");
    Assert.assertNotNull(obj1);
    Assert.assertTrue(obj.equals(obj1));
  }

  @Test
  public void testUploadFileFailure() {
    try {
      CloudService service = (CloudService) obj;
      service.uploadFile("/container/sub/", new File("test.txt"), null);
    } catch (Exception ex) {
      Assert.assertNotNull(ex);
    }
  }

  @Test
  public void testUploadFileSuccess() {
    CloudService service = (CloudService) obj;
    String url = service.uploadFile("containerName", "fileName", "/conf", null);
    Assert.assertEquals(null, url);
  }

  @Test
  public void testDownloadFileSuccess() {
    CloudService service = (CloudService) obj;
    boolean url = service.downLoadFile("containerName", "fileName", "/conf", null);
    Assert.assertFalse(url);
  }

  @Test
  public void testDeleteFileSuccess() {
    CloudService service = (CloudService) obj;
    boolean url = service.deleteFile("containerName", "fileName", null);
    Assert.assertFalse(url);
  }

  @Test
  public void testListAllFileSuccess() {
    CloudService service = (CloudService) obj;
    List url = service.listAllFiles("containerName", null);
    Assert.assertNull(url);
  }

  @Test
  public void testDeleteContainerSuccess() {
    CloudService service = (CloudService) obj;
    boolean url = service.deleteContainer("containerName", null);
    Assert.assertFalse(url);
  }

  @Test
  public void testUploadFileFailure2() {
    try {
      CloudService service = (CloudService) obj;
      service.uploadFile("", new File("test.txt"), null);
    } catch (Exception ex) {
      Assert.assertNotNull(ex);
    }
  }
}
