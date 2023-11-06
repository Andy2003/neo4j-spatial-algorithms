package org.neo4j.spatial.neo4j.api.osm.utils;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.spatial.core.MultiPolygon;
import org.neo4j.spatial.core.MultiPolyline;
import org.neo4j.spatial.core.Polygon;
import org.neo4j.spatial.core.Polyline;
import org.neo4j.spatial.neo4j.api.osm.model.Neo4jSimpleGraphNodePolygon;
import org.neo4j.spatial.neo4j.api.osm.model.Neo4jSimpleGraphNodePolyline;
import org.neo4j.spatial.neo4j.api.osm.Relation;

import static org.neo4j.spatial.core.Polygon.RELATION_OSM_ID;

public class OSMUtils {
    private OSMUtils() {
    }

    public static MultiPolygon getGraphNodePolygon(Node main) {
        long relationId = (long) main.getProperty(RELATION_OSM_ID);
        MultiPolygon multiPolygon = new MultiPolygon();
        insertChildrenGraphNode(main, multiPolygon, relationId);

        return multiPolygon;
    }

    public static MultiPolygon getArrayPolygon(Node main) {
        MultiPolygon multiPolygon = new MultiPolygon();
        insertChildrenArray(main, multiPolygon);

        return multiPolygon;
    }

    public static MultiPolyline getArrayPolyline(Node main) {
        MultiPolyline multiPolyline = new MultiPolyline();

        for (Relationship relationship : main.getRelationships(Direction.OUTGOING, Relation.POLYLINE_STRUCTURE)) {
            Node start = relationship.getEndNode();
            Polyline polyline = Neo4jArrayToInMemoryConverter.convertToInMemoryPolyline(start);
            multiPolyline.insertPolyline(polyline);
        }

        return multiPolyline;
    }

    public static MultiPolyline getGraphNodePolyline(Node main) {
        long relationId = (long) main.getProperty(RELATION_OSM_ID);
        MultiPolyline multiPolyline = new MultiPolyline();

        for (Relationship relationship : main.getRelationships(Direction.OUTGOING, Relation.POLYLINE_STRUCTURE)) {
            Node start = relationship.getEndNode().getSingleRelationship(Relation.POLYLINE_START, Direction.OUTGOING).getEndNode();
            Polyline polyline = new Neo4jSimpleGraphNodePolyline(start, relationId);
            multiPolyline.insertPolyline(polyline);
        }

        return multiPolyline;
    }

    public static void insertChildrenGraphNode(Node node, MultiPolygon multiPolygon, long relationId) {
        for (Relationship polygonStructure : node.getRelationships(Direction.OUTGOING, Relation.POLYGON_STRUCTURE)) {
            Node child = polygonStructure.getEndNode();
            Node start = child.getSingleRelationship(Relation.POLYGON_START, Direction.OUTGOING).getEndNode().getSingleRelationship(Relation.FIRST_NODE, Direction.OUTGOING).getEndNode();

            Polygon.SimplePolygon polygon = new Neo4jSimpleGraphNodePolygon(start, relationId);
            MultiPolygon.MultiPolygonNode childNode = new MultiPolygon.MultiPolygonNode(polygon);
            multiPolygon.addChild(childNode);

            insertChildrenGraphNode(child, childNode, relationId);
        }
    }

    public static void insertChildrenArray(Node node, MultiPolygon multiPolygon) {
        for (Relationship polygonStructure : node.getRelationships(Direction.OUTGOING, Relation.POLYGON_STRUCTURE)) {
            Node child = polygonStructure.getEndNode();

            Polygon.SimplePolygon polygon = Neo4jArrayToInMemoryConverter.convertToInMemoryPolygon(child);
            MultiPolygon.MultiPolygonNode childNode = new MultiPolygon.MultiPolygonNode(polygon);
            multiPolygon.addChild(childNode);

            insertChildrenArray(child, childNode);
        }
    }
}
