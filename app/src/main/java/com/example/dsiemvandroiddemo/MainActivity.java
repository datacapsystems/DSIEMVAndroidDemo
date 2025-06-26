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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import com.example.dsiemvandroiddemo.transaction.TransactionManager;
import com.example.dsiemvandroiddemo.transaction.SaleTransactionRequest;
import com.example.dsiemvandroiddemo.ui.ResponseBottomSheetFragment;
import com.example.dsiemvandroiddemo.ui.TransactionHistoryBottomSheetFragment;
import com.example.dsiemvandroiddemo.model.TransactionHistoryManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;


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

public class MainActivity extends AppCompatActivity implements LocalListener.TransactionProgressListener
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
    private TransactionManager transactionManager;
    private TransactionHistoryManager historyManager;
    private TransactionHistoryBottomSheetFragment currentHistoryBottomSheet;
    private String currentTransactionRequest = "";

    private static final Map<String, String> padMap;
    private static final int REQUEST_PERMISSIONS = 2;
    private static final String PREFS_NAME = "DSIEMVAndroidPrefs";
    private static final String PREF_MERCHANT_ID = "merchantId";
    private static final String PREF_IP_ADDRESS = "ipAddress";
    private static final String PREF_PORT = "port";
    private SharedPreferences sharedPreferences;

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

    // UI Components
    private TextInputEditText amountInput;
    private TextInputEditText merchantIdInput;
    private TextInputEditText ipAddressInput;
    private TextInputEditText portInput;
    private ChipGroup environmentChipGroup;
    private Chip deviceStatusChip;
    private TextView deviceInfoText;
    private MaterialCardView statusBanner;
    private TextView statusText;
    private ExtendedFloatingActionButton responseDetailsFab;
    private LinearLayout deviceConnectionHeader;
    private ImageView deviceConnectionExpandIcon;
    private LinearLayout deviceConnectionContent;
    private LinearLayout quickTransactionHeader;
    private ImageView quickTransactionExpandIcon;
    private LinearLayout quickTransactionContent;
    private LinearLayout additionalTranCodesHeader;
    private ImageView additionalTranCodesExpandIcon;
    private LinearLayout additionalTranCodesContent;
    private boolean isDeviceConnectionExpanded = true;
    private boolean isQuickTransactionExpanded = true;
    private boolean isAdditionalTranCodesExpanded = true;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize TransactionManager and HistoryManager
        transactionManager = new TransactionManager(this);
        historyManager = TransactionHistoryManager.getInstance();
        
        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Initialize UI components
        initializeViews();
        setupClickListeners();
        setupSDKListeners();
        
        // Load saved values
        loadSavedValues();

        // Check permissions
        hasPermissions();
        
        try
        {
            //sets up local endpoint to be used with EMV US Test Client
            LocalListener li = new LocalListener(MainActivity.this);
            li.setTransactionProgressListener(this);
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
                    TextView nodt = findViewById(R.id.nameOfDeviceText);
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
                    TextView nodt = findViewById(R.id.nameOfDeviceText);
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
                    updateDeviceConnectionStatus();
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
        listView.setOnItemClickListener((parent, view, position, id) -> {
            mConnectedDevice = mDeviceList.get(position);
            updateDeviceConnectionStatus();
            mBTdialog.dismiss(); // Close the dialog if desired
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Choose a Device" + System.lineSeparator() + "Searching...");
        builder.setView(dialogView);
        mBTdialog = builder.create();

        // Set default values for inputs
        amountInput.setText("1.00");
        if (environmentChipGroup.getCheckedChipId() == View.NO_ID) {
            findViewById(R.id.certChip).performClick();
        }
        
        // Update initial device connection status
        updateDeviceConnectionStatus();
        
        // Set initial expand icon rotations
        if (deviceConnectionExpandIcon != null) {
            deviceConnectionExpandIcon.setRotation(isDeviceConnectionExpanded ? 180 : 0);
        }
        if (quickTransactionExpandIcon != null) {
            quickTransactionExpandIcon.setRotation(isQuickTransactionExpanded ? 180 : 0);
        }
        if (additionalTranCodesExpandIcon != null) {
            additionalTranCodesExpandIcon.setRotation(isAdditionalTranCodesExpanded ? 180 : 0);
        }

        //adding message listener for the VP3300, since the device has no screen the control sends messages back to the UI for card removal, etc.
        // This will be handled by setupSDKListeners() method

        dsiEMVAndroidinstance.getInstance(MainActivity.this).AddEstablishBluetoothConnectionResponseListener(response ->
        {
            //run on ui thread to tell user connection was successful
            // use either a Handler to MainThread or runOnUiThread call.
            handler.post(() ->
            {
                if (response.contains("Success"))
                {
                    updateDeviceConnectionStatus();
                    updateStatusText("Connected to " + mConnectedDevice);
                }
                else
                {
                    mConnectedDevice = "";
                    updateDeviceConnectionStatus();
                    updateStatusText("Could not connect to device");
                }
                
                // Add connection response to history
                historyManager.addTransaction("Bluetooth Connection", "", "", response, 
                    response.contains("Success"), mConnectedDevice);
                
                LOGGER.info("EstablishBluetoothConnectionResponse: " + response);
            });
        });

        dsiEMVAndroidinstance.getInstance(MainActivity.this).AddBluetoothConnectionListener(isConnected ->
        {
            //run on ui thread to tell user connection was successful
            handler.post(() ->
            {
                if (isConnected)
                {
                    updateDeviceConnectionStatus();
                    updateStatusText("Connected to " + mConnectedDevice);
                }
                else
                {
                    updateStatusText("Disconnected from " + mConnectedDevice);
                    mConnectedDevice = "";
                    updateDeviceConnectionStatus();
                }
            });
        });

        // Response listeners are now handled in setupSDKListeners()

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources
        if (transactionManager != null) {
            transactionManager.shutdown();
        }
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void initializeViews() {
        amountInput = findViewById(R.id.amountInput);
        merchantIdInput = findViewById(R.id.merchantIdInput);
        ipAddressInput = findViewById(R.id.ipAddressInput);
        portInput = findViewById(R.id.portInput);
        environmentChipGroup = findViewById(R.id.environmentChipGroup);
        deviceStatusChip = findViewById(R.id.deviceStatusChip);
        deviceInfoText = findViewById(R.id.deviceInfoText);
        statusBanner = findViewById(R.id.statusBanner);
        statusText = findViewById(R.id.statusText);
        responseDetailsFab = findViewById(R.id.responseDetailsFab);
        
        // Collapsible card views
        deviceConnectionHeader = findViewById(R.id.deviceConnectionHeader);
        deviceConnectionExpandIcon = findViewById(R.id.deviceConnectionExpandIcon);
        deviceConnectionContent = findViewById(R.id.deviceConnectionContent);
        quickTransactionHeader = findViewById(R.id.quickTransactionHeader);
        quickTransactionExpandIcon = findViewById(R.id.quickTransactionExpandIcon);
        quickTransactionContent = findViewById(R.id.quickTransactionContent);
        additionalTranCodesHeader = findViewById(R.id.additionalTranCodesHeader);
        additionalTranCodesExpandIcon = findViewById(R.id.additionalTranCodesExpandIcon);
        additionalTranCodesContent = findViewById(R.id.additionalTranCodesContent);
    }

    private void setupClickListeners() {
        // Sale button
        findViewById(R.id.saleButton).setOnClickListener(v -> performSale());
        
        // Return button
        findViewById(R.id.returnButton).setOnClickListener(v -> performReturn());
        
        // Reset button
        findViewById(R.id.resetButton).setOnClickListener(v -> performReset());
        
        // Select device button
        findViewById(R.id.selectDeviceButton).setOnClickListener(v -> {
            searchForBt();
            mBTdialog.show();
        });
        
        // Additional TranCodes buttons
        findViewById(R.id.paramDownloadButton).setOnClickListener(v -> performParamDownload());
        findViewById(R.id.deviceInfoButton).setOnClickListener(v -> performDeviceInfo());
        
        
        // Collapsible card headers
        deviceConnectionHeader.setOnClickListener(v -> toggleDeviceConnection());
        quickTransactionHeader.setOnClickListener(v -> toggleQuickTransaction());
        additionalTranCodesHeader.setOnClickListener(v -> toggleAdditionalTranCodes());
        
        // Additional action buttons (now handled in settings dialog)
        
        // Cancel button in status banner
        findViewById(R.id.cancelButton).setOnClickListener(v -> cancelCurrentTransaction());
        
        // Response details FAB
        responseDetailsFab.setOnClickListener(v -> showResponseDetails());
        
        // Environment chip selection
        environmentChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.certChip)) {
                mOperationMode = "CERT";
            } else if (checkedIds.contains(R.id.prodChip)) {
                mOperationMode = "PROD";
            }
        });
    }

    private void setupSDKListeners() {
        // SDK response listeners
        dsiEMVAndroidinstance.getInstance(MainActivity.this)
            .AddProcessTransactionResponseListener(response -> {
                runOnUiThread(() -> {
                    handleTransactionResponse(response);
                });
            });

        // SDK display message listener
        dsiEMVAndroidinstance.getInstance(MainActivity.this)
            .AddDisplayMessageListener(message -> {
                runOnUiThread(() -> {
                    updateStatusText(message);
                });
            });
    }

    private void performSale() {
        // Check if device is connected first
        if (mConnectedDevice.isEmpty()) {
            promptDeviceSelection("Please select a device before processing a sale.");
            return;
        }
        
        String amount = amountInput.getText().toString().trim();
        String merchantId = merchantIdInput.getText().toString().trim();
        String ipAddress = ipAddressInput.getText().toString().trim();
        String port = portInput.getText().toString().trim();
        
        if (amount.isEmpty()) {
            amountInput.setError("Amount is required");
            return;
        }
        
        // Save the field values for next time
        saveFieldValues(merchantId, ipAddress, port);
        
        showTransactionInProgress("Processing sale...");
        
        // Generate the request XML using the existing setupSale method
        currentTransactionRequest = setupSale(amount, merchantId, ipAddress, port);
        
        SaleTransactionRequest saleRequest = new SaleTransactionRequest(
            amount, merchantId, ipAddress, port, mOperationMode, mConnectedDevice
        );
        
        transactionManager.executeTransaction(saleRequest, new TransactionManager.TransactionCallback() {
            @Override
            public void onSuccess(String response) {
                // Response will be handled by SDK listener
            }
            
            @Override
            public void onError(Exception e) {
                hideTransactionInProgress();
                showErrorMessage("Sale failed: " + e.getMessage());
            }
            
            @Override
            public void onTransactionBusy() {
                Toast.makeText(MainActivity.this, 
                    "Another transaction is in progress. Please wait.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performReturn() {
        // Check if device is connected first
        if (mConnectedDevice.isEmpty()) {
            promptDeviceSelection("Please select a device before processing a return.");
            return;
        }
        
        // Similar to performSale but for returns
        String amount = amountInput.getText().toString().trim();
        String merchantId = merchantIdInput.getText().toString().trim();
        String ipAddress = ipAddressInput.getText().toString().trim();
        String port = portInput.getText().toString().trim();
        
        if (amount.isEmpty()) {
            amountInput.setError("Amount is required");
            return;
        }
        
        // Save the field values for next time
        saveFieldValues(merchantId, ipAddress, port);
        
        showTransactionInProgress("Processing return...");
        
        // Generate the request XML using the existing setupReturn method
        currentTransactionRequest = setupReturn(amount, merchantId, ipAddress, port);
        
        // For now, simulate a return transaction (would need actual implementation)
        executor.submit(() -> {
            try {
                Thread.sleep(2000); // Simulate processing time
                String simulatedResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><TStream><Transaction><ResponseOrigin>Client</ResponseOrigin><ResponseCode>000</ResponseCode><ResponseText>APPROVAL</ResponseText><TransactionID>12345</TransactionID><TransType>Return</TransType><Authorize>" + amount + "</Authorize><AuthCode>654321</AuthCode></Transaction></TStream>";
                
                runOnUiThread(() -> {
                    historyManager.addTransaction("Return", "", currentTransactionRequest, simulatedResponse, true, mConnectedDevice);
                    currentTransactionRequest = "";
                    hideTransactionInProgress();
                    responseDetailsFab.setVisibility(View.VISIBLE);
                    responseDetailsFab.setTag(historyManager.getLatestTransaction().getId());
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    
    private void toggleDeviceConnection() {
        isDeviceConnectionExpanded = !isDeviceConnectionExpanded;
        
        if (isDeviceConnectionExpanded) {
            deviceConnectionContent.setVisibility(View.VISIBLE);
            deviceConnectionExpandIcon.setRotation(180);
        } else {
            deviceConnectionContent.setVisibility(View.GONE);
            deviceConnectionExpandIcon.setRotation(0);
        }
    }
    
    private void toggleQuickTransaction() {
        isQuickTransactionExpanded = !isQuickTransactionExpanded;
        
        if (isQuickTransactionExpanded) {
            quickTransactionContent.setVisibility(View.VISIBLE);
            quickTransactionExpandIcon.setRotation(180);
        } else {
            quickTransactionContent.setVisibility(View.GONE);
            quickTransactionExpandIcon.setRotation(0);
        }
    }
    
    private void toggleAdditionalTranCodes() {
        isAdditionalTranCodesExpanded = !isAdditionalTranCodesExpanded;
        
        if (isAdditionalTranCodesExpanded) {
            additionalTranCodesContent.setVisibility(View.VISIBLE);
            additionalTranCodesExpandIcon.setRotation(180);
        } else {
            additionalTranCodesContent.setVisibility(View.GONE);
            additionalTranCodesExpandIcon.setRotation(0);
        }
    }

    private void showTransactionInProgress(String message) {
        statusText.setText(message);
        statusBanner.setVisibility(View.VISIBLE);
        findViewById(R.id.statusProgress).setVisibility(View.VISIBLE);
    }

    private void hideTransactionInProgress() {
        statusBanner.setVisibility(View.GONE);
    }

    private void updateStatusText(String message) {
        statusText.setText(message);
        if (statusBanner.getVisibility() != View.VISIBLE) {
            statusBanner.setVisibility(View.VISIBLE);
        }
    }

    private void handleTransactionResponse(String response) {
        hideTransactionInProgress();
        
        // Check if this response was already added by LocalListener
        // LocalListener adds transactions immediately when they're processed
        boolean alreadyInHistory = false;
        if (historyManager.getLatestTransaction() != null) {
            String latestResponse = historyManager.getLatestTransaction().getResponse();
            // If the latest transaction has the same response and was added within the last 5 seconds,
            // it's likely the same transaction from LocalListener
            long timeDiff = System.currentTimeMillis() - historyManager.getLatestTransaction().getTimestamp();
            if (latestResponse != null && latestResponse.equals(response) && timeDiff < 5000) {
                alreadyInHistory = true;
            }
        }
        
        if (!alreadyInHistory) {
            // Add to transaction history - determine transaction type from response
            boolean isSuccess = !response.toLowerCase().contains("error") && 
                              !response.toLowerCase().contains("fail");
            
            String transactionType = determineTransactionTypeFromResponse(response);
            historyManager.addTransaction(transactionType, "", currentTransactionRequest, response, isSuccess, mConnectedDevice);
            // Clear the current request after adding to history
            currentTransactionRequest = "";
        }
        
        // Auto-show transaction history unless it's a PadReset transaction
        if (!isPadResetTransaction(response)) {
            if (currentHistoryBottomSheet != null && currentHistoryBottomSheet.isVisible()) {
                // History is already showing, just navigate to latest transaction
                currentHistoryBottomSheet.navigateToLatestTransaction();
            } else {
                // Show new history bottom sheet
                currentHistoryBottomSheet = TransactionHistoryBottomSheetFragment
                    .newInstance(historyManager.getLatestTransaction().getId());
                currentHistoryBottomSheet.show(getSupportFragmentManager(), "TransactionResponseBottomSheet");
            }
        }
        
        // Show response FAB for future access
        responseDetailsFab.setVisibility(View.VISIBLE);
        responseDetailsFab.setTag(historyManager.getLatestTransaction().getId()); // Store transaction ID
        
        // Update device status if connected
        updateDeviceConnectionStatus();
    }

    private void showResponseDetails() {
        String transactionId = (String) responseDetailsFab.getTag();
        if (transactionId != null) {
            currentHistoryBottomSheet = TransactionHistoryBottomSheetFragment
                .newInstance(transactionId);
            currentHistoryBottomSheet.show(getSupportFragmentManager(), "TransactionHistoryBottomSheet");
        }
    }

    private void updateDeviceConnectionStatus() {
        if (!mConnectedDevice.isEmpty()) {
            deviceStatusChip.setText("Connected");
            deviceStatusChip.setChipBackgroundColorResource(R.color.success_background);
            deviceStatusChip.setChipIconResource(R.drawable.ic_check_circle);
            deviceInfoText.setText("Connected to: " + mConnectedDevice);
        } else {
            deviceStatusChip.setText("Not Connected");
            deviceStatusChip.setChipBackgroundColorResource(R.color.error_background);
            deviceStatusChip.setChipIconResource(R.drawable.ic_device_unknown);
            deviceInfoText.setText("No device connected");
        }
    }

    private void showErrorMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
    
    private void promptDeviceSelection(String message) {
        new AlertDialog.Builder(this)
            .setTitle("No Device Connected")
            .setMessage(message)
            .setPositiveButton("Select Device", (dialog, which) -> {
                searchForBt();
                mBTdialog.show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void loadSavedValues() {
        // Load saved values from SharedPreferences
        String savedMerchantId = sharedPreferences.getString(PREF_MERCHANT_ID, "");
        String savedIpAddress = sharedPreferences.getString(PREF_IP_ADDRESS, "");
        String savedPort = sharedPreferences.getString(PREF_PORT, "");
        
        // Set the values to the input fields
        if (merchantIdInput != null && !savedMerchantId.isEmpty()) {
            merchantIdInput.setText(savedMerchantId);
        }
        if (ipAddressInput != null && !savedIpAddress.isEmpty()) {
            ipAddressInput.setText(savedIpAddress);
        }
        if (portInput != null && !savedPort.isEmpty()) {
            portInput.setText(savedPort);
        }
    }
    
    private void saveFieldValues(String merchantId, String ipAddress, String port) {
        // Save values to SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_MERCHANT_ID, merchantId);
        editor.putString(PREF_IP_ADDRESS, ipAddress);
        editor.putString(PREF_PORT, port);
        editor.apply();
    }

    private void performParamDownload() {
        // Check if device is connected first
        if (mConnectedDevice.isEmpty()) {
            promptDeviceSelection("Please select a device before downloading parameters.");
            return;
        }
        
        String merchantId = merchantIdInput.getText().toString().trim();
        String ipAddress = ipAddressInput.getText().toString().trim();
        String port = portInput.getText().toString().trim();
        
        // Save the field values for next time
        saveFieldValues(merchantId, ipAddress, port);
        
        showTransactionInProgress("Downloading parameters...");
        
        // Generate the request XML using the existing setupParamDownload method
        currentTransactionRequest = setupParamDownload(merchantId, ipAddress, port);
        
        executor.submit(() -> {
            try {
                String response = dsiEMVAndroidinstance.getInstance(MainActivity.this).ProcessTransaction(currentTransactionRequest);
                
                runOnUiThread(() -> {
                    historyManager.addTransaction("Param Download", "", currentTransactionRequest, response, true, mConnectedDevice);
                    currentTransactionRequest = "";
                    hideTransactionInProgress();
                    
                    // Auto-show transaction history unless it's a PadReset transaction
                    if (!isPadResetTransaction(response)) {
                        if (currentHistoryBottomSheet != null && currentHistoryBottomSheet.isVisible()) {
                            currentHistoryBottomSheet.navigateToLatestTransaction();
                        } else {
                            currentHistoryBottomSheet = TransactionHistoryBottomSheetFragment
                                .newInstance(historyManager.getLatestTransaction().getId());
                            currentHistoryBottomSheet.show(getSupportFragmentManager(), "ParamDownloadBottomSheet");
                        }
                    }
                    
                    responseDetailsFab.setVisibility(View.VISIBLE);
                    responseDetailsFab.setTag(historyManager.getLatestTransaction().getId());
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    hideTransactionInProgress();
                    showErrorMessage("Parameter download failed: " + e.getMessage());
                });
            }
        });
    }

    private void performDeviceInfo() {
        String response = dsiEMVAndroidinstance.getInstance(MainActivity.this).GetDevicesInfo();
        
        // Add to history - device info has no request
        historyManager.addTransaction("Device Info", "", "", response, true, mConnectedDevice);
        
        // Show latest transaction (device info will be at index 0)
        currentHistoryBottomSheet = TransactionHistoryBottomSheetFragment
            .newInstance(historyManager.getLatestTransaction().getId());
        currentHistoryBottomSheet.show(getSupportFragmentManager(), "DeviceInfoBottomSheet");
    }

    private void performReset() {
        // Check if device is connected first
        if (mConnectedDevice.isEmpty()) {
            promptDeviceSelection("Please select a device before resetting.");
            return;
        }
        
        String merchantId = merchantIdInput.getText().toString().trim();
        String ipAddress = ipAddressInput.getText().toString().trim();
        String port = portInput.getText().toString().trim();
        
        // Save the field values for next time
        saveFieldValues(merchantId, ipAddress, port);
        
        showTransactionInProgress("Resetting device...");
        
        // Generate the request XML using the existing setupPadReset method
        currentTransactionRequest = setupPadReset(merchantId, ipAddress, port);
        
        executor.submit(() -> {
            try {
                String response = dsiEMVAndroidinstance.getInstance(MainActivity.this).ProcessTransaction(currentTransactionRequest);
                
                runOnUiThread(() -> {
                    historyManager.addTransaction("Pad Reset", "", currentTransactionRequest, response, true, mConnectedDevice);
                    currentTransactionRequest = "";
                    hideTransactionInProgress();
                    
                    // PadReset transactions do not auto-show history as per user requirement
                    responseDetailsFab.setVisibility(View.VISIBLE);
                    responseDetailsFab.setTag(historyManager.getLatestTransaction().getId());
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    hideTransactionInProgress();
                    showErrorMessage("Device reset failed: " + e.getMessage());
                });
            }
        });
    }

    private void cancelCurrentTransaction() {
        transactionManager.cancelCurrentTransaction();
        executor.submit(() -> {
            dsiEMVAndroidinstance.getInstance(MainActivity.this).CancelRequest();
        });
        hideTransactionInProgress();
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

    private String setupPadReset(String merchID, String padIP, String padPort)
    {

        Transaction newReturn = new Transaction(
                merchID,
                "DSIEMVAndroidDemo:1.00",
                "EMVPadReset",
                "0010010010"
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

    public static String determineSecureDevice()
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

    public static String determineSecureDeviceByBTName(String btName)
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
    
    /**
     * Check if a transaction response indicates a PadReset operation
     */
    private boolean isPadResetTransaction(String response) {
        if (response == null) return false;
        return response.contains("<TranCode>EMVPadReset</TranCode>") || 
               response.contains("<TranCode>PadReset</TranCode>");
    }
    
    /**
     * Determine transaction type from XML response
     */
    private String determineTransactionTypeFromResponse(String response) {
        if (response == null) return "Transaction";
        
        // Check TranCode first
        if (response.contains("<TranCode>")) {
            String tranCode = extractXmlValue(response, "TranCode");
            if (tranCode != null) {
                switch (tranCode) {
                    case "EMVPadReset":
                    case "PadReset":
                        return "Pad Reset";
                    case "EMVSale":
                        return "Sale";
                    case "EMVReturn":
                        return "Return";
                    case "EMVAuth":
                        return "Authorization";
                    case "EMVCapture":
                        return "Capture";
                    case "EMVVoid":
                        return "Void";
                    case "CollectCardData":
                        return "Collect Card Data";
                    default:
                        return tranCode;
                }
            }
        }
        
        // Check TransType as fallback
        if (response.contains("<TransType>")) {
            String transType = extractXmlValue(response, "TransType");
            if (transType != null) {
                switch (transType) {
                    case "EMVPadReset":
                    case "PadReset":
                        return "Pad Reset";
                    case "EMVSale":
                        return "Sale";
                    case "EMVReturn":
                        return "Return";
                    case "EMVAuth":
                        return "Authorization";
                    case "EMVCapture":
                        return "Capture";
                    case "EMVVoid":
                        return "Void";
                    default:
                        return transType;
                }
            }
        }
        
        return "Transaction";
    }
    
    /**
     * Extract value from XML tag
     */
    private String extractXmlValue(String xml, String tagName) {
        try {
            String startTag = "<" + tagName + ">";
            String endTag = "</" + tagName + ">";
            int start = xml.indexOf(startTag);
            if (start != -1) {
                start += startTag.length();
                int end = xml.indexOf(endTag, start);
                if (end != -1) {
                    return xml.substring(start, end);
                }
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return null;
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            showSettingsDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Settings");
        
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_settings, null);
        builder.setView(dialogView);
        
        // Initialize dialog components
        ChipGroup dialogEnvironmentChipGroup = dialogView.findViewById(R.id.dialogEnvironmentChipGroup);
        TextInputEditText dialogMerchantIdInput = dialogView.findViewById(R.id.dialogMerchantIdInput);
        TextInputEditText dialogIpAddressInput = dialogView.findViewById(R.id.dialogIpAddressInput);
        TextInputEditText dialogPortInput = dialogView.findViewById(R.id.dialogPortInput);
        
        // Load current values
        dialogMerchantIdInput.setText(merchantIdInput.getText());
        dialogIpAddressInput.setText(ipAddressInput.getText());
        dialogPortInput.setText(portInput.getText());
        
        // Set current environment selection
        if (mOperationMode.equals("CERT")) {
            dialogView.findViewById(R.id.dialogCertChip).performClick();
        } else {
            dialogView.findViewById(R.id.dialogProdChip).performClick();
        }
        
        
        builder.setPositiveButton("Save", (dialog, which) -> {
            // Save the settings
            merchantIdInput.setText(dialogMerchantIdInput.getText());
            ipAddressInput.setText(dialogIpAddressInput.getText());
            portInput.setText(dialogPortInput.getText());
            
            // Update environment selection
            if (dialogEnvironmentChipGroup.getCheckedChipId() == R.id.dialogCertChip) {
                mOperationMode = "CERT";
                findViewById(R.id.certChip).performClick();
            } else {
                mOperationMode = "PROD";
                findViewById(R.id.prodChip).performClick();
            }
            
            // Save to preferences
            saveFieldValues(
                dialogMerchantIdInput.getText().toString(),
                dialogIpAddressInput.getText().toString(),
                dialogPortInput.getText().toString()
            );
        });
        
        builder.setNegativeButton("Cancel", null);
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // LocalListener.TransactionProgressListener implementation
    @Override
    public void onTransactionStarted(String message) {
        runOnUiThread(() -> {
            showTransactionInProgress(message);
            // Auto-collapse cards when LocalListener transaction starts
            collapseCardsForLocalTransaction();
        });
    }

    @Override
    public void onTransactionCompleted() {
        runOnUiThread(() -> {
            hideTransactionInProgress();
        });
    }
    
    private void collapseCardsForLocalTransaction() {
        // Collapse Device Connection, Quick Transaction, and Additional TranCodes cards
        if (isDeviceConnectionExpanded) {
            isDeviceConnectionExpanded = false;
            deviceConnectionContent.setVisibility(View.GONE);
            deviceConnectionExpandIcon.setRotation(0);
        }
        
        if (isQuickTransactionExpanded) {
            isQuickTransactionExpanded = false;
            quickTransactionContent.setVisibility(View.GONE);
            quickTransactionExpandIcon.setRotation(0);
        }
        
        if (isAdditionalTranCodesExpanded) {
            isAdditionalTranCodesExpanded = false;
            additionalTranCodesContent.setVisibility(View.GONE);
            additionalTranCodesExpandIcon.setRotation(0);
        }
    }

}
