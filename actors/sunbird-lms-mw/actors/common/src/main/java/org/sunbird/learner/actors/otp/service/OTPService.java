package org.sunbird.learner.actors.otp.service;

import java.io.StringWriter;
import java.util.Map;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.learner.actors.notificationservice.dao.EmailTemplateDao;
import org.sunbird.learner.actors.notificationservice.dao.impl.EmailTemplateDaoImpl;
import org.sunbird.learner.actors.otp.dao.OTPDao;
import org.sunbird.learner.actors.otp.dao.impl.OTPDaoImpl;

public class OTPService {

  private static OTPDao otpDao = OTPDaoImpl.getInstance();
  private static EmailTemplateDao emailTemplateDao = EmailTemplateDaoImpl.getInstance();

  public static String getOTPSMSTemplate(String templateName) {
    return emailTemplateDao.getTemplate(templateName);
  }

  public Map<String, Object> getOTPDetails(String type, String key) {
    return otpDao.getOTPDetails(type, key);
  }

  public void insertOTPDetails(String type, String key, String otp) {
    otpDao.insertOTPDetails(type, key, otp);
  }

  public void deleteOtp(String type, String key) {
    otpDao.deleteOtp(type, key);
  }

  public static String getSmsBody(String templateFile, Map<String, String> smsTemplate) {
    try {
      String sms = getOTPSMSTemplate(templateFile);
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
      ProjectLogger.log(
          "OTPService:getSmsBody: Exception occurred with error message = " + ex.getMessage(), ex);
    }
    return "";
  }

  public void updateAttemptCount(Map<String, Object> otpDetails) {
    otpDao.updateAttemptCount(otpDetails);
  }
}
