package org.sunbird.user.dao;

import org.sunbird.common.request.Request;

public interface UserExternalIdentityDao {

  String getUserId(Request request);
}
