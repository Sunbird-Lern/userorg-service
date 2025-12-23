/** */
package mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.sunbird.exception.ResponseCode;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.util.ProjectUtil;

import java.text.MessageFormat;

/**
 * This class will map the requested json data into custom class.
 *
 * @author Manzarul
 */
public class RequestMapper {
  private static LoggerUtil logger = new LoggerUtil(RequestMapper.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Method to map request
   *
   * @param requestData JsonNode
   * @param obj Class<T>
   * @exception RuntimeException
   * @return <T>
   */
  public static <T> Object mapRequest(JsonNode requestData, Class<T> obj) throws RuntimeException {

    if (requestData == null)
      throw ProjectUtil.createClientException(ResponseCode.mandatoryHeaderParamsMissing,
        MessageFormat.format(
          ResponseCode.mandatoryHeaderParamsMissing.getErrorMessage(), "Content-Type with value application/json"
        ));

    try {
      return objectMapper.convertValue(requestData, obj);
    } catch (Exception e) {
      logger.error("ControllerRequestMapper error : " + e.getMessage(), e);
      logger.info("RequestMapper:mapRequest Requested data : " + requestData);
      throw ProjectUtil.createClientException(ResponseCode.invalidRequestData);
    }
  }
}
