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

public class Filters implements HttpFilters {
  private EssentialFilter[] filters;

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
    this.filter = new GzipFilter(Gzip.gzip(8192), 102400, shouldGzip);
  }

  private boolean shouldGzipFunction(RequestHeader v1, ResponseHeader v2) {
    if (v1.headers().get("Accept-Encoding") != null) {
      if (v1.headers().get("Accept-Encoding").toString().toLowerCase().contains("gzip")) {
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
