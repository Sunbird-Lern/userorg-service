package org.sunbird.learner.actors.skill.dao;

import java.util.List;
import java.util.Map;
import org.sunbird.models.user.skill.Skill;

public interface UserSkillDao {

  /**
   * Add user skills.
   *
   * @param userSkill User skills information
   */
  void add(Map<String, Object> userSkill);

  /**
   * Delete user skills.
   *
   * @param identifierList List of identifiers for user skills to be deleted
   * @return Status of delete skills operation
   */
  boolean delete(List<String> identifierList);

  /**
   * Get skill information.
   *
   * @param id Skill identifier
   * @return Skill information
   */
  Skill read(String id);

  /**
   * Update user skills.
   *
   * @param skill Skill which needs to be updated
   */
  void update(Skill skill);
}
