package filters;

import akka.stream.Materializer;
import org.apache.http.HttpHeaders;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.HeaderParam;
import play.filters.gzip.GzipFilter;
import play.filters.gzip.GzipFilterConfig;
import play.mvc.EssentialAction;
import play.mvc.EssentialFilter;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.function.BiFunction;

public class CustomGzipFilter extends EssentialFilter {
    private static boolean GzipFilterEnabled =
            Boolean.parseBoolean(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_GZIP_ENABLE));
    private static final double gzipThreshold =
            Double.parseDouble(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_GZIP_SIZE_THRESHOLD));
    private static final String GZIP = "gzip";
    // Size of buffer to use for gzip.
    private static final int BUFFER_SIZE = 8192;
    // Content length threshold, after which the filter will switch to chunking the result.
    private static final int CHUNKED_THRESHOLD = 102400;

    private GzipFilter gzipFilter;

    @Inject
    public CustomGzipFilter(Materializer materializer) {
        GzipFilterConfig gzipFilterConfig = new GzipFilterConfig();
        gzipFilter = new GzipFilter(
                gzipFilterConfig.withBufferSize(BUFFER_SIZE)
                        .withChunkedThreshold(CHUNKED_THRESHOLD)
                        .withShouldGzip((BiFunction<Http.RequestHeader, Result, Object>)
                                (req, res) -> shouldGzipFunction(req, res)),
                materializer
        );
    }

    @Override
    public EssentialAction apply(EssentialAction essentialAction) {
        return gzipFilter.asJava().apply(essentialAction);
    }

    // Whether the given request/result should be gzipped or not
    private static boolean shouldGzipFunction(Http.RequestHeader requestHeader, Result responseHeader) {
        double responseSize = 0.0;
        boolean responseLengthKeyExist = responseHeader.headers().containsKey(HeaderParam.X_Response_Length.getName());
        if (responseLengthKeyExist) {
            if (responseHeader.headers().get(HeaderParam.X_Response_Length.getName()) != null) {
                String strValue = responseHeader.headers().get(HeaderParam.X_Response_Length.getName());
                responseSize = Double.parseDouble(strValue);
            }
        }
        if (GzipFilterEnabled && (requestHeader.header(HttpHeaders.ACCEPT_ENCODING) != null)) {
            if (requestHeader.header(HttpHeaders.ACCEPT_ENCODING).toString().toLowerCase().contains(GZIP)) {
                if (responseSize >= gzipThreshold) {
                    return true;
                }
            }
        }
        return false;
    }
}
