package org.sunbird.actorutil.systemsettings;

import akka.actor.ActorRef;
import com.fasterxml.jackson.core.type.TypeReference;
import org.sunbird.models.systemsetting.SystemSetting;

/**
 * This interface defines methods supported by System Setting service.
 *
 * @author Amit Kumar
 */
public interface SystemSettingClient {

  /**
   * Get system setting information for given field (setting) name.
   *
   * @param actorRef Actor reference
   * @param field System setting field name
   * @return System setting details
   */
  SystemSetting getSystemSettingByField(ActorRef actorRef, String field);

  /**
   * Get system setting information for given field (setting) and key name.
   *
   * @param actorRef Actor reference
   * @param field System setting field name
   * @param key Key (e.g. csv.mandatoryColumns) within system setting information
   * @param typeReference Type reference for value corresponding to specified key
   * @return System setting value corresponding to given field and key name
   */
  <T> T getSystemSettingByFieldAndKey(
      ActorRef actorRef, String field, String key, TypeReference typeReference);
}
