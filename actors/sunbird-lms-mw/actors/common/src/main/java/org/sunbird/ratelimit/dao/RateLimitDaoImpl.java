package org.sunbird.ratelimit.dao;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.ratelimit.limiter.RateLimit;

public class RateLimitDaoImpl implements RateLimitDao {

  private static final String TABLE_NAME = JsonKey.RATE_LIMIT;
  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private static volatile RateLimitDao rateLimitDao;

  public static RateLimitDao getInstance() {
    if (rateLimitDao == null) {
      synchronized (RateLimitDaoImpl.class) {
        if (rateLimitDao == null) {
          rateLimitDao = new RateLimitDaoImpl();
        }
      }
    }
    return rateLimitDao;
  }

  @Override
  public void insertRateLimits(List<RateLimit> rateLimits) {
    if (CollectionUtils.isEmpty(rateLimits)) {
      return;
    }
    List<Integer> ttls =
        rateLimits.stream().map(rateLimit -> rateLimit.getTTL()).collect(Collectors.toList());
    List<Map<String, Object>> records =
        rateLimits.stream().map(rateLimit -> rateLimit.getRecord()).collect(Collectors.toList());

    cassandraOperation.batchInsertWithTTL(Util.KEY_SPACE_NAME, TABLE_NAME, records, ttls);
  }

  @Override
  public List<Map<String, Object>> getRateLimits(String key) {
    Map<String, Object> partitionKey = new HashMap<>();
    partitionKey.put(JsonKey.KEY, key);

    Map<String, String> ttlPropsWithAlias = new HashMap<>();
    ttlPropsWithAlias.put(JsonKey.COUNT, JsonKey.TTL);

    List<String> properties =
        Arrays.asList(JsonKey.KEY, JsonKey.RATE_LIMIT_UNIT, JsonKey.COUNT, JsonKey.RATE);

    Response response =
        cassandraOperation.getRecordsByIdsWithSpecifiedColumnsAndTTL(
            Util.KEY_SPACE_NAME, TABLE_NAME, partitionKey, properties, ttlPropsWithAlias);

    return (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
  }
}
