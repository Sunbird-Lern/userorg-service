package org.sunbird.telemetry.util;

import com.lmax.disruptor.dsl.Disruptor;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.Request;

/**
 * Lmax Disruptor engine to receive the telemetry request and forward the request to event handler
 *
 * @author arvind
 */
public class TelemetryLmaxWriter {

  private static Disruptor<Request> disruptor;
  private WriteEventProducer writeEventProducer;
  private int ringBufferSize;
  private static TelemetryLmaxWriter lmaxWriter;

  private TelemetryLmaxWriter() {
    init();
    registerShutDownHook();
  }

  /**
   * Method to get the singleton object of TelemetryLmaxWriter
   *
   * @return TelemetryLmaxWriter singleton object
   */
  public static TelemetryLmaxWriter getInstance() {
    if (lmaxWriter != null) {
      return lmaxWriter;
    }
    synchronized (TelemetryLmaxWriter.class) {
      if (null == lmaxWriter) {
        lmaxWriter = new TelemetryLmaxWriter();
        lmaxWriter.setRingBufferSize(8);
      }
    }
    return lmaxWriter;
  }

  public void setRingBufferSize(int ringBufferSize) {
    this.ringBufferSize = ringBufferSize;
  }

  /** Initialize the disruptor engine. */
  @SuppressWarnings("unchecked")
  private void init() {
    // create a thread pool executor to be used by disruptor
    Executor executor = Executors.newCachedThreadPool();

    // initialize our event factory
    WriteEventFactory factory = new WriteEventFactory();

    if (ringBufferSize == 0) {
      ringBufferSize = 65536;
    }

    // ring buffer size always has to be the power of 2.
    // so if it is not, make it equal to the nearest integer.
    double power = Math.log(ringBufferSize) / Math.log(2);
    if (power % 1 != 0) {
      power = Math.ceil(power);
      ringBufferSize = (int) Math.pow(2, power);
      ProjectLogger.log("New ring buffer size = " + ringBufferSize);
    }

    // initialize our event handler.
    WriteEventHandler handler = new WriteEventHandler();

    // initialize the disruptor
    disruptor = new Disruptor<Request>(factory, ringBufferSize, executor);
    disruptor.handleEventsWith(handler);

    // start the disruptor and get the generated ring buffer instance
    disruptor.start();

    // initialize the event producer to submit messages
    writeEventProducer = new WriteEventProducer(disruptor);
  }

  /**
   * Method to receive the message (represents telemetry event)
   *
   * @param message telemetry request which contains telemetry event
   */
  public void submitMessage(Request message) {
    if (writeEventProducer != null) {
      // publish the messages via event producer
      writeEventProducer.onData(message);
    }
  }

  /**
   * Clean up thread to gracefully shutdown TelemetryLmaxWriter
   *
   * @author Manzarul
   */
  static class ResourceCleanUp extends Thread {
    public void run() {
      ProjectLogger.log("started resource cleanup.");
      if (disruptor != null) {
        disruptor.halt();
        disruptor.shutdown();
      }
      ProjectLogger.log("completed resource cleanup.");
    }
  }

  /** Register a shutdown hook to gracefully shutdown TelemetryLmaxWriter */
  public static void registerShutDownHook() {
    Runtime runtime = Runtime.getRuntime();
    runtime.addShutdownHook(new ResourceCleanUp());
    ProjectLogger.log("ShutDownHook registered.");
  }
}
