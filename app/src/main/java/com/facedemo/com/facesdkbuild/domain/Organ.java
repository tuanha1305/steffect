package com.facedemo.com.facesdkbuild.domain;

import java.io.Serializable;
import java.util.List;

/**
 * Created by liupan on 17/8/15.
 */

public class Organ implements Serializable {
    String organName;
    String organLogo;
    String organDesc;
    String organNameMini;
    List<Cosmetic> cosmeticList;

    public String getOrganName() {
        return organName;
    }

    public void setOrganName(String organName) {
        this.organName = organName;
    }

    public String getOrganLogo() {
        return organLogo;
    }

    public void setOrganLogo(String organLogo) {
        this.organLogo = organLogo;
    }

    public String getOrganDesc() {
        return organDesc;
    }

    public void setOrganDesc(String organDesc) {
        this.organDesc = organDesc;
    }

    public String getOrganNameMini() {
        return organNameMini;
    }

    public void setOrganNameMini(String organNameMini) {
        this.organNameMini = organNameMini;
    }

    public List<Cosmetic> getCosmeticList() {
        return cosmeticList;
    }

    public void setCosmeticList(List<Cosmetic> cosmeticList) {
        this.cosmeticList = cosmeticList;
    }
}
