package org.neo4j.spatial.algo;

import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class WithinTest {
    @Test
    public void shouldBeCompletelyWithinSquare() {
        Polygon.SimplePolygon inner = makeSquare(new double[]{-10, -10}, 20);
        Polygon.SimplePolygon outer = makeSquare(new double[]{-15, -15}, 30);
        assertThat(Within.within(outer, inner), equalTo(true));
        assertThat(Within.within(inner, outer), equalTo(false));
    }

    @Test
    public void shouldBeTouchingWithinSquare() {
        Polygon.SimplePolygon inner = makeSquare(new double[]{-15, -10}, 20);
        Polygon.SimplePolygon outer = makeSquare(new double[]{-15, -15}, 30);
        assertThat(Within.within(outer, inner), equalTo(false));
        assertThat(Within.within(inner, outer), equalTo(false));
    }

    @Test
    public void shouldBeWithinSquare() {
        Polygon.SimplePolygon square = makeSquare(new double[]{-10, -10}, 20);
        assertThat(Within.within(square, Point.point(0, 0)), equalTo(true));
        assertThat(Within.within(square, Point.point(-20, 0)), equalTo(false));
        assertThat(Within.within(square, Point.point(20, 0)), equalTo(false));
        assertThat(Within.within(square, Point.point(0, -20)), equalTo(false));
        assertThat(Within.within(square, Point.point(0, 20)), equalTo(false));
    }

    @Ignore
    // TODO still some bugs with touching logic
    public void shouldBeTouchingSquare() {
        Polygon.SimplePolygon square = makeSquare(new double[]{-10, -10}, 20);
        for (boolean touching : new boolean[]{false, true}) {
            assertThat(Within.within(square, Point.point(-10, -20), touching), equalTo(false));
            assertThat(Within.within(square, Point.point(-10, -10), touching), equalTo(touching));
            assertThat(Within.within(square, Point.point(-10, 0), touching), equalTo(touching));
            assertThat(Within.within(square, Point.point(-10, 10), touching), equalTo(touching));
            assertThat(Within.within(square, Point.point(-10, 20), touching), equalTo(false));
            assertThat(Within.within(square, Point.point(-20, -10), touching), equalTo(false));
            assertThat(Within.within(square, Point.point(-10, -10), touching), equalTo(touching));
            assertThat(Within.within(square, Point.point(0, -10), touching), equalTo(touching));
            assertThat(Within.within(square, Point.point(10, -10), touching), equalTo(touching));
            assertThat(Within.within(square, Point.point(20, -10), touching), equalTo(false));
        }
    }

    private static double[] move(double[] coords, int dim, double move) {
        double[] moved = Arrays.copyOf(coords, coords.length);
        moved[dim] += move;
        return moved;
    }

    private static Polygon.SimplePolygon makeSquare(double[] bottomLeftCoords, double width) {
        Point bottomLeft = Point.point(bottomLeftCoords);
        Point bottomRight = Point.point(move(bottomLeftCoords, 0, width));
        Point topRight = Point.point(move(bottomRight.getCoordinate(), 1, width));
        Point topLeft = Point.point(move(topRight.getCoordinate(), 0, -width));
        return Polygon.simple(bottomLeft, bottomRight, topRight, topLeft);
    }

}
