package controllers.textbook;

import controllers.BaseController;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.TextbookActorOperation;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import play.libs.F.Promise;
import play.mvc.Result;

/**
 * Handles Textbook TOC APIs.
 *
 * @author gauraw
 */
public class TextbookController extends BaseController {

    public Promise<Result> uploadTOC(String textbookId, String mode) {
        try {
            String operation = null;
            if (mode.equalsIgnoreCase(JsonKey.CREATE)) {
                operation = TextbookActorOperation.TEXTBOOK_TOC_UPLOAD.getValue();
            } else if (mode.equalsIgnoreCase(JsonKey.UPDATE)) {
                operation = TextbookActorOperation.TEXTBOOK_TOC_UPDATE.getValue();
            } else {
                throw new ProjectCommonException("ERR_INAVLID_MODE", "Please Provide Valid mode.", ResponseCode.CLIENT_ERROR.getResponseCode());
            }

            Request request = createAndInitUploadRequest(operation, JsonKey.TEXTBOOK);
            request.put(JsonKey.TEXTBOOK_ID, textbookId);
            return actorResponseHandler(getActorRef(), request, timeout, null, request());
        } catch (Exception e) {
            return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
        }
    }

    /**
     * @param textbookId
     * @return
     */
    public Promise<Result> getTocUrl(String textbookId) {
        try {
            return handleRequest(TextbookActorOperation.TEXTBOOK_TOC_URL.getValue(), textbookId, JsonKey.TEXTBOOK_ID);
        } catch (Exception e) {
            return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
        }
    }

}
