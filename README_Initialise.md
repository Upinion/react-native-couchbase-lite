## Using REST API
When the given port is being used the REST API server is created in a different one and returned in the onEnd callback.

### Create local couchbase with REST API enabled
```java
 /** 
 * starts a local couchbase server
 * @param  listen_port      Integer     port to start server
 * @param  userLocal        String      user for local server
 * @param  passwordLocal    String      password for local server
 * @param  onEnd            Callback    function to call when finish (recieves port being used: function(int))
 */
CouchBase.serverLocal(Integer listen_port, String userLocal, String passwordLocal, Callback onEnd)
```
### Create local couchbase with REST API enabled and syncs local database with remote
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
 * @param  onEnd            Callback    function to call when finish (recieves port being used: function(int))
 */
CouchBase.serverLocalRemote(Integer listen_port, String userLocal, String passwordLocal, String databaseLocal,
                                String remoteURL, String  remoteUser, String remotePassword, Boolean events,
                                Callback onEnd)
```
## Not using REST API
This does not create a HTTP Server, so you can only access couchbase through the available functions within the plugin.
### Initialise couchbase
```java
 /** 
 * starts a couchbase manager instance
 * @param  onEnd            Callback    function to call when finish
 */
CouchBase.serverManager(Callback onEnd)
```

## Syncing remote

### Syncs already created local database with remote
If `events = true` will generate events for push / pull / change database.
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

### Adds listener for change database events.
It will generate change database events.
```java
/**
 * Function to be shared to React-native, it adds a listener for change events in database
 * @param  databaseLocal    String      database for local server
 * @param  promise          Promise     Promise to be returned to the JavaScript engine.
 */
CouchBase.databaseChangeEvents(String databaseLocal, Promise promise) {
```

### Starts already created local db pull replication from remote.
It will generate pull events.
```java
/**
 * Function to be shared to React-native, it starts already created local db pull replication from remote
 * @param  databaseLocal    String      database for local server
 * @param  remoteURL        String      URL to remote couchbase
 * @param  remoteUser       String      user for remote server
 * @param  remotePassword   String      password for remote server
 * @param  events           Boolean     activate the events for pull
 * @param  promise          Promise     Promise to be returned to the JavaScript engine.
 */
CouchBase.serverRemotePull(String databaseLocal, String remoteURL, String  remoteUser,
                                  String remotePassword, Boolean events, Promise promise) {
```

### Starts already created local db push replication to remote.
It will generate push events.
```java
/**
 * Function to be shared to React-native, it starts already created local db push replication to remote
 * @param  databaseLocal    String      database for local server
 * @param  remoteURL        String      URL to remote couchbase
 * @param  remoteUser       String      user for remote server
 * @param  remotePassword   String      password for remote server
 * @param  events           Boolean     activate the events for push
 * @param  promise          Promise     Promise to be returned to the JavaScript engine.
 */
CouchBase.serverRemotePull(String databaseLocal, String remoteURL, String  remoteUser,
                                  String remotePassword, Boolean events, Promise promise) {
```
