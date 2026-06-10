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

    // Hace azules/cian un poco más bonitos, pero con cuidado.
    public double skyBlueBoost = 0.0;

    // Suaviza sombras para look más amable.
    public double shadowSoftness = 0.0;

    public static IdyllicLookParameters disabled() {
        return new IdyllicLookParameters();
    }

    public static IdyllicLookParameters softIdyllicLook() {
        IdyllicLookParameters params = new IdyllicLookParameters();

        params.idyllicLookEnabled = true;
        params.idyllicStrength = 0.35;

        params.warmBoost = 0.025;
        params.saturationBoost = 0.045;
        params.contrastBoost = 0.020;
        params.dehazeBoost = 0.015;
        params.midtoneLift = 0.020;
        params.highlightSoftness = 0.020;
        params.greenBoost = 0.025;
        params.skyBlueBoost = 0.015;
        params.shadowSoftness = 0.015;

        return params;
    }

    public static IdyllicLookParameters strongIdyllicLook() {
        IdyllicLookParameters params = new IdyllicLookParameters();

        params.idyllicLookEnabled = true;
        params.idyllicStrength = 0.55;

        params.warmBoost = 0.040;
        params.saturationBoost = 0.075;
        params.contrastBoost = 0.035;
        params.dehazeBoost = 0.030;
        params.midtoneLift = 0.035;
        params.highlightSoftness = 0.030;
        params.greenBoost = 0.045;
        params.skyBlueBoost = 0.025;
        params.shadowSoftness = 0.025;

        return params;
    }

    public static IdyllicLookParameters fantasyIdyllicLook() {
        IdyllicLookParameters params = new IdyllicLookParameters();

        params.idyllicLookEnabled = true;
        params.idyllicStrength = 0.75;

        params.warmBoost = 0.055;
        params.saturationBoost = 0.110;
        params.contrastBoost = 0.050;
        params.dehazeBoost = 0.045;
        params.midtoneLift = 0.050;
        params.highlightSoftness = 0.040;
        params.greenBoost = 0.070;
        params.skyBlueBoost = 0.040;
        params.shadowSoftness = 0.035;

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
                ", skyBlueBoost=" + skyBlueBoost +
                ", shadowSoftness=" + shadowSoftness +
                '}';
    }

    public static IdyllicLookParameters cleanIdyllicLook() {
        IdyllicLookParameters params = new IdyllicLookParameters();

        params.idyllicLookEnabled = true;
        params.idyllicStrength = 0.55;

        // Menos amarillo que strongIdyllicLook.
        params.warmBoost = 0.010;

        // Mantiene color bonito sin calentar demasiado.
        params.saturationBoost = 0.075;

        // Definición agradable.
        params.contrastBoost = 0.035;

        // Limpieza de neblina.
        params.dehazeBoost = 0.030;

        // Luminosidad en medios tonos.
        params.midtoneLift = 0.035;

        // Menos calidez en blancos/highlights.
        params.highlightSoftness = 0.030;

        // Vegetación bonita.
        params.greenBoost = 0.045;

        // Azules/cian un poco más limpios.
        params.skyBlueBoost = 0.030;

        // Sombras suaves.
        params.shadowSoftness = 0.025;

        return params;
    }

    public static IdyllicLookParameters vividCleanIdyllicLook() {
        IdyllicLookParameters params = new IdyllicLookParameters();

        params.idyllicLookEnabled = true;

        // Un poco más fuerte que cleanIdyllicLook, pero sin ir a fantasía.
        params.idyllicStrength = 0.65;

        // Mantener bajo para no amarillear.
        params.warmBoost = 0.014;

        // Más viveza/color.
        params.saturationBoost = 0.105;

        // Un poco más de definición.
        params.contrastBoost = 0.045;

        // Más limpieza de neblina.
        params.dehazeBoost = 0.035;

        // Más luz en medios tonos: aquí está el efecto "más iluminado".
        params.midtoneLift = 0.055;

        // Suaviza highlights para evitar blancos quemados.
        params.highlightSoftness = 0.040;

        // Vegetación más viva.
        params.greenBoost = 0.060;

        // Azules/cian más limpios y agradables.
        params.skyBlueBoost = 0.040;

        // Sombras amables, no demasiado duras.
        params.shadowSoftness = 0.030;

        return params;
    }

    public static IdyllicLookParameters vividFreshIdyllicLook() {
        IdyllicLookParameters params = new IdyllicLookParameters();

        params.idyllicLookEnabled = true;

        // Un poco fuerte, pero todavía bonito.
        params.idyllicStrength = 0.72;

        // Menos amarillo.
        params.warmBoost = 0.008;

        // Más viveza general.
        params.saturationBoost = 0.125;

        // Un poco más de definición.
        params.contrastBoost = 0.050;

        // Limpia bastante la neblina.
        params.dehazeBoost = 0.040;

        // Más luminosidad en medios tonos.
        params.midtoneLift = 0.065;

        // Suaviza blancos para que no se quemen.
        params.highlightSoftness = 0.040;

        // Vegetación más viva.
        params.greenBoost = 0.070;

        // Azules/cian más bonitos y frescos.
        params.skyBlueBoost = 0.050;

        // Sombras agradables.
        params.shadowSoftness = 0.030;

        return params;
    }

    public static IdyllicLookParameters ultraFreshLushGreenIdyllicLook() {
        IdyllicLookParameters params = new IdyllicLookParameters();

        params.idyllicLookEnabled = true;

        params.idyllicStrength = 0.82;

        // Muy poco amarillo.
        params.warmBoost = 0.002;

        // Más viveza general.
        params.saturationBoost = 0.145;

        // Un poco más de definición.
        params.contrastBoost = 0.12;

        // Limpieza de neblina.
        params.dehazeBoost = 0.045;

        // Más luz en medios tonos.
        params.midtoneLift = 0.080;

        // Suaviza blancos.
        params.highlightSoftness = 0.040;

        // Vegetación claramente más viva.
        params.greenBoost = 0.105;

        // Azules limpios, pero sin robar protagonismo al verde.
        params.skyBlueBoost = 0.050;

        // Sombras agradables para que el follaje no se vea duro.
        params.shadowSoftness = 0.045;

        return params;
    }
}