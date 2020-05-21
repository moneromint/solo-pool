package com.moneromint.solo.stratum.message;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum StratumMethod {
    @JsonProperty("login")
    LOGIN,

    @JsonProperty("submit")
    SUBMIT,
}
