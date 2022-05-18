package org.sunbird.service.user;

import org.sunbird.request.Request;

public interface ExtendedUserProfileService {
    /**
     * Validate json payload of user profile of a given user request
     * @param userRequest
     */
    public void validateProfile(Request userRequest);
}
