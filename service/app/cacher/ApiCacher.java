package cacher;

import java.util.Map;
import org.sunbird.cache.CacheFactory;
import org.sunbird.cache.interfaces.Cache;
import org.sunbird.notification.utils.JsonUtil;

public class ApiCacher {
  private static Cache cache = CacheFactory.getInstance();

  public static String getCachedResponse(String mapName, Map<String, Object> request) {
    String requestHashCode = JsonUtil.getHashCode(request) + "";
    return cache.get(mapName, requestHashCode);
  }
}
