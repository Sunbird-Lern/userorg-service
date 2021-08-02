package org.sunbird.dao.notification;

import org.sunbird.request.RequestContext;

public interface EmailTemplateDao {

  /**
   * Get email template information for given name.
   *
   * @param templateName Email template name
   * @param context
   * @return String containing email template information
   */
  String getTemplate(String templateName, RequestContext context);
}
