# react-native-couchbase

React native Couchbase allows to create a couchbase server.

## Installation

```bash
npm install --save react-native-couchbase

or manually

git clone the directory to [node_modules/react-native-couchbase]
```

## Constants

* Using `DeviceEventEmitter`

* Pull Event
```bash
DeviceEventEmitter.addListener(CouchBase.PULL ...
```

* Push Event
```bash
DeviceEventEmitter.addListener(CouchBase.PUSH ...
```

* DB Changed Event
```bash
DeviceEventEmitter.addListener(CouchBase.DBChanged ...
```

* Authentication Error Event
```bash
// Use for detecting wrong credentials in remote database
DeviceEventEmitter.addListener(CouchBase.AuthError ...
```

* Remote Not Found Error Event
```bash
// Use for detecting not existing remote database
DeviceEventEmitter.addListener(CouchBase.NotFound ...
```

* Online / Offline Events
```bash
// Use for detecting offline status
DeviceEventEmitter.addListener(CouchBase.Online ...
DeviceEventEmitter.addListener(CouchBase.Offline ...
```

* Event DB Changed attributes
```bash
event.databaseName          (String)    //Database related to event
event.id                    (String)    //ID of the document changed
```
* Event PULL/PUSH attributes
```bash
event.databaseName          (String)    //Database related to event
event.completedChangesCount (Integer)   //Changes pulled/pushed at the moment
event.changesCount          (Integer)   //Total of changes to pull/push
```
* Event AuthError attributes
```bash
event.databaseName          (String)    //Database related to event
```
* Event NotFound attributes
```bash
event.databaseName          (String)    //Database related to event
```
* Event Online / Offline attributes
```bash
event.databaseName          (String)    //Database related to event
```

* Example of use
```java
DeviceEventEmitter.addListener(CouchBase.DBChanged, (event) => {
    if (event.databaseName == 'your database')
        //do something related to document changed event on that database
        console.log(event.id);
    });
```

## Functions

### Close Database and Replicators (ONLY iOS)
### Useful if you want to destroy a database which has opened replicators
```java
 /**
 * IMPORTANT: This function is only available in IOS
 * Close Database and Replicators
 * @param  database     string      name of database
 * @param  onEnd        Callback    function to call when finish
 */
CouchBase.closeDatabase(string database, Callback onEnd)
```

### Set timeout for PULL or PUSH (ONLY iOS)
```java
 /**
 * IMPORTANT: This function is only available in IOS
 * Set timeout for pull or push requests
 * @param  timeout      integer     timeout for requests in ms
 */
CouchBase.setTimeout(integer timeout)
```
### Resfresh PUSH Replications (ONLY Android)
```java
 /**
 * IMPORTANT: This function is only available in Android
 * Used to triggers events like not existing Database after replaction started.
 * @param  database     string      name of database
 */
CouchBase.setTimeout(integer timeout)
```
### Enable debug log
```java
 /**
 * Enable debug log for CBL
 * @param  debug_mode      boolean      debug module for develop: true for VERBOSE log, false for Default log level.
 */
CouchBase.enableLog(boolean debug_mode)
```

### Create local couchbase

```java
 /** 
 * starts a local couchbase server
 * @param  listen_port      Integer     port to start server
 * @param  userLocal        String      user for local server
 * @param  passwordLocal    String      password for local server
 * @param  onEnd            Callback    function to call when finish (recieve port being used: function(int))
 */
CouchBase.serverLocal(Integer listen_port, String userLocal, String passwordLocal, Callback onEnd)
```

### Create local couchbase and syncing with remote

```java 
/**
 * starts a local couchbase server and syncs with remote
 * @param  listen_port      Integer     port to start server
 * @param  userLocal        String      user for local server
 * @param  passwordLocal    String      password for local server
 * @param  databaseLocal    String      database for local server
 * @param  remoteURL        String      URL to remote couchbase
 * @param  remoteUser       String      user for remote server
 * @param  remotePassword   String      password for remote server
 * @param  events           Boolean     activate the events for push and pull
 * @param  onEnd            Callback    function to call when finish (recieve port being used: function(int))
 */
CouchBase.serverLocalRemote(Integer listen_port, String userLocal, String passwordLocal, String databaseLocal,
                                String remoteURL, String  remoteUser, String remotePassword, Boolean events,
                                Callback onEnd)
```

### Create syncing with already created local database
```java
/**
 * starts syncing between, already created, local db and remote
 * @param  databaseLocal    String      database for local server
 * @param  remoteURL        String      URL to remote couchbase
 * @param  remoteUser       String      user for remote server
 * @param  remotePassword   String      password for remote server
 * @param  events           Boolean     activate the events for push and pull
 * @param  onEnd            Callback    function to call when finish (doesnt recieve any value: function(void))
 */
CouchBase.serverRemote(String databaseLocal, String remoteURL, String  remoteUser,
                            String remotePassword, Boolean events, Callback onEnd) {
```
### Compact an existing local database
```java
/**
 * compacts an already created local database
 * @param  databaseLocal    String      database for local server
 */
public void compact(String databaseLocal)
```

## Retrieving (WIP)

### Get document.
```java
/**
 * Gets an existing document from the database.
 * @param database  String  Database name.
 * @param docId     String  Document id.
 * @param promise   Promise Promise to be returned to the JavaScript engine.
 */
 public void getDocument(String database, String docId, Promise promise)
```
### Get all documents.
```java
/**
 * Gets all the existing documents from the database.
 * @param database  String          Database name.
 * @param docIds    ReadableArray   JavaScript array containing the keys.
 * @param promise   Promise         Promise to be returned to the JavaScript engine.
 */
 public void getAllDocuments(String database, RedeableArray docIds, Promise promise)
```
### Get view.
```java
/**
 * Gets all the documents returned by the given view.
 * @param database  String          Database name.
 * @param design    String          Design document where the view can be found.
 * @param viewName  String          Name of the view to be queried.
 * @param params    ReadableMap     JavaScript object containing the extra parameters to pass to the view.
 * @param docIds    ReadableArray   JavaScript array containing the keys.
 * @param promise   Promise         Promise to be returned to the JavaScript engine.
 */
 public void getView(String database, String design, String viewName, ReadableMap params, ReadableArray docIds, Promise promise)
```

## Access to get, getAll and getView (WIP)

### Get document.
```java
/**
 * Gets an existing document from the database.
 * @param database  String  Database name.
 * @param docId     String  Document id.
 * @param promise   Promise Promise to be returned to the JavaScript engine.
 */
 public void getDocument(String database, String docId, Promise promise)
```
### Get all documents.
```java
/**
 * Gets all the existing documents from the database.
 * @param database  String          Database name.
 * @param docIds    ReadableArray   JavaScript array containing the keys.
 * @param promise   Promise         Promise to be returned to the JavaScript engine.
 */
 public void getAllDocuments(String database, RedeableArray docIds, Promise promise)
```
### Get view.
```java
/**
 * Gets all the documents returned by the given view.
 * @param database  String          Database name.
 * @param design    String          Design document where the view can be found.
 * @param viewName  String          Name of the view to be queried.
 * @param params    ReadableMap     JavaScript object containing the extra parameters to pass to the view.
 * @param docIds    ReadableArray   JavaScript array containing the keys.
 * @param promise   Promise         Promise to be returned to the JavaScript engine.
 */
 public void getView(String database, String design, String viewName, ReadableMap params, ReadableArray docIds, Promise promise)
```

## Access to create / destroy dbs (WIP)

### Create database.
```java
/**
 * Creates a database without HTTP server or replicators.
 * @param database  String  Database name.
 * @param promise   Promise Promise to be returned to the JavaScript engine.
 */
 public void createDatabase(String database, Promise promise)
```
### Destroy database.
```java
/**
 * Destroys an existing database (also stops replicators).
 * @param database  String          Database name.
 * @param promise   Promise         Promise to be returned to the JavaScript engine.
 */
 public void detroyDatabase(String database, Promise promise)
```

## Saving (WIP)
```java
/**
 * Creates/Updates a document.
 * @param database  String  Database name.
 * @param docId     String  Document id.
 * @param params    ReadableMap Javascript object containing document data.
 * @param promise   Promise Promise to be returned to the JavaScript engine.
 */
 public void getDocument(String database, String docId, ReadableMap params, Promise promise)
```

## Setup

### Android

* In `android/setting.gradle`

```gradle
...
include ':CouchBase', ':app'
project(':CouchBase').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-couchbase/android')
```

* In `android/app/build.gradle`

```gradle
android {
    ...
    
    // workaround for "duplicate files during packaging of APK" issue
    // see https://groups.google.com/d/msg/adt-dev/bl5Rc4Szpzg/wC8cylTWuIEJ
    packagingOptions {
        exclude 'META-INF/ASL2.0'
        exclude 'META-INF/LICENSE'
        exclude 'META-INF/NOTICE'
    }
}


dependencies {
    ...
    compile project(':CouchBase')
}
```

* give internet and network-state access, in `android/app/src/main/AndroidManifest.xml`:
 
```xml
<manifest ...>
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    ...
</manifest>
```

* register module (in MainActivity.java)

```java
import com.upinion.CouchBase.CouchBasePackage;  // <--- import

public class MainActivity extends Activity implements DefaultHardwareBackBtnHandler {
  ......
    
    /**
     * A list of packages used by the app. If the app uses additional views
     * or modules besides the default ones, add more packages here.
     */
    @Override
    protected List<ReactPackage> getPackages() {
      return Arrays.<ReactPackage>asList(
        new MainReactPackage(),
        new CouchBasePackage()
      );
    }
}
```

### iOS

1. [Download](http://www.couchbase.com/nosql-databases/downloads) Couchbase
   Mobile for iOS and add the libraries to the React native project, including
   CBLRegisterJSViewCompiler.h
1. Add the project ```RCTCouchBase``` to the React Native project.
2. In ```RCTCouchBase```, in ```Header Search Paths```, add the route to your
   ```${react native mobile application}/node_modules/react-native/React``` folder and also Frameworks headers
3. In the React Native project, you have to add the following frameworks and
   libraries to your ```Build phase```:
   - libCBJSViewCompiler.a
   - libRCTCouchBase (from the product folder in RCTCouchBase)
   - CouchbaseLite.framework
   - CouchbaseLiteListener.framework
   - CFNetwork.framework
   - Security.framework
   - SystemConfiguration.framework
   - libsqlite3.dylib
   - libz.dylib
4. Compile ```RCTCouchBase``` and run the React native project.
