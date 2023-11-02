package org.neo4j.spatial.algo;

import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.spatial.algo.cartesian.CartesianConvexHull;
import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.MultiPolygon;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class CartesianConvexHullTest {

    @Test
    public void convexHull() {
        Polygon.SimplePolygon testPolygon = makeSimpleTestPolygon();
        Polygon.SimplePolygon convexHull = CartesianConvexHull.convexHull(testPolygon);

        System.out.println(testPolygon.toWKT());
        System.out.println(convexHull.toWKT());

        assertThat("expected polygon of size 6", convexHull.getPoints().length, equalTo(6));
        assertThat("expected convex hull", convexHull.getPoints(), equalTo(Polygon.simple(
                Point.point(CRS.CARTESIAN, -10,-10),
                Point.point(CRS.CARTESIAN, 10,-10),
                Point.point(CRS.CARTESIAN, 10,10),
                Point.point(CRS.CARTESIAN, 0,20),
                Point.point(CRS.CARTESIAN, -10,10)
        ).getPoints()));
    }

    @Test
    public void convexHullCollinear() {
        Polygon.SimplePolygon testPolygon = makeCollinearPolygon();
        Polygon.SimplePolygon convexHull = CartesianConvexHull.convexHull(testPolygon);

        System.out.println(testPolygon.toWKT());
        System.out.println(convexHull.toWKT());

        assertThat("expected polygon of size 5", convexHull.getPoints().length, equalTo(5));
        assertThat("expected convex hull", convexHull.getPoints(), equalTo(Polygon.simple(
                Point.point(CRS.CARTESIAN, -10,-10),
                Point.point(CRS.CARTESIAN, 10,-10),
                Point.point(CRS.CARTESIAN, 10,10),
                Point.point(CRS.CARTESIAN, -10,10)
        ).getPoints()));
    }

    @Test
    public void convexHullCollinearFromReference() {
        Polygon.SimplePolygon testPolygon = makeCollinearPolygonFromReference();
        Polygon.SimplePolygon convexHull = CartesianConvexHull.convexHull(testPolygon);

        System.out.println(testPolygon.toWKT());
        System.out.println(convexHull.toWKT());

        assertThat("expected polygon of size 5", convexHull.getPoints().length, equalTo(5));
        assertThat("expected convex hull", convexHull.getPoints(), equalTo(Polygon.simple(
                Point.point(CRS.CARTESIAN, -10,-10),
                Point.point(CRS.CARTESIAN, 20,-10),
                Point.point(CRS.CARTESIAN, 10,10),
                Point.point(CRS.CARTESIAN, -10,10)
        ).getPoints()));
    }

    @Test
    public void convexHullStar() {
        Polygon.SimplePolygon testPolygon = makeStar();
        Polygon.SimplePolygon convexHull = CartesianConvexHull.convexHull(testPolygon);

        System.out.println(testPolygon.toWKT());
        System.out.println(convexHull.toWKT());

        assertThat("expected polygon of size 6", convexHull.getPoints().length, equalTo(6));
        assertThat("expected convex hull", convexHull, equalTo(Polygon.simple(
                Point.point(CRS.CARTESIAN, -4,-7),
                Point.point(CRS.CARTESIAN, 2,-8),
                Point.point(CRS.CARTESIAN, 4,0),
                Point.point(CRS.CARTESIAN, -1,3),
                Point.point(CRS.CARTESIAN, -7,0)
        )));
    }

    @Test
    public void convexHullHard() {
        Polygon.SimplePolygon testPolygon = makeHardTestPolygon();
        Polygon.SimplePolygon convexHull = CartesianConvexHull.convexHull(testPolygon);

        System.out.println(testPolygon.toWKT());
        System.out.println(convexHull.toWKT());

        assertThat("expected polygon of size 9", convexHull.getPoints().length, equalTo(9));
        assertThat("expected convex hull", convexHull, equalTo(Polygon.simple(
                Point.point(CRS.CARTESIAN, -22,-18),
                Point.point(CRS.CARTESIAN, 27,-17),
                Point.point(CRS.CARTESIAN, 30,-9),
                Point.point(CRS.CARTESIAN, 23,19),
                Point.point(CRS.CARTESIAN, -8,34),
                Point.point(CRS.CARTESIAN, -42,44),
                Point.point(CRS.CARTESIAN, -67,19),
                Point.point(CRS.CARTESIAN, -58,-16),
                Point.point(CRS.CARTESIAN, -22,-18)
        )));
    }

    @Test
    public void convexHullPrecision() {
        Polygon.SimplePolygon testPolygon = makeHighPrecisionPolygon();
        Polygon.SimplePolygon convexHull = CartesianConvexHull.convexHull(testPolygon);

        System.out.println(testPolygon.toWKT());
        System.out.println(convexHull.toWKT());

        assertThat("expected polygon of size 5", convexHull.getPoints().length, equalTo(5));
        assertThat("expected convex hull", convexHull, equalTo(Polygon.simple(
                Point.point(CRS.CARTESIAN, 0,0),
                Point.point(CRS.CARTESIAN, 1,0),
                Point.point(CRS.CARTESIAN, 1,1),
                Point.point(CRS.CARTESIAN, 0,1)
        )));
    }

    @Test
    public void shouldMakeConvexHullFromMultiPolygon() {
        MultiPolygon multiPolygon = makeMultiPolygon();
        Polygon.SimplePolygon convexHull = CartesianConvexHull.convexHull(multiPolygon);

        System.out.println(multiPolygon.toWKT());
        System.out.println(convexHull.toWKT());

        Polygon.SimplePolygon expected = Polygon.simple(
                Point.point(CRS.CARTESIAN, 2,0),
                Point.point(CRS.CARTESIAN, 0,2),
                Point.point(CRS.CARTESIAN, 0,9),
                Point.point(CRS.CARTESIAN, 1,12),
                Point.point(CRS.CARTESIAN, 4,13),
                Point.point(CRS.CARTESIAN, 6,7),
                Point.point(CRS.CARTESIAN, 6,4),
                Point.point(CRS.CARTESIAN, 5,0)
        );

        assertThat("expected polygon of size 9", convexHull.getPoints().length, equalTo(9));
        assertThat("expected convex hull", Polygon.SimplePolygon.areEqual(convexHull, expected), equalTo(true));
    }

    private MultiPolygon makeMultiPolygon() {
        Point[][] input = new Point[][]{
                {
                    Point.point(CRS.CARTESIAN, 0,2),
                    Point.point(CRS.CARTESIAN, 1,4),
                    Point.point(CRS.CARTESIAN, 0,7),
                    Point.point(CRS.CARTESIAN, 0,9),
                    Point.point(CRS.CARTESIAN, 1,12),
                    Point.point(CRS.CARTESIAN, 4,13),
                    Point.point(CRS.CARTESIAN, 6,7),
                    Point.point(CRS.CARTESIAN, 6,4),
                    Point.point(CRS.CARTESIAN, 5,3),
                    Point.point(CRS.CARTESIAN, 5,0),
                    Point.point(CRS.CARTESIAN, 2,0)
                },
                {
                    Point.point(CRS.CARTESIAN, 0.5,7),
                    Point.point(CRS.CARTESIAN, 0.5,9),
                    Point.point(CRS.CARTESIAN, 3,12),
                    Point.point(CRS.CARTESIAN, 5,7),
                    Point.point(CRS.CARTESIAN, 5,4),
                    Point.point(CRS.CARTESIAN, 2,4)
                },
                {
                    Point.point(CRS.CARTESIAN, 4.5, 4.5),
                    Point.point(CRS.CARTESIAN, 2, 5),
                    Point.point(CRS.CARTESIAN, 2, 6),
                    Point.point(CRS.CARTESIAN, 4.5, 6)
                },
                {
                    Point.point(CRS.CARTESIAN, 1.5,7.5),
                    Point.point(CRS.CARTESIAN, 2,10),
                    Point.point(CRS.CARTESIAN, 4,7)
                },
                {
                    Point.point(CRS.CARTESIAN, 2,0.6),
                    Point.point(CRS.CARTESIAN, 1,2),
                    Point.point(CRS.CARTESIAN, 4,3),
                    Point.point(CRS.CARTESIAN, 4.5,0.5)
                }
        };

        Polygon.SimplePolygon[] polygons = new Polygon.SimplePolygon[input.length];

        for (int i = 0; i < polygons.length; i++) {
            polygons[i] = Polygon.simple(input[i]);
        }

        MultiPolygon multiPolygon = new MultiPolygon();
        for (Polygon.SimplePolygon polygon : polygons) {
            multiPolygon.insertPolygon(polygon);
        }

        return multiPolygon;
    }

    private static Polygon.SimplePolygon makeSimpleTestPolygon() {
        return Polygon.simple(
                Point.point(CRS.CARTESIAN, -10,-10),
                Point.point(CRS.CARTESIAN, 10,-10),
                Point.point(CRS.CARTESIAN, 1, 0),
                Point.point(CRS.CARTESIAN, 10,10),
                Point.point(CRS.CARTESIAN, 0,20),
                Point.point(CRS.CARTESIAN, -10,10),
                Point.point(CRS.CARTESIAN, -1, 0)
        );
    }

    private static Polygon.SimplePolygon makeStar() {
        return Polygon.simple(
                Point.point(CRS.CARTESIAN, -4,-7),
                Point.point(CRS.CARTESIAN, -1,-3),
                Point.point(CRS.CARTESIAN, 2,-8),
                Point.point(CRS.CARTESIAN, 0,-2),
                Point.point(CRS.CARTESIAN, 4,-0),
                Point.point(CRS.CARTESIAN, -0,-1),
                Point.point(CRS.CARTESIAN, -1,3),
                Point.point(CRS.CARTESIAN, -2,-1),
                Point.point(CRS.CARTESIAN, -7,0),
                Point.point(CRS.CARTESIAN, -3,-3),
                Point.point(CRS.CARTESIAN, -4,-7)
        );
    }

    private static Polygon.SimplePolygon makeHardTestPolygon() {
        return Polygon.simple(
                Point.point(CRS.CARTESIAN, -12,-9),
                Point.point(CRS.CARTESIAN, 0,-7),
                Point.point(CRS.CARTESIAN, 12,-14),
                Point.point(CRS.CARTESIAN, 18,-5),
                Point.point(CRS.CARTESIAN, 2,-0),
                Point.point(CRS.CARTESIAN, 8,-6),
                Point.point(CRS.CARTESIAN, -9,-3),
                Point.point(CRS.CARTESIAN, -6,5),
                Point.point(CRS.CARTESIAN, 13,7),
                Point.point(CRS.CARTESIAN, 19,1),
                Point.point(CRS.CARTESIAN, 6,1),
                Point.point(CRS.CARTESIAN, 19,-2),
                Point.point(CRS.CARTESIAN, 19,-16),
                Point.point(CRS.CARTESIAN, 27,-17),
                Point.point(CRS.CARTESIAN, 30,-9),
                Point.point(CRS.CARTESIAN, 23,19),
                Point.point(CRS.CARTESIAN, 14,23),
                Point.point(CRS.CARTESIAN, -3,16),
                Point.point(CRS.CARTESIAN, -15,27),
                Point.point(CRS.CARTESIAN, -1,28),
                Point.point(CRS.CARTESIAN, -8,34),
                Point.point(CRS.CARTESIAN, -26,25),
                Point.point(CRS.CARTESIAN, -18,16),
                Point.point(CRS.CARTESIAN, 1,12),
                Point.point(CRS.CARTESIAN, 13,14),
                Point.point(CRS.CARTESIAN, 22,7),
                Point.point(CRS.CARTESIAN, 6,10),
                Point.point(CRS.CARTESIAN, -23,8),
                Point.point(CRS.CARTESIAN, -42,44),
                Point.point(CRS.CARTESIAN, -23,-5),
                Point.point(CRS.CARTESIAN, -67,19),
                Point.point(CRS.CARTESIAN, -58,-16),
                Point.point(CRS.CARTESIAN, -51,-1),
                Point.point(CRS.CARTESIAN, -37,-17),
                Point.point(CRS.CARTESIAN, -30,-7),
                Point.point(CRS.CARTESIAN, -22,-18),
                Point.point(CRS.CARTESIAN, -22,-10),
                Point.point(CRS.CARTESIAN, -15,-16),
                Point.point(CRS.CARTESIAN, -18,-9),
                Point.point(CRS.CARTESIAN, -12,-9)
        );
    }

    private Polygon.SimplePolygon makeCollinearPolygon() {
        return Polygon.simple(
                Point.point(CRS.CARTESIAN, -10,-10),
                Point.point(CRS.CARTESIAN, 10,-10),
                Point.point(CRS.CARTESIAN, 10,10),
                Point.point(CRS.CARTESIAN, 5,10),
                Point.point(CRS.CARTESIAN, 0,10),
                Point.point(CRS.CARTESIAN, -5,10),
                Point.point(CRS.CARTESIAN, -10,10),
                Point.point(CRS.CARTESIAN, -1, 0)
        );
    }

    private Polygon.SimplePolygon makeCollinearPolygonFromReference() {
        return Polygon.simple(
                Point.point(CRS.CARTESIAN, -10,-10),
                Point.point(CRS.CARTESIAN, 0,-10),
                Point.point(CRS.CARTESIAN, 10,-10),
                Point.point(CRS.CARTESIAN, 20, -10),
                Point.point(CRS.CARTESIAN, 10,10),
                Point.point(CRS.CARTESIAN, -10,10),
                Point.point(CRS.CARTESIAN, -1, 0)
        );
    }

    private Polygon.SimplePolygon makeHighPrecisionPolygon() {
        return Polygon.simple(
                Point.point(CRS.CARTESIAN, 0,0),
                Point.point(CRS.CARTESIAN, 1,0),
                Point.point(CRS.CARTESIAN, 1,1.0000000000000001),
                Point.point(CRS.CARTESIAN, 0,1.0000000000000001)
                );
    }
}
