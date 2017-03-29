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
