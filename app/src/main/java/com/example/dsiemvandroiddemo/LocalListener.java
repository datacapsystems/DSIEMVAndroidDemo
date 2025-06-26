package com.example.dsiemvandroiddemo;

import android.content.Context;
import android.util.Log;

import com.example.dsiemvandroiddemo.model.TransactionHistoryManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import fi.iki.elonen.NanoHTTPD;

//Local Listener class starts a post endpoint at this devices IP address on port 8080.
//Using the Datacap Client Test utility an integrator can test different transactions on the VP3300
public class LocalListener extends NanoHTTPD {

    public interface TransactionProgressListener {
        void onTransactionStarted(String message);
        void onTransactionCompleted();
    }

    private Context AppContext;
    private TransactionHistoryManager historyManager;
    private TransactionProgressListener progressListener;
    private static final Logger LOG = Logger.getLogger(LocalListener.class.getName());

    public LocalListener(Context appContext) throws IOException {
        super(8080);
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        AppContext = appContext;
        historyManager = TransactionHistoryManager.getInstance();
        System.out.println("\nRunning! Point your browsers to http://localhost:8080/ \n");
    }
    
    public void setTransactionProgressListener(TransactionProgressListener listener) {
        this.progressListener = listener;
    }


    @Override
    public Response serve(IHTTPSession session) {
        Method method = session.getMethod();
        String uri = session.getUri();
        LocalListener.LOG.info(method + " '" + uri + "' ");
        Map<String, String> files = new HashMap<String, String>();
        if (method == Method.POST) {
            String returnMSG = "";
            try {
                byte[ ] Latin1RequestArray = new byte[ session.getInputStream( ).available( ) ];
                session.getInputStream( ).read( Latin1RequestArray );
                String UTF8RequestMsg = new String( Latin1RequestArray, StandardCharsets.ISO_8859_1 );

                if ( !UTF8RequestMsg.contains( "TransactionCancel" ) )
                {
                    if ( UTF8RequestMsg.contains( "PlaceHolderAmount" ) )
                    {
                        // Notify listener that transaction is starting
                        if (progressListener != null) {
                            progressListener.onTransactionStarted("Processing card data collection...");
                        }
                        
                        returnMSG = dsiEMVAndroidinstance.getInstance(AppContext).CollectCardData( UTF8RequestMsg );
                        
                        // Notify listener that transaction is complete
                        if (progressListener != null) {
                            progressListener.onTransactionCompleted();
                        }
                        
                        // Add to transaction history
                        String amount = extractAmountFromRequest(UTF8RequestMsg);
                        boolean isSuccess = !returnMSG.toLowerCase().contains("error") && 
                                          !returnMSG.toLowerCase().contains("fail");
                        historyManager.addTransaction("Collect Card Data", amount, UTF8RequestMsg, returnMSG, 
                            isSuccess, "Local HTTP Client");
                    }
                    else
                    {
                        // Determine transaction type for progress message
                        String transactionType = extractTransactionTypeFromRequest(UTF8RequestMsg);
                        String progressMessage = getProgressMessage(transactionType);
                        
                        // Notify listener that transaction is starting
                        if (progressListener != null) {
                            progressListener.onTransactionStarted(progressMessage);
                        }
                        
                        returnMSG = dsiEMVAndroidinstance.getInstance(AppContext).ProcessTransaction( UTF8RequestMsg );
                        
                        // Notify listener that transaction is complete
                        if (progressListener != null) {
                            progressListener.onTransactionCompleted();
                        }
                        
                        // Add to transaction history
                        String amount = extractAmountFromRequest(UTF8RequestMsg);
                        boolean isSuccess = !returnMSG.toLowerCase().contains("error") && 
                                          !returnMSG.toLowerCase().contains("fail");
                        historyManager.addTransaction(transactionType, amount, UTF8RequestMsg, returnMSG, 
                            isSuccess, "Local HTTP Client");
                    }
                }
                else
                {
                    // Cancel is quick, no need to show progress
                    dsiEMVAndroidinstance.getInstance(AppContext).CancelRequest();
                    
                    // Add cancellation to history
                    historyManager.addTransaction("Transaction Cancel", "", UTF8RequestMsg,
                        "Transaction was cancelled by client", true, "Local HTTP Client");
                }

            } catch (Exception ex) {
                Log.i("Local Listener", ex.getMessage());
            }


            return newFixedLengthResponse(returnMSG);
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT,
                "The requested resource does not exist");
    }

    /**
     * Extract transaction type from XML request
     */
    private String extractTransactionTypeFromRequest(String xmlRequest) {
        try {
            // Check TranCode first
            if (xmlRequest.contains("<TranCode>")) {
                int start = xmlRequest.indexOf("<TranCode>") + "<TranCode>".length();
                int end = xmlRequest.indexOf("</TranCode>");
                if (start < end) {
                    String tranCode = xmlRequest.substring(start, end);
                    // Convert common transaction types to friendly names
                    switch (tranCode) {
                        case "EMVPadReset":
                        case "PadReset": return "Pad Reset";
                        case "EMVSale": return "Sale";
                        case "EMVReturn": return "Return";
                        case "EMVCredit": return "Credit";
                        case "EMVAuth": return "Authorization";
                        case "EMVCapture": return "Capture";
                        case "EMVVoid": return "Void";
                        case "CollectCardData": return "Collect Card Data";
                        default: return tranCode;
                    }
                }
            }
            
            // Check TransType as fallback
            if (xmlRequest.contains("<TransType>")) {
                int start = xmlRequest.indexOf("<TransType>") + "<TransType>".length();
                int end = xmlRequest.indexOf("</TransType>");
                if (start < end) {
                    String transType = xmlRequest.substring(start, end);
                    // Convert common transaction types to friendly names
                    switch (transType) {
                        case "EMVPadReset":
                        case "PadReset": return "Pad Reset";
                        case "EMVSale": return "Sale";
                        case "EMVReturn": return "Return";
                        case "EMVCredit": return "Credit";
                        case "EMVAuth": return "Authorization";
                        case "EMVCapture": return "Capture";
                        case "EMVVoid": return "Void";
                        default: return transType;
                    }
                }
            }
        } catch (Exception e) {
            LOG.warning("Failed to extract transaction type: " + e.getMessage());
        }
        return "Transaction";
    }

    /**
     * Extract amount from XML request
     */
    private String extractAmountFromRequest(String xmlRequest) {
        try {
            if (xmlRequest.contains("<Amount>")) {
                int start = xmlRequest.indexOf("<Amount>") + "<Amount>".length();
                int end = xmlRequest.indexOf("</Amount>");
                if (start < end) {
                    return xmlRequest.substring(start, end);
                }
            }
        } catch (Exception e) {
            LOG.warning("Failed to extract amount: " + e.getMessage());
        }
        return "";
    }
    
    /**
     * Get progress message based on transaction type
     */
    private String getProgressMessage(String transactionType) {
        switch (transactionType) {
            case "Sale":
                return "Processing sale...";
            case "Return":
                return "Processing return...";
            case "Void":
                return "Processing void...";
            case "Authorization":
                return "Processing authorization...";
            case "Capture":
                return "Processing capture...";
            case "Pad Reset":
                return "Resetting device...";
            default:
                return "Processing " + transactionType.toLowerCase() + "...";
        }
    }

}