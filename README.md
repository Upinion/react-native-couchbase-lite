# react-native-couchbase

React native Couchbase allows to create a couchbase server.

## Installation

```bash
npm install --save react-native-couchbase

or manually

git clone the directory to [node_modules/react-native-couchbase]
```

## Guide
* [Initialising](https://github.com/Upinion/react-native-couchbase-lite/blob/master/README_Initialise.md): How to start couchbase.

* [Functions](https://github.com/Upinion/react-native-couchbase-lite/blob/master/README_Functions.md): Available functions to interact with couchbase.

* [Events](https://github.com/Upinion/react-native-couchbase-lite/blob/master/README_Constants.md): All the different events emitted.

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
