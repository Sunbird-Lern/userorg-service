package org.sunbird.common.models.util.azure;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sunbird.common.models.util.ProjectUtil;

/**
 * Factory class to store the various upload download services like Azure , Amazon S3 etc... Created
 * by arvind on 24/8/17.
 */
public class CloudServiceFactory {

  private static Map<String, CloudService> factory = new HashMap<>();
  private static List<String> allowedServiceNames = Arrays.asList("Azure", "Amazon S3");

  private CloudServiceFactory() {}

  /**
   * @param serviceName
   * @return
   */
  public static Object get(String serviceName) {

    if (ProjectUtil.isNotNull(factory.get(serviceName))) {
      return factory.get(serviceName);
    } else {
      // create the service with the given name
      return createService(serviceName);
    }
  }

  /**
   * @param serviceName
   * @return
   */
  private static CloudService createService(String serviceName) {

    if (!(allowedServiceNames.contains(serviceName))) {
      return null;
    }

    synchronized (CloudServiceFactory.class) {
      if (ProjectUtil.isNull(factory.get(serviceName)) && "Azure".equalsIgnoreCase(serviceName)) {
        CloudService service = new AzureCloudService();
        factory.put("Azure", service);
      }
    }
    return factory.get(serviceName);
  }
}
