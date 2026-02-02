package util;

import org.apache.commons.lang3.StringUtils;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.response.ResponseParams;
import play.libs.typedmap.TypedKey;
import play.mvc.Http;

public class Common {

  public static String getFromRequest(Http.Request httpReq, TypedKey<?> attribute) {
    String attributeValue = null;
    if (httpReq.attrs() != null && httpReq.attrs().containsKey(attribute)) {
      attributeValue = (String) httpReq.attrs().get(attribute);
    }
    return attributeValue;
  }

  public static ResponseParams createResponseParamObj(
      ResponseCode code, String customMessage, String requestId) {
    ResponseParams params = new ResponseParams();
    if (code.getResponseCode() != 200) {
      params.setErr(code.getErrorCode());
      params.setErrmsg(
          StringUtils.isNotBlank(customMessage) ? customMessage : code.getErrorMessage());
      params.setStatus(JsonKey.FAILED);
    } else {
      params.setStatus(JsonKey.SUCCESS);
    }
    params.setResmsgid(requestId);
    params.setMsgid(requestId);
    return params;
  }
}
