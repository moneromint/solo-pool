package com.moneromint.solo.stratum.message;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class StratumSubmitParams extends StratumRequestParams {
    private String nonce;
    private String result;
    @JsonProperty("job_id")
    private String jobId;

    public StratumSubmitParams() {
    }

    public StratumSubmitParams(String nonce, String result, String jobId) {
        this.nonce = nonce;
        this.result = result;
        this.jobId = jobId;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    @Override
    public StratumMethod getMethod() {
        return StratumMethod.SUBMIT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StratumSubmitParams that = (StratumSubmitParams) o;
        return Objects.equals(nonce, that.nonce) &&
                Objects.equals(result, that.result) &&
                Objects.equals(jobId, that.jobId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nonce, result, jobId);
    }
}
