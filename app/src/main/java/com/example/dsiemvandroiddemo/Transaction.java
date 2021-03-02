package com.example.dsiemvandroiddemo;

public class Transaction {
    private String MerchantID;
    private String UserTrace;
    private String POSPackageID;
    private String TranType;
    private String TranCode;
    private String SecureDevice;
    private String ComPort;
    private String InvoiceNo;
    private String RefNo;
    private String AuthCode;
    private Amount Amount;
    private String SequenceNo;
    private String AcqRefData;
    private String ProcessData;
    private String RecordNo;
    private String Frequency;
    private String BluetoothDeviceName;
    private String OperationMode;
    private String PinPadIpAddress;
    private String PinPadIpPort;

    public Transaction(String merchantID, String userTrace, String pOSPackageID, String tranCode, String secureDevice, String invoiceNo,  Amount amount, String sequenceNo,  String bluetoothDeviceName, String operationMode, String recordNo, String refNo){
        this.MerchantID = merchantID;
        this.UserTrace = userTrace;
        this.POSPackageID = pOSPackageID;
        this.TranCode = tranCode;
        this.SecureDevice = secureDevice;
        this.InvoiceNo = invoiceNo;
        this.Amount = amount;
        this.SequenceNo = sequenceNo;
        this.BluetoothDeviceName = bluetoothDeviceName;
        this.OperationMode = operationMode;
        this.RecordNo = recordNo;
        this.RefNo = refNo;
    }
    public Transaction(String merchantID, String userTrace, String pOSPackageID, String tranCode, String secureDevice, String invoiceNo,  Amount amount, String sequenceNo, String operationMode, String recordNo, String refNo){
        this.MerchantID = merchantID;
        this.UserTrace = userTrace;
        this.POSPackageID = pOSPackageID;
        this.TranCode = tranCode;
        this.SecureDevice = secureDevice;
        this.InvoiceNo = invoiceNo;
        this.Amount = amount;
        this.SequenceNo = sequenceNo;
        this.OperationMode = operationMode;
        this.RecordNo = recordNo;
        this.RefNo = refNo;
    }

    public Transaction(String merchantID, String userTrace, String pOSPackageID, String tranCode, String secureDevice, String invoiceNo,  Amount amount, String sequenceNo, String operationMode, String recordNo, String refNo, String pinPadIpAddress, String pinPadIpPort){
        this.MerchantID = merchantID;
        this.UserTrace = userTrace;
        this.POSPackageID = pOSPackageID;
        this.TranCode = tranCode;
        this.SecureDevice = secureDevice;
        this.InvoiceNo = invoiceNo;
        this.Amount = amount;
        this.SequenceNo = sequenceNo;
        this.OperationMode = operationMode;
        this.RecordNo = recordNo;
        this.RefNo = refNo;
        this.PinPadIpAddress = pinPadIpAddress;
        this.PinPadIpPort = pinPadIpPort;
    }

    public String getMerchantID() {
        return MerchantID;
    }

    public void setMerchantID(String merchantID) {
        this.MerchantID = merchantID;
    }

    public String getUserTrace() {
        return UserTrace;
    }

    public void setUserTrace(String userTrace) {
        this.UserTrace = userTrace;
    }

    public String getPOSPackageID() {
        return POSPackageID;
    }

    public void setPOSPackageID(String POSPackageID) {
        this.POSPackageID = POSPackageID;
    }

    public String getTranType() {
        return TranType;
    }

    public void setTranType(String tranType) {
        this.TranType = tranType;
    }

    public String getTranCode() {
        return TranCode;
    }

    public void setTranCode(String tranCode) {
        this.TranCode = tranCode;
    }

    public String getSecureDevice() {
        return SecureDevice;
    }

    public void setSecureDevice(String secureDevice) {
        this.SecureDevice = secureDevice;
    }

    public String getComPort() {
        return ComPort;
    }

    public void setComPort(String comPort) {
        this.ComPort = comPort;
    }

    public String getInvoiceNo() {
        return InvoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.InvoiceNo = invoiceNo;
    }

    public String getRefNo() {
        return RefNo;
    }

    public void setRefNo(String refNo) {
        this.RefNo = refNo;
    }

    public String getAuthCode() {
        return AuthCode;
    }

    public void setAuthCode(String authCode) {
        this.AuthCode = authCode;
    }

    public Amount getAmount() {
        return Amount;
    }

    public void setAmount(Amount amount) {
        this.Amount = amount;
    }

    public String getSequenceNo() {
        return SequenceNo;
    }

    public void setSequenceNo(String sequenceNo) {
        this.SequenceNo = sequenceNo;
    }

    public String getAcqRefData() {
        return AcqRefData;
    }

    public void setAcqRefData(String acqRefData) {
        this.AcqRefData = acqRefData;
    }

    public String getProcessData() {
        return ProcessData;
    }

    public void setProcessData(String processData) {
        this.ProcessData = processData;
    }

    public String getRecordNo() {
        return RecordNo;
    }

    public void setRecordNo(String recordNo) {
        this.RecordNo = recordNo;
    }

    public String getFrequency() {
        return Frequency;
    }

    public void setFrequency(String frequency) {
        this.Frequency = frequency;
    }

    public String getBluetoothDeviceName() {
        return BluetoothDeviceName;
    }

    public void setBluetoothDeviceName(String bluetoothDeviceName) {
        this.BluetoothDeviceName = bluetoothDeviceName;
    }

    public String getOperationMode() {
        return OperationMode;
    }

    public void setOperationMode(String operationMode) {
        this.OperationMode = operationMode;
    }

    public String getPinPadIpAddress() {
        return PinPadIpAddress;
    }

    public void setPinPadIpAddress(String pinPadIpAddress) {
        PinPadIpAddress = pinPadIpAddress;
    }
}
