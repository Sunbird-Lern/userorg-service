package util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.sunbird.common.models.util.*;

public class CapthaHelper {

  public static ObjectMapper mapper = new ObjectMapper();

  public static boolean validate(String captcha) {
    boolean isCaptchaValid = false;
    String captchaUrl = "https://www.google.com/recaptcha/api/siteverify";
    Map requestMap = new HashMap<String, String>();
    requestMap.put(JsonKey.RESPONSE, captcha);
    requestMap.put("secret", ProjectUtil.getConfigValue(JsonKey.CAPTCHA_SECRET));
    // requestMap.put("remoteip", context.getConnection().getRemoteAddr());

    Map<String, String> headers = new HashMap<>();
    headers.put("Accept", "application/json");
    headers.put("Content-type", "application/json");
    try {
      String response = HttpClientUtil.postFormData(captchaUrl, requestMap, headers);
      Map<String, Object> responseMap = mapper.readValue(response, Map.class);
      isCaptchaValid = (boolean) responseMap.get("success");
    } catch (Exception e) {
      ProjectLogger.log("exception in validating the captcha: ", captcha, LoggerEnum.ERROR.name());
    }
    return isCaptchaValid;
  }
}
