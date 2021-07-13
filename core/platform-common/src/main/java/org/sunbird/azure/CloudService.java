package org.sunbird.azure;

import java.io.File;
import java.util.List;
import org.sunbird.common.request.RequestContext;

/** Created by arvind on 24/8/17. */
public interface CloudService {

  String uploadFile(
      String containerName, String filName, String fileLocation, RequestContext context);

  boolean downLoadFile(
      String containerName, String fileName, String downloadFolder, RequestContext context);

  String uploadFile(String containerName, File file, RequestContext context);

  boolean deleteFile(String containerName, String fileName, RequestContext context);

  List<String> listAllFiles(String containerName, RequestContext context);

  boolean deleteContainer(String containerName, RequestContext context);
}
