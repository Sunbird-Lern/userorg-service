package util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.models.util.*;

public class CaptchaHelper {

  public static final ObjectMapper mapper = new ObjectMapper();

  public static boolean validate(String captcha, String mobileApp) {
    boolean isCaptchaValid = false;
    String captchaUrl = "https://www.google.com/recaptcha/api/siteverify";
    Map requestMap = new HashMap<String, String>();
    requestMap.put(JsonKey.RESPONSE, captcha);
    requestMap.put(
        "secret",
        StringUtils.isNotEmpty(mobileApp)
            ? ProjectUtil.getConfigValue(JsonKey.MOBILE_CAPTCHA_SECRET)
            : ProjectUtil.getConfigValue(JsonKey.CAPTCHA_SECRET));
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
