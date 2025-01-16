package com.example.dsiemvandroiddemo;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;


import static com.example.dsiemvandroiddemo.R.id.device_list_view;
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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

public class MainActivity extends AppCompatActivity
{

    private AtomicBoolean cardDataCollect = new AtomicBoolean(false);
    private final Logger LOGGER = Logger.getLogger("dsiEMVAndroidDemo");
    private static final int PERMISSION_REQUEST_BACKGROUND_LOCATION = 2;
    private static final String VP3300_USB = "IDTECH-VP3300-USB";
    private static final String VP3300_RS232 = "IDTECH-VP3300-RS232";
    private static final String VP3350_USB = "IDTECH-VP3350-USB";
    private static final String LANE3000_IP = "INGENICO_LANE_3000_IP";
    private static final String PAX_ANDROID_IP = "PAX_ANDROID_IP";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String mConnectedDevice = "";
    private List<String> mDeviceList = new ArrayList<>();
    String[] devicesArray = mDeviceList.toArray(new String[0]);
    private BluetoothAdapter bluetoothAdapter;
    private AlertDialog mBTdialog;
    private ArrayAdapter<String> listAdapter;
    private String mOperationMode = "CERT";
    private final SAFListener safListener = new SAFListener(getSupportFragmentManager());

    private static final Map<String, String> padMap;
    private static final int REQUEST_PERMISSIONS = 2;

    static
    {
        padMap = new HashMap<>();
        padMap.put("A77", "EMV_A77_DATACAP_E2E");
        padMap.put("A60", "EMV_A60_DATACAP_E2E");
        padMap.put("A920Pro", "EMV_A920PRO_DATACAP_E2E");
        padMap.put("A920", "EMV_A920PRO_DATACAP_E2E");
        padMap.put("Aries6", "EMV_ARIES6_DATACAP_E2E");
        padMap.put("Aries8", "EMV_ARIES8_DATACAP_E2E");
        padMap.put("A35", "EMV_A35_DATACAP_E2E");
        padMap.put("A30", "EMV_A30_DATACAP_E2E");
        padMap.put("IM30", "EMV_IM30_DATACAP_E2E");
        padMap.put("A920MAX", "EMV_A920PRO_DATACAP_E2E");
        padMap.put("A3700", "EMV_A3700_DATACAP_E2E");
        padMap.put("A800", "EMV_A800_DATACAP_E2E");
        padMap.put("A6650", "EMV_A6650_DATACAP_E2E");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewPager2 viewPager = findViewById(R.id.optionPager);
        CardPagerAdapter adapter = new CardPagerAdapter(this);
        viewPager.setAdapter(adapter);

        viewPager.setOffscreenPageLimit(adapter.getItemCount());

        int pageMargin = getResources().getDimensionPixelOffset(R.dimen.pageMargin);
        int offsetPx = getResources().getDimensionPixelOffset(R.dimen.offsetPx);

        viewPager.setPageTransformer((page, position) ->
        {
            int pageWidth = page.getWidth();
            float pageTranslationX = -offsetPx * position;
            if (position < -1)
            {
                page.setTranslationX(-pageWidth * position);
            }
            else if (position <= 1)
            {
                page.setTranslationX(pageTranslationX);
            }
            else
            {
                page.setTranslationX(0);
            }
        });

        viewPager.addItemDecoration(new HorizontalMarginItemDecoration(pageMargin));

        getSupportFragmentManager();
//        dsiEMVAndroidinstance.getInstance(this).SetSAFEventListener(safListener);
        //check for bluetooth and location permissions for bluetooth LE to work.
        // the user must agree to location sharing because locations is part of bluetooth LE spec.
        hasPermissions();
        try
        {
            //sets up local endpoint to be used with EMV US Test Client
            LocalListener li = new LocalListener(MainActivity.this);
        }
        catch (Exception ex)
        {
            //could not start the local server listener
        }

        //setup device dialog click action
        //Skipping over all of the non bluetooth devices
        //run the establish bluetooth connection to get the initial connection to the bluetooth device.
        //limited to one bluetooth device per instance of the DSIEMVAndroid control.
        //run in a separate thread to not block the UI.
        //A usb or IP based device needs no initial connection method, removing any previous connections here.
        DialogInterface.OnClickListener mDeviceSelection = (dialog, which) ->
        {
            ListView lv = ((AlertDialog) dialog).getListView();
            TextView v = (TextView) lv.getChildAt(which);
            String tempName = v.getText().toString();
            if (!tempName.isEmpty())
            {
                //Skipping over all of the non bluetooth devices
                boolean isBluetoothName = !tempName.equals(VP3300_USB)
                        && !tempName.equals(VP3300_RS232)
                        && !tempName.equals(VP3350_USB)
                        && !tempName.equals(LANE3000_IP)
                        && !tempName.equals(PAX_ANDROID_IP);
                if (mConnectedDevice.equals(tempName) &&
                        (!mConnectedDevice.equals(VP3300_USB)
                                && !mConnectedDevice.equals(VP3300_RS232)
                                && !mConnectedDevice.equals(VP3350_USB)
                                && !mConnectedDevice.equals(LANE3000_IP)
                                && !mConnectedDevice.equals(PAX_ANDROID_IP))
                        && isBluetoothName)
                {
                    TextView nodt = findViewById(nameOfDeviceText);
                    nodt.setText(R.string.connecting_to_device);
                    TextView transMessageView = findViewById(R.id.transMessage);
                    transMessageView.setText(R.string.connecting_to_device);
                    //run the establish bluetooth connection to get the initial connection to the bluetooth device.
                    //limited to one bluetooth device per instance of the DSIEMVAndroid control.
                    //run in a separate thread to not block the UI.
                    executor.submit(() ->
                    {
                        dsiEMVAndroidinstance.getInstance(MainActivity.this).Disconnect();
                        dsiEMVAndroidinstance.getInstance(MainActivity.this).EstablishBluetoothConnection(mConnectedDevice);
                    });

                }
                else if (isBluetoothName)
                {
                    mConnectedDevice = tempName;
                    TextView nodt = findViewById(nameOfDeviceText);
                    nodt.setText(R.string.connecting_to_device);
                    TextView transMessageView = findViewById(R.id.transMessage);
                    transMessageView.setText(R.string.connecting_to_device);
                    executor.submit(() ->
                    {
                        dsiEMVAndroidinstance.getInstance(MainActivity.this).Disconnect();
                        dsiEMVAndroidinstance.getInstance(MainActivity.this).EstablishBluetoothConnection(mConnectedDevice);
                    });
                }
                else
                {
                    //A usb or IP based device needs no initial connection method, removing any previous connections here.
                    mConnectedDevice = tempName;
                    TextView nodt = findViewById(nameOfDeviceText);
                    nodt.setText(mConnectedDevice);
                    new Thread(() -> dsiEMVAndroidinstance.getInstance(MainActivity.this).Disconnect()).start();
                }
            }
        };
        //Alert dialog for selecting a device
        mDeviceList.add(VP3300_USB);
        mDeviceList.add(VP3300_RS232);
        mDeviceList.add(VP3350_USB);
        mDeviceList.add(LANE3000_IP);
        mDeviceList.add(PAX_ANDROID_IP);

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.bt_scroll_view, null);
        ListView listView = dialogView.findViewById(R.id.device_list_view);
        // Create an ArrayAdapter
        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mDeviceList);
        listView.setAdapter(listAdapter);

        // Handle item clicks
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mConnectedDevice = mDeviceList.get(position);
                // Do something with the selected device
                mBTdialog.dismiss(); // Close the dialog if desired
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Choose a Device" + System.lineSeparator() + "Searching...");
        builder.setView(dialogView);
        mBTdialog = builder.create();

        viewPager.post(() ->
        {
            // Setting defaults for params
            //((EditText) findViewById(R.id.merchantIDText)).setText("CROSSCHAL1GD");
            //((EditText) findViewById(R.id.IPPadtext)).setText("192.168.0.99");
            //((EditText) findViewById(R.id.PadPorttext)).setText("1235");
            ((EditText) findViewById(R.id.amountText)).setText("1.00");
            ((RadioButton) findViewById(R.id.radioButtonCert)).toggle();
        });

        //button click listener for selecting device, brings up alert dialog
        viewPager.post(() ->
        {
            findViewById(selectDevice).setOnClickListener((v) ->
            {
                //does a local search for  devices in discovery mode
                searchForBt();
                mBTdialog.show();
            });
        });

        viewPager.post(() ->
        {
            findViewById(saleButton).setOnClickListener((v) ->
            {
                TextView transMessageView = findViewById(R.id.transMessage);
                transMessageView.setText(R.string.starting_sale);
                TextView transactionresponseText = findViewById(R.id.transResposne);
                transactionresponseText.setText(R.string.running_sale);
                TextView merchIDtv = findViewById(merchantIDText);
                final String merchID = merchIDtv.getText().toString().strip();
                TextView amounttv = findViewById(amountText);
                final String amount = amounttv.getText().toString().strip();
                TextView PainPadIptv = findViewById(IPPadtext);
                final String padIP = PainPadIptv.getText().toString().strip();
                TextView PadPorttexttv = findViewById(PadPorttext);
                final String padPort = PadPorttexttv.getText().toString().strip();
                executor.submit(() ->
                {
                    //generates xml for running a sale
                    String xmlRequest = setupSale(amount, merchID, padIP, padPort);
                    LOGGER.info(xmlRequest);
                    //runs the sale to the connected device, this does not have to be a singleton.
                    // It was used as a singleton here to support transactions through the local listener server.
                    dsiEMVAndroidinstance.getInstance(MainActivity.this).ProcessTransaction(xmlRequest);
                });
            });
        });

        viewPager.post(() ->
        {
            findViewById(returnButton).setOnClickListener((v) ->
            {
                TextView transMessageView = findViewById(R.id.transMessage);
                transMessageView.setText(R.string.starting_return);
                TextView transactionresponseText = findViewById(R.id.transResposne);
                transactionresponseText.setText(R.string.running_return);
                TextView merchIDtv = findViewById(merchantIDText);
                final String merchID = merchIDtv.getText().toString();
                TextView amounttv = findViewById(amountText);
                final String amount = amounttv.getText().toString();
                TextView PainPadIptv = findViewById(IPPadtext);
                final String padIP = PainPadIptv.getText().toString();
                TextView PadPorttexttv = findViewById(PadPorttext);
                final String padPort = PadPorttexttv.getText().toString();
                executor.submit(() ->
                {
                    //generates xml for running a return
                    String xmlRequest = setupReturn(amount, merchID, padIP, padPort);
                    LOGGER.info(xmlRequest);
                    //runs the sale to the connected device
                    dsiEMVAndroidinstance.getInstance(MainActivity.this).ProcessTransaction(xmlRequest);

                });
            });
        });

        viewPager.post(() ->
        {
            findViewById(cancelButton).setOnClickListener((v) ->
            {
                TextView transMessageView = findViewById(R.id.transMessage);
                transMessageView.setText(R.string.canceled_transaction);
                executor.submit(() ->
                {
                    //cancels any active transaction
                    dsiEMVAndroidinstance.getInstance(MainActivity.this).CancelRequest();
                });
            });
        });

        viewPager.post(() ->
        {
            findViewById(getDevicesInfoButton).setOnClickListener((v) ->
            {
                TextView transMessageView = findViewById(R.id.transMessage);
                transMessageView.setText(R.string.get_device_info);
                TextView transactionresponseText = findViewById(R.id.transResposne);

                //gets device information
                String response = dsiEMVAndroidinstance.getInstance(MainActivity.this).GetDevicesInfo();
                transactionresponseText.setText(response);
            });
        });

        viewPager.post(() ->
        {
            findViewById(emvParamDownloadButton).setOnClickListener((v) ->
            {
                TextView transMessageView = findViewById(R.id.transMessage);
                transMessageView.setText(R.string.emv_param_download);
                TextView transactionresponseText = findViewById(R.id.transResposne);
                transactionresponseText.setText("");
                TextView merchIDtv = findViewById(merchantIDText);
                final String merchID = merchIDtv.getText().toString();
                TextView PainPadIptv = findViewById(IPPadtext);
                final String padIP = PainPadIptv.getText().toString();
                TextView PadPorttexttv = findViewById(PadPorttext);
                final String padPort = PadPorttexttv.getText().toString();
                executor.submit(() ->
                {

                    //generates xml for running a EMVParamDownload
                    String xmlRequest = setupParamDownload(merchID, padIP, padPort);
                    LOGGER.info(xmlRequest);
                    //runs the sale to the connected device
                    dsiEMVAndroidinstance.getInstance(MainActivity.this).ProcessTransaction(xmlRequest);

                });
            });
        });

        //adding message listener for the VP3300, since the device has no screen the control sends messages back to the UI for card removal, etc.
        dsiEMVAndroidinstance.getInstance(MainActivity.this).AddDisplayMessageListener(message ->
        {
            //run on ui thread to set messages as they change form the control
            // use either a Handler to MainThread or runOnUiThread call.
            handler.post(() ->
            {
                TextView transMessageView = findViewById(R.id.transMessage);
                //get the newest message and set the text in the UI.
                transMessageView.setText(message);
                LOGGER.info("DisplayMessage: " + message);
            });
        });

        dsiEMVAndroidinstance.getInstance(MainActivity.this).AddEstablishBluetoothConnectionResponseListener(response ->
        {
            //run on ui thread to tell user connection was successful
            // use either a Handler to MainThread or runOnUiThread call.
            handler.post(() ->
            {
                TextView nodt = findViewById(nameOfDeviceText);
                TextView transResponseView = findViewById(R.id.transResposne);
                TextView transMessageView = findViewById(R.id.transMessage);
                transResponseView.setText(response);
                if (response.contains("Success"))
                {
                    nodt.setText(String.format("%s%s", getString(R.string.connected), mConnectedDevice));
                    transMessageView.setText(String.format("%s%s", getString(R.string.connected_to), mConnectedDevice));
                }
                else
                {
                    nodt.setText(R.string.could_not_connect_to_device);
                    transMessageView.setText(R.string.could_not_connect_to_device);
                }
            });
        });

        dsiEMVAndroidinstance.getInstance(MainActivity.this).AddBluetoothConnectionListener(isConnected ->
        {
            //run on ui thread to tell user connection was successful
            // use either a Handler to MainThread or runOnUiThread call.
            handler.post(() ->
            {
                TextView nodt = findViewById(nameOfDeviceText);
                if (isConnected)
                {
                    nodt.setText(String.format("%s%s", getString(R.string.connected), mConnectedDevice));
                }
                else
                {
                    nodt.setText(String.format("%s%s", getString(R.string.disconnected), mConnectedDevice));
                }
            });
        });

        //adding a response listener, since the processing the transaction could happen asynchronously we added support for a response callback.
        // This call back will return the response from the active "Process Transaction" call. In this demo app it is just displayed in the UI,
        // however normally it would be serialized into an object or parsed for receipt printing and persisted to an integrators transaction database.
        dsiEMVAndroidinstance.getInstance(MainActivity.this).AddProcessTransactionResponseListener(response -> handler.post(() ->
        {
            if (mConnectedDevice.equals(PAX_ANDROID_IP))
            {
                bringToFront();
            }
            TextView transactionresponseText = findViewById(R.id.transResposne);
            transactionresponseText.setText(response);
            LOGGER.info(response);
        }));

        dsiEMVAndroidinstance.getInstance(MainActivity.this).AddCollectCardDataResponseListener(response -> handler.post(() ->
        {
            if (mConnectedDevice.equals(PAX_ANDROID_IP))
            {
                bringToFront();
            }
            TextView transactionresponseText = findViewById(R.id.transResposne);
            transactionresponseText.setText(response);
        }));

        //get the IP of the Android Device
        String ipOfPhone = getIPAddress(true);
        TextView ipView = findViewById(R.id.ipText);
        ipView.setText(String.format("%s%s", getString(R.string.ip_address_of_this_device), ipOfPhone));

    }

    public void onRadioButtonClicked(View view)
    {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch (view.getId())
        {
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

    private String setupSale(String amount, String merchID, String padIP, String padPort)
    {
        Amount amt = new Amount(amount);

        Transaction newSale = new Transaction(
                merchID,
                "DSIEMVAndroidDemo:1.00",
                "EMVSale",
                "1",
                amt,
                "0010010010",
                mOperationMode,
                "RecordNumberRequested",
                "1"
        );

        switch (mConnectedDevice)
        {
            case LANE3000_IP:
                newSale.setSecureDevice("EMV_LANE3000_DATACAP_E2E");
                newSale.setPinPadIpAddress(padIP);
                newSale.setPinPadIpPort(padPort);
                break;
            case PAX_ANDROID_IP:
                newSale.setSecureDevice(determineSecureDevice());
                newSale.setPinPadIpAddress(padIP);
                newSale.setPinPadIpPort("1235");
                break;
            case VP3300_USB:
                newSale.setSecureDevice("EMV_VP3300_DATACAP");
                break;
            case VP3300_RS232:
                newSale.setSecureDevice("EMV_VP3300_DATACAP_RS232");
                break;
            case VP3350_USB:
                newSale.setSecureDevice("EMV_VP3350_DATACAP");
                break;
            default:
                // Must be a bluetooth device
                newSale.setBluetoothDeviceName(mConnectedDevice);
                newSale.setSecureDevice(determineSecureDeviceByBTName(mConnectedDevice));
                break;
        }
        TStream tStream = new TStream(newSale);

        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        Serializer serializer = new Persister();
        try
        {
            serializer.write(tStream, bao);
        }
        catch (Exception ex)
        {
            //serialization exception
        }
        return bao.toString();
    }

    private String setupReturn(String amount, String merchID, String padIP, String padPort)
    {
        Amount amt = new Amount(amount);

        Transaction newReturn = new Transaction(
                merchID,
                "DSIEMVAndroidDemo:1.00",
                "EMVReturn",
                "1",
                amt,
                "0010010010",
                mOperationMode,
                "RecordNumberRequested",
                "1"
        );

        switch (mConnectedDevice)
        {
            case LANE3000_IP:
                newReturn.setSecureDevice("EMV_LANE3000_DATACAP_E2E");
                newReturn.setPinPadIpAddress(padIP);
                newReturn.setPinPadIpPort(padPort);
                break;
            case PAX_ANDROID_IP:
                newReturn.setSecureDevice(determineSecureDevice());
                newReturn.setPinPadIpAddress(padIP);
                newReturn.setPinPadIpPort("1235");
                break;
            case VP3300_USB:
                newReturn.setSecureDevice("EMV_VP3300_DATACAP");
                break;
            case VP3300_RS232:
                newReturn.setSecureDevice("EMV_VP3300_DATACAP_RS232");
                break;
            case VP3350_USB:
                newReturn.setSecureDevice("EMV_VP3350_DATACAP");
                break;
            default:
                // Must be a bluetooth device
                newReturn.setBluetoothDeviceName(mConnectedDevice);
                newReturn.setSecureDevice(determineSecureDeviceByBTName(mConnectedDevice));
                break;
        }
        TStream tStream = new TStream(newReturn);

        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        Serializer serializer = new Persister();
        try
        {
            serializer.write(tStream, bao);
        }
        catch (Exception ex)
        {
            //serialization exception
        }
        return bao.toString();
    }

    private String setupCollectCardData(String amount, String merchID, String padIP, String padPort)
    {
        String tranCode = "CollectCardData";
        Amount amt = new Amount(amount);
        Transaction newReturn;
        switch (mConnectedDevice)
        {
            case LANE3000_IP:
                newReturn = new Transaction(merchID,
                        "DSIEMVAndroid_Demo",
                        "EMVUSClient:1.27",
                        tranCode,
                        "EMV_LANE3000_DATACAP_E2E",
                        "100",
                        amt,
                        "0010010010",
                        mOperationMode,
                        "RecordNumberRequested",
                        "23",
                        padIP,
                        padPort);
                break;
            case PAX_ANDROID_IP:
                newReturn = new Transaction(merchID,
                        "DSIEMVAndroid_Demo",
                        "EMVUSClient:1.27",
                        tranCode,
                        determineSecureDevice(),
                        "10",
                        amt,
                        "0010010010",
                        mOperationMode,
                        "RecordNumberRequested",
                        "1",
                        padIP,
                        "1235");

                break;
            case VP3300_USB:
            case VP3300_RS232:
                //USB connected devices need no "BluetoothDeviceName"
                String secureDevice = "EMV_VP3300_DATACAP";
                //RS232 takes a different secure device name
                if (mConnectedDevice.equals(VP3300_RS232))
                {
                    secureDevice = "EMV_VP3300_DATACAP_RS232";
                }
                newReturn = new Transaction(merchID,
                        "DSIEMVAndroid_Demo",
                        "EMVUSClient:1.27",
                        tranCode,
                        secureDevice,
                        "100",
                        amt,
                        "0010010010",
                        mOperationMode,
                        "RecordNumberRequested",
                        "23");
                break;
            default:
                newReturn = new Transaction(merchID,
                        "DSIEMVAndroid_Demo",
                        "EMVUSClient:1.27",
                        tranCode,
                        "EMV_VP3300_DATACAP",
                        "100",
                        amt,
                        "0010010010",
                        mConnectedDevice,
                        mOperationMode,
                        "RecordNumberRequested",
                        "23");
                break;
        }
        TStream tStream = new TStream(newReturn);

        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        Serializer serializer = new Persister();
        try
        {
            serializer.write(tStream, bao);
        }
        catch (Exception ex)
        {
            //serialization exception
        }
        return bao.toString();
    }

    private String setupParamDownload(String merchID, String padIP, String padPort)
    {
        Admin newParam = new Admin(
                merchID,
                "DSIEMVAndroidDemo:1.00",
                "EMVParamDownload",
                "0010010010",
                mOperationMode
        );

        switch (mConnectedDevice)
        {
            case LANE3000_IP:
                newParam.setSecureDevice("EMV_LANE3000_DATACAP_E2E");
                newParam.setPinPadIpAddress(padIP);
                newParam.setPinPadIpPort(padPort);
                break;
            case PAX_ANDROID_IP:
                newParam.setSecureDevice(determineSecureDevice());
                newParam.setPinPadIpAddress(padIP);
                newParam.setPinPadIpPort("1235");
                break;
            case VP3300_USB:
                newParam.setSecureDevice("EMV_VP3300_DATACAP");
                break;
            case VP3300_RS232:
                newParam.setSecureDevice("EMV_VP3300_DATACAP_RS232");
                break;
            case VP3350_USB:
                newParam.setSecureDevice("EMV_VP3350_DATACAP");
                break;
            default:
                // Must be a bluetooth device
                newParam.setBluetoothDeviceName(mConnectedDevice);
                newParam.setSecureDevice(determineSecureDeviceByBTName(mConnectedDevice));
                break;
        }

        TStream tStream = new TStream(newParam);

        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        Serializer serializer = new Persister();
        try
        {
            serializer.write(tStream, bao);
        }
        catch (Exception ex)
        {
            //serialization exception
        }
        return bao.toString();
    }

    //code to look for bluetooth le devices. This can be used to show the user a list of available devices,
    // then pass a selected device name to the DSIEMVAndroid control to connect to it.
    private void searchForBt()
    {
        List<ScanFilter> filters = new ArrayList<>();
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build();
        final BtleScanCallback mScanCallback = new BtleScanCallback();
        // Getting the Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter != null)
        {
            checkPermissions();
        }
        else
        {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkPermissions()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            // Android 12 and above
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(this, new String[]
                        {
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT
                        }, REQUEST_PERMISSIONS);
            }
            else
            {
                startScanning();
            }
        }
        else
        {
            // Android 6 to Android 11
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(this, new String[]
                        {
                                Manifest.permission.ACCESS_FINE_LOCATION
                        }, REQUEST_PERMISSIONS);
            }
            else
            {
                // Check if location services are enabled
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                boolean isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                        || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                if (!isLocationEnabled)
                {
                    Toast.makeText(this, "Please enable location services", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }
                else
                {
                    startScanning();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        if (requestCode == REQUEST_PERMISSIONS)
        {
            boolean allGranted = grantResults.length > 0;
            for (int result : grantResults)
            {
                if (result != PackageManager.PERMISSION_GRANTED)
                {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted)
            {
                startScanning();
            }
            else
            {
                Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show();
            }
        }
        else
        {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void startScanning()
    {
        // Get the BluetoothLeScanner
        BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothLeScanner == null)
        {
            Toast.makeText(this, "Bluetooth LE Scanner not available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Set up scan filters and settings
        List<ScanFilter> filters = new ArrayList<>();
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build();

        // Initialize ScanCallback
        BtleScanCallback scanCallback = new BtleScanCallback();

        // Start scanning
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "BLUETOOTH_SCAN permission required", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        bluetoothLeScanner.startScan(filters, settings, scanCallback);

        // Stop scanning after a pre-defined scan period
        handler.postDelayed(() ->
        {
            bluetoothLeScanner.stopScan(scanCallback);
            Toast.makeText(MainActivity.this, "Scanning stopped", Toast.LENGTH_SHORT).show();
        }, 60000);
    }

    private class BtleScanCallback extends ScanCallback
    {
        @Override
        public void onScanResult(int callbackType, ScanResult result)
        {
            BluetoothDevice device = result.getDevice();
            String devName = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
                {
                    devName = device.getName();
                }
                else
                {
                    devName = "Unknown Device";
                }
            }
            else
            {
                devName = device.getName();
            }

            if (devName != null && !devName.isEmpty() && !containsDevice(device))
            {
                Logger.getLogger("Scanner").info(String.format("Adding Device Name: %s", devName));
                mDeviceList.add(devName);
                listAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results)
        {
            for (ScanResult result : results)
            {
                BluetoothDevice device = result.getDevice();
                String devName = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
                    {
                        devName = device.getName();
                    }
                    else
                    {
                        devName = "Unknown Device";
                    }
                }
                else
                {
                    devName = device.getName();
                }

                if (devName != null && !devName.isEmpty() && !containsDevice(device))
                {
                    Logger.getLogger("Scanner").info(String.format("Adding Device Name: %s", devName));
                    mDeviceList.add(devName);
                }
            }
            listAdapter.notifyDataSetChanged();
        }

        @Override
        public void onScanFailed(int errorCode)
        {
            Toast.makeText(MainActivity.this, "Scan failed with error: " + errorCode, Toast.LENGTH_SHORT).show();
        }

        private boolean containsDevice(BluetoothDevice device)
        {
            for (String d : mDeviceList)
            {
                if (Objects.equals(d, device.getName()))
                {
                    return true;
                }
            }
            return false;
        }
    }

    //sample code to check for permissions that are needed for bluetooth communication.
    private void hasPermissions()
    {

        ActivityResultLauncher<String[]> permissionRequest =
                this.registerForActivityResult(new ActivityResultContracts
                                .RequestMultiplePermissions(), result ->
                        {
                            if (Build.VERSION.SDK_INT <= 30)
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
                                if (!Boolean.TRUE.equals(fineLocationGranted) | !Boolean.TRUE.equals(coarseLocationGranted))
                                {
                                    buildDialogFor("Functionality Limited", "Grant fine location access to discover beacons.");
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
                        }
                );

        // Before you perform the actual permission request, check whether your app
        // already has the permissions, and whether your app needs to show a permission
        // rationale dialog. For more details, see Request permissions. https://developer.android.com/training/permissions/requesting
        if (Build.VERSION.SDK_INT > 30)
        {
            permissionRequest.launch(new String[] {
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
            });
        }
        else
        {
            permissionRequest.launch(new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
            });
        }

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
    }

    private void buildDialogFor(String title, String message)
    {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .setOnDismissListener((dialog) ->
                {
                })
                .show();
    }

    private void requestBluetoothEnable()
    {
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

    public static String getIPAddress(boolean useIPv4)
    {
        try
        {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces)
            {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs)
                {
                    if (!addr.isLoopbackAddress())
                    {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':') < 0;

                        if (useIPv4)
                        {
                            if (isIPv4)
                                return sAddr;
                        }
                        else
                        {
                            if (!isIPv4)
                            {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim < 0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        }
        catch (Exception ignored) { } // for now eat exceptions
        return "";
    }

    private static String determineSecureDevice()
    {
        String deviceModel = android.os.Build.MODEL;
        String secureDevice = padMap.get(deviceModel);
        //if secure device cant be found, treat it like an A920 Pro
        if (secureDevice == null)
        {
            secureDevice = "EMV_A920PRO_DATACAP_E2E";
        }
        return secureDevice;
    }

    private static String determineSecureDeviceByBTName(String btName)
    {
        String secureDevice = "Unknown Bluetooth Device";
        if (btName.contains("IDTECH-VP3300"))
        {
            secureDevice = "EMV_VP3300_DATACAP";
        }
        else if (btName.contains("IDTECH-VP3350"))
        {
            secureDevice = "EMV_VP3350_DATACAP";
        }
        return secureDevice;
    }

    private void bringToFront()
    {
        handler.post(() ->
        {
            Intent intent = new Intent(MainActivity.this.getApplicationContext(), MainActivity.this.getClass());
            // You need this if starting
            //  the activity from a service
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivityIfNeeded(intent, 0);
        });
    }

    // Assorted classes for appearances
    public static class CardPagerAdapter extends FragmentStateAdapter
    {
        public CardPagerAdapter(@NonNull FragmentActivity fragmentActivity)
        {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position)
        {
            switch (position)
            {
                case 0:
                    return new ParamFragment();
                case 1:
                    return new TranCodeFragment();
                default:
                    return new ParamFragment(); // Default case
            }
        }

        @Override
        public int getItemCount()
        {
            return 2;
        }

    }

    public static class ParamFragment extends Fragment
    {
        public ParamFragment()
        {
            super(R.layout.parameters_card);
        }
    }

    public static class TranCodeFragment extends Fragment
    {
        public TranCodeFragment()
        {
            super(R.layout.trancode_card);
        }
    }

    public static class HorizontalMarginItemDecoration extends RecyclerView.ItemDecoration
    {
        private final int horizontalMargin;

        public HorizontalMarginItemDecoration(int horizontalMargin)
        {
            this.horizontalMargin = horizontalMargin;
        }

        @Override
        public void getItemOffsets(Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state)
        {
            outRect.right = horizontalMargin;
        }
    }


}
