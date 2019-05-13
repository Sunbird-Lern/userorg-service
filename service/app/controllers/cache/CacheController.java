package controllers.cache;

import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;

import controllers.BaseController;
import play.libs.F.Promise;
import play.mvc.Result;

public class CacheController extends BaseController{
  
  @SuppressWarnings("unchecked")
  public Promise<Result> clearCache(String mapName) {
    return handleRequest(
        ActorOperations.CLEAR_CACHE.getValue(),
        mapName,
        JsonKey.MAP_NAME);
  }

}
