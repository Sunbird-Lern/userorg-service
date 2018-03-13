package controllers.badging;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import org.apache.commons.io.IOUtils;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.BadgingActorOperations;
import org.sunbird.common.models.util.BadgingJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.Request;
import play.libs.F;
import play.mvc.BodyParser;
import play.mvc.Http;
import play.mvc.Result;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BadgeClassController handles BadgeClass APIs.
 */
public class BadgeClassController extends BaseController {
    /**
     * Create a new badge class for a particular issuer.
     *
     * @return Return a promise for create badge class API result.
     */
    public F.Promise<Result> createBadgeClass() {
        ProjectLogger.log("createBadgeClass called", LoggerEnum.INFO.name());

        try {
            Request request = createAndInitRequest(BadgingActorOperations.CREATE_BADGE_CLASS.getValue());

            HashMap<String, Object> outerMap = new HashMap<>();
            HashMap<String, String> formParamsMap = new HashMap<>();
            HashMap<String, Object> fileParamsMap = new HashMap<>();

            Http.MultipartFormData multipartBody = request().body().asMultipartFormData();

            if (multipartBody != null) {
                Map<String, String[]> data = multipartBody.asFormUrlEncoded();
                for (Map.Entry<String, String[]> entry : data.entrySet()) {
                    formParamsMap.put(entry.getKey(), entry.getValue()[0]);
                }

                List<Http.MultipartFormData.FilePart> imageFilePart = multipartBody.getFiles();
                if (imageFilePart.size() > 0) {
                    InputStream inputStream = new FileInputStream(imageFilePart.get(0).getFile());
                    byte[] imageByteArray = IOUtils.toByteArray(inputStream);
                    fileParamsMap.put(JsonKey.IMAGE, imageByteArray);
                }
            }

            outerMap.put(JsonKey.FORM_PARAMS, formParamsMap);
            outerMap.put(JsonKey.FILE_PARAMS, fileParamsMap);

            request.setRequest(outerMap);

            new BadgeClassValidator().validateCreateBadgeClass(request);

            return actorResponseHandler(getActorRef(), request, timeout, null, request());
        } catch (Exception e) {
            ProjectLogger.log("createBadgeClass: exception = ", e);

            return F.Promise.pure(createCommonExceptionResponse(e, request()));
        }
    }

    /**
     * Get details of badge class for given issuer and badge class.
     *
     * @return Return a promise for get badge class API result.
     */
    public F.Promise<Result> getBadgeClass() {
        ProjectLogger.log("getBadgeClass called", LoggerEnum.INFO.name());

        try {
            JsonNode bodyJson = request().body().asJson();

            Request request = createAndInitRequest(BadgingActorOperations.GET_BADGE_CLASS.getValue(), bodyJson);

            new BadgeClassValidator().validateGetBadgeClass(request);

            return actorResponseHandler(getActorRef(), request, timeout, null, request());
        } catch (Exception e) {
            ProjectLogger.log("getBadgeClass: exception = ", e);

            return F.Promise.pure(createCommonExceptionResponse(e, request()));
        }
    }

    /**
     * Get list of badge classes for given issuer(s) and matching given context.
     *
     * @return Return a promise for list badge class API result.
     */
    public F.Promise<Result> listBadgeClass() {
        ProjectLogger.log("listBadgeClass called", LoggerEnum.INFO.name());

        try {
            JsonNode bodyJson = request().body().asJson();

            Request request = createAndInitRequest(BadgingActorOperations.LIST_BADGE_CLASS.getValue(), bodyJson);

            new BadgeClassValidator().validateListBadgeClass(request);

            return actorResponseHandler(getActorRef(), request, timeout, null, request());
        } catch (Exception e) {
            ProjectLogger.log("listBadgeClass: exception = ", e);

            return F.Promise.pure(createCommonExceptionResponse(e, request()));
        }
    }

    /**
     * Delete a badge class that has never been issued.
     *
     * @return Return a promise for delete badge class API result.
     */
    @BodyParser.Of(BodyParser.Json.class)
    public F.Promise<Result> deleteBadgeClass() {
        ProjectLogger.log("deleteBadgeClass called", LoggerEnum.INFO.name());

        try {
            JsonNode bodyJson = request().body().asJson();

            Request request = createAndInitRequest(BadgingActorOperations.DELETE_BADGE_CLASS.getValue(), bodyJson);

            new BadgeClassValidator().validateDeleteBadgeClass(request);

            return actorResponseHandler(getActorRef(), request, timeout, null, request());
        } catch (Exception e) {
            ProjectLogger.log("deleteBadgeClass: exception = ", e);

            return F.Promise.pure(createCommonExceptionResponse(e, request()));
        }
    }
}
