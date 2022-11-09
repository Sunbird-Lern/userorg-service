package org.sunbird.cloud.gcp;

import java.io.File;
import java.util.List;
import org.sunbird.cloud.CloudService;
import org.sunbird.request.RequestContext;

/** Created by arvind on 24/8/17. */
public class GcpCloudService implements CloudService {

  @Override
  public String uploadFile(
      String containerName, String filName, String fileLocation, RequestContext context) {
    return null;
  }

  @Override
  public boolean downLoadFile(
      String containerName, String fileName, String downloadFolder, RequestContext context) {
    return false;
  }

  @Override
  public String uploadFile(String containerName, File file, RequestContext context) {
    return GcpFileUtility.uploadFile(containerName, file, context);
  }

  @Override
  public boolean deleteFile(String containerName, String fileName, RequestContext context) {
    return false;
  }

  @Override
  public List<String> listAllFiles(String containerName, RequestContext context) {
    return null;
  }

  @Override
  public boolean deleteContainer(String containerName, RequestContext context) {
    return false;
  }
}
