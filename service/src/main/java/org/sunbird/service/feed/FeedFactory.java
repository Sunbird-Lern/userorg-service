package org.sunbird.service.feed;

import org.sunbird.client.NotificationServiceClient;
import org.sunbird.service.feed.impl.FeedServiceImpl;

/** This class will create instance of FeedService */
public class FeedFactory {
  private static IFeedService instance;

  public static IFeedService getInstance(NotificationServiceClient service) {
    if (instance == null) {
      synchronized (FeedFactory.class) {
        if (instance == null){
          instance = new FeedServiceImpl(service);
        }
      }
    }
    return instance;
  }
}
