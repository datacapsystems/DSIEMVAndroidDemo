# DSIEMVAndroidDemo
The DSIEMVAndroid Demo project gives the end user a sample usage as well a local listener to run test transactions.

#Download the apk here: 
[APK](https://raw.githubusercontent.com/datacapsystems/DSIEMVAndroidDemo/development/dsiEMVAndroidDemo.apk)


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


### Report bugs
If you encounter any bugs or issues with the latest version of dsiEMVAndroid, please report them to us by opening a [GitHub Issue](https://github.com/datacapsystems/DSIEMVAndroidDemo/issues)!