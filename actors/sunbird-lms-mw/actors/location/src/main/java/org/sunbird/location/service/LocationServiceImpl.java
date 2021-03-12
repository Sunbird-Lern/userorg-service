package org.sunbird.location.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.location.dao.LocationDao;
import org.sunbird.location.dao.impl.LocationDaoFactory;
import org.sunbird.models.location.Location;
import org.sunbird.common.exception.ProjectCommonException;

import java.util.*;
import java.util.stream.Collectors;

public class LocationServiceImpl implements LocationService {
    public static LocationService locationService = null;
    private LocationDao locationDao = LocationDaoFactory.getInstance();

    public static synchronized LocationService getInstance() {
        if (locationService == null) locationService = new LocationServiceImpl();
        return locationService;
    }

    @Override
    public List<Map<String, String>> getValidatedRelatedLocationIdAndType(List<String> codeList, RequestContext context) {
        List<Map<String,String>> locationIdAndType = new ArrayList<>();
        List<Location> locationIdTypeList = locationSearch(JsonKey.CODE, codeList, context);
        Map<String,String> locationIdType = null;
        List<String> codes = new ArrayList<>(codeList);
        if (CollectionUtils.isNotEmpty(locationIdTypeList)) {
            if (locationIdTypeList.size() != codes.size()) {
                List<String> resCodeList =
                        locationIdTypeList.stream().map(Location::getCode).collect(Collectors.toList());
                List<String> invalidCodeList =
                        codes.stream().filter(s -> !resCodeList.contains(s)).collect(Collectors.toList());
                throwInvalidParameterValueException(invalidCodeList);
            } else {
                locationIdType = getValidatedRelatedLocationList(locationIdTypeList, context);
            }
        } else {
            throwInvalidParameterValueException(codeList);
        }
        locationIdAndType.add(locationIdType);
        return locationIdAndType;
    }

    private List<Location> locationSearch(String param, Object value, RequestContext context) {
        Map<String, Object> filter = new HashMap<>();
        Map<String, Object> searchRequestMap = new HashMap<>();
        List<Location> locationResponseList = new ArrayList<>();
        filter.put(param, value);
        searchRequestMap.put(JsonKey.FILTERS, filter);
        Response response = locationDao.search(searchRequestMap, context);
        if (response != null) {
            List<Map<String, Object>> responseList =
                    (List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE);
            ObjectMapper mapper = new ObjectMapper();
            locationResponseList = responseList
                    .stream()
                    .map(s -> mapper.convertValue(s, Location.class))
                    .collect(Collectors.toList());
        }
        return locationResponseList;
    }

    public Map<String,String> getValidatedRelatedLocationList(
            List<Location> locationList, RequestContext context) {
        Set<Location> locationSet = new HashSet<>();
        for (Location requestedLocation : locationList) {
            Set<Location> parentLocnSet = getParentLocations(requestedLocation, context);
            if (CollectionUtils.sizeIsEmpty(locationSet)) {
                locationSet.addAll(parentLocnSet);
            } else {
                for (Location currentLocation : parentLocnSet) {
                    String type = currentLocation.getType();
                    locationSet
                            .stream()
                            .forEach(
                                    location -> {
                                        if (type.equalsIgnoreCase(location.getType())
                                                && !(currentLocation.getId().equals(location.getId()))) {
                                            throw new ProjectCommonException(
                                                    ResponseCode.conflictingOrgLocations.getErrorCode(),
                                                    ProjectUtil.formatMessage(
                                                            ResponseCode.conflictingOrgLocations.getErrorMessage(),
                                                            (requestedLocation).getCode(),
                                                            location.getCode(),
                                                            type),
                                                    ResponseCode.CLIENT_ERROR.getResponseCode());
                                        }
                                    });
                    locationSet.add(currentLocation);
                }
            }
        }


        return locationSet.stream().collect(Collectors.toMap(Location::getId,Location::getType));
    }

    private Set<Location> getParentLocations(Location locationObj, RequestContext context) {
        Set<Location> locationSet = new LinkedHashSet<>();
        Location location = locationObj;
        int count = getOrder(location.getType());
        locationSet.add(location);
        while (count > 0) {
            Location parent = null;
            if (getOrder(location.getType()) == 0 && StringUtils.isNotEmpty(location.getId())) {
                parent = getLocation(location.getId(), context);
            } else if (StringUtils.isNotEmpty(location.getParentId())) {
                parent = getLocation(location.getParentId(), context);
            }
            if (null != parent) {
                locationSet.add(parent);
                location = parent;
            }
            count--;
        }
        return locationSet;
    }

    public int getOrder(String type) {
        return DataCacheHandler.getLocationOrderMap().get(type);
    }

    private Location getLocation(String locationId, RequestContext context) {
        List<Location> locations = locationSearch(JsonKey.ID, locationId, context);
        if (locations.isEmpty()) return null;

        return locations.get(0);
    }

    private void throwInvalidParameterValueException(List<String> codeList) {
        throw new ProjectCommonException(
                ResponseCode.invalidParameterValue.getErrorCode(),
                ProjectUtil.formatMessage(
                        ResponseCode.invalidParameterValue.getErrorMessage(), codeList, JsonKey.LOCATION_CODE),
                ResponseCode.CLIENT_ERROR.getResponseCode());
    }


}