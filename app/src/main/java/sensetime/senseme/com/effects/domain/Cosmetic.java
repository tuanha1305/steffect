package sensetime.senseme.com.effects.domain;

import java.io.Serializable;

/**
 * Created by liupan on 17/8/15.
 */

public class Cosmetic implements Serializable {
    String cosmeticId ;
    String cosmeticLogo;
    String cosmeticDesc;
    String rgbColor;
    String materialImage;

    public String getMaterialImage() {
        return materialImage;
    }
    public void setMaterialImage(String materialImage) {
        this.materialImage = materialImage;
    }
    public void setRgbColor(String rgbColor) {
        this.rgbColor = rgbColor;
    }

    public String getRgbColor() {
        return rgbColor;
    }

    public String getCosmeticId() {
        return cosmeticId;
    }

    public void setCosmeticId(String cosmeticId) {
        this.cosmeticId = cosmeticId;
    }

    public String getCosmeticLogo() {
        return cosmeticLogo;
    }

    public void setCosmeticLogo(String cosmeticLogo) {
        this.cosmeticLogo = cosmeticLogo;
    }

    public String getCosmeticDesc() {
        return cosmeticDesc;
    }

    public void setCosmeticDesc(String cosmeticDesc) {
        this.cosmeticDesc = cosmeticDesc;
    }
}
