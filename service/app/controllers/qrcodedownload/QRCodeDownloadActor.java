package controllers.qrcodedownload;

import controllers.BaseController;
import controllers.qrcodedownload.validator.QRCodeDownloadRequestValidator;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.Request;
import play.libs.F;
import play.mvc.Result;

public class QRCodeDownloadActor extends BaseController {

    public F.Promise<Result> downloadQRCodes() {
        ProjectLogger.log("Download QR Code method is called = " + request().body().asJson(), LoggerEnum.DEBUG.name());
        return handleRequest(
                ActorOperations.DOWNLOAD_QR_CODES.getValue(),
                request().body().asJson(),
                (request) -> {
                    QRCodeDownloadRequestValidator.validateRequest((Request) request);
                    return null;
                });
    }
}
