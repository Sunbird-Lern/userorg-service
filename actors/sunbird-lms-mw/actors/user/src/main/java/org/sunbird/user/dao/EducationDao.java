package org.sunbird.user.dao;

import java.util.Map;
import org.sunbird.common.models.response.Response;

public interface EducationDao {

  void createEducation(Map<String, Object> education);

  void updateEducation(Map<String, Object> education);

  void upsertEducation(Map<String, Object> education);

  void deleteEducation(String educationId);

  Response getPropertiesValueById(String propertyName, String identifier);
}
