/**
 * 
 */
package mapper;

import com.fasterxml.jackson.databind.JsonNode;

import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.responsecode.ResponseCode;

import play.libs.Json;

/**
 * This class will map the requested json data into custom class.
 * 
 * @author Manzarul
 *
 */
public class RequestMapper {

  /**
   * Method to map request
   * 
   * @param requestData JsonNode
   * @param obj Class<T>
   * @exception RuntimeException
   * @return <T>
   */
  public static <T> Object mapRequest(JsonNode requestData, Class<T> obj) throws RuntimeException {

    try {
      return Json.fromJson(requestData, obj);
    } catch (Exception e) {
      ProjectLogger.log("ControllerRequestMapper error : " + e.getMessage(), e);
      ProjectLogger.log("Request Data" + requestData.toString(),
          LoggerEnum.INFO.name());
      throw new ProjectCommonException(ResponseCode.invalidData.getErrorCode(),
          ResponseCode.invalidData.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

}
