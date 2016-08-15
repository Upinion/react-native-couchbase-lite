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
