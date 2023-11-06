package org.neo4j.spatial.neo4j.api;

import java.util.*;
import java.util.stream.Stream;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.spatial.CRS;
import org.neo4j.graphdb.spatial.Point;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;
import org.neo4j.spatial.algo.*;
import org.neo4j.spatial.algo.cartesian.CartesianConvexHull;
import org.neo4j.spatial.algo.cartesian.intersect.CartesianMCSweepLineIntersect;
import org.neo4j.spatial.algo.cartesian.intersect.CartesianNaiveIntersect;
import org.neo4j.spatial.algo.wgs84.WGS84ConvexHull;
import org.neo4j.spatial.core.MultiPolygon;
import org.neo4j.spatial.core.Polygon;
import org.neo4j.spatial.neo4j.api.osm.utils.CRSConverter;
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

    @Description("converts a polygon structure to a List of line-strings, each line-string is a list of points")
    @UserFunction(name = "spatial.parsePolygon")
    public List<List<Point>> parsePolygon(@Name("polygon") Node node, @Name(value = "config", defaultValue = "{ polygonLabels: [\"Polygon\"], relationshipTypes: [\"HAS_SHELL\", \"HAS_HOLE\"], shapeProperty: \"shape\"}") Map<String, Object> config) {
        if (!(config.getOrDefault("polygonLabels", List.of("Polygon")) instanceof List<?> polygonLabels)) {
            throw new IllegalArgumentException("Invalid 'polygonLabels', should be a List, but was: " + config.get("polygonLabels").getClass());
        }
        if (!(config.getOrDefault("relationshipTypes", List.of("HAS_SHELL", "HAS_HOLE")) instanceof List<?> relationshipTypes)) {
            throw new IllegalArgumentException("Invalid 'relationshipTypes', should be a List, but was: " + config.get("relationshipTypes").getClass());
        }
        if (!(config.getOrDefault("shapeProperty", "shape") instanceof String shapeProperty)) {
            throw new IllegalArgumentException("Invalid 'shapeProperty', should be a String");
        }

        var parseConfig = new ParsePolygonConfig(
                relationshipTypes.stream()
                        .map(String.class::cast)
                        .map(RelationshipType::withName)
                        .toArray(RelationshipType[]::new),
                shapeProperty,
                polygonLabels.stream()
                        .map(String.class::cast)
                        .map(Label::label)
                        .toList()
        );
        return parsePolygon(node, parseConfig);
    }

    private static List<List<Point>> parsePolygon(@Name("polygon") Node node, ParsePolygonConfig config) {
        List<List<Point>> result = new ArrayList<>();

        if (config.polygonLabels.stream().anyMatch(node::hasLabel)) {
            var data = node.getProperty(config.shapeProperty);
            if (!(data instanceof Point[] points)) {
                throw new IllegalArgumentException("Invalid '" + config.shapeProperty + "', should be a list, but was: " + (data == null ? "null" : data.getClass()));
            }
            if (points.length < 3) {
                throw new IllegalArgumentException("Invalid '" + config.shapeProperty + "', should be a list of at least 3 points, but was: " + points.length);
            }
            result.add(Arrays.asList(points));
        }
        for (Relationship hasPolygon : node.getRelationships(Direction.OUTGOING, config.relationshipTypes)) {
            Node polygonNode = hasPolygon.getEndNode();
            var nestedStructure = parsePolygon(polygonNode, config);
            result.addAll(nestedStructure);
        }
        return result;
    }

    @UserFunction("spatial.boundingBox")
    public Map<String, Point> boundingBoxFor(@Name("polygon") List<Object> data) {
        MultiPolygon mp = tryConvertMultiPolygon(data);
        double[] min = null;
        double[] max = null;
        CoordinateReferenceSystem crs = null;
        for (Polygon.SimplePolygon shell : mp.getShells()) {
            for (org.neo4j.spatial.core.Point point : shell.getPoints()) {
                double[] vertex = point.getCoordinate();
                if (crs == null) {
                    crs = CRSConverter.toNeo4jCRS(point);
                    min = Arrays.copyOf(vertex, vertex.length);
                    max = Arrays.copyOf(vertex, vertex.length);
                    continue;
                }
                for (int i = 0; i < vertex.length; i++) {
                    if (vertex[i] < min[i]) {
                        min[i] = vertex[i];
                    }
                    if (vertex[i] > max[i]) {
                        max[i] = vertex[i];
                    }
                }
            }
        }
        HashMap<String, Point> bbox = new HashMap<>();
        bbox.put("min", GeoUtils.asNeo4jPoint(crs, min));
        bbox.put("max", GeoUtils.asNeo4jPoint(crs, max));
        return bbox;
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
                return WithinCalculator.within(geometry, GeoUtils.asInMemoryPoint(point));
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

    @UserFunction("spatial.algo.intersects")
    public boolean doesIntersect(@Name("polygon1") List<Object> polygon1, @Name("polygon2") List<Object> polygon2) {
        var p1 = tryConvertMultiPolygon(polygon1);
        var p2 = tryConvertMultiPolygon(polygon2);
        return IntersectCalculator.getCalculator(p1, IntersectCalculator.AlgorithmVariant.MC_SWEEP_LINE).doesIntersect(p1, p2);
    }

    private static MultiPolygon tryConvertMultiPolygon(List<?> data) {
        MultiPolygon result = new MultiPolygon();
        extractInnerPointList(data)
                .forEach(lineString -> result.insertPolygon(Polygon.simple(GeoUtils.asInMemoryPoints(lineString))));
        return result;
    }

    private static Stream<List<Point>> extractInnerPointList(List<?> data) {
        if (data.isEmpty()) {
            throw new IllegalArgumentException("Invalid 'data', should be a list of at least 1, but was empty");
        }
        Object firstItem = data.get(0);
        if (firstItem instanceof Point) {
            //noinspection unchecked
            return Stream.of((List<Point>) data);
        }
        if (firstItem instanceof List<?>) {
            return data.stream().flatMap(nestedList -> extractInnerPointList((List<?>) nestedList));
        }
        throw new IllegalArgumentException("Invalid 'data', should be a polyline (list of point), a polygon (list of polylines) or a multipolygon (list of polygons), but was: " + firstItem.getClass());
    }

    record ParsePolygonConfig(
            RelationshipType[] relationshipTypes,
            String shapeProperty,
            List<Label> polygonLabels
    ) {
    }
}
