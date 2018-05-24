package util;

import filters.LoggingFilter;
import javax.inject.Inject;
import play.api.mvc.EssentialFilter;
import play.http.HttpFilters;

/** @author Mahesh Kumar Gangula */
public class Filters implements HttpFilters {

  private final LoggingFilter loggingFilter;

  @Inject
  public Filters(LoggingFilter loggingFilter) {
    this.loggingFilter = loggingFilter;
  }

  @Override
  public EssentialFilter[] filters() {
    return new EssentialFilter[] {loggingFilter};
  }
}
