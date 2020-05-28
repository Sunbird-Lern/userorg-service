package org.sunbird.user.dao.impl;

import java.util.Map;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.user.dao.AddressDao;

public class AddressDaoImpl implements AddressDao {

  private Util.DbInfo addrDbInfo = Util.dbInfoMap.get(JsonKey.ADDRESS_DB);

  private AddressDaoImpl() {}

  private static class LazyInitializer {
    private static AddressDao INSTANCE = new AddressDaoImpl();
  }

  public static AddressDao getInstance() {
    return LazyInitializer.INSTANCE;
  }

  @Override
  public void createAddress(Map<String, Object> address) {
    getCassandraOperation().insertRecord(addrDbInfo.getKeySpace(), addrDbInfo.getTableName(), address);
  }

  @Override
  public void updateAddress(Map<String, Object> address) {
    getCassandraOperation().updateRecord(addrDbInfo.getKeySpace(), addrDbInfo.getTableName(), address);
  }

  @Override
  public void deleteAddress(String addressId) {
    getCassandraOperation().deleteRecord(addrDbInfo.getKeySpace(), addrDbInfo.getTableName(), addressId);
  }

  @Override
  public Response upsertAddress(Map<String, Object> address) {
    return getCassandraOperation().upsertRecord(
        addrDbInfo.getKeySpace(), addrDbInfo.getTableName(), address);
  }

  private CassandraOperation getCassandraOperation()
  {
    return ServiceFactory.getInstance();
  }
}
