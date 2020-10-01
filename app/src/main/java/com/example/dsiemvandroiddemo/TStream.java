package com.example.dsiemvandroiddemo;

public class TStream {

    private Transaction Transaction;
    private Admin Admin;

    public TStream( Transaction transaction){
        this.Transaction = transaction;
    }
    public TStream( Admin admin){
        this.Admin = admin;
    }


    public com.example.dsiemvandroiddemo.Admin getAdmin() {
        return Admin;
    }

    public void setAdmin(com.example.dsiemvandroiddemo.Admin admin) {
        Admin = admin;
    }

    public com.example.dsiemvandroiddemo.Transaction getTransaction() {
        return Transaction;
    }

    public void setTransaction(com.example.dsiemvandroiddemo.Transaction transaction) {
        Transaction = transaction;
    }
}
