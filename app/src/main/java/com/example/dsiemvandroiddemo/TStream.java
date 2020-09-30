package com.example.dsiemvandroiddemo;

public class TStream {

    private Transaction Transaction;

    public TStream( Transaction transaction){
        this.Transaction = transaction;
    }

    public com.example.dsiemvandroiddemo.Transaction getTransaction() {
        return Transaction;
    }

    public void setTransaction(com.example.dsiemvandroiddemo.Transaction transaction) {
        Transaction = transaction;
    }
}
