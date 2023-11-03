package com.example.dsiemvandroiddemo;

import com.datacap.android.BluetoothConnectionListener;
import com.datacap.android.EstablishBluetoothConnectionResponseListener;
import com.datacap.android.DisplayMessageListener;
import com.datacap.android.ProcessTransactionResponseListener;

import android.Manifest;
import android.app.AlertDialog;
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
import android.widget.RadioButton;
import android.widget.TextView;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;


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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private final Logger LOGGER = Logger.getLogger("dsiEMVAndroidDemo");
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_BACKGROUND_LOCATION = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private static final String VP3300_USB = "IDTECH-VP3300-USB";
    private static final String VP3300_RS232 = "IDTECH-VP3300-RS232";
    private static final String LANE3000_IP = "INGENICO_LANE_3000_IP";
    private static final String PAX_ANDROID_IP = "PAX_ANDROID_IP";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private BluetoothAdapter mBtAdapter;
    private DialogInterface.OnClickListener mDeviceSelection;
    private String mConnectedDevice = "";
    private int mNamePos = 1;
    private String[] mDeviceList = {"", "", "", "", "", "", "", "", ""};
    private AlertDialog mBTdialog;
    private String mOperationMode = "CERT";

    private static final Map<String, String> padMap;

    static {
        padMap = new HashMap<String, String>();
        padMap.put("A77", "EMV_A77_DATACAP_E2E");
        padMap.put("A60", "EMV_A60_DATACAP_E2E");
        padMap.put("A920Pro", "EMV_A920PRO_DATACAP_E2E");
        padMap.put("A920", "EMV_A920PRO_DATACAP_E2E");
        padMap.put("Aries6", "EMV_ARIES6_DATACAP_E2E");
        padMap.put("Aries8", "EMV_ARIES8_DATACAP_E2E");
        padMap.put("A35", "EMV_A35_DATACAP_E2E");
        padMap.put("A30", "EMV_A30_DATACAP_E2E");
        padMap.put("IM30", "EMV_A920PRO_DATACAP_E2E");
    }

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
                    //Skipping over all of the non bluetooth devices
                    boolean isBluetoothName = !tempName.equals(VP3300_USB) && !tempName.equals(LANE3000_IP) && !tempName.equals(VP3300_RS232) && !tempName.equals(PAX_ANDROID_IP);
                    if (mConnectedDevice.equals(tempName) &&
                            (!mConnectedDevice.equals(VP3300_USB) &&
                                    !mConnectedDevice.equals(LANE3000_IP) &&
                                    !mConnectedDevice.equals(PAX_ANDROID_IP) &&
                                    !mConnectedDevice.equals(VP3300_RS232)) &&
                            isBluetoothName) {
                        TextView nodt = (TextView) findViewById(nameOfDeviceText);
                        nodt.setText("Connecting to Device...");
                        TextView transMessageView = findViewById(R.id.transMessage);
                        transMessageView.setText("Connecting to Device...");
                        //run the establish bluetooth connection to get the initial connection to the bluetooth device.
                        //limited to one bluetooth device per instance of the DSIEMVAndroid control.
                        //run in a separate thread to not block the UI.
                        new Thread(new Runnable() {
                            public void run() {
                                dsiEMVAndroidinstance.getInstance(MainActivity.this).Disconnect();
                                dsiEMVAndroidinstance.getInstance(MainActivity.this).EstablishBluetoothConnection(mConnectedDevice);
                            }
                        }).start();

                    } else if (isBluetoothName) {
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
        mDeviceList[0] = VP3300_USB;
        mDeviceList[1] = LANE3000_IP;
        mDeviceList[2] = PAX_ANDROID_IP;
        mDeviceList[3] = VP3300_RS232;
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
                            transMessageView.setText("Could not connect to device");
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
                        if (mConnectedDevice.equals(PAX_ANDROID_IP)) {
                            bringtofront();
                        }
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

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch (view.getId()) {
            case R.id.radioButtonCert:
                if (checked)
                    mOperationMode = "CERT";
                break;
            case R.id.radioButtonProd:
                if (checked)
                    mOperationMode = "PROD";
                break;
        }
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
                    mOperationMode,
                    "RecordNumberRequested",
                    "1",
                    padIP,
                    padPort);
        } else if (mConnectedDevice.equals(PAX_ANDROID_IP)) {
            newSale = new Transaction(merchID,
                    "DSIEMVAndroind_Demo",
                    "EMVUSClient:1.27",
                    "EMVSale",
                    determineSecureDevice(),
                    "10",
                    amt,
                    "0010010010",
                    mOperationMode,
                    "RecordNumberRequested",
                    "1",
                    padIP,
                    "1235");

        } else if (mConnectedDevice.equals(VP3300_USB) || mConnectedDevice.equals(VP3300_RS232)) {
            String secureDevice = "EMV_VP3300_DATACAP";
            //RS232 takes a different secure device name
            if (mConnectedDevice.equals(VP3300_RS232)) {
                secureDevice = "EMV_VP3300_DATACAP_RS232";
            }
            newSale = new Transaction(merchID,
                    "DSIEMVAndroind_Demo",
                    "EMVUSClient:1.27",
                    "EMVSale",
                    secureDevice,
                    "10",
                    amt,
                    "0010010010",
                    mOperationMode,
                    "RecordNumberRequested",
                    "1");
        } else {
            newSale = new Transaction(merchID,
                    "DSIEMVAndroind_Demo",
                    "EMVUSClient:1.27",
                    "EMVSale",
                    "EMV_VP3300_DATACAP",
                    "10",
                    amt,
                    "0010010010",
                    mConnectedDevice,
                    mOperationMode,
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
                    mOperationMode,
                    "RecordNumberRequested",
                    "23",
                    padIP,
                    padPort);
        } else if (mConnectedDevice.equals(PAX_ANDROID_IP)) {
            newReturn = new Transaction(merchID,
                    "DSIEMVAndroind_Demo",
                    "EMVUSClient:1.27",
                    "EMVReturn",
                    determineSecureDevice(),
                    "10",
                    amt,
                    "0010010010",
                    mOperationMode,
                    "RecordNumberRequested",
                    "1",
                    padIP,
                    "1235");

        } else if (mConnectedDevice.equals(VP3300_USB) || mConnectedDevice.equals(VP3300_RS232)) {
            //USB connected devices need no "BluetoothDeviceName"
            String secureDevice = "EMV_VP3300_DATACAP";
            //RS232 takes a different secure device name
            if (mConnectedDevice.equals(VP3300_RS232)) {
                secureDevice = "EMV_VP3300_DATACAP_RS232";
            }
            newReturn = new Transaction(merchID,
                    "DSIEMVAndroind_Demo",
                    "EMVUSClient:1.27",
                    "EMVReturn",
                    secureDevice,
                    "100",
                    amt,
                    "0010010010",
                    mOperationMode,
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
                    mOperationMode,
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
                    mOperationMode,
                    padIP,
                    padPort);
        } else if (mConnectedDevice.equals(PAX_ANDROID_IP)) {
            newParam = new Admin(merchID,
                    "DSIEMVAndroind_Demo",
                    "EMVUSClient:1.27",
                    "EMVParamDownload",
                    determineSecureDevice(),
                    "0010010010",
                    mOperationMode,
                    padIP,
                    "1235");
        } else if (mConnectedDevice.equals(VP3300_USB) || mConnectedDevice.equals(VP3300_RS232)) {
            //USB connected devices need no "BluetoothDeviceName"
            String secureDevice = "EMV_VP3300_DATACAP";
            //RS232 takes a different secure device name
            if (mConnectedDevice.equals(VP3300_RS232)) {
                secureDevice = "EMV_VP3300_DATACAP_RS232";
            }
            newParam = new Admin(merchID,
                    "DSIEMVAndroind_Demo",
                    "EMVUSClient:1.27",
                    "EMVParamDownload",
                    secureDevice,
                    "0010010010",
                    mOperationMode);
        } else {
            newParam = new Admin(merchID,
                    "DSIEMVAndroind_Demo",
                    "EMVUSClient:1.27",
                    "EMVParamDownload",
                    "EMV_VP3300_DATACAP",
                    "0010010010",
                    mConnectedDevice,
                    mOperationMode);
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
        mNamePos = 4;
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
                if (mNamePos > 8) {
                    mNamePos = 4;
                }
            }
        }
    }

    //sample code to check for permissions that are needed for bluetooth communication.
    private void hasPermissions()
    {

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null)
        {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Functionality limited");
            builder.setMessage("This device does not support bluetooth. bluetooth devices cannot be communicated with");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(dialog -> { });
            builder.show();
        }
        else if (!mBluetoothAdapter.isEnabled())
        {
            requestBluetoothEnable();
        }
        else
        {
            //Bluetooth enabled
        }

        ActivityResultLauncher<String[]> permissionRequest =
                this.registerForActivityResult(new ActivityResultContracts
                                .RequestMultiplePermissions(), result ->
                        {
                            Boolean fineLocationGranted;
                            Boolean coarseLocationGranted;
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                fineLocationGranted = result.getOrDefault(
                                        Manifest.permission.ACCESS_FINE_LOCATION, false);
                                coarseLocationGranted = result.getOrDefault(
                                        Manifest.permission.ACCESS_COARSE_LOCATION,false);
                            }
                            else
                            {
                                fineLocationGranted = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                                coarseLocationGranted = result.get(Manifest.permission.ACCESS_COARSE_LOCATION);
                            }

                            if (Build.VERSION.SDK_INT > 29)
                            {
                                Boolean backgroundLocationGranted = result.getOrDefault(
                                        Manifest.permission.ACCESS_BACKGROUND_LOCATION, false);
                                if (!Boolean.TRUE.equals(backgroundLocationGranted))
                                {
                                    if (this.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)
                                    {
                                        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                                        builder.setTitle("This app needs background location access");
                                        builder.setMessage("Please grant location access so this app can operate bluetooth devices.");
                                        builder.setPositiveButton(android.R.string.ok, null);
                                        builder.setOnDismissListener((dialog) ->
                                                {
                                                    requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                                            PERMISSION_REQUEST_BACKGROUND_LOCATION);
                                                }
                                        );
                                        builder.show();
                                    }
                                }
                            }
                            if (Build.VERSION.SDK_INT >= 31)
                            {
                                Boolean bluetoothScanGranted = result.getOrDefault(Manifest.permission.BLUETOOTH_SCAN, false);
                                Boolean bluetoothConnectGranted = result.getOrDefault(Manifest.permission.BLUETOOTH_CONNECT, false);
                                if (!Boolean.TRUE.equals(bluetoothScanGranted) | !Boolean.TRUE.equals(bluetoothConnectGranted))
                                {
                                    buildDialogFor("Bluetooth Scanning required", "Enable Bluetooth access for this application to work properly.");
                                }
                            }
                            else
                            {
                                Boolean bluetoothGranted;
                                Boolean bluetoothAdminGranted;
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                    bluetoothGranted = result.getOrDefault(Manifest.permission.BLUETOOTH, false);
                                    bluetoothAdminGranted = result.getOrDefault(Manifest.permission.BLUETOOTH_ADMIN, false);
                                }
                                else
                                {
                                    bluetoothGranted = result.get(Manifest.permission.BLUETOOTH);
                                    bluetoothAdminGranted = result.get(Manifest.permission.BLUETOOTH_ADMIN);
                                }
                                if (!Boolean.TRUE.equals(bluetoothGranted) | !Boolean.TRUE.equals(bluetoothAdminGranted))
                                {
                                    buildDialogFor("Bluetooth required", "Enable Bluetooth access for this application to work properly.");
                                }
                            }
                            if (!Boolean.TRUE.equals(fineLocationGranted) | !Boolean.TRUE.equals(coarseLocationGranted))
                            {
                                buildDialogFor("Functionality Limited", "Grant fine location access to discover beacons.");
                            }
                        }
                );

        // Before you perform the actual permission request, check whether your app
        // already has the permissions, and whether your app needs to show a permission
        // rationale dialog. For more details, see Request permissions. https://developer.android.com/training/permissions/requesting
        permissionRequest.launch(new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
        });
    }

    private AlertDialog buildDialogFor(String title, String message)
    {
        return new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .setOnDismissListener((dialog) -> { })
                .show();

    }

    private void requestBluetoothEnable() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result ->
            {
                // This callback can be used to provide additional logic
                LOGGER.info("RESULT CODE FOR INTENT " + result.getResultCode());
                // -1 (Activity.RESULT_OK) is 'Allow'
                // 0 (Activity.RESULT_CANCELED) is 'Deny'
                if (result.getResultCode() == 0)
                {
                    buildDialogFor("Bluetooth is Not Enabled", "In order to communicate with Bluetooth devices, please turn on Bluetooth.");
                }
            })
            .launch(enableBtIntent);
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

    private static String determineSecureDevice() {
        String deviceModel = android.os.Build.MODEL;
        String secureDevice = padMap.get(deviceModel);
        //if secure device cant be found, treat it like an A920 Pro
        if (secureDevice == null) {
            secureDevice = "EMV_A920PRO_DATACAP_E2E";
        }
        return secureDevice;
    }
    private void bringtofront()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(MainActivity.this.getApplicationContext(), MainActivity.this.getClass());
                // You need this if starting
                //  the activity from a service
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivityIfNeeded(intent, 0);
            }
        });
    }
}
