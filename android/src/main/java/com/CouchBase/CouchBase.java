package com.upinion.CouchBase;

import android.content.Intent;
import android.content.Context;

import com.couchbase.lite.Document;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.JavascriptException;
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
            Database db = ss.getDatabase(databaseLocal);
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
            Database db = ss.getDatabase(databaseLocal);
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
            Database db = ss.getDatabase(databaseLocal);
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
            Database db = ss.getDatabase(databaseLocal);
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
            } else if (entry.getValue() instanceof String) {
                wm.putString(entry.getKey(), ((String) entry.getValue()));
            } else if (entry instanceof Map<?, ?>) {
                wm.putMap(entry.getKey(), CouchBase.mapToWritableMap((Map<String, Object>) entry.getValue()));
            } else {
                WritableArray array = CouchBase.arrayToWritableMap((Object[]) entry.getValue());
                if (array != null) {
                    wm.putArray(entry.getKey(), array);
                } else {
                    wm.putNull(entry.getKey());
                }
            }
        }
        return wm;
    }

    private static WritableArray arrayToWritableMap(Object[] array) {
        WritableArray wa = Arguments.createArray();
        if (array instanceof String[]) {
            for (String v: (String[]) array) {
                wa.pushString(v);
            }

        } else if (array instanceof Map<?, ?>[]) {
            for (Map<String, Object> v: (Map<String, Object>[])  array) {
                wa.pushMap(CouchBase.mapToWritableMap(v));
            }
        } else if (array instanceof Integer[]) {
            for (Integer v : (Integer[]) array) {
                wa.pushInt(v.intValue());
            }
        } else if (array instanceof Float[]) {
            for (Float v: (Float[]) array) {
                wa.pushDouble(v.doubleValue());
            }
        } else if (array instanceof Double[]) {
            for (Double v: (Double[]) array) {
                wa.pushDouble(v.doubleValue());
            }
        } else if (array instanceof Boolean[]) {
            for (Boolean v: (Boolean[]) array) {
                wa.pushBoolean(v.booleanValue());
            }
        } else {
            return null;
        }
    }

    @ReactMethod
    public void getDocument(String database, String docId, Promise promise) {
        try {
            Manager ss = this.startCBLite();
            Database db = ss.getDatabase(database);
            Map<String, Object> properties = null;

            Pattern pattern = Pattern.compile("^_local/(.+)");
            Matcher matcher = pattern.matcher(docId);

            // If it matches, it is a local document.
            if (matcher.find()) {
                String localDocId = matcher.group(0);
                 properties = db.getExistingLocalDocument(localDocId);
            } else {
                Document doc = db.getExistingDocument(docId);
                if (doc != null) {
                    properties = doc.getProperties();
                }
            }

            if (properties != null) {
                WritableMap result = CouchBase.mapToWritableMap(properties);

                promise.resolve(result);
            } else {
                promise.resolve(Arguments.createMap());
            }
        } catch (IOException e) {
            promise.reject("NOT_OPENED", e);
        } catch (CouchbaseLiteException e) {
            promise.reject("COUCHBASE_ERROR", e);
        }
    }


    @ReactMethod
    private void getAll(String database, String[] docIds, Promise promise) {
        try {
            Manager ss = this.startCBLite();
            Database db = ss.getDatabase(database);
            List<WritableMap> results = new ArrayList<WritableMap>();

            if (docIds == null || docIds.length == 0) {
                Query query = db.createAllDocumentsQuery();
                query.setAllDocsMode(Query.AllDocsMode.ALL_DOCS);

                QueryEnumerator queryResults = query.run();

                Iterator<QueryRow> it = queryResults;
                while (it.hasNext()) {
                    QueryRow row = it.next();
                    if (row.getDocumentProperties() != null) {
                        results.add(CouchBase.mapToWritableMap(row.getDocumentProperties()));
                    }
                }
            } else {
                Map<String, Object> properties = null;
                Pattern pattern = Pattern.compile("^_local/(.+)");

                for (String docId : docIds) {
                    Matcher matcher = pattern.matcher(docId);

                    // If it matches, it is a local document.
                    if (matcher.find()) {
                        String localDocId = matcher.group(0);
                        properties = db.getExistingLocalDocument(localDocId);
                    } else {
                        Document doc = db.getExistingDocument(docId);
                        if (doc != null) {
                            properties = doc.getProperties();
                        }
                    }

                    if (properties != null) {
                        results.add(CouchBase.mapToWritableMap(properties));
                    }
                }
            }

            promise.resolve((WritableMap[]) results.toArray());
        } catch (IOException e) {
            promise.reject("NOT_OPENED", e);
        } catch (CouchbaseLiteException e) {
            promise.reject("COUCHBASE_ERROR", e);
        }
    }
    
    @ReactMethod
    private void getView(String database, String design, String viewName, Map<String, Object> params, String[] docIds, Promise promise) {
        try {
            Manager ss = this.startCBLite();
            Database db = ss.getDatabase(database);

            db.getExistingView()
        } catch (IOException e) {
            promise.reject("NOT_OPENED", e);
        } catch (CouchbaseLiteException e) {
            promise.reject("COUCHBASE_ERROR", e);
        }
    }
}



