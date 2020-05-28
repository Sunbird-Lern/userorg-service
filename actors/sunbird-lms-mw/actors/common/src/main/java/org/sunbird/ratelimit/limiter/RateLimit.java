package org.sunbird.ratelimit.limiter;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.responsecode.ResponseCode;

public class RateLimit {

  private String key;
  private String unit;
  private Integer count;
  private Integer limit;
  private Integer ttl;

  public RateLimit(String key, Map<String, Object> rateLimitMap) {
    this.key = key;
    this.unit = (String) rateLimitMap.get(JsonKey.RATE_LIMIT_UNIT);
    this.limit = (int) rateLimitMap.get(JsonKey.RATE);
    this.count = (int) rateLimitMap.get(JsonKey.COUNT);
    this.ttl = (int) rateLimitMap.get(JsonKey.TTL);
  }

  public RateLimit(String key, String unit, Integer limit, int ttl) {
    this.key = key;
    this.unit = unit;
    this.limit = limit;
    this.count = 1;
    this.ttl = ttl;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getUnit() {
    return unit;
  }

  public void setUnit(String unit) {
    this.unit = unit;
  }

  public Integer getCount() {
    return count;
  }

  public Integer getLimit() {
    return limit;
  }

  public void setLimit(Integer limit) {
    this.limit = limit;
  }

  public Integer getTTL() {
    return ttl;
  }

  public void setTTL(Integer ttl) {
    this.ttl = ttl;
  }

  public synchronized void incrementCount() {
    this.count += 1;
  }

  public Map<String, Object> getRecord() {
    if (!isValid()) {
      ProjectLogger.log("RateLimit:getRecord: Invalid record =" + toString(), LoggerEnum.ERROR);
      ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
    }
    Map<String, Object> rateLimitMap = new HashMap<>();
    rateLimitMap.put(JsonKey.KEY, this.key);
    rateLimitMap.put(JsonKey.RATE_LIMIT_UNIT, this.unit);
    rateLimitMap.put(JsonKey.RATE, this.limit);
    rateLimitMap.put(JsonKey.COUNT, this.count);
    return rateLimitMap;
  }

  public boolean isValid() {
    if (StringUtils.isBlank(key) || StringUtils.isBlank(unit)) {
      return false;
    }
    if (count < 1 || limit < 1) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((key == null) ? 0 : key.hashCode());
    result = prime * result + ((unit == null) ? 0 : unit.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    RateLimit other = (RateLimit) obj;
    if (key == null) {
      if (other.key != null) return false;
    } else if (!key.equals(other.key)) return false;
    if (unit == null) {
      if (other.unit != null) return false;
    } else if (!unit.equals(other.unit)) return false;
    return true;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("RateLimit [unit=");
    builder.append(unit);
    builder.append(", count=");
    builder.append(count);
    builder.append(", limit=");
    builder.append(limit);
    builder.append(", ttl=");
    builder.append(ttl);
    builder.append("]");
    return builder.toString();
  }

}
