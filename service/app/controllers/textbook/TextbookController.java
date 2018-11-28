package controllers.textbook;

import controllers.BaseController;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.TextbookActorOperation;
import org.sunbird.common.request.Request;
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
            Request request = createAndInitUploadRequest(TextbookActorOperation.TEXTBOOK_TOC_UPLOAD.getValue(), JsonKey.TEXTBOOK);
            return actorResponseHandler(getActorRef(), request, timeout, null, request());
        } catch (Exception e) {
            return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
        }
    }

    public Promise<Result> downloadTOC(String textbookId) {
        try {
            return handleRequest(TextbookActorOperation.TEXTBOOK_TOC_DOWNLOAD.getValue(), textbookId, null);
        } catch (Exception e) {
            return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
        }
    }

}
