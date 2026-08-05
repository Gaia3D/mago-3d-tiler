package com.gaia3d.basic.texture.atlas.corrector;

public class SunnyLookParameters {

    public boolean sunnyLookEnabled = false;
    public double sunnyStrength = 0.0;
    public double warmStrength = 0.0;
    public double sunnySaturationBoost = 0.0;
    public double sunnyContrastBoost = 0.0;
    public double sunnyDehazeBoost = 0.0;
    public double highlightWarmth = 0.0;
    public double shadowCoolness = 0.0;

    public static SunnyLookParameters disabled() {
        return new SunnyLookParameters();
    }

    public static SunnyLookParameters softSunnyLook() {
        SunnyLookParameters params = new SunnyLookParameters();

        params.sunnyLookEnabled = true;

        params.sunnyStrength = 0.35;
        params.warmStrength = 0.025;
        params.sunnySaturationBoost = 0.03;
        params.sunnyContrastBoost = 0.02;
        params.sunnyDehazeBoost = 0.015;
        params.highlightWarmth = 0.025;
        params.shadowCoolness = 0.010;

        return params;
    }

    public static SunnyLookParameters strongSunnyLook() {
        SunnyLookParameters params = new SunnyLookParameters();

        params.sunnyLookEnabled = true;

        params.sunnyStrength = 0.50;
        params.warmStrength = 0.035;
        params.sunnySaturationBoost = 0.05;
        params.sunnyContrastBoost = 0.03;
        params.sunnyDehazeBoost = 0.025;
        params.highlightWarmth = 0.035;
        params.shadowCoolness = 0.015;

        return params;
    }

    public static SunnyLookParameters veryStrongSunnyLook() {
        SunnyLookParameters params = new SunnyLookParameters();

        params.sunnyLookEnabled = true;

        // Intensidad general.
        // 0.65 ya es bastante fuerte para realistic mesh.
        params.sunnyStrength = 0.65;

        // Más calidez general.
        params.warmStrength = 0.045;

        // Más color, pero sin pasarse demasiado.
        params.sunnySaturationBoost = 0.065;

        // Más definición.
        params.sunnyContrastBoost = 0.045;

        // Más limpieza de neblina.
        params.sunnyDehazeBoost = 0.035;

        // Highlights más cálidos.
        params.highlightWarmth = 0.050;

        // Sombras un poco más frías para separar luz/sombra.
        params.shadowCoolness = 0.020;

        return params;
    }

    @Override
    public String toString() {
        return "SunnyLookParameters{" + "sunnyLookEnabled=" + sunnyLookEnabled + ", sunnyStrength=" + sunnyStrength + ", warmStrength=" + warmStrength + ", sunnySaturationBoost=" + sunnySaturationBoost + ", sunnyContrastBoost=" + sunnyContrastBoost + ", sunnyDehazeBoost=" + sunnyDehazeBoost + ", highlightWarmth=" + highlightWarmth + ", shadowCoolness=" + shadowCoolness + '}';
    }
}