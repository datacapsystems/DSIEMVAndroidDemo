# DSIEMVAndroidDemo
The DSIEMVAndroid Demo project gives the end user a sample usage as well a local listener to run test transactions.

# Getting started with dsiEMVAndroid

### Add dsiEMVAndroid.aar to your Android (Gradle) project
1. Place DSIEMVAndroid.aar in the `libs` folder of your app.
2. Ensure `libs` folder is included in `flatDir` repositories in your app's build.gradle file.

    ```
    repositories {
      flatDir {
        dirs 'libs'
      }
    }
    ```

3. Add dsiEMVAndroid.aar to the `dependencies` in your app's build.gradle file.

    ```
    dependencies {
      compile (name: 'dsiEMVAndroid', ext: 'aar')
    }
    ```

### Include the library in your code
```java
import com.datacap.android.dsiEMVAndroid;
```

### Example usage to connect to a Bluetooth device
*When connecting to a USB device, this method is not needed
```java
//pass the current activity as context for constructing the Andorid control
dsiEMVAndroid mDSIEMVAndroid = new dsiEMVAndroid(MainActivity.this);

//start the connection process
new Thread(new Runnable() {
    public void run() {
        mDSIEMVAndroid.EstablishBluetoothConnection("IDTECH-VP3300-79156");
    }
}).start();

//listen for the connection response to see if connection was successful
mDSIEMVAndroid.AddEstablishBluetoothConnectionResponseListener(new EstablishBluetoothConnectionResponseListener() {
    @Override
    public void OnEstablishBluetoothConnectionResponseChanged(String response) {
	//read and process the xml response
    }
});

```

### Example usage to process a transcation
```java


//pass in xml datacap command for running a sale or any other transaction
new Thread(new Runnable() {
    public void run() {
        mDSIEMVAndroid.ProcessTransaction("<xml request>");
    }
}).start();

//listen for the process transaction response
DSIEMVAndroidInstance.getInstance(MainActivity.this).AddProcessTransactionResponseListener(new ProcessTransactionResponseListener() {
    @Override
    public void OnProcessTransactionResponseChanged(String response) {
	//read and process xml response

    }
});

```

### Local listener (port 8080)
The app runs its HTTP listener as a foreground service: the live dispatch is the nested `LocalListener` class inside `LocalListenerService.java` (the top-level `LocalListener.java` is the older activity-hosted copy, kept in step). It accepts any `POST` and hands the body to `ProcessTransaction`, so a POS or test tool on the same network can drive the pad with ordinary TStream XML. A `POST` to a path containing `collectcarddata` goes to `CollectCardData` instead, and two bodies are special-cased on substring and never reach `ProcessTransaction`:

| Body contains | Listener does | Returns |
|---|---|---|
| `TransactionCancel` | `CancelRequest()` — cancels the in-flight transaction; must be sent on a second connection while the original `POST` is still open | empty body |
| `GetDevicesInfo` | `GetDevicesInfo()` — the library's supported-device list, which is a direct call rather than a TranCode | `<Devices>` XML (`SecureDeviceID<n>` / `Description<n>` / `Interface<n>`) |

A minimal device-list request is `<TStream><Admin><TranCode>GetDevicesInfo</TranCode></Admin></TStream>`.


### Report bugs
If you encounter any bugs or issues with the latest version of dsiEMVAndroid, please report them to us by opening a [GitHub Issue](https://github.com/datacapsystems/DSIEMVAndroidDemo/issues)!