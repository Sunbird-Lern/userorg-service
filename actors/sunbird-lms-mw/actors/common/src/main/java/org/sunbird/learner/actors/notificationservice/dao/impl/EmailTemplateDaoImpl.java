package org.sunbird.learner.actors.notificationservice.dao.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.notificationservice.dao.EmailTemplateDao;

public class EmailTemplateDaoImpl implements EmailTemplateDao {

  static EmailTemplateDao emailTemplateDao;
  private static final String EMAIL_TEMPLATE = "email_template";
  private static final String DEFAULT_EMAIL_TEMPLATE_NAME = "default";
  private static final String TEMPLATE = "template";

  public static EmailTemplateDao getInstance() {
    if (emailTemplateDao == null) {
      emailTemplateDao = new EmailTemplateDaoImpl();
    }
    return emailTemplateDao;
  }

  @Override
  public String getTemplate(String templateName) {

    List<String> idList = new ArrayList<>();
    if (StringUtils.isBlank(templateName)) {
      idList.add(DEFAULT_EMAIL_TEMPLATE_NAME);
    } else {
      idList.add(templateName);
    }
    Response response =
        getCassandraOperation().getRecordsByPrimaryKeys(
            JsonKey.SUNBIRD, EMAIL_TEMPLATE, idList, JsonKey.NAME);
    List<Map<String, Object>> emailTemplateList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    Map<String, Object> map = Collections.emptyMap();
    if (CollectionUtils.isNotEmpty(emailTemplateList)) {
      map = emailTemplateList.get(0);
    }
    return (String) map.get(TEMPLATE);
  }

  private CassandraOperation getCassandraOperation(){
    return ServiceFactory.getInstance();
  }

}
