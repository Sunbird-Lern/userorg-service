package controllers.textbook;

import controllers.BaseController;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.TextbookActorOperation;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Result;

/**
 * This controller class has all methods for Textbook Toc API
 * @author gauraw
 */
public class TextbookController extends BaseController {

    /**
     * @param textbookId
     * @return
     */
    public Promise<Result> uploadTOC(String textbookId) {
        try {
            Request request = createAndInitUploadRequest(TextbookActorOperation.TEXTBOOK_TOC_UPLOAD.getValue(), JsonKey.TEXTBOOK);
            return actorResponseHandler(getActorRef(), request, timeout, null, request());
        } catch (Exception e) {
            return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
        }
    }

    /**
     * @param textbookId
     * @return
     */
    public Promise<Result> downloadTOC(String textbookId) {
        try {
            return handleRequest(TextbookActorOperation.TEXTBOOK_TOC_DOWNLOAD.getValue(), textbookId, null);
        } catch (Exception e) {
            return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
        }
    }
}
