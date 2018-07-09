/** */
package controllers.initialization;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.ProjectUtil.EsType;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestValidator;
import play.libs.F.Promise;
import play.mvc.Result;

/**
 *
 *
 * @author Loganathan Shanmugam
 */
public class InitializationController extends BaseController {
  /**
   * This method will do the initialisation process to create the first rootOrg in the system.
   *
   * @return Promise<Result>
   */
  @SuppressWarnings("unchecked")
  public Promise<Result> initalizeSystem() {
  /* 
  * Here the request data will be validated with help of validators and 
  * using actor method (InitalisationActor.createFirstRootOrg)
  */
  }
}