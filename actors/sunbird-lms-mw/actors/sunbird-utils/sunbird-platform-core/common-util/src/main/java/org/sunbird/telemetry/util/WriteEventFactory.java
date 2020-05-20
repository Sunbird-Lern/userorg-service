package org.sunbird.telemetry.util;

import com.lmax.disruptor.EventFactory;
import org.sunbird.common.request.Request;

/** @author Manzarul */
public class WriteEventFactory implements EventFactory<Request> {
  @Override
  public Request newInstance() {
    return new Request();
  }
}
