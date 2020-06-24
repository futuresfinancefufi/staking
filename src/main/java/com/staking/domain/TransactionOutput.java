package com.staking.domain;

import com.staking.utils.CryptoUtils;

public class TransactionOutput {

    private String id;
    private String recipient;
    private long value;
    private String parentTransactionId;

    public TransactionOutput(String recipient, long value, String parentTransactionId) {
        this.recipient = recipient;
        this.value = value;
        this.parentTransactionId = parentTransactionId;
        this.id = CryptoUtils.applySha256((recipient==null?"": recipient)+value+parentTransactionId);
    }

    public String getId() {
        return id;
    }

    public String getRecipient() {
        return recipient;
    }

    public long getValue() {
        return value;
    }

    public String getParentTransactionId() {
        return parentTransactionId;
    }

}
