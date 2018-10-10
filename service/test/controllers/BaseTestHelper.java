package controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.keycloak.admin.client.Keycloak;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.KeyCloakConnectionProvider;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.HeaderParam;
import org.sunbird.learner.util.Util;
import org.sunbird.middleware.Application;
import org.sunbird.services.sso.SSOManager;
import org.sunbird.services.sso.SSOServiceFactory;
import org.sunbird.services.sso.impl.KeyCloakServiceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import play.test.FakeApplication;
import play.test.Helpers;
import util.AuthenticationHelper;
import util.RequestInterceptor;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RequestInterceptor.class,AuthenticationHelper.class,Application.class,SSOServiceFactory.class,KeyCloakConnectionProvider.class,Util.class})
@SuppressStaticInitializationFor({"util.AuthenticationHelper","util.Global"})
@PowerMockIgnore("javax.management.*")
public class BaseTestHelper {

	public static FakeApplication app;
	public static Map<String, String[]> headerMap;
	public static ActorSystem system;
	public static final Props props = Props.create(DummyActor.class);

	@BeforeClass
	public static void startApp() {
		app = Helpers.fakeApplication();
		Helpers.start(app);
		headerMap = new HashMap<String, String[]>();
		headerMap.put(HeaderParam.X_Consumer_ID.getName(), new String[] { "Service test consumer" });
		headerMap.put(HeaderParam.X_Device_ID.getName(), new String[] { "Some Device Id" });
		headerMap.put(HeaderParam.X_Authenticated_Userid.getName(), new String[] { "Authenticated user id" });
		headerMap.put(JsonKey.MESSAGE_ID, new String[] { "Unique Message id" });

		system = ActorSystem.create("system");
		ActorRef subject = system.actorOf(props);
		BaseController.setActorRef(subject);
	}

	@Before
	public void doResourceMock() {
		SSOManager impl = Mockito.mock(KeyCloakServiceImpl.class);
		Keycloak keycloak = Mockito.mock(Keycloak.class);
		PowerMockito.mockStatic(SSOServiceFactory.class);
		PowerMockito.when(SSOServiceFactory.getInstance()).thenReturn(impl);
		PowerMockito.mockStatic(KeyCloakConnectionProvider.class);
		try {
			Mockito.when(KeyCloakConnectionProvider.initialiseConnection()).thenReturn(keycloak);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		PowerMockito.mockStatic(AuthenticationHelper.class);
		Mockito.when(AuthenticationHelper.verifyUserAccesToken(Mockito.anyString())).thenReturn("userId");
		PowerMockito.mockStatic(RequestInterceptor.class);
		Mockito.when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
				.thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
		PowerMockito.mockStatic(Util.class);
		PowerMockito.mockStatic(Application.class);
		try {
			PowerMockito.doNothing().when(Util.class, "checkCassandraDbConnections");
			PowerMockito.doNothing().when(Application.class, "checkCassandraConnection");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static String mapToJson(Map map) {
		ObjectMapper mapperObj = new ObjectMapper();
		String jsonResp = "";
		try {
			jsonResp = mapperObj.writeValueAsString(map);
		} catch (IOException e) {
			ProjectLogger.log(e.getMessage(), e);
		}
		return jsonResp;
	}
}
