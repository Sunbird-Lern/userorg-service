package filters;

import org.apache.pekko.stream.Materializer;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import javax.inject.Inject;
import org.apache.http.HttpHeaders;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.HeaderParam;
import org.sunbird.util.ProjectUtil;
import play.mvc.EssentialAction;
import play.mvc.EssentialFilter;
import play.mvc.Http;
import play.mvc.Result;

/**
 * Custom Gzip Filter that conditionally applies gzip compression based on configuration
 * Note: In Play 3.0, GzipFilter constructor has changed. Using passthrough for now.
 */
public class CustomGzipFilter extends EssentialFilter {
  private static boolean GzipFilterEnabled =
      Boolean.parseBoolean(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_GZIP_ENABLE));
  private static final double gzipThreshold =
      Double.parseDouble(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_GZIP_SIZE_THRESHOLD));
  private static final String GZIP = "gzip";

  @Inject
  public CustomGzipFilter(Materializer materializer) {
    // Constructor for DI
  }

  @Override
  public EssentialAction apply(EssentialAction next) {
    // For now, pass through - Gzip can be configured via play.filters.enabled
    return next;
  }

  // Whether the given request/result should be gzipped or not
  private static boolean shouldGzipFunction(
      Http.RequestHeader requestHeader, Result responseHeader) {
    double responseSize = 0.0;
    boolean responseLengthKeyExist =
        responseHeader.headers().containsKey(HeaderParam.X_Response_Length.getName());
    if (responseLengthKeyExist) {
      if (responseHeader.headers().get(HeaderParam.X_Response_Length.getName()) != null) {
        String strValue = responseHeader.headers().get(HeaderParam.X_Response_Length.getName());
        responseSize = Double.parseDouble(strValue);
      }
    }
    if (GzipFilterEnabled && (requestHeader.header(HttpHeaders.ACCEPT_ENCODING) != null)) {
      if (requestHeader
          .header(HttpHeaders.ACCEPT_ENCODING)
          .toString()
          .toLowerCase()
          .contains(GZIP)) {
        if (responseSize >= gzipThreshold) {
          return true;
        }
      }
    }
    return false;
  }
}
