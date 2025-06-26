package com.example.dsiemvandroiddemo.transaction;

import static com.example.dsiemvandroiddemo.MainActivity.determineSecureDevice;
import static com.example.dsiemvandroiddemo.MainActivity.determineSecureDeviceByBTName;

import android.content.Context;
import com.example.dsiemvandroiddemo.Amount;
import com.example.dsiemvandroiddemo.TStream;
import com.example.dsiemvandroiddemo.Transaction;
import com.example.dsiemvandroiddemo.dsiEMVAndroidinstance;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Encapsulates a sale transaction request
 */
public class SaleTransactionRequest implements TransactionManager.TransactionRequest {
    private final String amount;
    private final String merchantId;
    private final String padIP;
    private final String padPort;
    private final String operationMode;
    private final String connectedDevice;
    private static final String VP3300_USB = "IDTECH-VP3300-USB";
    private static final String VP3300_RS232 = "IDTECH-VP3300-RS232";
    private static final String VP3350_USB = "IDTECH-VP3350-USB";
    private static final String LANE3000_IP = "INGENICO_LANE_3000_IP";
    private static final String PAX_ANDROID_IP = "PAX_ANDROID_IP";
    
    public SaleTransactionRequest(String amount, String merchantId, String padIP, 
                                  String padPort, String operationMode, String connectedDevice) {
        this.amount = amount;
        this.merchantId = merchantId;
        this.padIP = padIP;
        this.padPort = padPort;
        this.operationMode = operationMode;
        this.connectedDevice = connectedDevice;
    }
    
    @Override
    public String getTransactionType() {
        return "EMVSale";
    }
    
    @Override
    public String execute(Context context) throws Exception {
        // Build transaction object
        Amount amt = new Amount(amount);

        Transaction sale = new Transaction(
            merchantId,
            "DSIEMVAndroidDemo:1.00",
            "EMVSale",
            "1",
            amt,
            "0010010010",
            operationMode,
            "RecordNumberRequested",
            "1"
        );
        
        // Configure device-specific settings
        configureDeviceSettings(sale);
        
        // Serialize to XML
        String xmlRequest = serializeTransaction(sale);
        
        // Process through SDK
        return dsiEMVAndroidinstance.getInstance(context).ProcessTransaction(xmlRequest);
    }
    
    private void configureDeviceSettings(Transaction transaction) {
        // Device-specific configuration based on connectedDevice
        switch (connectedDevice)
        {
            case LANE3000_IP:
                transaction.setSecureDevice("EMV_LANE3000_DATACAP_E2E");
                transaction.setPinPadIpAddress(padIP);
                transaction.setPinPadIpPort(padPort);
                break;
            case PAX_ANDROID_IP:
                transaction.setSecureDevice(determineSecureDevice());
                transaction.setPinPadIpAddress(padIP);
                transaction.setPinPadIpPort("1235");
                break;
            case VP3300_USB:
                transaction.setSecureDevice("EMV_VP3300_DATACAP");
                break;
            case VP3300_RS232:
                transaction.setSecureDevice("EMV_VP3300_DATACAP_RS232");
                break;
            case VP3350_USB:
                transaction.setSecureDevice("EMV_VP3350_DATACAP");
                break;
            default:
                // Must be a bluetooth device
                transaction.setBluetoothDeviceName(connectedDevice);
                transaction.setSecureDevice(determineSecureDeviceByBTName(connectedDevice));
                break;
        }
        if (padIP != null && !padIP.isEmpty()) {
            transaction.setPinPadIpAddress(padIP);
        }
        if (padPort != null && !padPort.isEmpty()) {
            transaction.setPinPadIpPort(padPort);
        }
    }
    
    private String serializeTransaction(Transaction transaction) throws Exception {
        TStream tStream = new TStream(transaction);
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        Serializer serializer = new Persister();
        
        try {
            serializer.write(tStream, bao);
            return bao.toString();
        } catch (Exception e) {
            throw new Exception("Failed to serialize transaction: " + e.getMessage(), e);
        }
    }
}