package com.example.dsiemvandroiddemo.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Represents a single transaction in the history
 */
public class TransactionHistoryItem {
    private String id;
    private String transactionType;
    private String amount;
    private String request;
    private String response;
    private boolean isSuccess;
    private long timestamp;
    private String deviceName;
    
    public TransactionHistoryItem(String transactionType, String amount, String request,
                                 String response, boolean isSuccess, String deviceName) {
        this.id = generateId();
        this.transactionType = transactionType;
        this.amount = amount;
        this.request = request;
        this.response = response;
        this.isSuccess = isSuccess;
        this.timestamp = System.currentTimeMillis();
        this.deviceName = deviceName;
    }
    
    private String generateId() {
        return "TXN_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
    
    public String getFormattedTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
    
    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
    
    public String getDisplayTitle() {
        // Don't show amount in title - it will be shown in the detailed view
        // This prevents showing incorrect amounts from input fields
        return transactionType;
    }
    
    public String getDisplayTitleWithAmount(String authorizedAmount) {
        if (authorizedAmount != null && !authorizedAmount.isEmpty()) {
            return transactionType + " - $" + authorizedAmount;
        }
        return transactionType;
    }
    
    public String getStatusText() {
        return isSuccess ? "Success" : "Failed";
    }
    
    // Getters and setters
    public String getId() { return id; }
    
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    
    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }
    
    public String getRequest() { return request; }
    public void setRequest(String request) { this.request = request; }
    
    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }
    
    public boolean isSuccess() { return isSuccess; }
    public void setSuccess(boolean success) { isSuccess = success; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
}