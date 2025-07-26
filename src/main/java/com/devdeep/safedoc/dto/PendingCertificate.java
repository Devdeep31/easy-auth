package com.devdeep.safedoc.dto;


import java.io.Serializable;

public class PendingCertificate implements Serializable {
    private String certificateId;
    private int attemptCount;

    public PendingCertificate(String certificateId, int attemptCount) {
        this.certificateId = certificateId;
        this.attemptCount = attemptCount;
    }

    public String getCertificateId() {
        return certificateId;
    }

    public void setCertificateId(String certificateId) {
        this.certificateId = certificateId;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }
}