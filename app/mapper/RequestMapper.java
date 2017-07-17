package mapper;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.Json;

/**
 * This class will map the requested json data into custom class.
 * 
 * @author Manzarul
 *
 */
public class RequestMapper {

  private static final Logger LOGGER = LogManager.getLogger(RequestMapper.class.getName());

  /**
   * @param <T>
   * @param requestData
   * @param obj Object
   * @exception RequestDataMissingException
   * @return Object
   */
  public static <T> Object mapRequest(JsonNode requestData, Class<T> obj) throws RuntimeException {
    try {
      return Json.fromJson(requestData, obj);
    } catch (Exception e) {
      LOGGER.error("ControllerRequestMapper error : " + e.getMessage());
      LOGGER.error("Request Data" + requestData.toString());
      ProjectLogger.log("ControllerRequestMapper error : " + e.getMessage(), e);
      ProjectLogger.log("Request Data" + requestData.toString(), LoggerEnum.INFO.name());
      throw new RuntimeException();
    }
  }



}
