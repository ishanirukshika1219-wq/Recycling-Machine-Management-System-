
package com.example.myproject.dto;

public class TransactionActionRequest {

    private Integer staffId;
    private String reason;

    public TransactionActionRequest() {
    }

    public Integer getStaffId() {
        return staffId;
    }

    public void setStaffId(Integer staffId) {
        this.staffId = staffId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
