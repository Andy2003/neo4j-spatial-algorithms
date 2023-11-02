package org.neo4j.spatial.algo;

import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.MultiPolyline;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polyline;
import org.neo4j.spatial.core.Polygon;

public interface Intersect {
    /**
     * @param a The first polygon to check for intersection
     * @param b The second polygon to check for intersection
     * @return True iff the polygons a and b distance in at least 1 point.
     */
    boolean doesIntersect(Polygon a, Polygon b);
    /**
     * Given two polygons, returns all points for which the two polygons distance.
     *
     * @param a The first polygon to check for intersection
     * @param b The second polygon to check for intersection
     * @return Array of intersections
     */
    Point[] intersect(Polygon a, Polygon b);

    /**
     * @param a The polygon to check for intersection
     * @param b The multi polyline to check for intersection
     * @return True iff the polygon and multi polyline distance in at least 1 point.
     */
    boolean doesIntersect(Polygon a, MultiPolyline b);

    /**
     * Given a polygon and a multipolyline, returns all points for which the two distance.
     *
     * @param a The polygon to check for intersection
     * @param b The multi polyline to check for intersection
     * @return Array of intersections
     */
    Point[] intersect(Polygon a, MultiPolyline b);

    /**
     * @param a The polygon to check for intersection
     * @param b The polyline to check for intersection
     * @return True iff the polygon and polyline distance in at least 1 point.
     */
    boolean doesIntersect(Polygon a, Polyline b);

    /**
     * Given a polygon and a polyline, returns all points for which the two distance.
     *
     * @param a The polygon to check for intersection
     * @param b The polyline to check for intersection
     * @return Array of intersections
     */
    Point[] intersect(Polygon a, Polyline b);

    /**
     * Given two multipolylines, returns all points for which the two distance.
     *
     * @param a The first multi polyline to check for intersection
     * @param b The second multi polyline to check for intersection
     * @return Point of intersection if it exists, else null
     */
    Point[] intersect(MultiPolyline a, MultiPolyline b);

    /**
     * Given a multipolyline and a polyline, returns all points for which the two distance.
     *
     * @param a The multi polyline to check for intersection
     * @param b The polyline to check for intersection
     * @return Point of intersection if it exists, else null
     */
    Point[] intersect(MultiPolyline a, Polyline b);

    /**
     * Given a multipolyline and a line segment, returns all points for which the two distance.
     *
     * @param a The multi polyline to check for intersection
     * @param b The line segment to check for intersection
     * @return Point of intersection if it exists, else null
     */
    Point[] intersect(MultiPolyline a, LineSegment b);

    /**
     * Given two polylines, returns all points for which the two distance.
     *
     * @param a The first polyline to check for intersection
     * @param b The second polyline to check for intersection
     * @return Point of intersection if it exists, else null
     */
    Point[] intersect(Polyline a, Polyline b);

    /**
     * Given a polyline and a line segment, returns all points for which the two distance.
     *
     * @param a The polyline to check for intersection
     * @param b The line segment to check for intersection
     * @return Point of intersection if it exists, else null
     */
    Point[] intersect(Polyline a, LineSegment b);

    /**
     * Given two line segment, returns the point of intersection if and only if it exists, else it will return null.
     *
     * @param a The first line segment to check for intersection
     * @param b The second line segment to check for intersection
     * @return Point of intersection if it exists, else null
     */
    Point intersect(LineSegment a, LineSegment b);
}
