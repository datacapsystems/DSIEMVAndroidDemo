package com.example.dsiemvandroiddemo;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.datacap.android.BluetoothConnectionListener;
import com.datacap.android.EstablishBluetoothConnectionResponseListener;
import com.datacap.android.DisplayMessageListener;
import com.datacap.android.ProcessTransactionResponseListener;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


import static com.example.dsiemvandroiddemo.R.id.selectDevice;
import static com.example.dsiemvandroiddemo.R.id.saleButton;
import static com.example.dsiemvandroiddemo.R.id.returnButton;
import static com.example.dsiemvandroiddemo.R.id.cancelButton;
import static com.example.dsiemvandroiddemo.R.id.getDevicesInfoButton;
import static com.example.dsiemvandroiddemo.R.id.emvParamDownloadButton;
import static com.example.dsiemvandroiddemo.R.id.amountText;
import static com.example.dsiemvandroiddemo.R.id.merchantIDText;
import static com.example.dsiemvandroiddemo.R.id.IPPadtext;
import static com.example.dsiemvandroiddemo.R.id.PadPorttext;
import static com.example.dsiemvandroiddemo.R.id.nameOfDeviceText;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_BACKGROUND_LOCATION = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private static final String VP300_USB = "IDTECH-VP3300-USB";
    private static final String LANE3000_IP = "INGENICO_LANE_3000_IP";
    private BluetoothAdapter mBtAdapter;
    private DialogInterface.OnClickListener mDeviceSelection;
    private String mConnectedDevice = "";
    private int mNamePos = 1;
    private String[] mDeviceList = {"", "", "", "", "", "", "", ""};
    private AlertDialog mBTdialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //check for bluetooth and location permissions for bluetooth LE to work.
        // the user must agree to location sharing because locations is part of bluetooth LE spec.
        hasPermissions();
        try {
            //sets up local endpoint to be used with EMV US Test Client
            LocalListener li = new LocalListener(MainActivity.this);
        } catch (Exception ex) {
            //could not start the local server listener
        }

        //setup device dialog click action
        mDeviceSelection = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ListView lv = ((AlertDialog) dialog).getListView();
                TextView v = (TextView) lv.getChildAt(which);
                String tempName = v.getText().toString();
                if (!tempName.equals("")) {
                    if (mConnectedDevice.equals(tempName) &&
                            ((!mConnectedDevice.equals(VP300_USB)) && !mConnectedDevice.equals(LANE3000_IP)) &&
                            (!tempName.equals(VP300_USB) && !tempName.equals(LANE3000_IP))) {
                        TextView nodt = (TextView) findViewById(nameOfDeviceText);
                        nodt.setText("Connecting to Device...");
                        TextView transMessageView = findViewById(R.id.transMessage);
                        transMessageView.setText("Connecting to Device...");
                        //run the establish bluetooth connection to get the initial connection to the bluetooth device.
                        //limited to one bluetooth device per instance of the DSIEMVAndroid control.
                        //run in a separate thread to not block the UI.
                        new Thread(new Runnable() {
                            public void run() {
                                dsiEMVAndroidinstance.getInstance(MainActivity.this).EstablishBluetoothConnection(mConnectedDevice);
                            }
                        }).start();

                    } else if (!tempName.equals(VP300_USB) && !tempName.equals(LANE3000_IP)) {
                        mConnectedDevice = tempName;
                        TextView nodt = (TextView) findViewById(nameOfDeviceText);
                        nodt.setText("Connecting to Device...");
                        TextView transMessageView = findViewById(R.id.transMessage);
                        transMessageView.setText("Connecting to Device...");
                        new Thread(new Runnable() {
                            public void run() {
                                dsiEMVAndroidinstance.getInstance(MainActivity.this).Disconnect();
                                dsiEMVAndroidinstance.getInstance(MainActivity.this).EstablishBluetoothConnection(mConnectedDevice);
                            }
                        }).start();
                    } else {
                        //A usb or IP based device needs no initial connection method, removing any previous connections here.
                        mConnectedDevice = tempName;
                        TextView nodt = (TextView) findViewById(nameOfDeviceText);
                        nodt.setText(mConnectedDevice);
                        new Thread(new Runnable() {
                            public void run() {
                                dsiEMVAndroidinstance.getInstance(MainActivity.this).Disconnect();
                            }
                        }).start();
                    }
                }
            }
        };
        //Alert dialog for selecting a device
        mDeviceList[0] = VP300_USB;
        mDeviceList[1] = LANE3000_IP;
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Choose a Device" + System.lineSeparator() + "Searching...");
        builder.setItems(mDeviceList, mDeviceSelection);
        mBTdialog = builder.create();

        //button click listener for selecting device, brings up alert dialog
        Button btn = (Button) findViewById(selectDevice);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //does a local search for  devices in discovery mode
                searchForBt();
                mBTdialog.show();
            }
        });

        Button salebtn = (Button) findViewById(saleButton);
        salebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView transMessageView = findViewById(R.id.transMessage);
                transMessageView.setText("Starting Sale");
                TextView transactionresponseText = findViewById(R.id.transResposne);
                transactionresponseText.setText("Running Sale");
                TextView merchIDtv = (TextView) findViewById(merchantIDText);
                final String merchID = merchIDtv.getText().toString();
                TextView amounttv = (TextView) findViewById(amountText);
                final String amount = amounttv.getText().toString();
                TextView PainPadIptv = (TextView) findViewById(IPPadtext);
                final String padIP = PainPadIptv.getText().toString();
                TextView PadPorttexttv = (TextView) findViewById(PadPorttext);
                final String padPort = PadPorttexttv.getText().toString();
                new Thread(new Runnable() {
                    public void run() {
                        //generates xml for running a sale
                        String xmlRequest = setupSale(amount, merchID, padIP, padPort);
                        //runs the sale to the connected device, this does not have to be a singleton.
                        // It was used as a singleton here to support transactions through the local listener server.
                        dsiEMVAndroidinstance.getInstance(MainActivity.this).ProcessTransaction(xmlRequest);
                    }

                }).start();
            }
        });

        Button returnbtn = (Button) findViewById(returnButton);
        returnbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView transMessageView = findViewById(R.id.transMessage);
                transMessageView.setText("Starting Return");
                TextView transactionresponseText = findViewById(R.id.transResposne);
                transactionresponseText.setText("Running Return");
                TextView merchIDtv = (TextView) findViewById(merchantIDText);
                final String merchID = merchIDtv.getText().toString();
                TextView amounttv = (TextView) findViewById(amountText);
                final String amount = amounttv.getText().toString();
                TextView PainPadIptv = (TextView) findViewById(IPPadtext);
                final String padIP = PainPadIptv.getText().toString();
                TextView PadPorttexttv = (TextView) findViewById(PadPorttext);
                final String padPort = PadPorttexttv.getText().toString();
                new Thread(new Runnable() {
                    public void run() {
                        //generates xml for running a sale
                        String xmlRequest = setupReturn(amount, merchID, padIP, padPort);
                        //runs the sale to the connected device
                        dsiEMVAndroidinstance.getInstance(MainActivity.this).ProcessTransaction(xmlRequest);

                    }

                }).start();
            }
        });
        Button cancelbtn = (Button) findViewById(cancelButton);
        cancelbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView transMessageView = findViewById(R.id.transMessage);
                transMessageView.setText("Canceled Transaction");
                TextView transactionresponseText = findViewById(R.id.transResposne);
                transactionresponseText.setText("Canceled");
                new Thread(new Runnable() {
                    public void run() {
                        //cancels any active transaction
                        dsiEMVAndroidinstance.getInstance(MainActivity.this).CancelRequest();
                    }

                }).start();
            }
        });

        Button gdibtn = (Button) findViewById(getDevicesInfoButton);
        gdibtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView transMessageView = findViewById(R.id.transMessage);
                transMessageView.setText("Get Device Info");
                TextView transactionresponseText = findViewById(R.id.transResposne);

                //gets device information
                String response = dsiEMVAndroidinstance.getInstance(MainActivity.this).GetDevicesInfo();
                transactionresponseText.setText(response);

            }
        });

        Button emvbtn = (Button) findViewById(emvParamDownloadButton);
        emvbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView transMessageView = findViewById(R.id.transMessage);
                transMessageView.setText("EMV Param Download");
                TextView transactionresponseText = findViewById(R.id.transResposne);
                transactionresponseText.setText("");
                TextView merchIDtv = (TextView) findViewById(merchantIDText);
                final String merchID = merchIDtv.getText().toString();
                TextView PainPadIptv = (TextView) findViewById(IPPadtext);
                final String padIP = PainPadIptv.getText().toString();
                TextView PadPorttexttv = (TextView) findViewById(PadPorttext);
                final String padPort = PadPorttexttv.getText().toString();
                new Thread(new Runnable() {
                    public void run() {

                        //generates xml for running a EMVParamDownload
                        String xmlRequest = setupParamDownload(merchID, padIP, padPort);
                        //runs the sale to the connected device
                        dsiEMVAndroidinstance.getInstance(MainActivity.this).ProcessTransaction(xmlRequest);

                    }

                }).start();

            }
        });

        //adding message listener for the VP3300, since the device has no screen the control sends messages back to the UI for card removal, etc.
        dsiEMVAndroidinstance.getInstance(MainActivity.this).AddDisplayMessageListener(new DisplayMessageListener() {
            @Override
            public void OnDisplayMessageChanged(final String message) {
                //run on ui thread to set messages as they change form the control
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView transMessageView = findViewById(R.id.transMessage);
                        //get the newest message and set the text in the UI.
                        transMessageView.setText(message);
                    }
                });
            }
        });

        dsiEMVAndroidinstance.getInstance(MainActivity.this).AddEstablishBluetoothConnectionResponseListener(new EstablishBluetoothConnectionResponseListener() {
            @Override
            public void OnEstablishBluetoothConnectionResponseChanged(final String response) {
                //run on ui thread to tell user connection was successful
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView nodt = (TextView) findViewById(nameOfDeviceText);
                        TextView transResponseView = findViewById(R.id.transResposne);
                        TextView transMessageView = findViewById(R.id.transMessage);
                        transResponseView.setText(response);
                        if (response.contains("Success")) {
                            nodt.setText("Connected: " + mConnectedDevice);
                            transMessageView.setText("Connected to " + mConnectedDevice);
                        } else {
                            nodt.setText("Could not connect to device");
                        }
                    }
                });
            }
        });

        dsiEMVAndroidinstance.getInstance(MainActivity.this).AddBluetoothConnectionListener(new BluetoothConnectionListener() {
            @Override
            public void OnBluetoothConnectionListenerChanged(final boolean isConnected) {
                //run on ui thread to tell user connection was successful
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView nodt = (TextView) findViewById(nameOfDeviceText);
                        if (isConnected) {
                            nodt.setText("Connected: " + mConnectedDevice);
                        } else {
                            nodt.setText("Disconnected: " + mConnectedDevice);
                        }
                    }
                });
            }
        });

        //adding a response listener, since the processing the transaction could happen asynchronously we added support for a response callback.
        // This call back will return the response from the active "Process Transaction" call. In this demo app it is just displayed in the UI,
        // however normally it would be serialized into an object or parsed for receipt printing and persisted to an integrators transaction database.
        dsiEMVAndroidinstance.getInstance(MainActivity.this).AddProcessTransactionResponseListener(new ProcessTransactionResponseListener() {
            @Override
            public void OnProcessTransactionResponseChanged(final String response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView transactionresponseText = findViewById(R.id.transResposne);
                        transactionresponseText.setText(response);
                    }
                });
            }
        });

        //get the IP of the Android Device
        String ipOfPhone = getIPAddress(true);
        TextView ipView = findViewById(R.id.ipText);
        ipView.setText("This Device IP: " + ipOfPhone);


    }

    private String setupSale(String amount, String merchID, String padIP, String padPort) {
        Amount amt = new Amount(amount);
        Transaction newSale;
        if (mConnectedDevice.equals(LANE3000_IP)) {
            newSale = new Transaction(merchID,
                    "DSIEMVAndroind_Demo",
                    "EMVUSClient:1.27",
                    "EMVSale",
                    "EMV_LANE3000_DATACAP_E2E",
                    "10",
                    amt,
                    "0010010010",
                    "CERT",
                    "RecordNumberRequested",
                    "1",
                    padIP,
                    padPort);
        } else if (mConnectedDevice.equals(VP300_USB)) {
            newSale = new Transaction(merchID,
                    "DSIEMVAndroind_Demo",
                    "EMVUSClient:1.27",
                    "EMVSale",
                    "EMV_VP3300_DATACAP",
                    "10",
                    amt,
                    "0010010010",
                    "CERT",
                    "RecordNumberRequested",
                    "1");
        } else{
            newSale = new Transaction(merchID,
                    "DSIEMVAndroind_Demo",
                    "EMVUSClient:1.27",
                    "EMVSale",
                    "EMV_VP3300_DATACAP",
                    "10",
                    amt,
                    "0010010010",
                    mConnectedDevice,
                    "CERT",
                    "RecordNumberRequested",
                    "1");
        }
        TStream tStream = new TStream(newSale);

        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        Serializer serializer = new Persister();
        try {
            serializer.write(tStream, bao);
        } catch (Exception ex) {
            //serialization exception
        }
        return bao.toString();
    }

    private String setupReturn(String amount, String merchID, String padIP, String padPort) {
        Amount amt = new Amount(amount);
        Transaction newReturn;
        if (mConnectedDevice.equals(LANE3000_IP)) {
            newReturn = new Transaction(merchID,
                    "DSIEMVAndroind_Demo",
                    "EMVUSClient:1.27",
                    "EMVReturn",
                    "EMV_LANE3000_DATACAP_E2E",
                    "100",
                    amt,
                    "0010010010",
                    "CERT",
                    "RecordNumberRequested",
                    "23",
                    padIP,
                    padPort);
        } else if (mConnectedDevice.equals(VP300_USB)) {
            //USB connected devices need no "BluetoothDeviceName"
            newReturn = new Transaction(merchID,
                    "DSIEMVAndroind_Demo",
                    "EMVUSClient:1.27",
                    "EMVReturn",
                    "EMV_VP3300_DATACAP",
                    "100",
                    amt,
                    "0010010010",
                    "CERT",
                    "RecordNumberRequested",
                    "23");
        } else {
            newReturn = new Transaction(merchID,
                    "DSIEMVAndroind_Demo",
                    "EMVUSClient:1.27",
                    "EMVReturn",
                    "EMV_VP3300_DATACAP",
                    "100",
                    amt,
                    "0010010010",
                    mConnectedDevice,
                    "CERT",
                    "RecordNumberRequested",
                    "23");
        }
        TStream tStream = new TStream(newReturn);

        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        Serializer serializer = new Persister();
        try {
            serializer.write(tStream, bao);
        } catch (Exception ex) {
            //serialization exception
        }
        return bao.toString();
    }

    private String setupParamDownload(String merchID, String padIP, String padPort) {
        Admin newParam;
        if (mConnectedDevice.equals(LANE3000_IP)) {
            newParam = new Admin(merchID,
                    "DSIEMVAndroind_Demo",
                    "EMVUSClient:1.27",
                    "EMVParamDownload",
                    "EMV_LANE3000_DATACAP_E2E",
                    "0010010010",
                    "CERT",
                    padIP,
                    padPort);
        } else if(mConnectedDevice.equals(VP300_USB)){
            //USB connected devices need no "BluetoothDeviceName"
            newParam = new Admin(merchID,
                    "DSIEMVAndroind_Demo",
                    "EMVUSClient:1.27",
                    "EMVParamDownload",
                    "EMV_VP3300_DATACAP",
                    "0010010010",
                    "CERT");
        } else {
            newParam = new Admin(merchID,
                    "DSIEMVAndroind_Demo",
                    "EMVUSClient:1.27",
                    "EMVParamDownload",
                    "EMV_VP3300_DATACAP",
                    "0010010010",
                    mConnectedDevice,
                    "CERT");
        }

        TStream tStream = new TStream(newParam);

        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        Serializer serializer = new Persister();
        try {
            serializer.write(tStream, bao);
        } catch (Exception ex) {
            //serialization exception
        }
        return bao.toString();
    }

    //code to look for bluetooth le devices. This can be used to show the user a list of available devices,
    // then pass a selected device name to the DSIEMVAndroid control to connect to it.
    private void searchForBt() {
        mNamePos = 2;
        List<ScanFilter> filters = new ArrayList<>();
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build();
        final BtleScanCallback mScanCallback = new BtleScanCallback();
        // Getting the Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBtAdapter != null) {
            final BluetoothLeScanner mBluetoothLeScanner = mBtAdapter.getBluetoothLeScanner();
            mBluetoothLeScanner.startScan(filters, settings, mScanCallback);

            final Runnable r = new Runnable() {
                public void run() {
                    Handler mHandler = new Handler();
                    mHandler.postDelayed(this, 60000);
                    mBluetoothLeScanner.stopScan(mScanCallback);
                }
            };
        } else {

        }
    }

    private class BtleScanCallback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            String devName = result.getDevice().getName();
            //add new device to list of available devices
            if (devName != null && !devName.equals("") && !Arrays.asList(mDeviceList).contains(devName)) {
                mDeviceList[mNamePos] = devName;
                ListView list = mBTdialog.getListView();
                ArrayAdapter adapter = (ArrayAdapter) list.getAdapter();
                //update UI that there is an additional device in the list
                adapter.notifyDataSetChanged();
                mNamePos++;
                if (mNamePos > 7) {
                    mNamePos = 2;
                }
            }
        }
    }

    //sample code to check for permissions that are needed for bluetooth communication.
    private void hasPermissions() {

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Functionality limited");
            builder.setMessage("This device does not support bluetooth. bluetooth devices cannot be communicated with");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {
                }

            });
            builder.show();
        } else if (!mBluetoothAdapter.isEnabled()) {
            requestBluetoothEnable();
        } else {
            // Bluetooth is enabled
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= 29) {
                    if (this.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        if (this.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                            builder.setTitle("This app needs background location access");
                            builder.setMessage("Please grant location access so this app can operate bluetooth devices.");
                            builder.setPositiveButton(android.R.string.ok, null);
                            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                                @TargetApi(23)
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                            PERMISSION_REQUEST_BACKGROUND_LOCATION);
                                }

                            });
                            builder.show();
                        } else {
                            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                            builder.setTitle("Functionality limited");
                            builder.setMessage("Since background location access has not been granted, this app will not be able to can operate bluetooth devices.  Please go to Settings -> Applications -> Permissions and grant background location access to this app.");
                            builder.setPositiveButton(android.R.string.ok, null);
                            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                }

                            });
                            builder.show();
                        }

                    }
                }
            } else {
                if (!this.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                            PERMISSION_REQUEST_FINE_LOCATION);
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons.  Please go to Settings -> Applications -> Permissions and grant location access to this app.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }

            }
        }
    }

    private void requestBluetoothEnable() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':') < 0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim < 0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
        } // for now eat exceptions
        return "";
    }
}
