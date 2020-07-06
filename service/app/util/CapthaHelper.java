/*
package util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.HttpPost;
import org.sunbird.common.models.util.JsonKey;

import java.util.HashMap;
import java.util.Map;

public class CapthaHelper {
  ObjectMapper mapper = new ObjectMapper();

  public static boolean capthaValidator(String captcha) {

    HttpPost post = new HttpPost("https://www.google.com/recaptcha/api/siteverify");
    Map requestMap = new HashMap<String, String>();
    requestMap.put("response", captcha);
    requestMap.put("secret", secret);
    requestMap.put("remoteip", context.getConnection().getRemoteAddr());
    String json = mapper.writeValueAsString(requestMap);

    Map<String,String> headers = new HashMap<>();
    headers.put("Accept", "application/json");
    headers.put("Content-type", "application/json");
    try {
      UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
      post.setEntity(form);
      HttpResponse response = httpClient.execute(post);
      InputStream content = response.getEntity().getContent();
      try {
        Map json = JsonSerialization.readValue(content, Map.class);
        Object val = json.get("success");
        success = Boolean.TRUE.equals(val);
      } finally {
        content.close();
      }
    } catch (Exception e) {
      ServicesLogger.LOGGER.recaptchaFailed(e);
    }
    return success;


    */
/*HttpClient httpClient = context.getSession().getProvider(HttpClientProvider.class).getHttpClient();
HttpPost post = new HttpPost("https://www.google.com/recaptcha/api/siteverify");
List<NameValuePair> formparams = new LinkedList<>();
formparams.add(new BasicNameValuePair("secret", secret));
formparams.add(new BasicNameValuePair("response", captcha));
formparams.add(new BasicNameValuePair("remoteip", context.getConnection().getRemoteAddr()));
try {
  UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
  post.setEntity(form);
  HttpResponse response = httpClient.execute(post);
  InputStream content = response.getEntity().getContent();
  try {
    Map json = JsonSerialization.readValue(content, Map.class);
    Object val = json.get("success");
    success = Boolean.TRUE.equals(val);
  } finally {
    content.close();
  }
} catch (Exception e) {
  ServicesLogger.LOGGER.recaptchaFailed(e);
}
return success;*//*


                   }

                 }
                 */
