package org.sunbird.actor.systemsettings;

import java.util.Map;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.systemsettings.SystemSetting;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.systemsettings.SystemSettingsService;

public class SystemSettingsActor extends BaseActor {

  private final SystemSettingsService service = new SystemSettingsService();

  @Override
  public void onReceive(Request request) throws Throwable {
    switch (request.getOperation()) {
      case "getSystemSetting":
        getSystemSetting(request);
        break;
      case "getAllSystemSettings":
        getAllSystemSettings(request.getRequestContext());
        break;
      case "setSystemSetting":
        setSystemSetting(request);
        break;
      default:
        onReceiveUnsupportedOperation();
        break;
    }
  }

  private void getSystemSetting(Request actorMessage) {
    SystemSetting setting =
        service.getSystemSettingByKey(
            (String) actorMessage.getContext().get(JsonKey.FIELD),
            actorMessage.getRequestContext());
    Response response = new Response();
    response.put(JsonKey.RESPONSE, setting);
    sender().tell(response, self());
  }

  private void getAllSystemSettings(RequestContext context) {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, service.getAllSystemSettings(context));
    sender().tell(response, self());
  }

  private void setSystemSetting(Request actorMessage) {
    Map<String, Object> request = actorMessage.getRequest();
    Response response = service.setSystemSettings(request, actorMessage.getRequestContext());
    sender().tell(response, self());
  }
}
