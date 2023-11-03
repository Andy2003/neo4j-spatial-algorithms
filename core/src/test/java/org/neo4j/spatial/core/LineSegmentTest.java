package org.neo4j.spatial.core;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class LineSegmentTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldAcceptValidPoints() {
        LineSegment ls = LineSegment.lineSegment(Point.point(CRS.CARTESIAN, 0,1), Point.point(CRS.CARTESIAN, 5,3));
        assertThat(ls.getPoints(), equalTo(new Point[]{Point.point(CRS.CARTESIAN, 0,1), Point.point(CRS.CARTESIAN, 5,3)}));
    }

    @Test
    public void shouldSeeEqualLineSegments() {
        LineSegment a = LineSegment.lineSegment(Point.point(CRS.CARTESIAN, 0,1), Point.point(CRS.CARTESIAN, 5,3));
        LineSegment b = LineSegment.lineSegment(Point.point(CRS.CARTESIAN, 0,1), Point.point(CRS.CARTESIAN, 5,3));
        assertThat(a, equalTo(b));
    }

    @Test
    public void shouldSeeEqualLineSegmentsSwitched() {
        LineSegment a = LineSegment.lineSegment(Point.point(CRS.CARTESIAN, 0,1), Point.point(CRS.CARTESIAN, 5,3));
        LineSegment b = LineSegment.lineSegment(Point.point(CRS.CARTESIAN, 5,3), Point.point(CRS.CARTESIAN, 0,1));
        assertThat(a, equalTo(b));
    }

    @Test
    public void shouldNotSeeEqualLineSegments() {
        LineSegment a = LineSegment.lineSegment(Point.point(CRS.CARTESIAN, 1,1), Point.point(CRS.CARTESIAN, 5,3));
        LineSegment b = LineSegment.lineSegment(Point.point(CRS.CARTESIAN, 5,3), Point.point(CRS.CARTESIAN, 0,1));
        assertThat(a, not(equalTo(b)));
    }

    @Test
    public void shouldNotAcceptInvalidCoordinates() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Cannot create line segment from points with different dimensions");
        LineSegment.lineSegment(Point.point(CRS.CARTESIAN, 0,1), Point.point(CRS.CARTESIAN, 5,2,3));
    }
}
