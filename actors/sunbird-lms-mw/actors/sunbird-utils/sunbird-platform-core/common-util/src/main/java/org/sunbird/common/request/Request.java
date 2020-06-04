package org.sunbird.common.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.responsecode.ResponseCode;

/** @author Manzarul */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Request implements Serializable {

  private static final long serialVersionUID = -2362783406031347676L;
  private static final Integer MIN_TIMEOUT = 0;
  private static final Integer MAX_TIMEOUT = 30;
  private static final int WAIT_TIME_VALUE = 30;

  protected Map<String, Object> context;

  private String id;
  private String ver;
  private String ts;
  private RequestParams params;

  private Map<String, Object> request = new WeakHashMap<>();

  private String managerName;
  private String operation;
  private String requestId;
  private int env;

  private Integer timeout; // in seconds

  public Request() {
    this.context = new WeakHashMap<>();
    this.params = new RequestParams();
  }

  public void toLower() {
    Arrays.asList(
            ProjectUtil.getConfigValue(JsonKey.SUNBIRD_API_REQUEST_LOWER_CASE_FIELDS).split(","))
        .stream()
        .forEach(
            field -> {
              if (StringUtils.isNotBlank((String) this.getRequest().get(field))) {
                this.getRequest().put(field, ((String) this.getRequest().get(field)).toLowerCase());
              }
            });
  }

  public String getRequestId() {
    return requestId;
  }

  public Map<String, Object> getContext() {
    return context;
  }

  public void setContext(Map<String, Object> context) {
    this.context = context;
  }

  /** @return the requestValueObjects */
  public Map<String, Object> getRequest() {
    return request;
  }

  public void setRequest(Map<String, Object> request) {
    this.request = request;
  }

  public Object get(String key) {
    return request.get(key);
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public void put(String key, Object vo) {
    request.put(key, vo);
  }

  public String getManagerName() {
    return managerName;
  }

  public void setManagerName(String managerName) {
    this.managerName = managerName;
  }

  public String getOperation() {
    return operation;
  }

  public void setOperation(String operation) {
    this.operation = operation;
  }

  @Override
  public String toString() {
    return "Request ["
        + (context != null ? "context=" + context + ", " : "")
        + (request != null ? "requestValueObjects=" + request : "")
        + "]";
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getVer() {
    return ver;
  }

  public void setVer(String ver) {
    this.ver = ver;
  }

  public String getTs() {
    return ts;
  }

  public void setTs(String ts) {
    this.ts = ts;
  }

  public RequestParams getParams() {
    return params;
  }

  public void setParams(RequestParams params) {
    this.params = params;
    if (this.params.getMsgid() == null && requestId != null) this.params.setMsgid(requestId);
  }

  /** @return the env */
  public int getEnv() {
    return env;
  }

  /** @param env the env to set */
  public void setEnv(int env) {
    this.env = env;
  }

  public Integer getTimeout() {
    return timeout == null ? WAIT_TIME_VALUE : timeout;
  }

  public void setTimeout(Integer timeout) {
    if (timeout < MIN_TIMEOUT && timeout > MAX_TIMEOUT) {
      ProjectCommonException.throwServerErrorException(
          ResponseCode.invalidRequestTimeout,
          MessageFormat.format(ResponseCode.invalidRequestTimeout.getErrorMessage(), timeout));
    }
    this.timeout = timeout;
  }
}
