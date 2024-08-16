package com.gaia3d.converter.geometry;

import org.joml.Vector2d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


class InnerRingRemoverTest {

    @Test
    void crossSame() {
        Vector2d a = new Vector2d(73.7843483535828,18.62558007045379);
        Vector2d b = new Vector2d(73.7843483535828,18.62558007045379);
        Vector2d c = new Vector2d(73.78440228477372, 18.6259135389622);

        InnerRingRemover innerRingRemover = new InnerRingRemover();
        double result = innerRingRemover.cross(a, b, c);
        System.out.println(result);
        // 2.3946318491139483E-11
        //0.000000000023946318491139483
    }

    @Test
    void crossSame2() {
        Vector2d a = new Vector2d(73.7843483535828,18.62558007045379);
        Vector2d b = new Vector2d(73.78440228477372, 18.6259135389622);
        Vector2d c = new Vector2d(73.7843483535828,18.62558007045379);

        InnerRingRemover innerRingRemover = new InnerRingRemover();
        double result = innerRingRemover.cross(a, b, c);
        System.out.println(result);
        // 2.3946318491139483E-11
        //0.000000000023946318491139483

    }

    @Test
    void crossSame3() {
        Vector2d a = new Vector2d(73.7843483535828,18.62558007045379);
        Vector2d b = new Vector2d(73.7843483535828,18.62558007045379);
        Vector2d c = new Vector2d(73.7843483535828,18.62558007045379);

        InnerRingRemover innerRingRemover = new InnerRingRemover();
        double result = innerRingRemover.cross(a, b, c);
        System.out.println(result);
        // 2.3946318491139483E-11
        //0.000000000023946318491139483

    }

    @Test
    void cross() {
        Vector2d a = new Vector2d(73.7843483535828,18.62558007045379);
        Vector2d b = new Vector2d(73.78435220006716,18.62560341010312);
        Vector2d c = new Vector2d(73.78440228477372, 18.6259135389622);

        InnerRingRemover innerRingRemover = new InnerRingRemover();
        double result = innerRingRemover.cross(a, b, c);
        System.out.println(result);
        // 2.3946318491139483E-11
        //0.000000000023946318491139483

    }

    @Test
    void cross2() {
//        73.78440228477372	18.6259135389622
//        73.78441732294155	18.626005734391615
//        73.78467447041096	18.62596769054658



        Vector2d a = new Vector2d(73.78291833506383,	18.62630056325255);
        Vector2d b = new Vector2d(73.78291229,	18.626253743514127);
        Vector2d c = new Vector2d(73.78290929128853,	18.62623056109944);

//        73.78291833506383	18.62630056325255
//
//        73.78291229	18.626253743514127
//
//        73.78290929128853	18.62623056109944
//
//        73.78290613806246	18.626206152324304

        //-2.3534644180789321E-4
        //-0.00023534644180789321

//
//
//        Vector2d a = new Vector2d(73.78291229,	18.6259135389622);
//        Vector2d b = new Vector2d(73.78441732294155,	18.626005734391615);
//        Vector2d c = new Vector2d(73.78467447041096,	18.62596769054658);

        InnerRingRemover innerRingRemover = new InnerRingRemover();
        double result = innerRingRemover.cross(a, b, c);
        System.out.println(result);

        // -2.4279931091618658E-8
        // -0.000000024279931091618658

        // 2.3946318491139483E-11
        //0.000000000023946318491139483

    }

    @Test
    void isIntersect() {
        Vector2d a = new Vector2d(0, 0);
        Vector2d b = new Vector2d(1, 1);

        Vector2d c = new Vector2d(0, 1);
        Vector2d d = new Vector2d(1, 0);


        InnerRingRemover innerRingRemover = new InnerRingRemover();
        //boolean result = innerRingRemover.isIntersect(a, b, c, d);

        //System.out.println(result);
    }
}