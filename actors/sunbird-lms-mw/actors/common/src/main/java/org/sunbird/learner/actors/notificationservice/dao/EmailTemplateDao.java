package org.sunbird.learner.actors.notificationservice.dao;

public interface EmailTemplateDao {

  /**
   * Get email template information for given name.
   *
   * @param templateName Email template name
   * @return String containing email template information
   */
  String getTemplate(String templateName);
}
