package controllers.bulkapimanagement;

import akka.util.Timeout;
import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.HeaderParam;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestValidator;
import play.libs.F.Promise;
import play.mvc.Result;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;

/**
 * This controller will handle all the request related to bulk api's for user management.
 * 
 * @author Amit Kumar
 */
public class BulkUploadController extends BaseController {
  
  /**
   * This method will allow to upload bulk user.
   * 
   * @return Promise<Result>
   */
  public Promise<Result> uploadUser() {

    try {
      MultipartFormData body = request().body().asMultipartFormData();
      Map<String,Object> map = new HashMap<>();
      byte[] byteArray = null;
      if (body != null) {
          Map<String,String[]> data = body.asFormUrlEncoded();
          for(Entry<String, String[]> entry : data.entrySet()){
            map.put(entry.getKey(), entry.getValue()[0]);
          }
          List<FilePart> filePart = body.getFiles();
          InputStream is = new FileInputStream(filePart.get(0).getFile());
          byteArray = IOUtils.toByteArray(is);
      } else{
        //read data as string from request
      }
      Request reqObj = new Request();
      reqObj.getRequest().putAll(map);
      
     // RequestValidator.validateUploadUser(reqObj);
      reqObj.setOperation(ActorOperations.BULK_UPLOAD.getValue());
      reqObj.setRequest_id(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.DATA, map);
      map.put(JsonKey.OBJECT_TYPE, JsonKey.USER);
      map.put(JsonKey.CREATED_BY,
         getUserIdByAuthToken(request().getHeader(HeaderParam.X_Authenticated_Userid.getName())));
      reqObj.setRequest(innerMap);
      map.put(JsonKey.FILE, byteArray);
      
      
      Timeout timeout = new Timeout(300, TimeUnit.SECONDS);
      return actorResponseHandler(getRemoteActor(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will allow to upload bulk organisation.
   *
   * @return Promise<Result>
   */
  public Promise<Result> uploadOrg1() {

    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("getting bulk organisation upload data request = " + requestData,
          LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      RequestValidator.validateUploadUser(reqObj);
      reqObj.setOperation(ActorOperations.BULK_UPLOAD.getValue());
      reqObj.setRequest_id(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> map = new HashMap<>();
      map.put(JsonKey.DATA, reqObj.getRequest());
      reqObj.getRequest().put(JsonKey.OBJECT_TYPE, JsonKey.ORGANISATION);
      reqObj.getRequest().put(JsonKey.CREATED_BY,
          getUserIdByAuthToken(request().getHeader(HeaderParam.X_Authenticated_Userid.getName())));
      reqObj.setRequest(map);

      MultipartFormData body = request().body().asMultipartFormData();
      File file = null;
      if (body != null) {
        List<FilePart> filePart = body.getFiles();
        file = new File("BULK_FILE");
        InputStream is = new FileInputStream(filePart.get(0).getFile());
        byte[] byteArray = IOUtils.toByteArray(is);
        FileUtils.writeByteArrayToFile(file, byteArray);
      } else{
        //read data as string from request
      }
      reqObj.getRequest().put(JsonKey.FILE, file);


      Timeout timeout = new Timeout(300, TimeUnit.SECONDS);
      return actorResponseHandler(getRemoteActor(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }


  public Result uploadOrg() {

    MultipartFormData body = request().body().asMultipartFormData();
    FilePart picture = body.getFile("picture");
    if (picture != null) {
      String fileName = picture.getFilename();
      String contentType = picture.getContentType();
      File file = picture.getFile();
      return ok("File uploaded");
    } else {
      return badRequest("FILE NOT FOUND");
    }
  }

}
