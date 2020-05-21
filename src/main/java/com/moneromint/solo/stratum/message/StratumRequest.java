package com.moneromint.solo.stratum.message;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class StratumRequest<T extends StratumRequestParams> extends StratumMessage {
    @JsonProperty("id")
    private Object id;

    @JsonProperty("params")
    private T params;

    public StratumRequest() {
    }

    public StratumRequest(Object id, T params) {
        this.id = id;
        this.params = params;
    }

    @JsonProperty("method")
    public StratumMethod getMethod() {
        return params.getMethod();
    }

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }


    public T getParams() {
        return params;
    }

    public void setParams(T params) {
        this.params = params;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StratumRequest<?> that = (StratumRequest<?>) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(params, that.params);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, params);
    }
}
