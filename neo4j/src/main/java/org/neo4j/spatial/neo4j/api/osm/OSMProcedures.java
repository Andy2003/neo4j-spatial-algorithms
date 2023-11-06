package org.neo4j.spatial.neo4j.api.osm;

import java.util.*;
import java.util.stream.Stream;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.spatial.Point;
import org.neo4j.internal.helpers.collection.Pair;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;
import org.neo4j.spatial.algo.Intersect;
import org.neo4j.spatial.algo.IntersectCalculator;
import org.neo4j.spatial.core.MultiPolyline;
import org.neo4j.spatial.core.Polygon;
import org.neo4j.spatial.core.Polyline;
import org.neo4j.spatial.neo4j.api.GeoUtils;
import org.neo4j.spatial.neo4j.api.osm.model.*;
import org.neo4j.spatial.neo4j.api.osm.utils.OSMUtils;
import org.neo4j.values.storable.CoordinateReferenceSystem;
import org.neo4j.values.storable.Values;

import static org.neo4j.spatial.core.Polygon.RELATION_OSM_ID;

public class OSMProcedures {

    @Context
    public Log log;

    @Context
    public Transaction tx;

    // TODO write tests
    @Description("Creates a polygon as a Point[] property named 'polygon' on the node")
    @Procedure(name = "spatial.osm.property.createPolygon", mode = Mode.WRITE)
    public Stream<PointArraySizeResult> createArrayCache(@Name("main") Node main) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("main", main.getElementId());
        long relationOsmId = (long) main.getProperty(RELATION_OSM_ID);

        Result mainResult = tx.execute("MATCH (p:Polygon)<-[:POLYGON_STRUCTURE*]-(m:OSMRelation) WHERE elementId(m)=$main RETURN p AS polygonNode", parameters);
        if (!mainResult.hasNext()) {
            throw new IllegalArgumentException("No polygon structure found - does " + main + " really have :POLYGON_STRUCTURE relationships? Perhaps you have not run spatial.osm.graph.createPolygon(" + main + ") yet?");
        }

        List<PointArraySizeResult> result = new ArrayList<>();
        while (mainResult.hasNext()) {
            Node polygonNode = (Node) mainResult.next().get("polygonNode");

            parameters = new HashMap<>();
            parameters.put("polygonNode", polygonNode.getElementId());
            Result startNodeResult = tx.execute("MATCH (p:Polygon)-[:POLYGON_START]->(:OSMWay)-[:FIRST_NODE]->(n:OSMWayNode) WHERE elementId(p)=$polygonNode RETURN n AS startNode", parameters);

            if (!startNodeResult.hasNext()) {
                throw new IllegalArgumentException("Broken polygon structure found - polygon " + polygonNode + " is missing a ':POLYGON_START' relationship to an 'OSMWay' node");
            }

            Node startNode = (Node) startNodeResult.next().get("startNode");
            Neo4jSimpleGraphNodePolygon polygon = new Neo4jSimpleGraphNodePolygon(startNode, relationOsmId);
            Point[] polygonPoints = Arrays.stream(polygon.getPoints()).map(p -> Values.pointValue(CoordinateReferenceSystem.WGS_84, p.getCoordinate())).toArray(Point[]::new);
            result.add(new PointArraySizeResult(polygonNode.getElementId(), polygonPoints.length));
            polygonNode.setProperty("polygon", polygonPoints);
        }
        return result.stream();
    }

    // TODO write tests
    @Description("Creates a polyline as a Point[] property named 'polyline' on the node")
    @Procedure(name = "spatial.osm.property.createPolyline", mode = Mode.WRITE)
    public Stream<PointArraySizeResult> createArrayLine(@Name("main") Node main) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("main", main.getElementId());
        long relationOsmId = (long) main.getProperty(RELATION_OSM_ID);

        Result mainResult = tx.execute("MATCH (p:Polyline)<-[:POLYLINE_STRUCTURE*]-(m:OSMRelation) WHERE elementId(m)=$main RETURN p AS polylineNode", parameters);
        if (!mainResult.hasNext()) {
            throw new IllegalArgumentException("No polyline structure found - does " + main + " really have :POLYLINE_STRUCTURE relationships? Perhaps you have not run spatial.osm.graph.createPolygon(" + main + ") yet?");
        }

        // TODO: We could stream results from this iterator with a mapping function rather than building state
        List<PointArraySizeResult> result = new ArrayList<>();
        while (mainResult.hasNext()) {
            Node polylineNode = (Node) mainResult.next().get("polylineNode");

            parameters = new HashMap<>();
            parameters.put("polylineNode", polylineNode.getElementId());
            Result startNodeResult = tx.execute("MATCH (p:Polyline)-[:POLYLINE_START]->(n:OSMWayNode) WHERE elementId(p)=$polylineNode RETURN n AS startNode", parameters);

            if (!startNodeResult.hasNext()) {
                throw new IllegalArgumentException("Broken polyline structure found - polyline " + polylineNode + " is missing a ':POLYLINE_START' relationship to an 'OSMWayNode' node");
            }

            try {
                Node startNode = (Node) startNodeResult.next().get("startNode");
                Neo4jSimpleGraphNodePolyline polyline = new Neo4jSimpleGraphNodePolyline(startNode, relationOsmId);
                Point[] polylinePoints = Arrays.stream(polyline.getPoints()).map(p -> Values.pointValue(CoordinateReferenceSystem.WGS_84, p.getCoordinate())).toArray(Point[]::new);
                result.add(new PointArraySizeResult(polylineNode.getElementId(), polylinePoints.length));
                polylineNode.setProperty("polyline", polylinePoints);
            } catch (Exception e) {
                log.error("Failed to create polyline at " + polylineNode + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        return result.stream();
    }

    @Procedure(name = "spatial.osm.graph.createPolygon.nodeId", mode = Mode.WRITE)
    public void createOSMGraphGeometries(
            @Name("mainId") String mainId,
            @Name(value = "proximityThreshold", defaultValue = "250") double proximityThreshold)
    {
        createOSMGraphGeometries(tx.getNodeByElementId(mainId), proximityThreshold);
    }

    @Procedure(name = "spatial.osm.graph.createPolygon", mode = Mode.WRITE)
    public void createOSMGraphGeometries(
            @Name("main") Node main,
            @Name(value = "proximityThreshold", defaultValue = "250") double proximityThreshold)
    {
        long id = (long) main.getProperty(RELATION_OSM_ID);

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("id", id);
        tx.execute("MATCH (m:OSMRelation)-[:POLYGON_STRUCTURE*]->(p:Polygon) WHERE m.relation_osm_id = $id DETACH DELETE p", parameters);
        tx.execute("MATCH (m:OSMRelation)-[:POLYLINE_STRUCTURE*]->(p:Polyline) WHERE m.relation_osm_id = $id DETACH DELETE p", parameters);
        //TODO fix this by deleting id from array (NEXT_IN_... & END_OF_POLYLINE)
//        tx.execute("MATCH (:OSMWayNode)-[n:NEXT_IN_POLYGON]->(:OSMWayNode) DELETE n");
//        tx.execute("MATCH (:OSMWayNode)-[n:NEXT_IN_POLYLINE]->(:OSMWayNode) DELETE n");
//        tx.execute("MATCH (:OSMWayNode)-[n:END_OF_POLYLINE]->(:OSMWayNode) DELETE n");

        Pair<List<List<Node>>, List<List<Node>>> geometries = OSMTraverser.traverseOSMGraph(tx, main, proximityThreshold);
        List<List<Node>> polygons = geometries.first();
        List<List<Node>> polylines = geometries.other();

        // TODO: Old code would build from a superset of polygons and polylines, but this new code treats them separately - Verify!
        if (!polygons.isEmpty()) {
            log.info("Building " + polygons.size() + " polygons for node " + main + " with osm-id: " + id);
            try {
                new GraphPolygonBuilder(tx, main, polygons).build();
            } catch (Exception e) {
                log.error("Failed to build polygon/polyline structures for node elementId=" + main.getElementId() + ", osm-id=" + id + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        if (!polylines.isEmpty()) {
            log.info("Building " + polylines.size() + " polylines for node " + main + " with osm-id: " + id);
            try {
                // TODO: Can we not build polygons from multiple polylines?
                new GraphPolylineBuilder(tx, main, polylines).build();
            } catch (Exception e) {
                log.error("Failed to build polygon/polyline structures for node elementId=" + main.getElementId() + ", osm-id=" + id + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /*
    spatial.algo.intersection
    spatial.algo.property.intersection

    spatial.osm.graph.createGeometry
    spatial.osm.property.createGeometry

    -lower priority
    spatial.osm.graph.deleteGeometry
    spatial.osm.property.deleteGeometry
     */
    // TODO write tests
    @Procedure("spatial.osm.graph.intersection")
    public Stream<PointResult> intersectionGraphPolygonPolyline(@Name("polygonMain") Node polygonMain, @Name("polylineMain") Node polylineMain, @Name("variant") String variantString) {
        IntersectCalculator.AlgorithmVariant variant;
        if (variantString.equals("Naive")) {
            variant = IntersectCalculator.AlgorithmVariant.NAIVE;
        } else if (variantString.equals("MCSweepLine")) {
            variant = IntersectCalculator.AlgorithmVariant.MC_SWEEP_LINE;
        } else {
            throw new IllegalArgumentException("Illegal algorithm variant. Choose 'Naive' or 'MCSweepLine'");
        }

        List<org.neo4j.spatial.core.Point> result = new ArrayList<>();
        Polygon polygon = OSMUtils.getGraphNodePolygon(polygonMain);
        MultiPolyline multiPolyline = OSMUtils.getGraphNodePolyline(polylineMain);

        Intersect calculator = IntersectCalculator.getCalculator(polygon, variant);

        for (Polyline polyline : multiPolyline.getChildren()) {
            Collections.addAll(result, calculator.intersect(polygon, polyline));
        }
        return result.stream().map(a -> new PointResult(GeoUtils.asNeo4jPoint(a)));
    }


    public static class PointResult {
        public final Point point;

        private PointResult(Point point) {
            this.point = point;
        }
    }

    public static class PointArraySizeResult {
        public final String nodeId;
        public final long count;

        private PointArraySizeResult(String nodeId, long count) {
            this.nodeId = nodeId;
            this.count = count;
        }
    }
}
