package org.sunbird.notification.sms;

import static org.powermock.api.mockito.PowerMockito.doCallRealMethod;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.spy;

import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.AdditionalMatchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.notification.utils.PropertiesCache;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*", "javax.security.*" })
@PrepareForTest({ HttpClients.class, PropertiesCache.class })
public abstract class BaseMessageTest {

	@BeforeClass
	public static void initMockRules() {
		CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
		CloseableHttpResponse httpResp = mock(CloseableHttpResponse.class);
		StatusLine statusLine = mock(StatusLine.class);
		PowerMockito.mockStatic(HttpClients.class);
		try {
			doReturn(httpClient).when(HttpClients.class, "createDefault");
			doReturn(httpResp).when(httpClient).execute(Mockito.any(HttpPost.class));
			doReturn(statusLine).when(httpResp).getStatusLine();
			doReturn(200).when(statusLine).getStatusCode();
		} catch (Exception e) {
			Assert.fail("Exception while mocking static " + e.getLocalizedMessage());
		}

		PropertiesCache pc = spy(PropertiesCache.getInstance());
		PowerMockito.mockStatic(PropertiesCache.class);
		try {
			doReturn(pc).when(PropertiesCache.class, "getInstance");
		} catch (Exception e) {
			Assert.fail("Exception while mocking static " + e.getLocalizedMessage());
		}
//		doReturn("randomString").when(pc).getProperty(Mockito.eq("sunbird.msg.91.auth"));
//		doCallRealMethod().when(pc).getProperty(AdditionalMatchers.not(Mockito.eq("sunbird.msg.91.auth")));
	}

}
