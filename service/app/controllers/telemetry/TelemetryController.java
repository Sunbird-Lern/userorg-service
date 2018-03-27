/**
 * 
 */
package controllers.telemetry;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import mapper.RequestMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Result;

/**
 * This controller will handle all sunbird telemetry request data.
 * 
 * @author Manzarul
 *
 */
public class TelemetryController extends BaseController {

    /**
     * This method will receive the telemetry data and send it to EKStep to process it.
     * 
     * @return Promise<Result>
     */
    public Promise<Result> save() {

        try {
            JsonNode requestData = request().body().asJson();
            ProjectLogger.log("Receiving telemetry Data" + requestData, LoggerEnum.DEBUG.name());
            Request reqObj = null;
            File file = null;
            byte[] byteArray = null;
            InputStream stream = null;
            if (null == requestData) {
                try {
                    reqObj = new Request();
                    file = request().body().asRaw().asFile();
                    stream = FileUtils.openInputStream(file);
                    byteArray = IOUtils.toByteArray(stream);
                    Map<String, Object> map = new HashMap<>();
                    map.put(JsonKey.FILE, byteArray);
                    reqObj.getRequest().putAll(map);
                } finally {
                    if (null != stream) {
                        stream.close();
                    }
                }
            } else {
                reqObj = (Request) RequestMapper.mapRequest(requestData, Request.class);
            }

            reqObj = setExtraParam(reqObj, ExecutionContext.getRequestId(),
                    ActorOperations.SAVE_TELEMETRY.getValue(), ctx().flash().get(JsonKey.USER_ID),
                    getEnvironment());
            return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
        } catch (Exception e) {
            return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
        }
    }

}
