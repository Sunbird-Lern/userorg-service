package org.sunbird.user.util;

import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.services.sso.SSOManager;
import org.sunbird.services.sso.SSOServiceFactory;

public class UpdatePassword {
  private Logger logger = LoggerFactory.getLogger(UpdatePassword.class);
  private SSOManager ssoManager = SSOServiceFactory.getInstance();

  public UpdatePassword(Map<String, String> contextMap) {
    MDC.setContextMap(contextMap);
    System.out.println("Context from user actor: " + contextMap);
  }

  public boolean updatePassword(Map<String, Object> userMap) {
    logger.info("from logger2 Update user password for userid : " + userMap.get(JsonKey.ID));
    System.out.println("update password context :" + MDC.getCopyOfContextMap());
    if (StringUtils.isNotBlank((String) userMap.get(JsonKey.PASSWORD))) {
      return ssoManager.updatePassword(
          (String) userMap.get(JsonKey.ID), (String) userMap.get(JsonKey.PASSWORD));
    }
    return true;
  }
}
