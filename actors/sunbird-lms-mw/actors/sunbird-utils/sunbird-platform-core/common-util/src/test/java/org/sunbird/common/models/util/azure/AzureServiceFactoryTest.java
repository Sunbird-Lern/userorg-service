/** */
package org.sunbird.common.models.util.azure;

import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.ListBlobItem;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/** @author Manzarul */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(PowerMockRunner.class)
@PrepareForTest({
  CloudStorageAccount.class,
  CloudBlobClient.class,
  CloudBlobContainer.class,
  ListBlobItem.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "com.microsoft.azure.storage.*"
})
public class AzureServiceFactoryTest {

  private static Object obj = null;
  private static CloudBlobContainer container = null;
  private static CloudBlobContainer container1 = null;
  private static String containerName = "testcontainerxyz";

  @BeforeClass
  public static void getObject() {
    obj = CloudServiceFactory.get("Azure");
    Assert.assertTrue(obj instanceof CloudService);
    Assert.assertNotNull(obj);
  }

  @Before
  public void addMockRules() {
    CloudStorageAccount cloudStorageAccount = mock(CloudStorageAccount.class);
    CloudBlobClient cloudBlobClient = mock(CloudBlobClient.class);
    CloudBlobContainer cloudBlobContainer = mock(CloudBlobContainer.class);

    ListBlobItem listBlobItem = mock(ListBlobItem.class);
    List<ListBlobItem> lst = new ArrayList<>();
    lst.add(listBlobItem);
    PowerMockito.mockStatic(CloudStorageAccount.class);
    try {
      doReturn(cloudStorageAccount).when(CloudStorageAccount.class, "parse", Mockito.anyString());
      doReturn(cloudBlobClient).when(cloudStorageAccount).createCloudBlobClient();
      doReturn(cloudBlobContainer).when(cloudBlobClient).getContainerReference(Mockito.anyString());
      doReturn(true).when(cloudBlobContainer).exists();
      when(cloudBlobContainer.listBlobs()).thenReturn(lst);
      when(listBlobItem.getUri()).thenReturn(new URI("http://www.google.com"));

    } catch (Exception e) {
      Assert.fail("Could not initalize mocks, underlying reason " + e.getLocalizedMessage());
    }
  }

  @Test
  public void testGetFailureWithWrongType() {
    Object obj = CloudServiceFactory.get("Azure12");
    Assert.assertNull(obj);
  }

  @Test
  public void testGetSuccess() {
    Object obj1 = CloudServiceFactory.get("Azure");
    Assert.assertNotNull(obj1);
    Assert.assertTrue(obj.equals(obj1));
  }

  @Test
  public void testGetContainerSuccessWithAccessPublic() {
    container = AzureConnectionManager.getContainer(containerName, true);
    Assert.assertNotNull(container);
  }

  @Test
  public void testGetContainerReferenceSuccess() {
    container1 = AzureConnectionManager.getContainerReference(containerName);
    Assert.assertNotNull(container1);
  }

  @Test
  public void testUploadFileSuccess() {
    CloudService service = (CloudService) obj;
    String url = service.uploadFile(containerName, new File("test.txt"));
    Assert.assertEquals(null, url);
  }

  @Test
  public void testUploadFileFailureWithoutContainerName() {
    CloudService service = (CloudService) obj;
    String url = service.uploadFile("", new File("test.txt"));
    Assert.assertEquals(null, url);
  }

  @Test
  public void testUploadFileSuccessWithMultiplePath() {
    CloudService service = (CloudService) obj;
    String url = service.uploadFile("/tez/po/" + containerName, new File("test.txt"));
    Assert.assertEquals(null, url);
  }

  @Test
  public void testUploadFileSuccessWithFileLocation() {
    CloudService service = (CloudService) obj;
    String url = service.uploadFile(containerName, "test.txt", "");
    Assert.assertEquals(null, url);
  }

  @Test
  public void testListAllFilesSuccess() {
    CloudService service = (CloudService) obj;
    List<String> filesList = service.listAllFiles(containerName);
    Assert.assertEquals(1, filesList.size());
  }

  @Test
  public void testDownloadFileSuccess() {
    CloudService service = (CloudService) obj;
    Boolean isFileDeleted = service.downLoadFile(containerName, "test1.txt", "");
    Assert.assertFalse(isFileDeleted);
  }

  @Test
  public void testDeleteFileSuccess() {
    CloudService service = (CloudService) obj;
    Boolean isFileDeleted = service.deleteFile(containerName, "test1.txt");
    Assert.assertFalse(isFileDeleted);
  }

  @Test
  public void testDeleteFileSuccessWithoutContainerName() {
    CloudService service = (CloudService) obj;
    Boolean isFileDeleted = service.deleteFile("", "test.abc");
    Assert.assertFalse(isFileDeleted);
  }

  @Test
  public void testDeleteContainerSuccess() {
    CloudService service = (CloudService) obj;
    boolean response = service.deleteContainer(containerName);
    Assert.assertTrue(response);
  }

  @AfterClass
  public static void shutDown() {
    container1 = null;
    container = null;
    obj = null;
  }
}
