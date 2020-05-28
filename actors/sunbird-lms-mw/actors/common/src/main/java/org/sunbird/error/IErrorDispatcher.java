package org.sunbird.error;


/**
 * this is an interface class for the error dispatcher
 *
 * @author anmolgupta
 */
public interface IErrorDispatcher {


    /**
     * this method will prepare the error and will throw ProjectCommonException.
     */
    void dispatchError();
}
