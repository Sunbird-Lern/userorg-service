/** */
package org.sunbird.azure;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;

/** @author Manzarul */
public class AzureFileUtility {

  private static LoggerUtil logger = new LoggerUtil(AzureFileUtility.class);

  private static final String DEFAULT_CONTAINER = "default";

  /**
   * This method will remove the file from Azure Storage.
   *
   * @param containerName
   * @param fileName
   * @param context
   * @return boolean
   */
  public static boolean deleteFile(String containerName, String fileName, RequestContext context) {
    if (fileName == null) {
      logger.info(context, "File name can not be null");
      return false;
    }
    if (StringUtils.isBlank(containerName)) {
      logger.info(context, "Container name can't be null or empty");
      return false;
    }
    CloudBlobContainer container = AzureConnectionManager.getContainer(containerName, true);
    if (container == null) {
      logger.info(context, "Unable to get Azure contains object");
      return false;
    }
    try {
      // Retrieve reference to a blob named "myimage.jpg".
      CloudBlockBlob blob = container.getBlockBlobReference(fileName);
      // Delete the blob.
      boolean response = blob.deleteIfExists();
      if (!response) {
        logger.info(context, "Provided file not found to delete.");
      }
      return true;
    } catch (Exception e) {
      logger.error(context, e.getMessage(), e);
    }
    return false;
  }

  /**
   * This method will remove the container from Azure Storage.
   *
   * @param containerName
   * @param context
   * @return boolean
   */
  public static boolean deleteContainer(String containerName, RequestContext context) {
    if (StringUtils.isBlank(containerName)) {
      logger.info(context, "Container name can't be null or empty");
      return false;
    }
    CloudBlobContainer container = AzureConnectionManager.getContainer(containerName, true);
    if (container == null) {
      logger.info(context, "Unable to get Azure contains object");
      return false;
    }
    try {
      boolean response = container.deleteIfExists();
      if (!response) {
        logger.info(context, "Container not found..");
      } else {
        logger.info(context, "Container is deleted===");
      }
      return true;
    } catch (Exception e) {
      logger.error(context, e.getMessage(), e);
    }
    return false;
  }

  public static String uploadFile(
      String containerName, String blobName, String fileName, RequestContext context) {

    CloudBlobContainer container = AzureConnectionManager.getContainer(containerName, true);
    // Create or overwrite the "myimage.jpg" blob with contents from a local file.
    CloudBlockBlob blob = null;
    String fileUrl = null;
    FileInputStream fis = null;
    Tika tika = new Tika();
    try {
      blob = container.getBlockBlobReference(blobName);
      File source = new File(fileName);
      fis = new FileInputStream(source);
      String mimeType = tika.detect(source);
      logger.info(context, "File - " + source.getName() + " mimeType " + mimeType);
      blob.getProperties().setContentType(mimeType);
      blob.upload(fis, source.length());
      // fileUrl = blob.getStorageUri().getPrimaryUri().getPath();
      fileUrl = blob.getUri().toString();
    } catch (URISyntaxException | IOException e) {
      logger.error(context, "Unable to upload file :" + fileName, e);
    } catch (Exception e) {
      logger.error(context, "Unable to upload file :" + e.getMessage(), e);
    } finally {
      if (null != fis) {
        try {
          fis.close();
        } catch (IOException e) {
          logger.error(context, e.getMessage(), e);
        }
      }
    }

    return fileUrl;
  }

  public static String uploadFile(String containerName, File source, RequestContext context) {

    String containerPath = "";
    String filePath = "";
    Tika tika = new Tika();
    String contrName = containerName;

    if (StringUtils.isBlank(containerName)) {
      contrName = DEFAULT_CONTAINER;
    } else {
      contrName = containerName.toLowerCase();
    }
    if (containerName.startsWith("/")) {
      contrName = containerName.substring(1);
    }
    if (contrName.contains("/")) {
      String[] arr = contrName.split("/", 2);
      containerPath = arr[0];
      if (arr[1].length() > 0 && arr[1].endsWith("/")) {
        filePath = arr[1];
      } else if (arr[1].length() > 0) {
        filePath = arr[1] + "/";
      }
    } else {
      containerPath = contrName;
    }

    CloudBlobContainer container = AzureConnectionManager.getContainer(containerPath, true);
    // Create or overwrite the "myimage.jpg" blob with contents from a local file.
    CloudBlockBlob blob = null;
    String fileUrl = null;
    FileInputStream fis = null;
    try {
      blob = container.getBlockBlobReference(filePath + source.getName());
      // File source = new File(fileName);
      fis = new FileInputStream(source);
      String mimeType = tika.detect(source);
      logger.info(context, "File - " + source.getName() + " mimeType " + mimeType);
      blob.getProperties().setContentType(mimeType);
      blob.upload(fis, source.length());
      // fileUrl = blob.getStorageUri().getPrimaryUri().getPath();
      fileUrl = blob.getUri().toString();
    } catch (URISyntaxException | IOException e) {
      logger.error(context, "Unable to upload file :" + source.getName(), e);
    } catch (Exception e) {
      logger.error(context, e.getMessage(), e);
    } finally {
      if (null != fis) {
        try {
          fis.close();
        } catch (IOException e) {
          logger.error(context, e.getMessage(), e);
        }
      }
    }
    return fileUrl;
  }

  public static boolean downloadFile(
      String containerName, String blobName, String downloadFolder, RequestContext context) {

    String dwnldFolder = "";
    boolean flag = false;
    CloudBlobContainer container = AzureConnectionManager.getContainer(containerName, true);
    // Create or overwrite blob with contents .
    CloudBlockBlob blob = null;
    FileOutputStream fos = null;

    try {
      blob = container.getBlockBlobReference(blobName);
      if (blob.exists()) {
        if (!(downloadFolder.endsWith(("/")))) {
          dwnldFolder = downloadFolder + "/";
        }
        File file = new File(dwnldFolder + blobName);
        fos = new FileOutputStream(file);
        blob.download(fos);
      }
    } catch (URISyntaxException | StorageException | FileNotFoundException e) {
      logger.error(context, "Unable to upload blobfile :" + blobName, e);
    } catch (Exception e) {
      logger.error(context, e.getMessage(), e);
    } finally {
      if (null != fos) {
        try {
          fos.close();
        } catch (IOException e) {
          logger.error(context, e.getMessage(), e);
        }
      }
    }
    return flag;
  }

  public static List<String> listAllBlobbs(String containerName, RequestContext context) {

    List<String> blobsList = new ArrayList<>();
    CloudBlobContainer container = AzureConnectionManager.getContainer(containerName, true);
    // Loop over blobs within the container and output the URI to each of them.
    logger.info(context, "get all blobs details");
    if (container != null) {
      for (ListBlobItem blobItem : container.listBlobs()) {
        blobsList.add(blobItem.getUri().toString());
      }
    }
    return blobsList;
  }
}
