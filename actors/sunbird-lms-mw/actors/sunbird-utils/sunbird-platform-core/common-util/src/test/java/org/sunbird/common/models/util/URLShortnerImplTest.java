package org.sunbird.common.models.util;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.sunbird.common.models.util.url.URLShortner;
import org.sunbird.common.models.util.url.URLShortnerImpl;

public class URLShortnerImplTest {

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
