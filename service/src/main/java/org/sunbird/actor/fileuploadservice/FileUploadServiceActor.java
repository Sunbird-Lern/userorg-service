package org.sunbird.actor.fileuploadservice;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.userorg.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.utils.CloudStorageUtil;
import org.sunbird.common.ProjectUtil;

public class FileUploadServiceActor extends BaseActor {

  @Override
  public void onReceive(Request request) throws Throwable {
    if (request.getOperation().equalsIgnoreCase(ActorOperations.FILE_STORAGE_SERVICE.getValue())) {
      processFileUpload(request);
    } else {
      onReceiveUnsupportedOperation();
    }
  }

  private void processFileUpload(Request actorMessage) throws IOException {
    RequestContext context = actorMessage.getRequestContext();
    String processId = ProjectUtil.getUniqueIdFromTimestamp(1);
    Map<String, Object> req = (Map<String, Object>) actorMessage.getRequest().get(JsonKey.DATA);

    Response response = new Response();
    String fileExtension = "";
    String fileName = (String) req.get(JsonKey.FILE_NAME);
    if (!StringUtils.isBlank(fileName)) {
      String[] split = fileName.split("\\.");
      if (split.length > 1) {
        fileExtension = split[split.length - 1];
      }
    }
    String fName = "File-" + processId;
    if (!StringUtils.isBlank(fileExtension)) {
      fName = fName + "." + fileExtension.toLowerCase();
      logger.info(context, "File - " + fName + " Extension is " + fileExtension);
    }

    File file = new File(fName);
    FileOutputStream fos = null;
    String avatarUrl = null;
    try {
      fos = new FileOutputStream(file);
      fos.write((byte[]) req.get(JsonKey.FILE));
      String cspProvider = ProjectUtil.getConfigValue(JsonKey.CLOUD_SERVICE_PROVIDER);
      if (null == cspProvider) {
        logger.info(context, "The cloud service is not available");
        ProjectCommonException exception =
            new ProjectCommonException(
                ResponseCode.invalidRequestData,
                ResponseCode.invalidRequestData.getErrorMessage(),
                ResponseCode.CLIENT_ERROR.getResponseCode());
        sender().tell(exception, self());
      }
      String container = (String) req.get(JsonKey.CONTAINER);
      avatarUrl= CloudStorageUtil.upload(cspProvider,container,fileName,fName);
    } catch (IOException e) {
      logger.error(context, "Exception Occurred while reading file in FileUploadServiceActor", e);
      throw e;
    } finally {
      try {
        if (null != (fos)) {
          fos.close();
        }
        file.delete();
      } catch (IOException e) {
        logger.error(
            context,
            "Exception Occurred while closing fileInputStream in FileUploadServiceActor",
            e);
      }
    }
    response.put(JsonKey.URL, avatarUrl);
    sender().tell(response, self());
  }
}
