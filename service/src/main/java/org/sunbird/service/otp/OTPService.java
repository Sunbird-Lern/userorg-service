package org.sunbird.service.otp;

import java.io.StringWriter;
import java.util.Map;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.sunbird.keys.JsonKey;
import org.sunbird.learner.actors.notificationservice.dao.EmailTemplateDao;
import org.sunbird.learner.actors.notificationservice.dao.impl.EmailTemplateDaoImpl;
import org.sunbird.learner.actors.otp.dao.OTPDao;
import org.sunbird.learner.actors.otp.dao.impl.OTPDaoImpl;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;
import org.sunbird.util.ProjectUtil;

public class OTPService {
  private static LoggerUtil logger = new LoggerUtil(OTPService.class);

  private static OTPDao otpDao = OTPDaoImpl.getInstance();
  private static EmailTemplateDao emailTemplateDao = EmailTemplateDaoImpl.getInstance();

  public static String getOTPSMSTemplate(String templateName, RequestContext context) {
    return emailTemplateDao.getTemplate(templateName, context);
  }

  public Map<String, Object> getOTPDetails(String type, String key, RequestContext context) {
    return otpDao.getOTPDetails(type, key, context);
  }

  public void insertOTPDetails(String type, String key, String otp, RequestContext context) {
    otpDao.insertOTPDetails(type, key, otp, context);
  }

  public void deleteOtp(String type, String key, RequestContext context) {
    otpDao.deleteOtp(type, key, context);
  }

  public static String getSmsBody(
      String templateFile, Map<String, String> smsTemplate, RequestContext requestContext) {
    try {
      String sms = getOTPSMSTemplate(templateFile, requestContext);
      RuntimeServices rs = RuntimeSingleton.getRuntimeServices();
      SimpleNode sn = rs.parse(sms, "Sms Information");
      Template t = new Template();
      t.setRuntimeServices(rs);
      t.setData(sn);
      t.initDocument();
      VelocityContext context = new VelocityContext();
      context.put(JsonKey.OTP, smsTemplate.get(JsonKey.OTP));
      context.put(
          JsonKey.OTP_EXPIRATION_IN_MINUTES, smsTemplate.get(JsonKey.OTP_EXPIRATION_IN_MINUTES));
      context.put(
          JsonKey.INSTALLATION_NAME,
          ProjectUtil.getConfigValue(JsonKey.SUNBIRD_INSTALLATION_DISPLAY_NAME));

      StringWriter writer = new StringWriter();
      t.merge(context, writer);
      return writer.toString();
    } catch (Exception ex) {
      logger.error(
          requestContext,
          "OTPService:getSmsBody: Exception occurred with error message = " + ex.getMessage(),
          ex);
    }
    return "";
  }

  public void updateAttemptCount(Map<String, Object> otpDetails, RequestContext context) {
    otpDao.updateAttemptCount(otpDetails, context);
  }
}
