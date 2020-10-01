package com.example.dsiemvandroiddemo;

public class Admin {
    private String MerchantID;
    private String UserTrace;
    private String POSPackageID;
    private String TranCode;
    private String SecureDevice;
    private String ComPort;
    private String SequenceNo;
    private String BluetoothDeviceName;
    private String OperationMode;

    public Admin(String merchantID, String userTrace, String pOSPackageID, String tranCode, String secureDevice, String sequenceNo, String bluetoothDeviceName, String operationMode){
        this.MerchantID = merchantID;
        this.UserTrace = userTrace;
        this.POSPackageID = pOSPackageID;
        this.TranCode = tranCode;
        this.SecureDevice = secureDevice;
        this.ComPort = "1";
        this.SequenceNo = sequenceNo;
        this.BluetoothDeviceName = bluetoothDeviceName;
        this.OperationMode = operationMode;
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

    public String getSequenceNo() {
        return SequenceNo;
    }

    public void setSequenceNo(String sequenceNo) {
        this.SequenceNo = sequenceNo;
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
}
