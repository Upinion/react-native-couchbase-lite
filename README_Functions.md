## Create/Destroy databases

### Create database.
```java
/**
 * Creates a database without HTTP server or replicators.
 * IMPORTANT: If succeed, the promise is resolve returning an empty value.
 * IMPORTANT: If failed, the promise is rejected returning the error.
 * @param database  String  Database name.
 * @param promise   Promise Promise to be returned to the JavaScript engine.
 */
 public void createDatabase(String database, Promise promise)
```
### Destroy database.
```java
/**
 * Destroys an existing database (also stops replicators).
 * IMPORTANT: If succeed, the promise is resolve returning an empty value.
 * IMPORTANT: If failed, the promise is rejected returning the error.
 * @param database  String          Database name.
 * @param promise   Promise         Promise to be returned to the JavaScript engine.
 */
 public void detroyDatabase(String database, Promise promise)
```

## Retrieving data

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

## Storing data

### Put document.
```java
/**
 * Creates/Updates a document.
 * @param database  String  Database name.
 * @param docId     String  Document id.
 * @param params    ReadableMap Javascript object containing document data.
 * @param promise   Promise Promise to be returned to the JavaScript engine.
 */
 public void putDocument(String database, String docId, ReadableMap params, Promise promise)
```

## Useful Functions

### Close Database and Stop Replicators (ONLY iOS)
### Useful if you want to stop replicators (needed for destroying database through REST API)
```java
 /**
 * IMPORTANT: This function is only available in IOS
 * Close Database and Replicators
 * @param  database     string      name of database
 * @param  onEnd        Callback    function to call when finish
 */
CouchBase.closeDatabase(string database, Callback onEnd)
```
### Compact an existing local database
```java
/**
 * compacts an already created local database
 * @param  databaseLocal    String      database for local server
 */
public void compact(String databaseLocal)
```
### Set timeout for PULL and PUSH replicators (ONLY iOS)
```java
 /**
 * IMPORTANT: This function is only available in IOS
 * Set timeout for pull or push requests
 * @param  timeout      integer     timeout for requests in ms
 */
CouchBase.setTimeout(integer timeout)
```
### Resfresh PUSH replicators (ONLY Android)
```java
 /**
 * IMPORTANT: This function is only available in Android
 * Used to triggers events like not existing Database after replaction started.
 * @param  database     string      name of database
 */
CouchBase.refreshReplication(String databaseLocal)
```
### Enable debug log
```java
 /**
 * Enable debug log for CBL
 * @param  debug_mode      boolean      debug module for develop: true for VERBOSE log, false for Default log level.
 */
CouchBase.enableLog(boolean debug_mode)
```
