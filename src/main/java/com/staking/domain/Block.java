package com.staking.domain;

import java.util.List;

public class Block {


    private final List<String> transactions;
    private final String previousHash;
    private final String hash;
    private final long height;
    private final long timestamp;


    public Block(List<String> transactions, String previousHash, String hash, long height, long timestamp) {
        this.transactions = transactions;
        this.previousHash = previousHash;
        this.hash = hash;
        this.height = height;
        this.timestamp = timestamp;
    }

    public List<String> getTransactions() {
        return transactions;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public String getHash() {
        return hash;
    }

    public long getHeight() {
        return height;
    }

    public long getTimestamp() {
        return timestamp;
    }


}
