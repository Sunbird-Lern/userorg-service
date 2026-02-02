package org.sunbird.telemetry.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.common.ProjectUtil;
import org.sunbird.telemetry.dto.*;
import org.sunbird.telemetry.dto.TelemetryEnvKey;

/**
 * Utility class to generate telemetry events.
 * Provides static methods to construct standard telemetry objects.
 */
public class TelemetryGenerator {

  private static final ObjectMapper mapper = new ObjectMapper();
  private static final LoggerUtil logger = new LoggerUtil(TelemetryGenerator.class);

  /** Private constructor to prevent instantiation. */
  private TelemetryGenerator() {}

  /**
   * Generates api_access AUDIT telemetry JSON string.
   *
   * @param context Map contains the telemetry context info like actor info, env info etc.
   * @param params  Map contains the telemetry event data info
   * @return Telemetry event as JSON string
   */
  public static String audit(Map<String, Object> context, Map<String, Object> params) {
    if (!validateRequest(context, params)) {
      return "";
    }
    String actorId = (String) context.get(JsonKey.ACTOR_ID);
    String actorType = (String) context.get(JsonKey.ACTOR_TYPE);
    Target targetObject =
        generateTargetObject((Map<String, Object>) params.get(JsonKey.TARGET_OBJECT));

    Actor actor = new Actor(actorId, StringUtils.capitalize(actorType));
    Context eventContext = getContext(context);
    Map<String, Object> edata = generateAuditEdata(params);

    /* Assign cdata into context from params correlated objects */
    if (params.containsKey(JsonKey.CORRELATED_OBJECTS)) {
      setCorrelatedDataToContext(params.get(JsonKey.CORRELATED_OBJECTS), eventContext);
    }

    /* Assign request id into context cdata */
    String reqId = (String) context.get(JsonKey.X_REQUEST_ID);
    if (StringUtils.isBlank(reqId)) {
      reqId = (String) context.get(JsonKey.REQUEST_ID);
    }

    if (StringUtils.isNotBlank(reqId)) {
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.ID, reqId);
      map.put(JsonKey.TYPE, TelemetryEnvKey.REQUEST_UPPER_CAMEL);
      eventContext.getCdata().add(map);
    }

    /* Construct telemetry object and set message ID */
    Telemetry telemetry =
        new Telemetry(TelemetryEvents.AUDIT.getName(), actor, eventContext, edata, targetObject);
    telemetry.setMid(reqId);
    return getTelemetry(telemetry);
  }

  /**
   * Sets correlated data (cdata) to the telemetry context.
   *
   * @param correlatedObjects List of correlated objects
   * @param eventContext      The context object to update
   */
  private static void setCorrelatedDataToContext(Object correlatedObjects, Context eventContext) {
    ArrayList<Map<String, Object>> list = (ArrayList<Map<String, Object>>) correlatedObjects;
    ArrayList<Map<String, Object>> targetList = new ArrayList<>();

    /* Convert correlated objects to standardized map format */
    if (null != list && !list.isEmpty()) {
      for (Map<String, Object> m : list) {
        Map<String, Object> map = new HashMap<>();
        map.put(JsonKey.ID, m.get(JsonKey.ID));
        map.put(JsonKey.TYPE, StringUtils.capitalize((String) m.get(JsonKey.TYPE)));
        targetList.add(map);
      }
    }
    eventContext.setCdata(targetList);
  }

  /**
   * Generates a Target object from the provided map.
   *
   * @param targetObject Map containing target object properties (ID, Type, Rollup)
   * @return Constructed Target object
   */
  private static Target generateTargetObject(Map<String, Object> targetObject) {
    Target target =
        new Target(
            (String) targetObject.get(JsonKey.ID),
            StringUtils.capitalize((String) targetObject.get(JsonKey.TYPE)));
    if (targetObject.get(JsonKey.ROLLUP) != null) {
      target.setRollup((Map<String, String>) targetObject.get(JsonKey.ROLLUP));
    }
    return target;
  }

  /**
   * Generates event data (edata) for AUDIT telemetry events.
   *
   * @param params Map containing event parameters (props, type, target object)
   * @return Constructed edata map
   */
  private static Map<String, Object> generateAuditEdata(Map<String, Object> params) {
    Map<String, Object> edata = new HashMap<>();
    Map<String, Object> props = (Map<String, Object>) params.get(JsonKey.PROPS);

    // TODO: need to rethink about this one .. if map is null then what to do
    if (null != props) {
      edata.put(JsonKey.PROPS, getProps(props));
    }

    String type = (String) params.get(JsonKey.TYPE);
    if (null != type) {
      edata.put(JsonKey.TYPE, type);
    }

    Map<String, Object> target = (Map<String, Object>) params.get(JsonKey.TARGET_OBJECT);
    if (target != null && target.get(JsonKey.CURRENT_STATE) != null) {
      edata.put(JsonKey.STATE, StringUtils.capitalize((String) target.get(JsonKey.CURRENT_STATE)));
      if (JsonKey.UPDATE.equalsIgnoreCase((String) target.get(JsonKey.CURRENT_STATE))
          && edata.get(props) != null) {
        removeAttributes((Map<String, Object>) edata.get(props), JsonKey.ID);
      }
    }
    return edata;
  }

  /**
   * Removes specified attributes from the map.
   *
   * @param map        The map to modify
   * @param properties The keys to remove
   */
  private static void removeAttributes(Map<String, Object> map, String... properties) {
    for (String property : properties) {
      map.remove(property);
    }
  }

  /**
   * Recursively extracts properties from a map, flattening nested keys.
   *
   * @param map The map to extract properties from
   * @return List of property keys (dot-separated for nested keys)
   */
  private static List<String> getProps(Map<String, Object> map) {
    try {
      return map.entrySet()
          .stream()
          .map(entry -> entry.getKey())
          .map(
              key -> {
                if (map.get(key) instanceof Map) {
                  List<String> keys = getProps((Map<String, Object>) map.get(key));
                  return keys.stream()
                      .map(childKey -> key + "." + childKey)
                      .collect(Collectors.toList());
                } else {
                  return Arrays.asList(key);
                }
              })
          .flatMap(List::stream)
          .collect(Collectors.toList());
    } catch (Exception e) {
      logger.error("TelemetryGenerator:getProps error =", e);
    }
    return new ArrayList<>();
  }

  /**
   * Constructs the Context object from the context map.
   *
   * @param context Map containing context info
   * @return Constructed Context object
   */
  private static Context getContext(Map<String, Object> context) {
    String channel = (String) context.get(JsonKey.CHANNEL);
    String env = (String) context.get(JsonKey.ENV);
    String did = (String) context.get(JsonKey.DEVICE_ID);
    Producer producer = getProducer(context);
    Context eventContext = new Context(channel, StringUtils.capitalize(env), producer);
    eventContext.setDid(did);
    if (context.get(JsonKey.ROLLUP) != null
        && !((Map<String, String>) context.get(JsonKey.ROLLUP)).isEmpty()) {
      eventContext.setRollup((Map<String, String>) context.get(JsonKey.ROLLUP));
    }
    return eventContext;
  }

  /**
   * Constructs a Producer object from the context map.
   *
   * @param context Map containing producer info
   * @return Constructed Producer object
   */
  private static Producer getProducer(Map<String, Object> context) {
    String id = "";
    if (context != null && context.size() != 0) {
      if (StringUtils.isNotBlank((String) context.get(JsonKey.APP_ID))) {
        id = (String) context.get(JsonKey.APP_ID);
      } else {
        id = (String) context.get(JsonKey.PDATA_ID);
      }
      String pid = (String) context.get(JsonKey.PDATA_PID);
      String ver = (String) context.get(JsonKey.PDATA_VERSION);
      return new Producer(id, pid, ver);
    } else {
      return new Producer("", "", "");
    }
  }

  /**
   * Serializes the Telemetry object to a JSON string.
   *
   * @param telemetry Telemetry object
   * @return JSON string representation
   */
  private static String getTelemetry(Telemetry telemetry) {
    String event = "";
    try {
      event = mapper.writeValueAsString(telemetry);
      logger.info("TelemetryGenerator:getTelemetry = Telemetry Event : " + event);
    } catch (Exception e) {
      logger.error(
          "TelemetryGenerator:getTelemetry = Telemetry Event: failed to generate audit events:", e);
    }
    return event;
  }
  
  /**
   * Generates SEARCH telemetry event.
   *
   * @param context Map contains the telemetry context info like actor info, env info etc.
   * @param params  Map contains the telemetry event data info
   * @return Search Telemetry event as JSON string
   */
  public static String search(Map<String, Object> context, Map<String, Object> params) {
    if (!validateRequest(context, params)) {
      return "";
    }
    String actorId = (String) context.get(JsonKey.ACTOR_ID);
    String actorType = (String) context.get(JsonKey.ACTOR_TYPE);
    Actor actor = new Actor(actorId, StringUtils.capitalize(actorType));

    Context eventContext = getContext(context);

    /* Assign request id into context cdata */
    String reqId = (String) context.get(JsonKey.X_REQUEST_ID);
    if (StringUtils.isBlank(reqId)) {
      reqId = (String) context.get(JsonKey.REQUEST_ID);
    }

    if (StringUtils.isNotBlank(reqId)) {
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.ID, reqId);
      map.put(JsonKey.TYPE, TelemetryEnvKey.REQUEST_UPPER_CAMEL);
      eventContext.getCdata().add(map);
    }

    Map<String, Object> edata = generateSearchEdata(params);
    Telemetry telemetry =
        new Telemetry(TelemetryEvents.SEARCH.getName(), actor, eventContext, edata);
    telemetry.setMid(reqId);
    return getTelemetry(telemetry);
  }

  /**
   * Generates event data (edata) for SEARCH telemetry events.
   *
   * @param params Map containing search event parameters
   * @return Constructed edata map
   */
  private static Map<String, Object> generateSearchEdata(Map<String, Object> params) {
    Map<String, Object> edata = new HashMap<>();
    String type = (String) params.get(JsonKey.TYPE);
    String query = (String) params.get(JsonKey.QUERY);
    Map filters = (Map) params.get(JsonKey.FILTERS);
    Map sort = (Map) params.get(JsonKey.SORT);
    List<Map> topn = (List<Map>) params.get(JsonKey.TOPN);

    edata.put(JsonKey.TYPE, StringUtils.capitalize(type));
    if (null == query) {
      query = "";
    }
    edata.put(JsonKey.QUERY, query);
    edata.put(JsonKey.FILTERS, filters);
    edata.put(JsonKey.SORT, sort);
    edata.put(JsonKey.SIZE, params.get(JsonKey.SIZE));
    edata.put(JsonKey.TOPN, topn);
    return edata;
  }

  /**
   * Generates LOG telemetry event.
   *
   * @param context Map contains the telemetry context info like actor info, env info etc.
   * @param params  Map contains the telemetry event data info
   * @return Log Telemetry event as JSON string
   */
  public static String log(Map<String, Object> context, Map<String, Object> params) {
    if (!validateRequest(context, params)) {
      return "";
    }
    String actorId = (String) context.get(JsonKey.ACTOR_ID);
    String actorType = (String) context.get(JsonKey.ACTOR_TYPE);
    Actor actor = new Actor(actorId, StringUtils.capitalize(actorType));

    Context eventContext = getContext(context);

    /* Assign request id into context cdata */
    String reqId = (String) context.get(JsonKey.X_REQUEST_ID);
    if (StringUtils.isBlank(reqId)) {
      reqId = (String) context.get(JsonKey.REQUEST_ID);
    }

    if (StringUtils.isNotBlank(reqId)) {
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.ID, reqId);
      map.put(JsonKey.TYPE, TelemetryEnvKey.REQUEST_UPPER_CAMEL);
      eventContext.getCdata().add(map);
    }

    Map<String, Object> edata = generateLogEdata(params);
    Telemetry telemetry = new Telemetry(TelemetryEvents.LOG.getName(), actor, eventContext, edata);
    telemetry.setMid(reqId);
    return getTelemetry(telemetry);
  }

  /**
   * Generates event data (edata) for LOG telemetry events.
   *
   * @param params Map containing log event parameters
   * @return Constructed edata map
   */
  private static Map<String, Object> generateLogEdata(Map<String, Object> params) {
    Map<String, Object> edata = new HashMap<>();
    String logType = (String) params.get(JsonKey.LOG_TYPE);
    String logLevel = (String) params.get(JsonKey.LOG_LEVEL);
    String message = (String) params.get(JsonKey.MESSAGE);

    edata.put(JsonKey.TYPE, StringUtils.capitalize(logType));
    edata.put(JsonKey.LEVEL, logLevel);
    edata.put(JsonKey.MESSAGE, message != null ? message : "");

    edata.put(
        JsonKey.PARAMS,
        getParamsList(params, Arrays.asList(JsonKey.LOG_TYPE, JsonKey.LOG_LEVEL, JsonKey.MESSAGE)));
    return edata;
  }

  /**
   * Extracts parameters from a map, excluding specified keys.
   *
   * @param params Map to extract parameters from
   * @param ignore List of keys to exclude
   * @return List of parameter maps
   */
  private static List<Map<String, Object>> getParamsList(
      Map<String, Object> params, List<String> ignore) {
    List<Map<String, Object>> paramsList = new ArrayList<>();
    if (null != params && !params.isEmpty()) {
      for (Map.Entry<String, Object> entry : params.entrySet()) {
        if (!ignore.contains(entry.getKey())) {
          Map<String, Object> param = new HashMap<>();
          param.put(entry.getKey(), entry.getValue());
          paramsList.add(param);
        }
      }
    }
    return paramsList;
  }

  /**
   * Generates ERROR telemetry event.
   *
   * @param context Map contains the telemetry context info like actor info, env info etc.
   * @param params  Map contains the error event data info
   * @return Error Telemetry event as JSON string
   */
  public static String error(Map<String, Object> context, Map<String, Object> params) {
    if (!validateRequest(context, params)) {
      return "";
    }
    String actorId = (String) context.get(JsonKey.ACTOR_ID);
    String actorType = (String) context.get(JsonKey.ACTOR_TYPE);
    Actor actor = new Actor(actorId, StringUtils.capitalize(actorType));

    Context eventContext = getContext(context);

    /* Assign request id into context cdata */
    String reqId = (String) context.get(JsonKey.X_REQUEST_ID);
    if (StringUtils.isBlank(reqId)) {
      reqId = (String) context.get(JsonKey.REQUEST_ID);
    }

    if (StringUtils.isNotBlank(reqId)) {
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.ID, reqId);
      map.put(JsonKey.TYPE, TelemetryEnvKey.REQUEST_UPPER_CAMEL);
      eventContext.getCdata().add(map);
    }

    Map<String, Object> edata = generateErrorEdata(params);
    edata.put(JsonKey.REQUEST_ID, reqId);
    Telemetry telemetry =
        new Telemetry(TelemetryEvents.ERROR.getName(), actor, eventContext, edata);
    telemetry.setMid(reqId);
    return getTelemetry(telemetry);
  }

  /**
   * Generates event data (edata) for ERROR telemetry events.
   *
   * @param params Map contains error event parameters
   * @return Constructed edata map
   */
  private static Map<String, Object> generateErrorEdata(Map<String, Object> params) {
    Map<String, Object> edata = new HashMap<>();
    String error = (String) params.get(JsonKey.ERROR);
    String errorType = (String) params.get(JsonKey.ERR_TYPE);
    String stackTrace = (String) params.get(JsonKey.STACKTRACE);
    edata.put(JsonKey.ERROR, error);
    edata.put(JsonKey.ERR_TYPE, errorType);
    
    int stackTraceLength = 100;
    try {
        String lengthStr = ProjectUtil.getConfigValue(JsonKey.STACKTRACE_CHAR_LENGTH);
        if (StringUtils.isNotBlank(lengthStr)) {
            stackTraceLength = Integer.parseInt(lengthStr);
        }
    } catch (Exception e) {
        logger.error("TelemetryGenerator:generateErrorEdata: Error parsing stacktrace length", e);
    }
    
    edata.put(
        JsonKey.STACKTRACE,
        ProjectUtil.getFirstNCharacterString(stackTrace, stackTraceLength));
    return edata;
  }
  
    /**
   * Validates if context and params are present.
   *
   * @param context Telemetry context map
   * @param params  Telemetry params map
   * @return true if valid, false otherwise
   */
  private static boolean validateRequest(Map<String, Object> context, Map<String, Object> params) {
    return context != null && !context.isEmpty() && params != null && !params.isEmpty();
  }

}