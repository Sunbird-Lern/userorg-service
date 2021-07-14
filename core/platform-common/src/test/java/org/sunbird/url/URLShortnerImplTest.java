package org.sunbird.url;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.util.PropertiesCache;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PropertiesCache.class})
@PowerMockIgnore({
        "javax.management.*",
        "javax.net.ssl.*",
        "javax.security.*",
        "jdk.internal.reflect.*"
})
public class URLShortnerImplTest {
  @Before
  public void beforeEachTest(){
    PowerMockito.mockStatic(PropertiesCache.class);
    PropertiesCache propertiesCache = mock(PropertiesCache.class);
    when(PropertiesCache.getInstance()).thenReturn(propertiesCache);
    PowerMockito.when(propertiesCache.getProperty(Mockito.anyString())).thenReturn("anyString");
  }
  @Test
  public void urlShortTest() {
    URLShortner shortner = new URLShortnerImpl();
    String url = shortner.shortUrl("https://staging.open-sunbird.org/");
    Assert.assertNotNull(url);
  }

  @Test
  public void getShortUrlTest() {

    String SUNBIRD_WEB_URL = "sunbird_web_url";

    String webUrl = System.getenv(SUNBIRD_WEB_URL);
    if (StringUtils.isBlank(webUrl)) {
      webUrl = PropertiesCache.getInstance().getProperty(SUNBIRD_WEB_URL);
    }

    URLShortnerImpl shortnerImpl = new URLShortnerImpl();
    String url = shortnerImpl.getUrl();
    Assert.assertEquals(url, webUrl);
  }
}
