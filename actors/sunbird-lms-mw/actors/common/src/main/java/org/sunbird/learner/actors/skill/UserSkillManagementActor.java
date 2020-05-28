package org.sunbird.learner.actors.skill;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.*;
import org.sunbird.common.models.util.ProjectUtil.EsType;
import org.sunbird.common.models.util.datasecurity.OneWayHashing;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.skill.dao.UserSkillDao;
import org.sunbird.learner.actors.skill.dao.impl.UserSkillDaoImpl;
import org.sunbird.learner.util.Util;
import org.sunbird.models.user.skill.Skill;
import org.sunbird.telemetry.util.TelemetryUtil;
import scala.concurrent.Future;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import static org.sunbird.learner.util.Util.isNotNull;
import static org.sunbird.learner.util.Util.isNull;

/**
 * Class to provide functionality for Add and Endorse the user skills . Created by arvind on
 * 18/10/17.
 */
@ActorConfig(
  tasks = {"addSkill", "getSkill", "getSkillsList", "updateSkill", "addUserSkillEndorsement"},
  asyncTasks = {}
)
public class UserSkillManagementActor extends BaseActor {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private static final String REF_SKILLS_DB_ID = "001";
  private UserSkillDao userSkillDao = UserSkillDaoImpl.getInstance();
  private ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);

  @Override
  public void onReceive(Request request) throws Throwable {
    String operation = request.getOperation();
    Util.initializeContext(request, TelemetryEnvKey.USER);

    switch (operation) {
      case "addSkill":
        addOrEndorseSkill(request);
        break;
      case "getSkill":
        getSkill(request);
        break;
      case "getSkillsList":
        getSkillsList();
        break;
      case "updateSkill":
        updateSkill(request);
        break;
      case "addUserSkillEndorsement":
        addUserSkillEndorsement(request);
        break;
      default:
        onReceiveUnsupportedOperation("UserSkillManagementActor");
    }
  }

  private void updateSkill(Request actorMessage) {
    ProjectLogger.log(
        "UserSkillManagementActor: updateSkill called",
        actorMessage.getRequest(),
        LoggerEnum.DEBUG.name());
    String userId = (String) actorMessage.getContext().get(JsonKey.REQUESTED_BY);
    getUser(userId, JsonKey.USER_ID);
    List<String> newUserSkillsSet = (List<String>) actorMessage.getRequest().get(JsonKey.SKILLS);

    Map<String, Object> result = findUserSkills(userId);
    if (result.isEmpty() || ((List<Map<String, Object>>) result.get(JsonKey.CONTENT)).isEmpty()) {
      saveUserSkill(newUserSkillsSet, userId);
    } else {
      List<Map<String, Object>> searchedUserList =
          (List<Map<String, Object>>) result.get(JsonKey.CONTENT);

      Map<String, Object> userMap = new HashMap();
      userMap = searchedUserList.get(0);

      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
      objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
      List<Map<String, Object>> userSkills =
          objectMapper.convertValue(userMap.get(JsonKey.SKILLS), List.class);
      if (!CollectionUtils.isEmpty(userSkills)) {
        List<Skill> skills =
            userSkills
                .stream()
                .map(
                    map -> {
                      return objectMapper.convertValue(map, Skill.class);
                    })
                .collect(Collectors.toList());
        HashSet<Skill> currentUserSkillsSet = new HashSet<>(skills);
        List<Skill> commonSkills =
            currentUserSkillsSet
                .stream()
                .flatMap(
                    skill ->
                        newUserSkillsSet
                            .stream()
                            .filter(
                                skillName -> {
                                  String id =
                                      OneWayHashing.encryptVal(
                                          userId
                                              + JsonKey.PRIMARY_KEY_DELIMETER
                                              + skillName.toLowerCase());
                                  return skill.getId().equals(id);
                                })
                            .map(skillName -> skill))
                .collect(Collectors.toList());
        List<String> addedSkillsList = newUserSkillsSet;
        HashSet<Skill> removedSkillsList = currentUserSkillsSet;

        commonSkills.forEach(
            skill -> {
              if (addedSkillsList.contains(skill.getSkillName())) {
                addedSkillsList.remove(skill.getSkillName());
                removedSkillsList.remove(skill);
              }
            });

        if (CollectionUtils.isNotEmpty(addedSkillsList)) {
          saveUserSkill(addedSkillsList, userId);
        }
        if (CollectionUtils.isNotEmpty(removedSkillsList)) {
          List<String> idList =
              removedSkillsList.stream().map(skill -> skill.getId()).collect(Collectors.toList());
          Boolean deleted = userSkillDao.delete(idList);
          if (!deleted) {
            ProjectLogger.log(
                "UserSkillManagementActor:updateSkill: Delete skills failed for " + userId,
                idList,
                LoggerEnum.ERROR.name());
          }

          updateES(userId);
        }
      } else {
        saveUserSkill(newUserSkillsSet, userId);
      }
    }
    Response response = new Response();
    response.getResult().put(JsonKey.RESULT, "SUCCESS");
    sender().tell(response, self());

    addTelemetry(userId, actorMessage);
    updateMasterSkillsList(newUserSkillsSet);
  }

  private void saveUserSkill(List<String> skillSet, String userId) {
    for (String skillName : skillSet) {
      String id =
          OneWayHashing.encryptVal(
              userId + JsonKey.PRIMARY_KEY_DELIMETER + skillName.toLowerCase());
      Map<String, Object> userSkillMap = new HashMap<>();
      userSkillMap.put(JsonKey.ID, id);
      userSkillMap.put(JsonKey.USER_ID, userId);
      userSkillMap.put(JsonKey.SKILL_NAME, skillName);
      userSkillMap.put(JsonKey.SKILL_NAME_TO_LOWERCASE, skillName.toLowerCase());
      userSkillMap.put(JsonKey.CREATED_BY, userId);
      userSkillMap.put(
          JsonKey.CREATED_ON, new Timestamp(Calendar.getInstance().getTime().getTime()));
      userSkillMap.put(JsonKey.LAST_UPDATED_BY, userId);
      userSkillMap.put(
          JsonKey.LAST_UPDATED_ON, new Timestamp(Calendar.getInstance().getTime().getTime()));
      userSkillMap.put(JsonKey.ENDORSEMENT_COUNT, 0);
      userSkillDao.add(userSkillMap);
      updateES(userId);
    }
  }

  private Map<String, Object> findUserSkills(String userId) {
    HashMap<String, Object> esDtoMap = new HashMap<>();

    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.USER_ID, userId);
    esDtoMap.put(JsonKey.FILTERS, filters);
    List<String> fields = new ArrayList<>();
    fields.add(JsonKey.SKILLS);
    esDtoMap.put(JsonKey.FIELDS, fields);
    Future<Map<String, Object>> resultF =
        esService.search(ElasticSearchHelper.createSearchDTO(esDtoMap), EsType.user.getTypeName());
    return (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
  }

  /** Method will return all the list of skills , it is type of reference data ... */
  private void getSkillsList() {
    Util.DbInfo skillsListDbInfo = Util.dbInfoMap.get(JsonKey.SKILLS_LIST_DB);
    ProjectLogger.log("UserSkillManagementActor:getSkillsList called");
    Map<String, Object> skills = new HashMap<>();
    Response skilldbresponse =
        cassandraOperation.getRecordById(
            skillsListDbInfo.getKeySpace(), skillsListDbInfo.getTableName(), REF_SKILLS_DB_ID);
    List<Map<String, Object>> skillList =
        (List<Map<String, Object>>) skilldbresponse.get(JsonKey.RESPONSE);

    if (!skillList.isEmpty()) {
      skills = skillList.get(0);
    }
    Response response = new Response();
    response.getResult().put(JsonKey.SKILLS, skills.get(JsonKey.SKILLS));
    sender().tell(response, self());
  }

  /**
   * Method to get the list of skills of the user on basis of UserId ...
   *
   * @param actorMessage
   */
  private void getSkill(Request actorMessage) {

    ProjectLogger.log("UserSkillManagementActor:getSkill called");
    String endorsedUserId = (String) actorMessage.getRequest().get(JsonKey.ENDORSED_USER_ID);
    if (StringUtils.isBlank(endorsedUserId)) {
      throw new ProjectCommonException(
          ResponseCode.endorsedUserIdRequired.getErrorCode(),
          ResponseCode.endorsedUserIdRequired.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    Map<String, Object> result = findUserSkills(endorsedUserId);
    if (result.isEmpty() || ((List<Map<String, Object>>) result.get(JsonKey.CONTENT)).isEmpty()) {
      throw new ProjectCommonException(
          ResponseCode.invalidUserId.getErrorCode(),
          ResponseCode.invalidUserId.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    List<Map<String, Object>> skillList = (List<Map<String, Object>>) result.get(JsonKey.CONTENT);

    Map<String, Object> skillMap = new HashMap();
    if (!skillList.isEmpty()) {
      skillMap = skillList.get(0);
    }

    Response response = new Response();
    response.getResult().put(JsonKey.SKILLS, skillMap.get(JsonKey.SKILLS));
    sender().tell(response, self());
  }

  /**
   * Method to add or endorse the user skill ...
   *
   * @param actorMessage
   */
  private void addOrEndorseSkill(Request actorMessage) {

    ProjectLogger.log("UserSkillManagementActor:endorseSkill called");
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    // object of telemetry event...
    Map<String, Object> targetObject = null;
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    Util.DbInfo userDbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
    String endoresedUserId = (String) actorMessage.getRequest().get(JsonKey.ENDORSED_USER_ID);

    List<String> list = (List<String>) actorMessage.getRequest().get(JsonKey.SKILL_NAME);
    CopyOnWriteArraySet<String> skillset = new CopyOnWriteArraySet<>(list);
    String requestedByUserId = (String) actorMessage.getRequest().get(JsonKey.USER_ID);
    if (StringUtils.isBlank(requestedByUserId)) {
      requestedByUserId = (String) actorMessage.getContext().get(JsonKey.REQUESTED_BY);
    }
    ProjectLogger.log(
        "UserSkillManagementActor:endorseSkill: context userId "
            + actorMessage.getContext().get(JsonKey.REQUESTED_BY),
        LoggerEnum.INFO.name());
    ProjectLogger.log(
        "UserSkillManagementActor:endorseSkill: context endorsedUserId " + endoresedUserId,
        LoggerEnum.INFO.name());
    Response response1 =
        cassandraOperation.getRecordById(
            userDbInfo.getKeySpace(), userDbInfo.getTableName(), endoresedUserId);
    Response response2 =
        cassandraOperation.getRecordById(
            userDbInfo.getKeySpace(), userDbInfo.getTableName(), requestedByUserId);
    List<Map<String, Object>> endoresedList =
        (List<Map<String, Object>>) response1.get(JsonKey.RESPONSE);
    List<Map<String, Object>> requestedUserList =
        (List<Map<String, Object>>) response2.get(JsonKey.RESPONSE);

    // check whether both userid exist or not if not throw exception
    if (endoresedList.isEmpty() || requestedUserList.isEmpty()) {
      // generate context and params here ...

      ProjectLogger.log(
          "UserSkillManagementActor:endorseSkill: context Valid User", LoggerEnum.INFO.name());
      throw new ProjectCommonException(
          ResponseCode.invalidUserId.getErrorCode(),
          ResponseCode.invalidUserId.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    Map<String, Object> endoresedMap = endoresedList.get(0);
    Map<String, Object> requestedUserMap = requestedUserList.get(0);

    // check whether both belongs to same org or not(check root or id of both users)
    // , if not then
    // throw exception ---
    if (!compareStrings(
        (String) endoresedMap.get(JsonKey.ROOT_ORG_ID),
        (String) requestedUserMap.get(JsonKey.ROOT_ORG_ID))) {
      ProjectLogger.log(
          "UserSkillManagementActor:endorseSkill: context rootOrg", LoggerEnum.INFO.name());
      throw new ProjectCommonException(
          ResponseCode.canNotEndorse.getErrorCode(),
          ResponseCode.canNotEndorse.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    Util.DbInfo userSkillDbInfo = Util.dbInfoMap.get(JsonKey.USER_SKILL_DB);
    for (String skillName : skillset) {

      if (!StringUtils.isBlank(skillName)) {

        // check whether user have already this skill or not -
        String id =
            OneWayHashing.encryptVal(
                endoresedUserId + JsonKey.PRIMARY_KEY_DELIMETER + skillName.toLowerCase());
        Response response =
            cassandraOperation.getRecordById(
                userSkillDbInfo.getKeySpace(), userSkillDbInfo.getTableName(), id);
        List<Map<String, Object>> responseList =
            (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);

        // prepare correlted object ...
        TelemetryUtil.generateCorrelatedObject(id, "skill", null, correlatedObject);

        if (responseList.isEmpty()) {
          // means this is first time skill coming so add this one
          Map<String, Object> skillMap = new HashMap<>();
          skillMap.put(JsonKey.ID, id);
          skillMap.put(JsonKey.USER_ID, endoresedUserId);
          skillMap.put(JsonKey.SKILL_NAME, skillName);
          ProjectLogger.log(
              "UserSkillManagementActor:endorseSkill: context skillName " + skillName,
              LoggerEnum.INFO.name());
          skillMap.put(JsonKey.SKILL_NAME_TO_LOWERCASE, skillName.toLowerCase());
          //          skillMap.put(JsonKey.ADDED_BY, requestedByUserId);
          //          skillMap.put(JsonKey.ADDED_AT, format.format(new Date()));
          Map<String, String> endoresers = new HashMap<>();

          List<Map<String, String>> endorsersList = new ArrayList<>();
          endoresers.put(JsonKey.USER_ID, requestedByUserId);
          endoresers.put(JsonKey.ENDORSE_DATE, format.format(new Date()));
          endorsersList.add(endoresers);

          skillMap.put(JsonKey.ENDORSERS_LIST, endorsersList);
          skillMap.put(JsonKey.ENDORSEMENT_COUNT, 0);
          ObjectMapper objectMapper = new ObjectMapper();
          try {
            String responseObj = (String) objectMapper.writeValueAsString(skillMap);
            ProjectLogger.log(
                "UserSkillManagementActor:endorseSkill: responseMap while insert" + responseObj,
                LoggerEnum.INFO.name());

          } catch (JsonProcessingException e) {
            ProjectLogger.log("Exception while converting", LoggerEnum.INFO);
          }
          cassandraOperation.insertRecord(
              userSkillDbInfo.getKeySpace(), userSkillDbInfo.getTableName(), skillMap);

          updateES(endoresedUserId);
        } else {
          // skill already exist for user simply update the then check if it is already
          // added by
          // same user then dont do anything
          // otherwise update the existing one ...

          Map<String, Object> responseMap = responseList.get(0);
          // check whether requested user has already endoresed to that user or not
          List<Map<String, String>> endoresersList =
              (List<Map<String, String>>) responseMap.get(JsonKey.ENDORSERS_LIST);
          boolean flag = false;
          for (Map<String, String> map : endoresersList) {
            if (map.get(JsonKey.USER_ID).equalsIgnoreCase(requestedByUserId)) {
              flag = true;
              break;
            }
          }
          if (flag) {
            // donot do anything..
            ProjectLogger.log(requestedByUserId + " has already endorsed the " + endoresedUserId);
          } else {
            ProjectLogger.log(
                "UserSkillManagementActor:endorseSkill: context skillName " + skillName,
                LoggerEnum.INFO.name());
            Integer endoresementCount = (Integer) responseMap.get(JsonKey.ENDORSEMENT_COUNT) + 1;
            Map<String, String> endorsersMap = new HashMap<>();
            endorsersMap.put(JsonKey.USER_ID, requestedByUserId);

            ProjectLogger.log(
                "UserSkillManagementActor:endorseSkill: context requestedByUserId "
                    + requestedByUserId,
                LoggerEnum.INFO.name());
            endorsersMap.put(JsonKey.ENDORSE_DATE, format.format(new Date()));
            endoresersList.add(endorsersMap);

            responseMap.put(JsonKey.ENDORSERS_LIST, endoresersList);
            responseMap.put(JsonKey.ENDORSEMENT_COUNT, endoresementCount);
            /*
             *Logs
             * */
            ObjectMapper objectMapper = new ObjectMapper();
            try {
              String responseObj = (String) objectMapper.writeValueAsString(responseMap);
              ProjectLogger.log(
                  "UserSkillManagementActor:endorseSkill:  responseMap while update" + responseObj,
                  LoggerEnum.INFO.name());

            } catch (JsonProcessingException e) {
              ProjectLogger.log("Exception while converting", LoggerEnum.INFO);
            }

            cassandraOperation.updateRecord(
                userSkillDbInfo.getKeySpace(), userSkillDbInfo.getTableName(), responseMap);
            updateES(endoresedUserId);
          }
        }
      } else {
        skillset.remove(skillName);
      }
    }

    Response response3 = new Response();
    response3.getResult().put(JsonKey.RESULT, "SUCCESS");
    sender().tell(response3, self());

    addTelemetry(endoresedUserId, actorMessage);

    updateMasterSkillsList(new ArrayList<>(skillset));
  }

  private void addUserSkillEndorsement(Request request) {
    String skillName = (String) request.getRequest().get(JsonKey.SKILL_NAME);
    String endorsedUserId = (String) request.getRequest().get(JsonKey.ENDORSED_USER_ID);
    String requestedUserId = (String) request.getRequest().get(JsonKey.USER_ID);
    String skillId =
        OneWayHashing.encryptVal(
            endorsedUserId + JsonKey.PRIMARY_KEY_DELIMETER + skillName.toLowerCase());
    validateUserRootOrg(requestedUserId, endorsedUserId);
    Skill skill = userSkillDao.read(skillId);
    if (null == skill) {
      throw new ProjectCommonException(
          ResponseCode.invalidParameterValue.getErrorCode(),
          ResponseCode.invalidParameterValue.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode(),
          skillName,
          JsonKey.SKILL_NAME);
    }
    String endorsersId = (String) request.getRequest().get(JsonKey.USER_ID);
    String endorsedId = (String) request.getRequest().get(JsonKey.ENDORSED_USER_ID);
    addEndorsement(skill, endorsedId, endorsersId);
    addTelemetry(endorsersId, request);
  }

  private void addEndorsement(Skill skill, String endorsedId, String endorsersId) {

    List<HashMap<String, String>> endorsersList = skill.getEndorsersList();
    skill = updateEndorsersList(skill, endorsersList, endorsersId, endorsedId);
    userSkillDao.update(skill);
    updateES(endorsedId);
    Response response = new Response();
    response.getResult().put(JsonKey.RESULT, "SUCCESS");
    sender().tell(response, self());
  }

  private Skill updateEndorsersList(
      Skill skill,
      List<HashMap<String, String>> endorsersList,
      String endorsersId,
      String endorsedId) {
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    HashMap<String, String> endorsers = new HashMap<>();
    if (CollectionUtils.isEmpty(endorsersList)) {
      endorsers.put(JsonKey.USER_ID, endorsersId);
      endorsers.put(JsonKey.ENDORSE_DATE, format.format(new Date()));
      skill.setEndorsementCount(1);
      endorsersList.add(endorsers);
    } else {
      boolean foundEndorser = false;
      for (Map<String, String> map : endorsersList) {
        if ((map.get(JsonKey.USER_ID)).equalsIgnoreCase(endorsersId)) {
          foundEndorser = true;
          break;
        }
      }
      if (foundEndorser) {
        // donot do anything..
        ProjectLogger.log(endorsersId + " has already endorsed the " + endorsedId);
      } else {
        endorsers.put(JsonKey.USER_ID, endorsersId);
        endorsers.put(JsonKey.ENDORSE_DATE, format.format(new Date()));
        skill.setEndorsementCount(skill.getEndorsementCount() + 1);
        endorsersList.add(endorsers);
      }
    }
    skill.setEndorsersList(endorsersList);
    return skill;
  }

  private void updateMasterSkillsList(List<String> skillset) {
    Util.DbInfo skillsListDbInfo = Util.dbInfoMap.get(JsonKey.SKILLS_LIST_DB);
    Map<String, Object> skills = new HashMap<>();
    List<String> skillsList = null;
    Response skilldbresponse =
        cassandraOperation.getRecordById(
            skillsListDbInfo.getKeySpace(), skillsListDbInfo.getTableName(), REF_SKILLS_DB_ID);
    List<Map<String, Object>> list =
        (List<Map<String, Object>>) skilldbresponse.get(JsonKey.RESPONSE);

    if (!list.isEmpty()) {
      skills = list.get(0);
      skillsList = (List<String>) skills.get(JsonKey.SKILLS);

    } else {
      // craete new Entry into the
      skillsList = new ArrayList<>();
    }

    for (String skillName : skillset) {
      if (!skillsList.contains(skillName.toLowerCase())) {
        skillsList.add(skillName.toLowerCase());
      }
    }

    skills.put(JsonKey.ID, REF_SKILLS_DB_ID);
    skills.put(JsonKey.SKILLS, skillsList);
    cassandraOperation.upsertRecord(
        skillsListDbInfo.getKeySpace(), skillsListDbInfo.getTableName(), skills);
  }

  @SuppressWarnings("unchecked")
  private void updateES(String userId) {

    // get all records from cassandra as list and add that list to user in
    // ElasticSearch ...
    Util.DbInfo userSkillDbInfo = Util.dbInfoMap.get(JsonKey.USER_SKILL_DB);
    Response response =
        cassandraOperation.getRecordsByProperty(
            userSkillDbInfo.getKeySpace(), userSkillDbInfo.getTableName(), JsonKey.USER_ID, userId);
    List<Map<String, Object>> responseList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    Map<String, Object> esMap = new HashMap<>();
    esMap.put(JsonKey.SKILLS, responseList);
    Future<Map<String, Object>> profileF =
        esService.getDataByIdentifier(EsType.user.getTypeName(), userId);
    Map<String, Object> profile =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(profileF);
    if (MapUtils.isNotEmpty(profile)) {
      Map<String, String> visibility =
          (Map<String, String>) profile.get(JsonKey.PROFILE_VISIBILITY);
      // Fetching complete private map including global settings
      Map<String, String> privateVisibilityMap =
          Util.getCompleteProfileVisibilityPrivateMap(
              visibility, getActorRef(ActorOperations.GET_SYSTEM_SETTING.getValue()));
      if (MapUtils.isNotEmpty(privateVisibilityMap)
          && privateVisibilityMap.containsKey(JsonKey.SKILLS)) {
        Future<Map<String, Object>> visibilityMapF =
            esService.getDataByIdentifier(EsType.userprofilevisibility.getTypeName(), userId);
        Map<String, Object> visibilityMap =
            (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(visibilityMapF);
        if (MapUtils.isNotEmpty(visibilityMap)) {
          visibilityMap.putAll(esMap);
          esService.save(EsType.userprofilevisibility.getTypeName(), userId, visibilityMap);
        }
      } else {
        esService.update(EsType.user.getTypeName(), userId, esMap);
      }
    }
  }

  // method will compare two strings and return true id both are same otherwise
  // false ...
  private boolean compareStrings(String first, String second) {
    if (isNull(first) && isNull(second)) {
      return true;
    }
    if ((isNull(first) && isNotNull(second)) || (isNull(second) && isNotNull(first))) {
      return false;
    }
    return first.equalsIgnoreCase(second);
  }

  protected SearchDTO createESRequest(
      Map<String, Object> filters, Map<String, String> aggs, List<String> fields) {
    SearchDTO searchDTO = new SearchDTO();

    searchDTO.getAdditionalProperties().put(JsonKey.FILTERS, filters);
    if (ProjectUtil.isNotNull(aggs)) {
      searchDTO.getFacets().add(aggs);
    }
    if (ProjectUtil.isNotNull(fields)) {
      searchDTO.setFields(fields);
    }
    return searchDTO;
  }

  private void addTelemetry(String userId, Request request) {
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    Map<String, Object> targetObject;
    targetObject = TelemetryUtil.generateTargetObject(userId, JsonKey.USER, JsonKey.UPDATE, null);
    TelemetryUtil.generateCorrelatedObject(userId, JsonKey.USER, null, correlatedObject);
    TelemetryUtil.telemetryProcessingCall(request.getRequest(), targetObject, correlatedObject, request.getContext());
  }

  private void validateUserRootOrg(String requestedUserId, String endorsedUserId) {
    Map<String, Object> endorsedMap = getUser(endorsedUserId, JsonKey.ENDORSED_USER_ID);
    Map<String, Object> requestedUserMap = getUser(requestedUserId, JsonKey.USER_ID);

    if (!compareStrings(
        (String) endorsedMap.get(JsonKey.ROOT_ORG_ID),
        (String) requestedUserMap.get(JsonKey.ROOT_ORG_ID))) {
      throw new ProjectCommonException(
          ResponseCode.canNotEndorse.getErrorCode(),
          ResponseCode.canNotEndorse.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  private Map<String, Object> getUser(String id, String parameter) {
    Util.DbInfo userDbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
    Response response =
        cassandraOperation.getRecordById(userDbInfo.getKeySpace(), userDbInfo.getTableName(), id);
    List<Map<String, Object>> responseUserList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);

    if (responseUserList.isEmpty()) {
      throw new ProjectCommonException(
          ResponseCode.invalidParameterValue.getErrorCode(),
          ResponseCode.invalidParameterValue.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode(),
          id,
          parameter);
    }
    return responseUserList.get(0);
  }
}
