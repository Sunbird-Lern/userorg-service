package org.sunbird.service.location;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.dao.location.LocationDao;
import org.sunbird.dao.location.impl.LocationDaoFactory;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.util.DataCacheHandler;
import org.sunbird.models.location.Location;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.util.ProjectUtil;

public class LocationServiceImpl implements LocationService {
    public static LocationService locationService = null;
    private LocationDao locationDao = LocationDaoFactory.getInstance();

    public static synchronized LocationService getInstance() {
        if (locationService == null) locationService = new LocationServiceImpl();
        return locationService;
    }

    @Override
    public List<Map<String, String>> getValidatedRelatedLocationIdAndType(
            List<String> codeList, RequestContext context) {
        List<Location> locationIdTypeList = locationSearch(JsonKey.CODE, codeList, context);
        List<Map<String, String>> locationIdType = null;
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
        return locationIdType;
    }

    public List<Location> locationSearch(String param, Object value, RequestContext context) {
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
            locationResponseList =
                    responseList
                            .stream()
                            .map(s -> mapper.convertValue(s, Location.class))
                            .collect(Collectors.toList());
        }
        return locationResponseList;
    }

    public List<Map<String, String>> getValidatedRelatedLocationList(
            List<Location> locationList, RequestContext context) {
        Map<String, Location> locationMap = new HashMap<>();
        for (Location requestedLocation : locationList) {
            Set<Location> parentLocnSet = getParentLocations(requestedLocation, context);
            for (Location currentLocation : parentLocnSet) {
                String type = currentLocation.getType();
                Location location = locationMap.get(type);
                if (null != location) {
                    if (!(currentLocation.getId().equals(location.getId()))) {
                        throw new ProjectCommonException(
                                ResponseCode.conflictingOrgLocations.getErrorCode(),
                                ProjectUtil.formatMessage(
                                        ResponseCode.conflictingOrgLocations.getErrorMessage(),
                                        requestedLocation.getCode(),
                                        location.getCode(),
                                        type),
                                ResponseCode.CLIENT_ERROR.getResponseCode());
                    }
                } else {
                    locationMap.put(type, currentLocation);
                }
            }
        }
        List<Map<String, String>> locationMapList = new ArrayList<>();
        locationMap
                .entrySet()
                .forEach(
                        m -> {
                            Map<String, String> locationIdTypeMap = new HashMap();
                            locationIdTypeMap.put(JsonKey.ID, m.getValue().getId());
                            locationIdTypeMap.put(JsonKey.TYPE, m.getValue().getType());
                            locationMapList.add(locationIdTypeMap);
                        });
        return locationMapList;
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
    public Location getLocationById(String param, Object value, RequestContext context){
        Location loc = null;
        List<Location> locList = locationSearch(param, value, context);
        if(CollectionUtils.isNotEmpty(locList)){
            loc = locList.get(0);
        }
        return loc;
    }
}
