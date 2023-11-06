package org.neo4j.spatial.neo4j.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.neo4j.graphdb.spatial.CRS;
import org.neo4j.graphdb.spatial.Coordinate;
import org.neo4j.graphdb.spatial.Point;
import org.neo4j.spatial.core.Polygon;
import org.neo4j.spatial.neo4j.api.osm.utils.CRSConverter;

import static org.neo4j.spatial.neo4j.api.osm.utils.CRSConverter.toNeo4jCRS;

public class GeoUtils {

    private GeoUtils() {
    }

    public static List<Point> asNeo4jPoints(CRS crs, org.neo4j.spatial.core.Point[] points) {
        List<Point> converted = new ArrayList<>();
        for (org.neo4j.spatial.core.Point point : points) {
            converted.add(asNeo4jPoint(crs, point));
        }
        return converted;
    }

    public static Point asNeo4jPoint(CRS crs, org.neo4j.spatial.core.Point point) {
        return new Neo4jPoint(crs, new Coordinate(point.getCoordinate()));
    }

    public static Point asNeo4jPoint(CRS crs, double[] coords) {
        return new Neo4jPoint(crs, new Coordinate(coords));
    }

    public static Point asNeo4jPoint(org.neo4j.spatial.core.Point point) {
        return new Neo4jPoint(toNeo4jCRS(point.getCRS()), new Coordinate(point.getCoordinate()));
    }

    public static org.neo4j.spatial.core.Point[] asInMemoryPoints(List<Point> polygon) {
        org.neo4j.spatial.core.Point[] points = new org.neo4j.spatial.core.Point[polygon.size()];
        for (int i = 0; i < points.length; i++) {
            points[i] = asInMemoryPoint(polygon.get(i));
        }
        return points;
    }

    public static org.neo4j.spatial.core.Point asInMemoryPoint(Point point) {
        double[] coords = point.getCoordinate().getCoordinate().clone();
        org.neo4j.spatial.core.CRS crs = CRSConverter.toInMemoryCRS(point.getCRS());
        return org.neo4j.spatial.core.Point.point(crs, coords);
    }

    public static Polygon.SimplePolygon getSimplePolygon(List<Point> polygon1) {
        org.neo4j.spatial.core.Point[] convertedPoints = asInMemoryPoints(polygon1);
        return Polygon.simple(convertedPoints);
    }

    public static void validatePolygons(List<Point> polygon1, List<Point> polygon2) {
        if (polygon1 == null) {
            throw new IllegalArgumentException("Invalid 'polygon1', 'polygon1' was not defined");
        } else if (polygon1.size() < 3) {
            throw new IllegalArgumentException("Invalid 'polygon1', should be a list of at least 3, but was: " + polygon1.size());
        } else if (polygon2 == null) {
            throw new IllegalArgumentException("Invalid 'polygon2', 'polygon2' was not defined");
        } else if (polygon2.size() < 3) {
            throw new IllegalArgumentException("Invalid 'polygon2', should be a list of at least 3, but was: " + polygon2.size());
        }

        CRS crs1 = polygon1.get(0).getCRS();
        CRS crs2 = polygon2.get(0).getCRS();
        if (!crs1.equals(crs2)) {
            throw new IllegalArgumentException("Cannot compare geometries of different CRS: " + crs1 + " !+ " + crs2);
        }
    }

    private static class Neo4jPoint implements Point {
        private final List<Coordinate> coordinates;
        private final CRS crs;

        private Neo4jPoint(CRS crs, Coordinate coordinate) {
            this.crs = crs;
            this.coordinates = Collections.singletonList(coordinate);
        }

        @Override
        public List<Coordinate> getCoordinates() {
            return coordinates;
        }

        @Override
        public CRS getCRS() {
            return crs;
        }
    }
}
