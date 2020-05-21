package org.sunbird.common.factory;

import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.ElasticSearchTcpImpl;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;

public class EsClientFactory {

  private static ElasticSearchService tcpClient = null;
  private static ElasticSearchService restClient = null;

  /**
   * This method return REST/TCP client for elastic search
   *
   * @param type can be "tcp" or "rest"
   * @return ElasticSearchService with the respected type impl
   */
  public static ElasticSearchService getInstance(String type) {
    if (JsonKey.TCP.equals(type)) {
      return getTcpClient();
    } else if (JsonKey.REST.equals(type)) {
      return getRestClient();
    } else {
      ProjectLogger.log(
          "EsClientFactory:getInstance: value for client type provided null ", LoggerEnum.ERROR);
    }
    return null;
  }

  private static ElasticSearchService getTcpClient() {
    if (tcpClient == null) {
      tcpClient = new ElasticSearchTcpImpl();
    }
    return tcpClient;
  }

  private static ElasticSearchService getRestClient() {
    if (restClient == null) {
      restClient = new ElasticSearchRestHighImpl();
    }
    return restClient;
  }
}
