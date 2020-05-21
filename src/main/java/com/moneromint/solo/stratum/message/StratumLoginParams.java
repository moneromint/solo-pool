package com.moneromint.solo.stratum.message;

import java.util.Objects;

public class StratumLoginParams extends StratumRequestParams {
    private String login;
    private String pass;

    public StratumLoginParams() {
    }

    public StratumLoginParams(String login, String pass) {
        this.login = login;
        this.pass = pass;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    @Override
    public StratumMethod getMethod() {
        return StratumMethod.LOGIN;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StratumLoginParams that = (StratumLoginParams) o;
        return Objects.equals(login, that.login) &&
                Objects.equals(pass, that.pass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(login, pass);
    }
}
