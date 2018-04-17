/** */
package controllers.telemetry;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import java.util.HashMap;
import java.util.Map;
import mapper.RequestMapper;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Http.RequestBody;
import play.mvc.Result;

/**
 * This controller will handle all sunbird telemetry request data.
 *
 * @author Manzarul
 */
public class TelemetryController extends BaseController {

  /**
   * This method will receive the telemetry data and send it to EKStep to process it.
   *
   * @return Promise<Result>
   */
  public Promise<Result> save() {

    try {
      Request request = null;
      if (isGzipRequest(request().headers())) {
        ProjectLogger.log("Receiving telemetry in gzip format.", LoggerEnum.DEBUG.name());
        request = new Request();
        byte[] byteArray = getBytes(request().body());
        Map<String, Object> map = new HashMap<>();
        map.put(JsonKey.FILE, byteArray);
        request.getRequest().putAll(map);
      } else {
        JsonNode requestData = request().body().asJson();
        ProjectLogger.log(
            "Receiving telemetry in json format.", requestData, LoggerEnum.DEBUG.name());
        request = (Request) RequestMapper.mapRequest(requestData, Request.class);
      }
      request =
          setExtraParam(
              request,
              ExecutionContext.getRequestId(),
              ActorOperations.SAVE_TELEMETRY.getValue(),
              ctx().flash().get(JsonKey.USER_ID),
              getEnvironment());
      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  private boolean isGzipRequest(Map<String, String[]> headers) {
    boolean isGzip = false;
    if (headers.containsKey("accept-encoding")) {
      for (String encoding : headers.get("accept-encoding")) {
        if (encoding.contains("gzip")) {
          isGzip = true;
          break;
        }
      }
    }
    return isGzip;
  }

  private byte[] getBytes(RequestBody requestBody) throws Exception {
    byte[] byteArray = requestBody.asRaw().asBytes();
    return byteArray;
  }
}
