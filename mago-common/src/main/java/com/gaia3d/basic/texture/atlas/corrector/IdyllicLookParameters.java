package com.gaia3d.basic.texture.atlas.corrector;

public class IdyllicLookParameters {

    public boolean idyllicLookEnabled = false;
    public double idyllicStrength = 0.0;
    public double warmBoost = 0.0;
    public double saturationBoost = 0.0;
    public double contrastBoost = 0.0;
    public double dehazeBoost = 0.0;
    public double midtoneLift = 0.0;
    public double highlightSoftness = 0.0;
    public double greenBoost = 0.0;
    public double greenLift = 0.0;
    public double skyBlueBoost = 0.0;
    public double shadowSoftness = 0.0;

    public static IdyllicLookParameters disabled() {
        return new IdyllicLookParameters();
    }

    public static IdyllicLookParameters ultraFreshLushGreenIdyllicLook() {
        IdyllicLookParameters params = new IdyllicLookParameters();

        params.idyllicLookEnabled = true;

        params.idyllicStrength = 0.82;
        params.warmBoost = 0.002;
        params.saturationBoost = 0.145;
        params.contrastBoost = 0.12;
        params.dehazeBoost = 0.045;
        params.midtoneLift = 0.080;
        params.highlightSoftness = 0.040;
        params.greenBoost = 0.115;
        params.greenLift = 0.25;
        params.skyBlueBoost = 0.050;
        params.shadowSoftness = 0.045;

        return params;
    }

    @Override
    public String toString() {
        return "IdyllicLookParameters{" + "idyllicLookEnabled=" + idyllicLookEnabled + ", idyllicStrength=" + idyllicStrength + ", warmBoost=" + warmBoost + ", saturationBoost=" + saturationBoost + ", contrastBoost=" + contrastBoost + ", dehazeBoost=" + dehazeBoost + ", midtoneLift=" + midtoneLift + ", highlightSoftness=" + highlightSoftness + ", greenBoost=" + greenBoost + ", greenLift=" + greenLift + ", skyBlueBoost=" + skyBlueBoost + ", shadowSoftness=" + shadowSoftness + '}';
    }
}