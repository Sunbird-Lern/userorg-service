package org.sunbird.util;

import java.io.StringWriter;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.sunbird.dao.notification.EmailTemplateDao;
import org.sunbird.dao.notification.impl.EmailTemplateDaoImpl;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;

public class SMSTemplateProvider {
  private static LoggerUtil logger = new LoggerUtil(SMSTemplateProvider.class);
  public static final String SMS_PROVIDER =
      ProjectUtil.getConfigValue(JsonKey.SMS_GATEWAY_PROVIDER);
  private static EmailTemplateDao emailTemplateDao = EmailTemplateDaoImpl.getInstance();

  private SMSTemplateProvider() {}

  private static String getTemplate(String templateId, RequestContext context) {
    String defaultTemplate = templateId;
    if (StringUtils.isNotBlank(templateId) && JsonKey.NIC.equalsIgnoreCase(SMS_PROVIDER)) {
      defaultTemplate = templateId + "_nic";
    }
    return emailTemplateDao.getTemplate(defaultTemplate, context);
  }

  public static String getSMSBody(
      String smsTemplate, Map<String, String> templateConfig, RequestContext requestContext) {
    try {
      String template = getTemplate(smsTemplate, requestContext);
      RuntimeServices rs = RuntimeSingleton.getRuntimeServices();
      SimpleNode sn = rs.parse(template, "Sms Information");
      Template t = new Template();
      t.setRuntimeServices(rs);
      t.setData(sn);
      t.initDocument();
      VelocityContext context = new VelocityContext(templateConfig);
      StringWriter writer = new StringWriter();
      t.merge(context, writer);
      return writer.toString();
    } catch (Exception ex) {
      logger.error("Exception occurred while formatting SMS ", ex);
    }
    return "";
  }
}
