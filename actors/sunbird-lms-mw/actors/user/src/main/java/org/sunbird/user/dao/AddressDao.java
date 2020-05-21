package org.sunbird.user.dao;

import java.util.Map;
import org.sunbird.common.models.response.Response;

public interface AddressDao {

  void createAddress(Map<String, Object> address);

  void updateAddress(Map<String, Object> address);

  Response upsertAddress(Map<String, Object> address);

  void deleteAddress(String addressId);
}
