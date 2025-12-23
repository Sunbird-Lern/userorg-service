package filters;

import org.apache.pekko.util.ByteString;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import play.libs.streams.Accumulator;
import play.mvc.EssentialAction;
import play.mvc.EssentialFilter;
import play.mvc.Result;

public class AccessLogFilter extends EssentialFilter {

  private final Executor executor;

  @Inject
  public AccessLogFilter(Executor executor) {
    super();
    this.executor = executor;
  }

  @Override
  public EssentialAction apply(EssentialAction next) {
    return EssentialAction.of(
        request -> {
          Accumulator<ByteString, Result> accumulator = next.apply(request);
          return accumulator.map(
              result -> {
                return result;
              },
              executor);
        });
  }
}
