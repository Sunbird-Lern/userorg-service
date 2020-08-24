package org.sunbird.common.models.util.azure;

import java.io.File;
import java.util.List;
import org.sunbird.common.request.RequestContext;

/** Created by arvind on 24/8/17. */
public class AzureCloudService implements CloudService {

  @Override
  public String uploadFile(
      String containerName, String fileName, String fileLocation, RequestContext context) {
    return AzureFileUtility.uploadFile(containerName, fileName, fileLocation);
  }

  @Override
  public boolean downLoadFile(
      String containerName, String fileName, String downloadFolder, RequestContext context) {
    return AzureFileUtility.downloadFile(containerName, fileName, downloadFolder);
  }

  @Override
  public String uploadFile(String containerName, File file, RequestContext context) {
    return AzureFileUtility.uploadFile(containerName, file);
  }

  @Override
  public boolean deleteFile(String containerName, String fileName, RequestContext context) {
    return AzureFileUtility.deleteFile(containerName, fileName);
  }

  @Override
  public List<String> listAllFiles(String containerName, RequestContext context) {
    return AzureFileUtility.listAllBlobbs(containerName);
  }

  @Override
  public boolean deleteContainer(String containerName, RequestContext context) {
    return AzureFileUtility.deleteContainer(containerName);
  }
}
