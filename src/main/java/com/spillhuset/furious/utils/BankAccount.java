package com.spillhuset.furious.utils;

import java.util.UUID;

public class BankAccount {
    private final UUID bankId;
    private final UUID playerId;
    private double balance;

    public BankAccount(UUID bankId, UUID playerId) {
        this.bankId = bankId;
        this.playerId = playerId;
        this.balance = 0.0d;
    }

    public UUID getBankId() { return bankId; }
    public UUID getPlayerId() { return playerId; }
    public double getBalance() { return balance; }

    public void setBalance(double balance) { this.balance = Math.max(0d, balance); }
    public void deposit(double amount) { this.balance = Math.max(0d, this.balance + amount); }
    public boolean withdraw(double amount) {
        if (amount <= 0d) return false;
        if (balance < amount) return false;
        balance -= amount;
        return true;
    }
}
