package org.neo4j.spatial.algo.wgs84.intersect;

import org.neo4j.spatial.algo.Intersect;
import org.neo4j.spatial.algo.wgs84.WGSUtil;
import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;

public abstract class WGS84Intersect implements Intersect {
    @Override
    public Point intersect(LineSegment a, LineSegment b) {
        return lineSegmentIntersect(a, b);
    }

    public static Point lineSegmentIntersect(LineSegment a, LineSegment b) {
        return WGSUtil.intersect(a, b);
    }
}
