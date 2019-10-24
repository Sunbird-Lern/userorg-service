package controllers.cache;

import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;

import controllers.BaseController;
import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

public class CacheController extends BaseController{
  
  @SuppressWarnings("unchecked")
  public CompletionStage<Result> clearCache(String mapName, Http.Request httpRequest) {
    return handleRequest(
        ActorOperations.CLEAR_CACHE.getValue(),
        mapName,
        JsonKey.MAP_NAME,
            httpRequest);
  }

}
