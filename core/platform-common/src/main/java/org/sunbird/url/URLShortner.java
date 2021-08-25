package org.sunbird.url;

import org.sunbird.request.RequestContext;

public interface URLShortner {

  public String shortUrl(String url, RequestContext context);
}
