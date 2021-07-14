package org.sunbird.common.quartz.scheduler;

import akka.actor.ActorRef;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import org.quartz.Job;
import org.sunbird.actor.service.SunbirdMWService;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.datasecurity.DecryptionService;
import org.sunbird.datasecurity.EncryptionService;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.learner.util.Util;
import org.sunbird.request.Request;

/** @author Mahesh Kumar Gangula */
public abstract class BaseJob implements Job {

  protected Util.DbInfo bulkUploadDbInfo = Util.dbInfoMap.get(JsonKey.BULK_OP_DB);
  protected CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  protected ObjectMapper mapper = new ObjectMapper();
  protected HashSet<String> verifiedChannelOrgExternalIdSet = new HashSet<>();
  protected ElasticSearchService elasticSearchService = EsClientFactory.getInstance(JsonKey.REST);
  protected DecryptionService decryptionService =
      org.sunbird.datasecurity.impl.ServiceFactory.getDecryptionServiceInstance(
          null);
  protected EncryptionService encryptionService =
      org.sunbird.datasecurity.impl.ServiceFactory.getEncryptionServiceInstance(
          null);

  public void tellToBGRouter(Request request) {
    SunbirdMWService.tellToBGRouter(request, ActorRef.noSender());
  }
}
