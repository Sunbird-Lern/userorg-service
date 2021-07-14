package util;

import org.sunbird.keys.JsonKey;
import play.libs.typedmap.TypedKey;

public class Attrs {
  public static final TypedKey<String> USER_ID = TypedKey.<String>create(JsonKey.USER_ID);
  public static final TypedKey<String> CONTEXT = TypedKey.<String>create(JsonKey.CONTEXT);
  public static final TypedKey<String> MANAGED_FOR = TypedKey.<String>create(JsonKey.MANAGED_FOR);
  public static final TypedKey<String> START_TIME = TypedKey.<String>create(JsonKey.START_TIME);
  public static final TypedKey<String> AUTH_WITH_MASTER_KEY =
      TypedKey.<String>create(JsonKey.AUTH_WITH_MASTER_KEY);
  public static final TypedKey<String> IS_AUTH_REQ = TypedKey.<String>create(JsonKey.IS_AUTH_REQ);
  public static final TypedKey<String> X_REQUEST_ID = TypedKey.<String>create(JsonKey.X_REQUEST_ID);
}
