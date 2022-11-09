/** */
package org.sunbird.cloud.gcp;

import java.io.File;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;
import scala.Option;

/** @author Manzarul */
public class GcpFileUtility {

  private static final LoggerUtil logger = new LoggerUtil(GcpFileUtility.class);

  private static final String DEFAULT_CONTAINER = "default";

  public static String uploadFile(String containerName, File source, RequestContext context) {

    String containerPath = "";
    String contrName = "";

    if (StringUtils.isBlank(containerName)) {
      contrName = DEFAULT_CONTAINER;
    } else {
      contrName = containerName.toLowerCase();
    }
    if (contrName.startsWith("/")) {
      contrName = containerName.substring(1);
    }
    if (contrName.contains("/")) {
      String[] arr = contrName.split("/", 2);
      containerPath = arr[0];
    } else {
      containerPath = contrName;
    }

    String objectKey = containerPath + source.getName();
    // upload(container, file, objectKey, isDirectory , attempt , retryCount , ttl )

    return GcpConnectionManager.getStorageService()
        .upload(
            contrName,
            source.getAbsolutePath(),
            objectKey,
            Option.apply(false),
            Option.apply(1),
            Option.apply(3),
            Option.empty());
  }
}
