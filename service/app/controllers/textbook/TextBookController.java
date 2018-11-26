package controllers.textbook;

import controllers.bulkapimanagement.BaseBulkUploadController;
import org.sunbird.common.models.util.BulkUploadActorOperation;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Result;

/**
 *
 * @author gauraw
 */
public class TextBookController extends BaseBulkUploadController {

    public Promise<Result> uploadTOC(String textBookId) {
        try {
            Request request = createAndInitBulkRequest(BulkUploadActorOperation.TEXTBOOK_TOC_UPLOAD.getValue(), JsonKey.TEXTBOOK, false);
            return actorResponseHandler(getActorRef(), request, timeout, null, request());
        } catch (Exception e) {
            e.printStackTrace();
            return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
        }
    }

    public Promise<Result> downloadTOC(String textBookId) {
        try {
            return handleRequest(BulkUploadActorOperation.TEXTBOOK_TOC_DOWNLOAD.getValue(), textBookId, null);
        } catch (Exception e) {
            e.printStackTrace();
            return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
        }
    }
}
