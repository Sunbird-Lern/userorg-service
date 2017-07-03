/**
 * 
 */
package util;

/**
 * This is role based access control class. 
 * this class will handle user role based 
 * access control.
 * @author Manzarul
 */
public class RBACUtil {
	
	/**
	 * Based on incoming user id and api , this method will decide user 
	 * has access to this api or not.
	 * @param uid String
	 * @param api String
	 * @return boolean
	 */
	public boolean hasAccess(String uid, String api) {
		return true;
	}

}
