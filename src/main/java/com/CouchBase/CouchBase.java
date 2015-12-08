package com.upinion.CouchBase;

import android.content.Intent;
import android.content.Context;

import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

public class CouchBase extends ReactContextBaseJavaModule {

    private Context context;

    /**
     * Constructor for the Native Module
     * @param  reactContext  React context object to comunicate with React-native
     */
    public LinkingAndroid(ReactApplicationContext reactContext) {
        super(reactContext);
        this.context = reactContext;
    }

    /**
     * Returns the name of this module in React-native (javascript)
     */
    @Override
    public String getName() {
        return "CouchBase";
    }

    /**
     * Function to be shared to React-native, it starts an activity with the given email to write to
     * @param  url  String with the email to use
     */
    @ReactMethod
    public void server() {
    }


}
