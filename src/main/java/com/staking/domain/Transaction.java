package com.staking.domain;

import java.nio.file.Path;
import java.util.List;

import static com.staking.resources.StakingResources.STORAGE_PREFIX;

public class Transaction {

    private final long height;
    private final String hash;
    private final String from;
    private final String to;
    private final long amount;
    private String signature;
    private final long timestamp;

    private final List<String> inputs;
    private final List<TransactionOutput> outputs;

    public Transaction(String hash, String from, String to, long amount, List<String> inputs, List<TransactionOutput> outputs, String signature, long timestamp, long height) {
        this.hash = hash;
        this.from = from;
        this.to = to;
        this.amount = amount;
        this.inputs = inputs;
        this.signature = signature;
        this.timestamp = timestamp;
        this.outputs = outputs;
        this.height = height;
    }


    public String getHash() {
        return hash;
    }

    public String getFrom() { return from;
    }

    public String getTo() { return to;
    }

    public long getAmount() {
        return amount;
    }

    public List<String> getInputs() {
        return inputs;
    }


    public long getTimestamp() {
        return timestamp;
    }

    public String getSignature() {
        return signature;
    }

    public List<TransactionOutput> getOutputs() {
        return outputs;
    }

    public long getHeight() {
        return height;
    }

    public Path getStoragePath(){
        return Path.of(STORAGE_PREFIX+"/transactions/"+hash);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "height=" + height +
                ", hash='" + hash + '\'' +
                ", from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", amount=" + amount +
                ", timestamp=" + timestamp +
                '}';
    }
}
