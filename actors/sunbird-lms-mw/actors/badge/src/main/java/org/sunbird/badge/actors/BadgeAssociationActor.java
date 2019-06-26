package org.sunbird.badge.actors;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.badge.dao.ContentBadgeAssociationDao;
import org.sunbird.badge.dao.impl.ContentBadgeAssociationDaoImpl;
import org.sunbird.badge.service.BadgeAssociationService;
import org.sunbird.badge.service.BadgingService;
import org.sunbird.badge.service.impl.BadgeAssociationServiceImpl;
import org.sunbird.badge.service.impl.BadgingFactory;
import org.sunbird.common.Constants;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.BadgingJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.StringFormatter;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.content.service.ContentService;
import org.sunbird.learner.actors.coursebatch.CourseEnrollmentActor;
import org.sunbird.learner.util.CourseBatchSchedulerUtil;

@ActorConfig(
  tasks = {"createBadgeAssociation", "removeBadgeAssociation"},
  asyncTasks = {}
)
public class BadgeAssociationActor extends BaseActor {

  private BadgingService service = BadgingFactory.getInstance();
  private BadgeAssociationService associationService = new BadgeAssociationServiceImpl();
  private ContentBadgeAssociationDao contentBadgeAssociationDao =
      new ContentBadgeAssociationDaoImpl();

  @Override
  public void onReceive(Request request) throws Throwable {
    String operation = request.getOperation();

    switch (operation) {
      case "createBadgeAssociation":
        createBadgeAssociation(request);
        break;

      case "removeBadgeAssociation":
        removeBadgeAssociation(request);
        break;

      default:
        onReceiveUnsupportedOperation("BadgeAssociationActor");
    }
  }

  @SuppressWarnings("unchecked")
  private void createBadgeAssociation(Request request) {
    String contentId = (String) request.getRequest().get(JsonKey.CONTENT_ID);
    String requestedBy = (String) request.getContext().get(JsonKey.REQUESTED_BY);
    Map<String, Object> contentDetails = getContentDetails(contentId);
    List<Map<String, Object>> activeBadges =
        (List<Map<String, Object>>) contentDetails.get(BadgingJsonKey.BADGE_ASSOCIATIONS);
    List<String> requestedBadges =
        (List<String>) request.getRequest().get(BadgingJsonKey.BADGE_IDs);
    List<Map<String, Object>> badgesTobeAddedList =
        getBadgesDetailsToBeAdded(activeBadges, requestedBadges);
    List<Map<String, Object>> cassandraCreateMapList = new ArrayList<>();
    Response response = new Response();
    if (!CollectionUtils.isEmpty(badgesTobeAddedList)) {
      activeBadges = createActiveBadgeForContentUpdate(badgesTobeAddedList, activeBadges);
      ProjectLogger.log(
          "BadgeAssociationActor:createBadgeAssociation: new list of badgeAssociation details for "
              + contentId
              + " is : "
              + activeBadges,
          LoggerEnum.INFO);
      boolean flag =
          ContentService.updateEkstepContent(
              contentId, BadgingJsonKey.BADGE_ASSOCIATIONS, activeBadges);
      if (flag) {
        ProjectLogger.log(
            "BadgeAssociationActor:createBadgeAssociation: adding content badge association details in cassandra for "
                + contentId
                + " is : "
                + badgesTobeAddedList,
            LoggerEnum.INFO);
        cassandraCreateMapList = newActiveBadgeMap(badgesTobeAddedList, requestedBy, contentId);
        response = contentBadgeAssociationDao.insertBadgeAssociation(cassandraCreateMapList);
      }
    }
    sender().tell(response, self());
    if (Constants.SUCCESS.equals(response.get(JsonKey.RESPONSE))) {
      associationService.syncToES(cassandraCreateMapList, true);
    }
  }

  @SuppressWarnings("unchecked")
  private void removeBadgeAssociation(Request request) {
    String contentId = (String) request.getRequest().get(JsonKey.CONTENT_ID);
    String requestedBy = (String) request.getContext().get(JsonKey.REQUESTED_BY);
    Map<String, Object> contentDetails = getContentDetails(contentId);
    List<Map<String, Object>> activeBadges =
        (List<Map<String, Object>>) contentDetails.get(BadgingJsonKey.BADGE_ASSOCIATIONS);
    List<String> reqestedBadges = (List<String>) request.getRequest().get(BadgingJsonKey.BADGE_IDs);
    List<String> associationIds = getAssociationIdsToBeRemoved(activeBadges, reqestedBadges);
    List<Map<String, Object>> updatedActiveBadges =
        getUpdatedActiveBadges(activeBadges, associationIds);
    List<Map<String, Object>> updateMapList = new ArrayList<>();
    Response response = new Response();
    boolean flag = false;
    if (CollectionUtils.isNotEmpty(associationIds)) {
      flag =
          ContentService.updateEkstepContent(
              contentId, BadgingJsonKey.BADGE_ASSOCIATIONS, updatedActiveBadges);
      if (flag) {
        updateMapList = updateCassandraAndGetUpdateMapList(associationIds, requestedBy);
      }
    }
    sender().tell(response, self());
    if (flag) {
      associationService.syncToES(updateMapList, false);
    }
  }

  private List<Map<String, Object>> getUpdatedActiveBadges(
      List<Map<String, Object>> activeBadges, List<String> associationIdsToBeRemoved) {
    List<Map<String, Object>> updatedBadges = new ArrayList<>();
    for (Map<String, Object> badgeDetails : activeBadges) {
      if (!associationIdsToBeRemoved.contains(
          (String) badgeDetails.get(BadgingJsonKey.ASSOCIATION_ID))) {
        updatedBadges.add(badgeDetails);
      }
    }
    return updatedBadges;
  }

  private List<Map<String, Object>> updateCassandraAndGetUpdateMapList(
      List<String> associationIds, String requestedBy) {
    List<Map<String, Object>> updateList = new ArrayList<>();
    for (String id : associationIds) {
      Map<String, Object> updateMap =
          associationService.getCassandraBadgeAssociationUpdateMap(id, requestedBy);
      updateList.add(updateMap);
      contentBadgeAssociationDao.updateBadgeAssociation(updateMap);
    }
    return updateList;
  }

  private List<String> getAssociationIdsToBeRemoved(
      List<Map<String, Object>> activeBadges, List<String> reqestedBadges) {
    List<String> badgeIds = getUncommonBadgeIds(reqestedBadges, activeBadges);
    if (CollectionUtils.isNotEmpty(badgeIds)) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.invalidParameterValue,
          MessageFormat.format(
              ResponseCode.invalidParameterValue.getErrorMessage(),
              StringFormatter.joinByComma(badgeIds.toArray(new String[0])),
              BadgingJsonKey.BADGE_IDs));
    }
    List<String> associationIds = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(activeBadges)) {
      for (int i = 0; i < activeBadges.size(); i++) {
        if (reqestedBadges.contains((String) activeBadges.get(i).get(BadgingJsonKey.BADGE_ID))) {
          String associationId = (String) activeBadges.get(i).get(BadgingJsonKey.ASSOCIATION_ID);
          associationIds.add(associationId);
        }
      }
    }
    return associationIds;
  }

  private Map<String, Object> getContentDetails(String contentId) {
    Map<String, String> headers = CourseBatchSchedulerUtil.headerMap;
    Map<String, Object> contentDetails =
        CourseEnrollmentActor.getCourseObjectFromEkStep(contentId, headers);
    if (MapUtils.isEmpty(contentDetails)) {
      ProjectCommonException.throwClientErrorException(ResponseCode.invalidContentId);
    }
    return contentDetails;
  }

  private List<Map<String, Object>> getBadgesDetailsToBeAdded(
      List<Map<String, Object>> activeBadgesList, List<String> requestedBadges) {
    List<String> newBadgeIdsList = getUncommonBadgeIds(requestedBadges, activeBadgesList);
    ProjectLogger.log(
        "BadgeAssociationAcotr: getBadgesDetailsToBeAdded: new BadgeIdsList is " + newBadgeIdsList,
        LoggerEnum.INFO);
    List<Map<String, Object>> newBadgesDetails = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(newBadgeIdsList)) {
      newBadgesDetails = getBadgesDetails(newBadgeIdsList);
      if (newBadgesDetails.size() != newBadgeIdsList.size()) {
        List<String> badgeIdsFoundList =
            newBadgesDetails
                .stream()
                .map(q -> (String) q.get(BadgingJsonKey.BADGE_ID))
                .collect(Collectors.toList());
        ProjectLogger.log(
            "BadgeAssociationAcotr: getBadgesDetailsToBeAdded: valid non-associatied requested Badgeid is "
                + badgeIdsFoundList,
            LoggerEnum.INFO);
        List<String> invalidBadgeIdsList =
            newBadgeIdsList
                .stream()
                .filter(q -> !badgeIdsFoundList.contains(q))
                .collect(Collectors.toList());
        ProjectCommonException.throwClientErrorException(
            ResponseCode.invalidParameterValue,
            MessageFormat.format(
                ResponseCode.invalidParameterValue.getErrorMessage(),
                StringFormatter.joinByComma(invalidBadgeIdsList.toArray(new String[0])),
                BadgingJsonKey.BADGE_IDs));
      }
    }
    return newBadgesDetails;
  }

  private List<String> getUncommonBadgeIds(
      List<String> requestedBadges, List<Map<String, Object>> activeBadges) {
    HashSet<String> badgeIds = new HashSet<>(requestedBadges);
    ProjectLogger.log(
        "BadgeAssociationActor: getUncommonBadgeIds: current active badge is " + activeBadges,
        LoggerEnum.INFO);
    if (CollectionUtils.isEmpty(activeBadges)) {
      return new ArrayList<>(badgeIds);
    }
    List<String> activeBadgeIds =
        activeBadges
            .stream()
            .map(q -> (String) q.get(BadgingJsonKey.BADGE_ID))
            .collect(Collectors.toList());
    return requestedBadges
        .stream()
        .filter(q -> !activeBadgeIds.contains(q))
        .collect(Collectors.toList());
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> getBadgesDetails(List<String> badgeIds) {
    Request request = new Request();
    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(BadgingJsonKey.BADGE_LIST, badgeIds);
    request.put(JsonKey.FILTERS, requestMap);
    ProjectLogger.log(
        "BadgeAssociationActor:getBadgesDetails: Requesting badge details from badgr server for badgeIds: "
            + badgeIds,
        LoggerEnum.INFO);
    Response response = service.searchBadgeClass(request);
    return (List<Map<String, Object>>) response.get(BadgingJsonKey.BADGES);
  }

  private List<Map<String, Object>> newActiveBadgeMap(
      List<Map<String, Object>> badgesTobeAddedList, String requestedBy, String contentId) {
    List<Map<String, Object>> cassandraMap = new ArrayList<>();
    for (Map<String, Object> badgeDetail : badgesTobeAddedList) {
      cassandraMap.add(
          associationService.getCassandraBadgeAssociationCreateMap(
              badgeDetail, requestedBy, contentId));
    }
    return cassandraMap;
  }

  private List<Map<String, Object>> createActiveBadgeForContentUpdate(
      List<Map<String, Object>> badgesTobeAddedList, List<Map<String, Object>> activeBadges) {
    List<Map<String, Object>> badgesList =
        CollectionUtils.isEmpty(activeBadges) ? new ArrayList<>() : activeBadges;
    for (Map<String, Object> badgeDetails : badgesTobeAddedList) {
      long timeStamp = System.currentTimeMillis();
      badgeDetails.put(BadgingJsonKey.CREATED_TS, timeStamp);
      String associationId = UUID.randomUUID().toString();
      badgeDetails.put(BadgingJsonKey.ASSOCIATION_ID, associationId);
      badgesList.add(associationService.getBadgeAssociationMapForContentUpdate(badgeDetails));
    }
    return badgesList;
  }
}
