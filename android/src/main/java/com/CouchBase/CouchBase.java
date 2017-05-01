package com.upinion.CouchBase;

import android.content.Intent;
import android.content.Context;

import com.couchbase.lite.Document;
import com.couchbase.lite.SavedRevision;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.Reducer;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableNativeArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableNativeMap;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.common.JavascriptException;
import com.facebook.react.modules.core.DeviceEventManagerModule;


import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.DocumentChange;
import com.couchbase.lite.Manager;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.listener.LiteListener;
import com.couchbase.lite.listener.LiteServlet;
import com.couchbase.lite.listener.Credentials;
import com.couchbase.lite.router.URLStreamHandlerFactory;
import com.couchbase.lite.View;
import com.couchbase.lite.javascript.JavaScriptViewCompiler;
import com.couchbase.lite.util.Log;
import com.couchbase.lite.auth.Authenticator;
import com.couchbase.lite.auth.AuthenticatorFactory;
import com.couchbase.lite.replicator.RemoteRequestResponseException;
import com.couchbase.lite.internal.RevisionInternal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CouchBase extends ReactContextBaseJavaModule {

    private ReactApplicationContext context;
    private Manager managerServer;
    private boolean initFailed = false;
    private int listenPort;
    protected Boolean isDebug = false;

    private static final String PUSH_EVENT_KEY = "couchBasePushEvent";
    private static final String PULL_EVENT_KEY = "couchBasePullEvent";
    private static final String DB_EVENT_KEY = "couchBaseDBEvent";
    private static final String AUTH_ERROR_KEY = "couchBaseAuthError";
    private static final String OFFLINE_KEY = "couchBaseOffline";
    private static final String ONLINE_KEY = "couchBaseOnline";
    private static final String NOT_FOUND_KEY = "couchBaseNotFound";
    public static final String TAG = "CouchBase";

    /**
     * Constructor for the Native Module
     * @param  reactContext  React context object to comunicate with React-native
     */
    public CouchBase(ReactApplicationContext reactContext) {
        super(reactContext);
        this.context = reactContext;
        // Register the JavaScript view compiler
        View.setCompiler(new JavaScriptViewCompiler());
    }

    /**
     * Returns the name of this module in React-native (javascript)
     */
    @Override
    public String getName() {
        return TAG;
    }

    /**
     * Returns constants of this module in React-native to share (javascript)
     */
    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put("PUSH", PUSH_EVENT_KEY);
        constants.put("PULL", PULL_EVENT_KEY);
        constants.put("DBChanged", DB_EVENT_KEY);
        constants.put("AuthError", AUTH_ERROR_KEY);
        constants.put("Offline", OFFLINE_KEY);
        constants.put("Online", ONLINE_KEY);
        constants.put("NotFound", NOT_FOUND_KEY);
        return constants;
    }
    /**
     * Function to be shared to React-native, it starts a couchbase manager
     * @param  listen_port      Integer     port to start server
     * @param  userLocal        String      user for local server
     * @param  passwordLocal    String      password for local server
     * @param  databaseLocal    String      database for local server
     * @param  onEnd            Callback    function to call when finish
     */
    @ReactMethod
    public void serverManager(Callback onEnd) {
        try {
            this.managerServer = startCBLite();

            if(onEnd != null)
                onEnd.invoke();
        } catch (Exception e) {
            throw new JavascriptException(e.getMessage());
        }
    }

    /**
     * Function to be shared to React-native, it starts a local couchbase server
     * @param  listen_port      Integer     port to start server
     * @param  userLocal        String      user for local server
     * @param  passwordLocal    String      password for local server
     * @param  databaseLocal    String      database for local server
     * @param  onEnd            Callback    function to call when finish
     */
    @ReactMethod
    public void serverLocal(Integer listen_port, String userLocal, String passwordLocal, Callback onEnd) {

        startServer(listen_port, userLocal, passwordLocal);

        if(onEnd != null)
            onEnd.invoke(this.listenPort);
    }

    /**
     * Function to be shared to React-native, it starts a local couchbase server and syncs with remote
     * @param  listen_port      Integer     port to start server
     * @param  userLocal        String      user for local server
     * @param  passwordLocal    String      password for local server
     * @param  databaseLocal    String      database for local server
     * @param  remoteURL        String      URL to remote couchbase
     * @param  remoteUser       String      user for remote server
     * @param  remotePassword   String      password for remote server
     * @param  events           Boolean     activate the events for push and pull
     * @param  onEnd            Callback    function to call when finish
     */
    @ReactMethod
    public void serverLocalRemote(Integer listen_port, String userLocal, String passwordLocal, String databaseLocal,
                       String remoteURL, String  remoteUser, String remotePassword, Boolean events,
                       Callback onEnd) {

        startServer(listen_port, userLocal, passwordLocal);
        Manager ss = this.managerServer;

        if(!(databaseLocal != null && remoteURL != null && remoteUser != null && remotePassword != null))
            throw new JavascriptException("CouchBase Server bad arguments");

        try {
            URL url = new URL(remoteURL);
            Database db = ss.getExistingDatabase(databaseLocal);
            final Replication push = db.createPushReplication(url);
            final Replication pull = db.createPullReplication(url);
            pull.setContinuous(true);
            push.setContinuous(true);

            Authenticator basicAuthenticator = AuthenticatorFactory.createBasicAuthenticator(remoteUser, remotePassword);
            pull.setAuthenticator(basicAuthenticator);
            push.setAuthenticator(basicAuthenticator);

            if (events) {
                push.addChangeListener(new Replication.ChangeListener() {
                    @Override
                    public void changed(Replication.ChangeEvent event) {
                        boolean offline = push.getStatus() == Replication.ReplicationStatus.REPLICATION_OFFLINE;
                        if (offline) {
                            WritableMap eventOffline = Arguments.createMap();
                            eventOffline.putString("databaseName", event.getSource().getLocalDatabase().getName());
                            sendEvent(context, OFFLINE_KEY, eventOffline);
                        } else {
                            WritableMap eventOnline = Arguments.createMap();
                            eventOnline.putString("databaseName", event.getSource().getLocalDatabase().getName());
                            sendEvent(context, ONLINE_KEY, eventOnline);
                        }
                        if (event.getError() != null) {
                            Throwable lastError = event.getError();
                            if (lastError instanceof RemoteRequestResponseException) {
                                RemoteRequestResponseException exception = (RemoteRequestResponseException) lastError;
                                if (exception.getCode() == 401) {
                                    // Authentication error
                                    WritableMap eventError = Arguments.createMap();
                                    eventError.putString("databaseName", event.getSource().getLocalDatabase().getName());
                                    sendEvent(context, AUTH_ERROR_KEY, eventError);
                                } else if (exception.getCode() == 404){
                                    // Database not found error
                                    WritableMap eventError = Arguments.createMap();
                                    eventError.putString("databaseName", event.getSource().getLocalDatabase().getName());
                                    sendEvent(context, NOT_FOUND_KEY, eventError);
                                }
                            }
                        } else {
                            WritableMap eventM = Arguments.createMap();
                            eventM.putString("databaseName", event.getSource().getLocalDatabase().getName());
                            eventM.putString("changesCount", String.valueOf(event.getSource().getCompletedChangesCount()));
                            eventM.putString("totalChanges", String.valueOf(event.getSource().getChangesCount()));
                            sendEvent(context, PUSH_EVENT_KEY, eventM);
                        }
                    }
                });
                pull.addChangeListener(new Replication.ChangeListener() {
                    @Override
                    public void changed(Replication.ChangeEvent event) {
                        boolean offline = pull.getStatus() == Replication.ReplicationStatus.REPLICATION_OFFLINE;
                        if (offline) {
                            WritableMap eventOffline = Arguments.createMap();
                            eventOffline.putString("databaseName", event.getSource().getLocalDatabase().getName());
                            sendEvent(context, OFFLINE_KEY, eventOffline);
                        } else {
                            WritableMap eventOnline = Arguments.createMap();
                            eventOnline.putString("databaseName", event.getSource().getLocalDatabase().getName());
                            sendEvent(context, ONLINE_KEY, eventOnline);
                        }
                        if (event.getError() != null) {
                            Throwable lastError = event.getError();
                            if (lastError instanceof RemoteRequestResponseException) {
                                RemoteRequestResponseException exception = (RemoteRequestResponseException) lastError;
                                if (exception.getCode() == 401) {
                                    // Authentication error
                                    WritableMap eventError = Arguments.createMap();
                                    eventError.putString("databaseName", event.getSource().getLocalDatabase().getName());
                                    sendEvent(context, AUTH_ERROR_KEY, eventError);
                                } else if (exception.getCode() == 404){
                                    // Database not found error
                                    WritableMap eventError = Arguments.createMap();
                                    eventError.putString("databaseName", event.getSource().getLocalDatabase().getName());
                                    sendEvent(context, NOT_FOUND_KEY, eventError);
                                }
                            }
                        } else {
                            WritableMap eventM = Arguments.createMap();
                            eventM.putString("databaseName", event.getSource().getLocalDatabase().getName());
                            eventM.putString("changesCount", String.valueOf(event.getSource().getCompletedChangesCount()));
                            eventM.putString("totalChanges", String.valueOf(event.getSource().getChangesCount()));
                            sendEvent(context, PULL_EVENT_KEY, eventM);
                        }
                    }
                });
                db.addChangeListener(new Database.ChangeListener() {
                    @Override
                    public void changed(Database.ChangeEvent event) {
                        for (DocumentChange dc : event.getChanges()) {
                            WritableMap eventM = Arguments.createMap();
                            eventM.putString("databaseName", event.getSource().getName());
                            eventM.putString("id", dc.getDocumentId());
                            sendEvent(context, DB_EVENT_KEY, eventM);
                        }
                    }
                });
            }
            push.start();
            pull.start();

            if (onEnd != null)
                onEnd.invoke(this.listenPort);
        }catch(Exception e){
            throw new JavascriptException(e.getMessage());
        }
    }

    /**
     * Function to be shared to React-native, it starts already created local db syncing with remote
     * @param  databaseLocal    String      database for local server
     * @param  remoteURL        String      URL to remote couchbase
     * @param  remoteUser       String      user for remote server
     * @param  remotePassword   String      password for remote server
     * @param  events           Boolean     activate the events for push and pull
     * @param  onEnd            Callback    function to call when finish
     */
    @ReactMethod
    public void serverRemote(String databaseLocal, String remoteURL, String  remoteUser,
                                  String remotePassword, Boolean events, Callback onEnd) {

        Manager ss = this.managerServer;

        if(ss == null)
            throw new JavascriptException("CouchBase local server needs to be started first");
        if(!(databaseLocal != null && remoteURL != null && remoteUser != null && remotePassword != null))
            throw new JavascriptException("CouchBase Server bad arguments");

        try {
            URL url = new URL(remoteURL);
            Database db = ss.getExistingDatabase(databaseLocal);
            final Replication push = db.createPushReplication(url);
            final Replication pull = db.createPullReplication(url);
            pull.setContinuous(true);
            push.setContinuous(true);

            Authenticator basicAuthenticator = AuthenticatorFactory.createBasicAuthenticator(remoteUser, remotePassword);
            pull.setAuthenticator(basicAuthenticator);
            push.setAuthenticator(basicAuthenticator);

            if (events) {
                push.addChangeListener(new Replication.ChangeListener() {
                    @Override
                    public void changed(Replication.ChangeEvent event) {
                        boolean offline = push.getStatus() == Replication.ReplicationStatus.REPLICATION_OFFLINE;
                        if (offline) {
                            WritableMap eventOffline = Arguments.createMap();
                            eventOffline.putString("databaseName", event.getSource().getLocalDatabase().getName());
                            sendEvent(context, OFFLINE_KEY, eventOffline);
                        } else {
                            WritableMap eventOnline = Arguments.createMap();
                            eventOnline.putString("databaseName", event.getSource().getLocalDatabase().getName());
                            sendEvent(context, ONLINE_KEY, eventOnline);
                        }
                        if (event.getError() != null) {
                            Throwable lastError = event.getError();
                            if (lastError instanceof RemoteRequestResponseException) {
                                RemoteRequestResponseException exception = (RemoteRequestResponseException) lastError;
                                if (exception.getCode() == 401) {
                                    // Authentication error
                                    WritableMap eventError = Arguments.createMap();
                                    eventError.putString("databaseName", event.getSource().getLocalDatabase().getName());
                                    sendEvent(context, AUTH_ERROR_KEY, eventError);
                                }else if (exception.getCode() == 404){
                                    // Database not found error
                                    WritableMap eventError = Arguments.createMap();
                                    eventError.putString("databaseName", event.getSource().getLocalDatabase().getName());
                                    sendEvent(context, NOT_FOUND_KEY, eventError);
                                }
                            }
                        } else {
                            WritableMap eventM = Arguments.createMap();
                            eventM.putString("databaseName", event.getSource().getLocalDatabase().getName());
                            eventM.putString("changesCount", String.valueOf(event.getSource().getCompletedChangesCount()));
                            eventM.putString("totalChanges", String.valueOf(event.getSource().getChangesCount()));
                            sendEvent(context, PUSH_EVENT_KEY, eventM);
                        }
                    }
                });
                pull.addChangeListener(new Replication.ChangeListener() {
                    @Override
                    public void changed(Replication.ChangeEvent event) {
                        boolean offline = pull.getStatus() == Replication.ReplicationStatus.REPLICATION_OFFLINE;
                        if (offline) {
                            WritableMap eventOffline = Arguments.createMap();
                            eventOffline.putString("databaseName", event.getSource().getLocalDatabase().getName());
                            sendEvent(context, OFFLINE_KEY, eventOffline);
                        } else {
                            WritableMap eventOnline = Arguments.createMap();
                            eventOnline.putString("databaseName", event.getSource().getLocalDatabase().getName());
                            sendEvent(context, ONLINE_KEY, eventOnline);
                        }
                        if (event.getError() != null) {
                            Throwable lastError = event.getError();
                            if (lastError instanceof RemoteRequestResponseException) {
                                RemoteRequestResponseException exception = (RemoteRequestResponseException) lastError;
                                if (exception.getCode() == 401) {
                                    // Authentication error
                                    WritableMap eventError = Arguments.createMap();
                                    eventError.putString("databaseName", event.getSource().getLocalDatabase().getName());
                                    sendEvent(context, AUTH_ERROR_KEY, eventError);
                                }else if (exception.getCode() == 404){
                                    // Database not found error
                                    WritableMap eventError = Arguments.createMap();
                                    eventError.putString("databaseName", event.getSource().getLocalDatabase().getName());
                                    sendEvent(context, NOT_FOUND_KEY, eventError);
                                }
                            }
                        } else {
                            WritableMap eventM = Arguments.createMap();
                            eventM.putString("databaseName", event.getSource().getLocalDatabase().getName());
                            eventM.putString("changesCount", String.valueOf(event.getSource().getCompletedChangesCount()));
                            eventM.putString("totalChanges", String.valueOf(event.getSource().getChangesCount()));
                            sendEvent(context, PULL_EVENT_KEY, eventM);
                        }
                    }
                });
                db.addChangeListener(new Database.ChangeListener() {
                    @Override
                    public void changed(Database.ChangeEvent event) {
                        for (DocumentChange dc : event.getChanges()) {
                            WritableMap eventM = Arguments.createMap();
                            eventM.putString("databaseName", event.getSource().getName());
                            eventM.putString("id", dc.getDocumentId());
                            sendEvent(context, DB_EVENT_KEY, eventM);
                        }
                    }
                });
            }

            push.start();
            pull.start();

            if (onEnd != null)
                onEnd.invoke();
        }catch(Exception e){
            throw new JavascriptException(e.getMessage());
        }
    }

    /**
     * Function to be shared to React-native, it adds a listener for change events in database
     * @param  databaseLocal    String      database for local server
     * @param  promise          Promise     Promise to be returned to the JavaScript engine.
     */
    @ReactMethod
    public void databaseChangeEvents(String databaseLocal, Promise promise) {
        Manager ss = this.managerServer;

        if(ss == null)
            throw new JavascriptException("CouchBase local server needs to be started first");
        if(!(databaseLocal != null))
            throw new JavascriptException("CouchBase Server bad arguments");

        try {
            Database db = ss.getExistingDatabase(databaseLocal);
            db.addChangeListener(new Database.ChangeListener() {
                @Override
                public void changed(Database.ChangeEvent event) {
                    for (DocumentChange dc : event.getChanges()) {
                        WritableMap eventM = Arguments.createMap();
                        eventM.putString("databaseName", event.getSource().getName());
                        eventM.putString("id", dc.getDocumentId());
                        sendEvent(context, DB_EVENT_KEY, eventM);
                    }
                }
            });

            promise.resolve(null);
        }catch(Exception e){
            promise.reject("NOT_OPENED", e);
        }
    }

    /**
     * Function to be shared to React-native, it starts already created local db pull replication from remote
     * @param  databaseLocal    String      database for local server
     * @param  remoteURL        String      URL to remote couchbase
     * @param  remoteUser       String      user for remote server
     * @param  remotePassword   String      password for remote server
     * @param  events           Boolean     activate the events for pull
     * @param  promise          Promise     Promise to be returned to the JavaScript engine.
     */
    @ReactMethod
    public void serverRemotePull(String databaseLocal, String remoteURL, String  remoteUser,
                                  String remotePassword, Boolean events, Promise promise) {

        Manager ss = this.managerServer;

        if(ss == null)
            throw new JavascriptException("CouchBase local server needs to be started first");
        if(!(databaseLocal != null && remoteURL != null && remoteUser != null && remotePassword != null))
            throw new JavascriptException("CouchBase Server bad arguments");

        try {
            URL url = new URL(remoteURL);
            Database db = ss.getExistingDatabase(databaseLocal);
            final Replication pull = db.createPullReplication(url);
            pull.setContinuous(true);

            Authenticator basicAuthenticator = AuthenticatorFactory.createBasicAuthenticator(remoteUser, remotePassword);
            pull.setAuthenticator(basicAuthenticator);

            if (events) {
                pull.addChangeListener(new Replication.ChangeListener() {
                    @Override
                    public void changed(Replication.ChangeEvent event) {
                        boolean offline = pull.getStatus() == Replication.ReplicationStatus.REPLICATION_OFFLINE;
                        if (offline) {
                            WritableMap eventOffline = Arguments.createMap();
                            eventOffline.putString("databaseName", event.getSource().getLocalDatabase().getName());
                            sendEvent(context, OFFLINE_KEY, eventOffline);
                        } else {
                            WritableMap eventOnline = Arguments.createMap();
                            eventOnline.putString("databaseName", event.getSource().getLocalDatabase().getName());
                            sendEvent(context, ONLINE_KEY, eventOnline);
                        }
                        if (event.getError() != null) {
                            Throwable lastError = event.getError();
                            if (lastError instanceof RemoteRequestResponseException) {
                                RemoteRequestResponseException exception = (RemoteRequestResponseException) lastError;
                                if (exception.getCode() == 401) {
                                    // Authentication error
                                    WritableMap eventError = Arguments.createMap();
                                    eventError.putString("databaseName", event.getSource().getLocalDatabase().getName());
                                    sendEvent(context, AUTH_ERROR_KEY, eventError);
                                }else if (exception.getCode() == 404){
                                    // Database not found error
                                    WritableMap eventError = Arguments.createMap();
                                    eventError.putString("databaseName", event.getSource().getLocalDatabase().getName());
                                    sendEvent(context, NOT_FOUND_KEY, eventError);
                                }
                            }
                        } else {
                            WritableMap eventM = Arguments.createMap();
                            eventM.putString("databaseName", event.getSource().getLocalDatabase().getName());
                            eventM.putString("changesCount", String.valueOf(event.getSource().getCompletedChangesCount()));
                            eventM.putString("totalChanges", String.valueOf(event.getSource().getChangesCount()));
                            sendEvent(context, PULL_EVENT_KEY, eventM);
                        }
                    }
                });
            }

            pull.start();

            promise.resolve(null);
        }catch(Exception e){
            promise.reject("NOT_OPENED", e);
        }
    }

    /**
     * Function to be shared to React-native, it starts already created local db push replication to remote
     * @param  databaseLocal    String      database for local server
     * @param  remoteURL        String      URL to remote couchbase
     * @param  remoteUser       String      user for remote server
     * @param  remotePassword   String      password for remote server
     * @param  events           Boolean     activate the events for push
     * @param  promise          Promise     Promise to be returned to the JavaScript engine.
     */
    @ReactMethod
    public void serverRemotePush(String databaseLocal, String remoteURL, String  remoteUser,
                                  String remotePassword, Boolean events, Promise promise) {

        Manager ss = this.managerServer;

        if(ss == null)
            throw new JavascriptException("CouchBase local server needs to be started first");
        if(!(databaseLocal != null && remoteURL != null && remoteUser != null && remotePassword != null))
            throw new JavascriptException("CouchBase Server bad arguments");

        try {
            URL url = new URL(remoteURL);
            Database db = ss.getExistingDatabase(databaseLocal);
            final Replication push = db.createPushReplication(url);
            push.setContinuous(true);

            Authenticator basicAuthenticator = AuthenticatorFactory.createBasicAuthenticator(remoteUser, remotePassword);
            push.setAuthenticator(basicAuthenticator);

            if (events) {
                push.addChangeListener(new Replication.ChangeListener() {
                    @Override
                    public void changed(Replication.ChangeEvent event) {
                        boolean offline = push.getStatus() == Replication.ReplicationStatus.REPLICATION_OFFLINE;
                        if (offline) {
                            WritableMap eventOffline = Arguments.createMap();
                            eventOffline.putString("databaseName", event.getSource().getLocalDatabase().getName());
                            sendEvent(context, OFFLINE_KEY, eventOffline);
                        } else {
                            WritableMap eventOnline = Arguments.createMap();
                            eventOnline.putString("databaseName", event.getSource().getLocalDatabase().getName());
                            sendEvent(context, ONLINE_KEY, eventOnline);
                        }
                        if (event.getError() != null) {
                            Throwable lastError = event.getError();
                            if (lastError instanceof RemoteRequestResponseException) {
                                RemoteRequestResponseException exception = (RemoteRequestResponseException) lastError;
                                if (exception.getCode() == 401) {
                                    // Authentication error
                                    WritableMap eventError = Arguments.createMap();
                                    eventError.putString("databaseName", event.getSource().getLocalDatabase().getName());
                                    sendEvent(context, AUTH_ERROR_KEY, eventError);
                                }else if (exception.getCode() == 404){
                                    // Database not found error
                                    WritableMap eventError = Arguments.createMap();
                                    eventError.putString("databaseName", event.getSource().getLocalDatabase().getName());
                                    sendEvent(context, NOT_FOUND_KEY, eventError);
                                }
                            }
                        } else {
                            WritableMap eventM = Arguments.createMap();
                            eventM.putString("databaseName", event.getSource().getLocalDatabase().getName());
                            eventM.putString("changesCount", String.valueOf(event.getSource().getCompletedChangesCount()));
                            eventM.putString("totalChanges", String.valueOf(event.getSource().getChangesCount()));
                            sendEvent(context, PUSH_EVENT_KEY, eventM);
                        }
                    }
                });
            }
            push.start();

            promise.resolve(null);
        }catch(Exception e){
            promise.reject("NOT_OPENED", e);
        }
    }

    /**
     * Function to be shared to React-native, compacts an already created local database
     * @param  databaseLocal    String      database for local server
     */
    @ReactMethod
    public void compact(String databaseLocal) {

        Manager ss = this.managerServer;

        if(ss == null)
            throw new JavascriptException("CouchBase local server needs to be started first");
        if(databaseLocal == null)
            throw new JavascriptException("CouchBase Server bad arguments");

        try {
            Database db = ss.getExistingDatabase(databaseLocal);
            db.compact();
        }catch(Exception e){
            throw new JavascriptException(e.getMessage());
        }
    }

    /**
     * Function to be shared with React-native, it restarts push replications in order to check for 404
     * @param  databaseLocal    String      database for local server
     * */
    @ReactMethod
    public void refreshReplication(String databaseLocal) {
        Manager ss = this.managerServer;

        if(ss == null)
            throw new JavascriptException("CouchBase local server needs to be started first");
        if(databaseLocal == null)
            throw new JavascriptException("CouchBase Server bad arguments");
        try {
            Database db = ss.getExistingDatabase(databaseLocal);
            List<Replication> replications = db.getActiveReplications();
            for (Replication replication : replications) {
                if (!replication.isPull() && replication.isRunning() &&
                        replication.getStatus() == Replication.ReplicationStatus.REPLICATION_IDLE) {
                    replication.restart();
                }
            }
        }catch(Exception e){
            throw new JavascriptException(e.getMessage());
        }

    }

    /**
     * Enable debug log for CBL
     * @param  debug_mode      boolean      debug module for develop: true for VERBOSE log, false for Default log level.
     * */
    @ReactMethod
    public void enableLog(boolean debug_mode) {
            isDebug = new Boolean(debug_mode);
    }


    /**
     * Private functions to create couchbase server
     */
    private void startServer(Integer listen_port, String userLocal, String passwordLocal) throws JavascriptException{

        if(!(listen_port != null && userLocal != null && passwordLocal != null))
            throw new JavascriptException("CouchBase Server bad arguments");

        Manager server;
        try {
            Credentials allowedCredentials = new Credentials(userLocal, passwordLocal);
            URLStreamHandlerFactory.registerSelfIgnoreError();
            server = startCBLite();

            listenPort = startCBLListener( listen_port, server, allowedCredentials);

        } catch (Exception e) {
            throw new JavascriptException(e.getMessage());
        }
        this.managerServer = server;
    }

    private Manager startCBLite() throws IOException {
        if (this.isDebug){
            Manager.enableLogging(TAG, Log.VERBOSE);
            Manager.enableLogging(Log.TAG, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_SYNC, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_SYNC_ASYNC_TASK, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_BATCHER, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_QUERY, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_VIEW, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_CHANGE_TRACKER, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_BLOB_STORE, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_DATABASE, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_LISTENER, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_MULTI_STREAM_WRITER, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_REMOTE_REQUEST, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_ROUTER, Log.VERBOSE);
        }
        
        return new Manager( new AndroidContext(this.context), Manager.DEFAULT_OPTIONS);
    }

    private int startCBLListener(int listenPort, Manager manager, Credentials allowedCredentials) {
        LiteListener listener = new LiteListener(manager, listenPort, allowedCredentials);
        int boundPort = listener.getListenPort();
        Thread thread = new Thread(listener);
        thread.start();
        return boundPort;

    }

    /**
     * Function to send: push pull events
     */
    private void sendEvent(ReactContext reactContext, String eventName, WritableMap params) {
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    private static WritableMap mapToWritableMap(Map <String, Object> map) {
        WritableMap wm = Arguments.createMap();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof Boolean) {
                wm.putBoolean(entry.getKey(), ((Boolean) entry.getValue()).booleanValue());
            } else if (entry.getValue() instanceof Integer) {
                wm.putInt(entry.getKey(), ((Integer) entry.getValue()).intValue());
            } else if (entry.getValue() instanceof Double) {
                wm.putDouble(entry.getKey(), ((Double) entry.getValue()).doubleValue());
            } else if (entry.getValue() instanceof Float){
                wm.putDouble(entry.getKey(), ((Float) entry.getValue()).doubleValue());
            } else if (entry.getValue() instanceof Long){
                wm.putDouble(entry.getKey(), ((Long) entry.getValue()).doubleValue());
            } else if (entry.getValue() instanceof String) {
                wm.putString(entry.getKey(), ((String) entry.getValue()));
            } else if (entry.getValue() instanceof LinkedHashMap) {
                wm.putMap(entry.getKey(), CouchBase.mapToWritableMap((LinkedHashMap) entry.getValue()));
            } else if (entry.getValue() instanceof WritableMap) {
                wm.putMap(entry.getKey(), (WritableMap) entry.getValue());
            } else if (entry.getValue() instanceof WritableArray) {
                wm.putArray(entry.getKey(), (WritableArray) entry.getValue());
            } else if (entry.getValue() instanceof ArrayList) {
                wm.putArray(entry.getKey(), CouchBase.arrayToWritableArray(((ArrayList) entry.getValue()).toArray()));
            } else if (entry.getValue() instanceof Object[]) {
                wm.putArray(entry.getKey(), CouchBase.arrayToWritableArray((Object[]) entry.getValue()));
            } else {
                wm.putNull(entry.getKey());
            }
        }
        return wm;
    }

    private static WritableArray arrayToWritableArray(Object[] array) {
        WritableArray wa = Arguments.createArray();
        for (Object o : array) {
            if (o instanceof Map) {
                wa.pushMap(CouchBase.mapToWritableMap((Map) o));
            } else if (o instanceof WritableMap) {
                wa.pushMap((WritableMap) o);
            } else if (o instanceof String) {
                wa.pushString((String) o);
            } else if (o instanceof Boolean) {
                wa.pushBoolean(((Boolean) o).booleanValue());
            } else if (o instanceof Double) {
                wa.pushDouble(((Double) o).doubleValue());
            } else if (o instanceof Float) {
                wa.pushDouble(((Float) o).doubleValue());
            } else if (o instanceof Long) {
                wa.pushDouble(((Long) o).doubleValue());
            } else if (o instanceof Integer) {
                wa.pushInt(((Integer) o).intValue());
            } else if (o instanceof WritableArray) {
                wa.pushArray((WritableArray) o);
            } else if (o instanceof Object[]) {
                wa.pushArray(CouchBase.arrayToWritableArray((Object[])o));
            } else {
                wa.pushNull();
            }
        }

        return wa;
    }

    /**
     * Creates a database.
     * @param database  String  Database name.
     * @param promise   Promise Promise to be returned to the JavaScript engine.
     */
    @ReactMethod
    public void createDatabase(String database, Promise promise) {
        try {
            Manager ss = this.managerServer;
            Database db = ss.getDatabase(database);
            promise.resolve(null);
        } catch (CouchbaseLiteException e) {
            promise.reject("COUCHBASE_ERROR", e);
        } catch (Exception e) {
            promise.reject("NOT_OPENED", e);
        }
    }

    /**
     * Destroys a database.
     * @param database  String  Database name.
     * @param promise   Promise Promise to be returned to the JavaScript engine.
     */
    @ReactMethod
    public void destroyDatabase(String database, Promise promise) {
        Manager ss = this.managerServer;
        try {
            Database db = ss.getExistingDatabase(database);
            db.delete();
            promise.resolve(null);
        } catch (CouchbaseLiteException e) {
            promise.reject("COUCHBASE_ERROR", e);
        } catch (Exception e) {
            promise.reject("NOT_OPENED", e);
        }
    }

    /**
     * Gets an existing document from the database.
     * @param database  String  Database name.
     * @param docId     String  Document id.
     * @param promise   Promise Promise to be returned to the JavaScript engine.
     */
    @ReactMethod
    public void getDocument(String database, String docId, Promise promise) {
        Manager ss = this.managerServer;
        Database db = null;
        try {
            db = ss.getExistingDatabase(database);
            Map<String, Object> properties = null;

            Pattern pattern = Pattern.compile("^_local/(.+)");
            Matcher matcher = pattern.matcher(docId);

            // If it matches, it is a local document.
            if (matcher.find()) {
                String localDocId = matcher.group(1);
                properties = db.getExistingLocalDocument(localDocId);
            } else {
                Document doc = db.getExistingDocument(docId);
                if (doc != null) {
                    properties = doc.getCurrentRevision().getProperties();
                }
                
            }

            if (properties != null) {
                WritableMap result = CouchBase.mapToWritableMap(properties);
                promise.resolve(result);
            } else {
                promise.resolve(Arguments.createMap());
            }
        } catch (CouchbaseLiteException e) {
            promise.reject("COUCHBASE_ERROR", e);
        } catch (Exception e) {
            promise.reject("NOT_OPENED", e);
        }
    }

    /**
     * Gets all the existing documents from the database.
     * @param database  String          Database name.
     * @param docIds    ReadableArray   JavaScript array containing the keys.
     * @param promise   Promise         Promise to be returned to the JavaScript engine.
     */
    @ReactMethod
    public void getAllDocuments(String database, ReadableArray docIds, Promise promise) {
        Manager ss = this.managerServer;
        Database db = null;
        try {
            db = ss.getExistingDatabase(database);
            WritableArray results = Arguments.createArray();

            if (docIds == null || docIds.size() == 0) {
                Query query = db.createAllDocumentsQuery();
                query.setAllDocsMode(Query.AllDocsMode.ALL_DOCS);

                Iterator<QueryRow> it = query.run();
                while (it.hasNext()) {
                    QueryRow row = it.next();
                    WritableMap m = Arguments.createMap();
                    WritableMap m2 = Arguments.createMap();
                    m2.putString("rev", row.getDocumentRevisionId());
                    m.putString("key", String.valueOf(row.getKey()));
                    m.putString("_id", String.valueOf(row.getKey()));
                    m.putMap("doc", CouchBase.mapToWritableMap((Map) row.getValue()));
                    m.putMap("value", m2);
                    results.pushMap(m);
                }
            } else {
                Pattern pattern = Pattern.compile("^_local/(.+)");

                for (int i = 0; i < docIds.size(); i++) {
                    Map<String, Object> properties = null;
                    String docId = docIds.getString(i);
                    Matcher matcher = pattern.matcher(docId);

                    WritableMap m = Arguments.createMap();
                    // If it matches, it is a local document.
                    if (matcher.find()) {
                        String localDocId = matcher.group(1);
                        properties = db.getExistingLocalDocument(localDocId);
                    } else {
                        Document doc = db.getExistingDocument(docId);
                        if (doc != null) {
                            properties = doc.getProperties();
                            WritableMap m2 = Arguments.createMap();
                            m2.putString("rev", doc.getCurrentRevisionId());
                            m.putMap("value", m2);
                        }
                    }

                    if (properties != null) {
                        m.putString("key", String.valueOf(docId));
                        m.putString("_id", String.valueOf(docId));
                        m.putMap("doc", CouchBase.mapToWritableMap(properties));
                        results.pushMap(m);
                    }
                }
            }
            WritableMap ret = Arguments.createMap();
            ret.putArray("rows", results);
            ret.putInt("total_rows", results.size());
            promise.resolve(ret);
        } catch (CouchbaseLiteException e) {
            promise.reject("COUCHBASE_ERROR", e);
        } catch (Exception e) {
            promise.reject("NOT_OPENED", e);
        }
    }

    /**
     * Gets all the documents returned by the given view.
     * @param database String Database name.
     * @param design String Design document where the view can be found.
     * @param viewName String Name of the view to be queried.
     * @param params ReadableMap JavaScript object containing the extra parameters to pass to the view.
     * @param docIds ReadableArray JavaScript array containing the keys.
     * @param forceRebuild Force the rebuild of the view before querying it.
     * @param promise Promise Promise to be returned to the JavaScript engine.
     */
    @ReactMethod
    public void getView(String database, String design, String viewName, ReadableMap params, ReadableArray docIds, Promise promise) {
        Manager ss = managerServer;
        Database db = null;
        View view = null;
        try {
            db = ss.getExistingDatabase(database);

            view = db.getExistingView(viewName);

            if (view == null || (view != null && view.getMap() == null)) {
                Document viewDoc = db.getExistingDocument("_design/" + design);

                if (viewDoc == null) {
                    promise.reject("NOT_FOUND", "The document _design/" + design + " could not be found.");
                    return;
                }

                Map<String, Object> views = null;
                try {
                    views = (Map<String, Object>) viewDoc.getProperty("views");
                } catch (Exception e) {
                    promise.reject("NOT_FOUND", "The views could not be retrieved.", e);
                    return;
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> viewDefinition = (Map<String, Object>) views.get(viewName);

                if (viewDefinition == null) {
                    promise.reject("NOT_FOUND", "The view " + viewName + " could not be found.");
                    return;
                }

                Mapper mapper = View.getCompiler().compileMap((String) viewDefinition.get("map"), "javascript");
                String version = (String)viewDefinition.get("version");
                if (mapper == null) {
                    promise.reject("COMPILE_ERROR", "The map function of " + viewName + "could not be compiled.");
                    return;
                }

            
                view = db.getView(viewName);
                if (viewDefinition.containsKey("reduce")) {
                    Reducer reducer = View.getCompiler().compileReduce((String) viewDefinition.get("reduce"), "javascript");
                    if (reducer == null) {
                        promise.reject("COMPILE_ERROR", "The reduce function of " + viewName + "could not be compiled.");
                        return;
                    }
                    view.setMapReduce(mapper, reducer, version != null ? version : "1.1");
                } else {
                    view.setMap(mapper, version != null ? version : "1.1");
                }
            } else {
                view.updateIndex();
            }

            Query query = view.createQuery();

            if (params != null) {
                if (params.hasKey("startkey")) {
                    switch (params.getType("startkey")) {
                        case Array:
                            ReadableNativeArray array = (ReadableNativeArray)params.getArray("startkey");
                            query.setStartKey(array.toArrayList());
                            break;
                        case String:
                            query.setStartKey(params.getString("startkey"));
                            break;
                        case Number:
                            query.setStartKey(params.getInt("startkey"));
                            break;
                    }
                }
                if (params.hasKey("endkey")) {
                    switch (params.getType("startkey")) {
                        case Array:
                            ReadableNativeArray array = (ReadableNativeArray)params.getArray("endkey");
                            query.setEndKey(array.toArrayList());
                            break;
                        case String:
                            query.setStartKey(params.getString("endkey"));
                            break;
                        case Number:
                            query.setStartKey(params.getInt("endkey"));
                            break;
                    }
                }
                if (params.hasKey("descending"))
                    query.setDescending(params.getBoolean("descending"));
                if (params.hasKey("limit")) query.setLimit(params.getInt("limit"));
                if (params.hasKey("skip")) query.setSkip(params.getInt("skip"));
                if (params.hasKey("group")) query.setGroupLevel(params.getInt("group"));
            }

            if (docIds != null && docIds.size() > 0) {
                List<Object> keys = new ArrayList<Object>();
                for (int i = 0; i < docIds.size(); i++) {
                    keys.add(docIds.getString(i).toString());
                }
                query.setKeys(keys);
            }

            QueryEnumerator it = query.run();
            WritableArray results = Arguments.createArray();

            for (int i = 0; i < it.getCount(); i++) {
                QueryRow row = it.getRow(i);

                WritableMap m = Arguments.createMap();
                m.putString("key", row.getKey() != null ? String.valueOf(row.getKey()) : null);
                m.putMap("value", CouchBase.mapToWritableMap((Map<String,Object>)row.getValue()));
                results.pushMap(m);
            }

            WritableMap ret = Arguments.createMap();
            ret.putArray("rows", results);
            ret.putInt("offset", params != null && params.hasKey("skip") ? params.getInt("skip") : 0);
            ret.putInt("total_rows", view.getTotalRows());
            if (params != null && params.hasKey("update_seq") && params.getBoolean("update_seq") == true) {
                ret.putInt("update_seq", Long.valueOf(it.getSequenceNumber()).intValue());
            }
            promise.resolve(ret);
        } catch (CouchbaseLiteException e) {
            promise.reject("COUCHBASE_ERROR", e);
            if (view != null) view.delete();
        } catch (Exception e) {
            promise.reject("NOT_OPENED", e);
            if (view != null) view.delete();
        }
    }

    /**
     * Puts a document in the database.
     * @param database  String  Database name.
     * @param docId     String  Document id.
     * @param document  Object  Document params.
     * @param promise   Promise Promise to be returned to the JavaScript engine.
     */
    @ReactMethod
    public void putDocument(String database, String docId, ReadableMap document, Promise promise) {
        Manager ss = this.managerServer;
        Database db = null;
        try {
            db = ss.getExistingDatabase(database);
            Map<String, Object> properties = ((ReadableNativeMap) document).toHashMap();

            Pattern pattern = Pattern.compile("^_local/(.+)");
            Matcher matcher = pattern.matcher(docId);
            // If it matches, it is a local document.
            if (matcher.find()) {
                String localDocId = matcher.group(1);
                db.putLocalDocument(localDocId, properties);
                promise.resolve(null);
            } else {
                Document doc = db.getDocument(docId);
                if (doc != null) {
                    doc.putProperties(properties);
                    promise.resolve(null);
                } else {
                    promise.reject("MISSING_DOCUMENT", "Could not create/update document");
                }
            }
        } catch (CouchbaseLiteException e) {
            promise.reject("COUCHBASE_ERROR", e);
        } catch (Exception e) {
            promise.reject("NOT_OPENED", e);
        }
    }
}
