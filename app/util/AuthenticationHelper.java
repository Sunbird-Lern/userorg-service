/**
 * 
 */
package util;

/**
 * 
 * This class will handle all the method related to authentication. example verifying user access
 * token , creating access token after success login.
 * 
 * @author Manzarul
 *
 */
public class AuthenticationHelper {

  /**
   * This method will verify the incoming user access token against store data base /cache . if
   * token is valid then it would be associated with some user id. in case of token matched it will
   * provide user id. else will provide empty string.
   * 
   * @param token String
   * @return String
   */
  public static String verifyUserAccesToken(String token) {
    // TODO need to see from where we will get the data.
    return token;

  }

  /**
   * This method will save the user access token in side data base.
   * 
   * @param token String
   * @param userId String
   * @return boolean
   */
  public static boolean saveUserAccessToken(String token, String userId) {
    return false;
  }

  /**
   * This method will invalidate the user access token.
   * 
   * @param token String
   * @return boolean
   */
  public static boolean invalidateToken(String token) {
    return false;
  }
}
