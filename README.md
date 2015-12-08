# react-native-linking-android
React native linking android gives you a general interface to interact with outgoing app links.

### Installation

```bash
npm install --save react-native-linking-android

or manually

git clone the directory to [node_modules/react-native-linking-android]
```

### Add it to your android project

* In `android/setting.gradle`

```gradle
...
include ':LinkingAndroid', ':app'
project(':LinkingAndroid').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-linking-android')
```

* In `android/app/build.gradle`

```gradle
...
dependencies {
    ...
    compile project(':LinkingAndroid')
}
```

* register module (in MainActivity.java)

```java
import com.upinion.LinkingAndroid.LinkingAndroidPackage;  // <--- import

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
      .addPackage(new LinkingAndroidPackage())              // <------ add here
      .setUseDeveloperSupport(BuildConfig.DEBUG)
      .setInitialLifecycleState(LifecycleState.RESUMED)
      .build();

 
  ......

}
```

## Example

LinkingAndroid.show(uri)

* **uri:** *(string)* url to be opened by the Android phone, 
for example: [http://domain.com] or [mailto:info@domain.com].

Example:

```javascript
var LinkingAndroid = require('react-native-linking-android');

LinkingAndroid.show('mailto:info@domain.com');
```
