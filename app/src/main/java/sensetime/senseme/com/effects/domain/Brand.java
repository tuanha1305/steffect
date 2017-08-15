package sensetime.senseme.com.effects.domain;

import java.io.Serializable;
import java.util.List;

/**
 * Created by liupan on 17/8/15.
 */

public class Brand implements Serializable {
    String brandDesc;
    String brandLogo;
    String brandName;
    String brandNameMini;
    List<Organ> brandList;

    public String getBrandDesc() {
        return brandDesc;
    }

    public void setBrandDesc(String brandDesc) {
        this.brandDesc = brandDesc;
    }

    public String getBrandLogo() {
        return brandLogo;
    }

    public void setBrandLogo(String brandLogo) {
        this.brandLogo = brandLogo;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getBrandNameMini() {
        return brandNameMini;
    }

    public void setBrandNameMini(String brandNameMini) {
        this.brandNameMini = brandNameMini;
    }

    public List<Organ> getBrandList() {
        return brandList;
    }

    public void setBrandList(List<Organ> brandList) {
        this.brandList = brandList;
    }
}
