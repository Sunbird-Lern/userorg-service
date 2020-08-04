package util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.models.util.*;

public class CaptchaHelper {

  String captchaUrl = null;
  String mobilePrivateKey = null;
  String portalPrivateKey = null;
  ObjectMapper mapper = new ObjectMapper();

  public CaptchaHelper() {
    captchaUrl = "https://www.google.com/recaptcha/api/siteverify";
    mobilePrivateKey = ProjectUtil.getConfigValue(JsonKey.GOOGLE_CAPTCHA_MOBILE_PRIVATE_KEY);
    portalPrivateKey = ProjectUtil.getConfigValue(JsonKey.GOOGLE_CAPTCHA_PRIVATE_KEY);
  }

  public boolean validate(String captcha, String mobileApp) {
    boolean isCaptchaValid = false;
    Map requestMap = new HashMap<String, String>();
    requestMap.put(JsonKey.RESPONSE, captcha);
    requestMap.put(
        "secret", StringUtils.isNotEmpty(mobileApp) ? mobilePrivateKey : portalPrivateKey);
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
