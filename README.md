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
* Event attributes
```bash
event.databaseName  (String)    //Database related to event
event.id            (String)    //ID of the document changed (only in DB Changed Event)
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
project(':CouchBase').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-couchbase')
```

* In `android/app/build.gradle`

```gradle
...
dependencies {
    ...
    compile project(':CouchBase')
}
```

* register module (in MainActivity.java)

```java
import com.upinion.CouchBase.CouchBasePackage;  // <--- import

public class MainActivity extends Activity implements DefaultHardwareBackBtnHandler {
  ......

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mReactRootView = new ReactRootView(this);

    mReactInstanceManager = ReactInstanceManager.builder()
      .setApplication(getApplication())
      .setBundleAssetName("index.android.bundle")
      .setJSMainModuleName("index.android")
      .addPackage(new MainReactPackage())
      .addPackage(new CouchBasePackage())              // <------ add here
      .setUseDeveloperSupport(BuildConfig.DEBUG)
      .setInitialLifecycleState(LifecycleState.RESUMED)
      .build();
  ......

}
```

### iOS

1. [Download](http://www.couchbase.com/nosql-databases/downloads) Couchbase
   Mobile for iOS and add the libraries to the React native project, including
   CBLRegisterJSViewCompiler.h
1. Add the project ```RCTCouchBase``` to the React Native project.
2. In ```RCTCouchBase```, in ```Header Search Paths```, add the route to your
   ```${react native mobile application}/node_modules/react-native/React``` folder
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
