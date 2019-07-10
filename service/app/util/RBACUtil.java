/** */
package util;

/**
 * This is role based access control class. This class will handle user role based access control.
 *
 * @author Manzarul
 */
public class RBACUtil {

  /**
   * Based on incoming user id and API, this method will decide user has access to this API or not.
   *
   * @param uid String
   * @param api String
   * @return boolean
   */
  public boolean hasAccess(String uid, String api) {

    return true;
  }
}
