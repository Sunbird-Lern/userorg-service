package org.sunbird.user.dao;

import java.util.Map;
import org.sunbird.common.models.response.Response;

public interface JobProfileDao {

  void createJobProfile(Map<String, Object> jobProfile);

  void updateJobProfile(Map<String, Object> jobProfile);

  void upsertJobProfile(Map<String, Object> jobProfile);

  void deleteJobProfile(String jobProfileId);

  Response getPropertiesValueById(String propertyName, String identifier);
}
