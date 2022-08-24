package org.sunbird.cloud.azure;

import java.io.File;
import java.util.List;
import org.sunbird.cloud.CloudService;
import org.sunbird.request.RequestContext;

/** Created by arvind on 24/8/17. */
public class AzureCloudService implements CloudService {

  @Override
  public String uploadFile(
      String containerName, String fileName, String fileLocation, RequestContext context) {
    return AzureFileUtility.uploadFile(containerName, fileName, fileLocation, context);
  }

  @Override
  public boolean downLoadFile(
      String containerName, String fileName, String downloadFolder, RequestContext context) {
    return AzureFileUtility.downloadFile(containerName, fileName, downloadFolder, context);
  }

  @Override
  public String uploadFile(String containerName, File file, RequestContext context) {
    return AzureFileUtility.uploadFile(containerName, file, context);
  }

  @Override
  public boolean deleteFile(String containerName, String fileName, RequestContext context) {
    return AzureFileUtility.deleteFile(containerName, fileName, context);
  }

  @Override
  public List<String> listAllFiles(String containerName, RequestContext context) {
    return AzureFileUtility.listAllBlobbs(containerName, context);
  }

  @Override
  public boolean deleteContainer(String containerName, RequestContext context) {
    return AzureFileUtility.deleteContainer(containerName, context);
  }
}
