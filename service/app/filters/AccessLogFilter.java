package filters;

import akka.util.ByteString;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import javax.inject.Inject;

import org.slf4j.MDC;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.telemetry.util.TelemetryEvents;
import org.sunbird.telemetry.util.TelemetryWriter;
import play.libs.streams.Accumulator;
import play.mvc.EssentialAction;
import play.mvc.EssentialFilter;
import play.mvc.Result;

public class AccessLogFilter extends EssentialFilter {

  private final Executor executor;
  private ObjectMapper objectMapper = new ObjectMapper();

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
                    //MDC.clear();
                    return result;
                },
                executor);
        }
    );
  }
}
