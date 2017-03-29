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
