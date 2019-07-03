package org.sunbird.learner.actors.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.learner.util.Util;
import scala.concurrent.Future;

/**
 * This class will handle search operation for course.
 *
 * @author Amit Kumar
 */
@ActorConfig(
  tasks = {"searchCourse", "getCourseById"},
  asyncTasks = {}
)
public class CourseSearchActor extends BaseActor {
  private ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);

  @Override
  public void onReceive(Request request) throws Throwable {
    if (request.getOperation().equalsIgnoreCase(ActorOperations.SEARCH_COURSE.getValue())) {
      Map<String, Object> req = request.getRequest();
      @SuppressWarnings("unchecked")
      Map<String, Object> searchQueryMap = (Map<String, Object>) req.get(JsonKey.SEARCH);
      Map<String, Object> ekStepSearchQuery = new HashMap<>();
      ekStepSearchQuery.put(JsonKey.REQUEST, searchQueryMap);

      String json;
      try {
        json = new ObjectMapper().writeValueAsString(ekStepSearchQuery);
        Map<String, Object> query = new HashMap<>();
        query.put(JsonKey.SEARCH_QUERY, json);
        Util.getContentData(query);
        Object ekStepResponse = query.get(JsonKey.CONTENTS);
        Response response = new Response();
        response.put(JsonKey.RESPONSE, ekStepResponse);
        sender().tell(response, self());
      } catch (JsonProcessingException e) {
        ProjectCommonException projectCommonException =
            new ProjectCommonException(
                ResponseCode.internalError.getErrorCode(),
                ResponseCode.internalError.getErrorMessage(),
                ResponseCode.internalError.getResponseCode());
        sender().tell(projectCommonException, self());
      }
    } else if (request
        .getOperation()
        .equalsIgnoreCase(ActorOperations.GET_COURSE_BY_ID.getValue())) {
      Map<String, Object> req = request.getRequest();
      String courseId = (String) req.get(JsonKey.ID);

      Future<Map<String, Object>> resultF =
          esService.getDataByIdentifier(ProjectUtil.EsType.course.getTypeName(), courseId);
      Map<String, Object> result =
          (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
      Response response = new Response();
      if (result != null) {
        response.put(JsonKey.RESPONSE, result);
      } else {
        result = new HashMap<>();
        response.put(JsonKey.RESPONSE, result);
      }
      sender().tell(response, self());
    } else {
      onReceiveUnsupportedOperation(request.getOperation());
    }
  }
}
