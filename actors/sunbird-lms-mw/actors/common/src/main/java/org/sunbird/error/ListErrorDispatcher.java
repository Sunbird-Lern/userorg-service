package org.sunbird.error;

import com.mchange.v1.util.ArrayUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.responsecode.ResponseCode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * this class will dispatch error in list format
 * @author anmolgupta
 */
public class ListErrorDispatcher implements IErrorDispatcher {

    private CsvError error;

    private ListErrorDispatcher(CsvError error) {
        this.error = error;
    }

    public static ListErrorDispatcher getInstance(CsvError error){
        return new ListErrorDispatcher(error);
    }

    @Override
    public void dispatchError() {
        Collections.sort(error.getErrorsList(), new RowComparator());
        List<String>errors=new ArrayList<>();
        error.getErrorsList().stream().forEach(errorDetails -> {
            errors.add(String.format("In Row %s:the Column %s:is %s",errorDetails.getRowId()+1,errorDetails.getHeader(),errorDetails.getErrorEnum().getValue()));
        });
        throw new ProjectCommonException(
                ResponseCode.invalidRequestData.getErrorCode(),
                ArrayUtils.stringifyContents(errors.toArray()),
                ResponseCode.CLIENT_ERROR.getResponseCode());
    }
}
