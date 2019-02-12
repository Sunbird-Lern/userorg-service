package controllers.usermanagement;

import controllers.BaseController;
import org.sunbird.common.models.util.ActorOperations;
import play.libs.F.Promise;
import play.mvc.Result;

public class UserTypeController extends BaseController {

  public Promise<Result> getUserTypes() {
    return handleRequest(ActorOperations.GET_USER_TYPES.getValue());
  }
}
