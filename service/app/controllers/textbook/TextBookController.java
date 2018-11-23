package controllers.textbook;

import controllers.bulkapimanagement.BaseBulkUploadController;
import org.sunbird.common.models.util.BulkUploadActorOperation;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Result;

public class TextBookController extends BaseBulkUploadController {

    public Promise<Result> uploadTOC(String textBookId) {
        try {
            Request request = createAndInitBulkRequest(BulkUploadActorOperation.TEXTBOOK_TOC_UPLOAD.getValue(), JsonKey.TEXTBOOK, false);
            return actorResponseHandler(getActorRef(), request, timeout, null, request());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getCause());
            return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
        }
    }
}
