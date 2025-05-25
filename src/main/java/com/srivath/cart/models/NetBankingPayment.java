package com.srivath.cart.models;

public class NetBankingPayment extends Payment{
    String bankName;
    String accountNumber;
    String ifscCode;

    public NetBankingPayment() {
    }

    public NetBankingPayment(long amount, String paymentMethod, String bankName, String accountNumber, String ifscCode) {
        super(amount, paymentMethod);
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.ifscCode = ifscCode;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getIfscCode() {
        return ifscCode;
    }

    public void setIfscCode(String ifscCode) {
        this.ifscCode = ifscCode;
    }
}
