package com.gaia3d.basic.texture.atlas.corrector;

public class IdyllicLookParameters {

    public boolean idyllicLookEnabled = false;

    // Intensidad general del look.
    // 0.0 = apagado, 1.0 = completo.
    public double idyllicStrength = 0.0;

    // Calidez global suave.
    public double warmBoost = 0.0;

    // Saturación más estética.
    public double saturationBoost = 0.0;

    // Contraste suave tipo imagen publicitaria.
    public double contrastBoost = 0.0;

    // Limpieza de neblina / grises.
    public double dehazeBoost = 0.0;

    // Levanta un poco los tonos medios para imagen más luminosa.
    public double midtoneLift = 0.0;

    // Protege blancos para que no se quemen.
    public double highlightSoftness = 0.0;

    // Hace vegetación un poco más viva.
    public double greenBoost = 0.0;
    public double greenLift = 0.0;

    // Hace azules/cian un poco más bonitos, pero con cuidado.
    public double skyBlueBoost = 0.0;

    // Suaviza sombras para look más amable.
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
        return "IdyllicLookParameters{" +
                "idyllicLookEnabled=" + idyllicLookEnabled +
                ", idyllicStrength=" + idyllicStrength +
                ", warmBoost=" + warmBoost +
                ", saturationBoost=" + saturationBoost +
                ", contrastBoost=" + contrastBoost +
                ", dehazeBoost=" + dehazeBoost +
                ", midtoneLift=" + midtoneLift +
                ", highlightSoftness=" + highlightSoftness +
                ", greenBoost=" + greenBoost +
                ", greenLift=" + greenLift +
                ", skyBlueBoost=" + skyBlueBoost +
                ", shadowSoftness=" + shadowSoftness +
                '}';
    }
}