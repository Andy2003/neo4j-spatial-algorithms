package org.neo4j.spatial.neo4j.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.spatial.CRS;
import org.neo4j.graphdb.spatial.Point;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;
import org.neo4j.spatial.algo.Area;
import org.neo4j.spatial.algo.AreaCalculator;
import org.neo4j.spatial.algo.Distance;
import org.neo4j.spatial.algo.DistanceCalculator;
import org.neo4j.spatial.algo.cartesian.CartesianConvexHull;
import org.neo4j.spatial.algo.cartesian.CartesianWithin;
import org.neo4j.spatial.algo.cartesian.intersect.CartesianMCSweepLineIntersect;
import org.neo4j.spatial.algo.cartesian.intersect.CartesianNaiveIntersect;
import org.neo4j.spatial.algo.wgs84.WGS84ConvexHull;
import org.neo4j.spatial.core.MultiPolygon;
import org.neo4j.spatial.core.Polygon;
import org.neo4j.spatial.neo4j.api.osm.utils.OSMUtils;
import org.neo4j.values.storable.CoordinateReferenceSystem;

public class SpatialFunctions {

    @UserFunction("spatial.polygon")
    public List<Point> makePolygon(@Name("points") List<Point> points) {
        if (points == null || points.size() < 3) {
            throw new IllegalArgumentException("Invalid 'points', should be a list of at least 3, but was: " + (points == null ? "null" : points.size()));
        } else if (points.get(0).equals(points.get(points.size() - 1))) {
            return points;
        } else {
            ArrayList<Point> polygon = new ArrayList<>(points.size() + 1);
            polygon.addAll(points);
            polygon.add(points.get(0));
            return polygon;
        }
    }

    @UserFunction("spatial.boundingBox")
    public Map<String, Point> boundingBoxFor(@Name("polygon") List<Point> polygon) {
        if (polygon == null || polygon.size() < 4) {
            throw new IllegalArgumentException("Invalid 'polygon', should be a list of at least 4, but was: " + (polygon == null ? "null" : polygon.size()));
        } else if (!polygon.get(0).equals(polygon.get(polygon.size() - 1))) {
            throw new IllegalArgumentException("Invalid 'polygon', first and last point should be the same, but were: " + polygon.get(0) + " and " + polygon.get(polygon.size() - 1));
        } else {
            CRS crs = polygon.get(0).getCRS();
            double[] min = GeoUtils.asInMemoryPoint(polygon.get(0)).getCoordinate();
            double[] max = GeoUtils.asInMemoryPoint(polygon.get(0)).getCoordinate();
            for (Point p : polygon) {
                double[] vertex = GeoUtils.asInMemoryPoint(p).getCoordinate();
                for (int i = 0; i < vertex.length; i++) {
                    if (vertex[i] < min[i]) {
                        min[i] = vertex[i];
                    }
                    if (vertex[i] > max[i]) {
                        max[i] = vertex[i];
                    }
                }
            }
            HashMap<String, Point> bbox = new HashMap<>();
            bbox.put("min", GeoUtils.asNeo4jPoint(crs, min));
            bbox.put("max", GeoUtils.asNeo4jPoint(crs, max));
            return bbox;
        }
    }

    @UserFunction("spatial.algo.withinPolygon")
    public boolean withinPolygon(@Name("point") Point point, @Name("polygon") List<Point> polygon) {
        if (polygon == null || polygon.size() < 4) {
            throw new IllegalArgumentException("Invalid 'polygon', should be a list of at least 4, but was: " + polygon.size());
        } else if (!polygon.get(0).equals(polygon.get(polygon.size() - 1))) {
            throw new IllegalArgumentException("Invalid 'polygon', first and last point should be the same, but were: " + polygon.get(0) + " and " + polygon.get(polygon.size() - 1));
        } else {
            CRS polyCrs = polygon.get(0).getCRS();
            CRS pointCrs = point.getCRS();
            if (!polyCrs.equals(pointCrs)) {
                throw new IllegalArgumentException("Cannot compare geometries of different CRS: " + polyCrs + " !+ " + pointCrs);
            } else {
                Polygon.SimplePolygon geometry = Polygon.simple(GeoUtils.asInMemoryPoints(polygon));
                return CartesianWithin.within(geometry, GeoUtils.asInMemoryPoint(point));
            }
        }
    }

    @UserFunction("spatial.algo.convexHull")
    public List<Point> convexHullPoints(@Name("points") List<Point> points) {
        Polygon.SimplePolygon convexHull = CartesianConvexHull.convexHull(GeoUtils.asInMemoryPoints(points));

        return GeoUtils.asNeo4jPoints(CoordinateReferenceSystem.WGS_84, convexHull.getPoints());
    }

    // TODO: write tests
    @UserFunction("spatial.algo.property.convexHull")
    public List<Point> convexHullArray(@Name("main") Node main) {
        MultiPolygon multiPolygon = OSMUtils.getArrayPolygon(main);
        Polygon.SimplePolygon convexHull = CartesianConvexHull.convexHull(multiPolygon);

        return GeoUtils.asNeo4jPoints(CoordinateReferenceSystem.WGS_84, convexHull.getPoints());
    }

    // TODO: write tests
    @UserFunction("spatial.algo.graph.convexHull")
    public List<Point> convexHullGraphNode(@Name("main") Node main) {
        MultiPolygon multiPolygon = OSMUtils.getGraphNodePolygon(main);
        Polygon.SimplePolygon convexHull = WGS84ConvexHull.convexHull(multiPolygon);

        return GeoUtils.asNeo4jPoints(CoordinateReferenceSystem.WGS_84, convexHull.getPoints());
    }

    @UserFunction("spatial.algo.area")
    public double area(@Name("polygon") List<Point> polygon) {
        Polygon.SimplePolygon convertedPolygon = GeoUtils.getSimplePolygon(polygon);
        Area area = AreaCalculator.getCalculator(convertedPolygon);
        return area.area(convertedPolygon);
    }

    @UserFunction("spatial.algo.distance")
    public double distance(@Name("polygon1") List<Point> polygon1, @Name("polygon2") List<Point> polygon2) {
        Polygon.SimplePolygon convertedPolygon1 = GeoUtils.getSimplePolygon(polygon1);
        Polygon.SimplePolygon convertedPolygon2 = GeoUtils.getSimplePolygon(polygon2);

        Distance distance = DistanceCalculator.getCalculator(convertedPolygon1);
        return distance.distance(convertedPolygon1, convertedPolygon2);
    }

    @UserFunction("spatial.algo.distance.ends")
    public Map<String, Object> distanceAndEndPoints(@Name("polygon1") List<Point> polygon1, @Name("polygon2") List<Point> polygon2) {
        try {
            Polygon.SimplePolygon convertedPolygon1 = GeoUtils.getSimplePolygon(polygon1);
            Polygon.SimplePolygon convertedPolygon2 = GeoUtils.getSimplePolygon(polygon2);
            final CRS crs = polygon1.get(0).getCRS();

            Distance distance = DistanceCalculator.getCalculator(convertedPolygon1);
            Distance.DistanceResult dae = distance.distanceAndEndpoints(convertedPolygon1, convertedPolygon2);
            return dae.asMap(p -> GeoUtils.asNeo4jPoint(crs, p));
        } catch (Exception e) {
            System.out.println("Failed to calculate polygon distance: " + e.getMessage());
            e.printStackTrace();
            return Distance.DistanceResult.NO_RESULT.withError(e).asMap();
        }
    }

    @UserFunction("spatial.algo.convexHull.distance")
    public double convexHullDistance(@Name("polygon1") List<Point> polygon1, @Name("polygon2") List<Point> polygon2) {
        Polygon.SimplePolygon convexHull1 = CartesianConvexHull.convexHull(GeoUtils.asInMemoryPoints(polygon1));
        Polygon.SimplePolygon convexHull2 = CartesianConvexHull.convexHull(GeoUtils.asInMemoryPoints(polygon2));

        Distance distance = DistanceCalculator.getCalculator(convexHull1);
        return distance.distance(convexHull1, convexHull2);
    }

    @UserFunction("spatial.algo.convexHull.distance.ends")
    public Map<String, Object> convexHullDistanceAndEndPoints(@Name("polygon1") List<Point> polygon1, @Name("polygon2") List<Point> polygon2) {
        try {
            Polygon.SimplePolygon convexHull1 = CartesianConvexHull.convexHull(GeoUtils.asInMemoryPoints(polygon1));
            Polygon.SimplePolygon convexHull2 = CartesianConvexHull.convexHull(GeoUtils.asInMemoryPoints(polygon2));
            final CRS crs = polygon1.get(0).getCRS();

            Distance distance = DistanceCalculator.getCalculator(convexHull1);
            Distance.DistanceResult dae = distance.distanceAndEndpoints(convexHull1, convexHull2);
            return dae.asMap(p -> GeoUtils.asNeo4jPoint(crs, p));
        } catch (Exception e) {
            System.out.println("Failed to calculate polygon distance: " + e.getMessage());
            e.printStackTrace();
            return Distance.DistanceResult.NO_RESULT.withError(e).asMap();
        }
    }

    // TODO write tests
    @UserFunction("spatial.algo.intersection")
    public List<Point> naiveIntersectArray(@Name("polygon1") List<Point> polygon1, @Name("polygon2") List<Point> polygon2) {
        GeoUtils.validatePolygons(polygon1, polygon2);

        Polygon.SimplePolygon convertedPolygon1 = GeoUtils.getSimplePolygon(polygon1);
        Polygon.SimplePolygon convertedPolygon2 = GeoUtils.getSimplePolygon(polygon2);

        org.neo4j.spatial.core.Point[] intersections = new CartesianNaiveIntersect().intersect(convertedPolygon1, convertedPolygon2);
        return GeoUtils.asNeo4jPoints(polygon1.get(0).getCRS(), intersections);
    }

    // TODO write tests
    @UserFunction("spatial.algo.intersection.sweepline")
    public List<Point> MCSweepLineIntersectArray(@Name("polygon1") List<Point> polygon1, @Name("polygon2") List<Point> polygon2) {
        GeoUtils.validatePolygons(polygon1, polygon2);

        Polygon.SimplePolygon convertedPolygon1 = GeoUtils.getSimplePolygon(polygon1);
        Polygon.SimplePolygon convertedPolygon2 = GeoUtils.getSimplePolygon(polygon2);

        org.neo4j.spatial.core.Point[] intersections = new CartesianMCSweepLineIntersect().intersect(convertedPolygon1, convertedPolygon2);
        return GeoUtils.asNeo4jPoints(polygon1.get(0).getCRS(), intersections);
    }
}
