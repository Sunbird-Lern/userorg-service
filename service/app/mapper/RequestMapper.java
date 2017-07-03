package mapper;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.Json;
/**
 * This class will map the requested json data into custom class.
 * @author Manzarul
 *
 */
public class RequestMapper {
	private static final Logger LOGGER = Logger.getLogger(RequestMapper.class); 

    /**
     * @param <T>
     * @param requestData
     * @param obj
     *            Object
     * @exception RequestDataMissingException
     * @return Object
     */
    public static <T> Object  mapRequest(JsonNode requestData, Class<T> obj)
	    throws RuntimeException {
	try {
	    return Json.fromJson(requestData, obj);
	} catch (Exception e) {
		LOGGER.error("ControllerRequestMapper error : " + e.getMessage());
		LOGGER.error("Request Data"+ requestData.toString());
	    throw new RuntimeException();
	}
    }



}
