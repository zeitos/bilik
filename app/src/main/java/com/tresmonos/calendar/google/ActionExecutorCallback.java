package com.tresmonos.calendar.google;

/**
 * Created by david on 9/9/14.
 */
public interface ActionExecutorCallback<X> {

    void onExecutionFinished(boolean success, X result);

}