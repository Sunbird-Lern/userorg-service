package org.sunbird.logging;

import org.sunbird.request.RequestContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Helper class to format log events in a standardised structure.
 * Constructs the event map including metadata, context, and actor information.
 */
public class CustomLogFormat {
  private String edataType = "system";
  private String eid = "LOG";
  private String ver = "3.0";
  private Map<String, Object> edata = new HashMap<>();
  private Map<String, Object> eventMap = new HashMap<>();

  /**
   * Constructor to initialize and format the log event.
   *
   * @param requestContext The request context containing IDs and levels.
   * @param msg The log message.
   * @param object The object associated with the log (optional).
   * @param params Additional parameters (optional).
   */
  CustomLogFormat(
      RequestContext requestContext,
      String msg,
      Map<String, Object> object,
      Map<String, Object> params) {
    if (params != null) {
      this.edata.put(
          "params",
          new ArrayList<Map<String, Object>>() {
            {
              add(params);
            }
          });
    }
    setEventMap(requestContext, msg);
    if (object != null) {
      this.eventMap.put("object", object);
    }
  }

  /**
   * Retrieves the formatted event map.
   *
   * @return The complete event map.
   */
  public Map<String, Object> getEventMap() {
    return this.eventMap;
  }

  /**
   * Constructs the event map with all required fields.
   *
   * @param requestContext The request context.
   * @param msg The log message.
   */
  public void setEventMap(RequestContext requestContext, String msg) {
    this.edata.put("type", edataType);
    this.edata.put("requestid", requestContext.getRequestId());
    this.edata.put("message", msg);
    this.edata.put("level", requestContext.getLoggerLevel());
    this.eventMap.putAll(
        new HashMap<String, Object>() {
          {
            put("eid", eid);
            put("ets", System.currentTimeMillis());
            put("ver", ver);
            put("mid", "LOG:" + UUID.randomUUID().toString());
            put("context", requestContext.getContextMap());
            put("actor",
                new HashMap<String, Object>() {
                  {
                    put("id", requestContext.getActorId());
                    put("type", requestContext.getActorType());
                  }
                });
            put("edata", edata);
          }
        });
  }
}
