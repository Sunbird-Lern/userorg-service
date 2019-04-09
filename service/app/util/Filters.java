package util;

import javax.inject.Inject;

import play.api.mvc.EssentialFilter;
import play.http.HttpFilters;
import play.filters.gzip.GzipFilter;
import play.filters.gzip.GzipFilterConfig;
import akka.stream.Materializer;
import filters.LoggingFilter;
import play.api.mvc.EssentialAction;
import play.api.mvc.RequestHeader;
import play.api.mvc.ResponseHeader;
import play.filters.gzip.Gzip;
import play.filters.gzip.GzipFilter;
import scala.Function2;
import scala.runtime.AbstractFunction2;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.PropertiesCache;

public class Filters implements HttpFilters {
  private EssentialFilter[] filters;
  private static boolean GzipFilterEnabled = Boolean
      .parseBoolean(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_GZIP_FILTER_ENABLED));
  private static final String GZIP = "gzip";
  // Size of buffer to use for gzip.
  private static final int BUFFER_SIZE = 8192;
  // Content length threshold, after which the filter will switch to chunking the result.
  private static final int  CHUNKED_THRESHOLD = 102400;

  private Function2<RequestHeader, ResponseHeader, Object> shouldGzip = new AbstractFunction2<RequestHeader, ResponseHeader, Object>() {

    @Override
    public Boolean apply(RequestHeader v1, ResponseHeader v2) {

      return shouldGzipFunction(v1, v2);
    }

  };
  private final GzipFilter filter;

  private final LoggingFilter loggingFilter;

  @Inject
  public Filters(LoggingFilter loggingFilter, GzipFilter filter) {
    this.loggingFilter = loggingFilter;
    this.filter = new GzipFilter(Gzip.gzip(BUFFER_SIZE), CHUNKED_THRESHOLD, shouldGzip);
  }

  // Whether the given request/result should be gzipped or not
  private boolean shouldGzipFunction(RequestHeader v1, ResponseHeader v2) {
    if (GzipFilterEnabled && (v1.headers().get(HttpHeaders.ACCEPT_ENCODING) != null)) {
      if (v1.headers().get(HttpHeaders.ACCEPT_ENCODING).toString().toLowerCase().contains(GZIP)) {
        return true;
      }
    }
    return false;
  }

  public EssentialAction apply(EssentialAction next) {
    return filter.apply(next);
  }

  @Override
  public EssentialFilter[] filters() {
    return new EssentialFilter[] { loggingFilter, filter };
  }

}
