package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.internal.helpers.ArrayUtil;
import org.neo4j.spatial.algo.CCW;
import org.neo4j.spatial.algo.CCWCalculator;
import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.PolygonUtil;

public class Neo4jSimpleGraphNodePolyline extends Neo4jSimpleGraphPolyline {
    private CRS crs;

    public Neo4jSimpleGraphNodePolyline(Node main, long osmRelationId) {
        super(main, osmRelationId);
        this.crs = extractPoint(main).getCRS();
    }

    @Override
    public CRS getCRS() {
        return crs;
    }

    @Override
    public Point[] getPoints() {
        Node[] wayNodes = traverseWholePolygon(main);
        Point[] points = extractPoints(wayNodes);

        CCW calculator = CCWCalculator.getCalculator(points);

        if (points.length < 4) {
            throw new IllegalArgumentException("Polygon cannot have less than 4 points");
        }
        if (!calculator.isCCW(points)) {
            ArrayUtil.reverse(points);
        }
        return points;
    }

    private Point[] extractPoints(Node[] wayNodes) {
        Point[] points = new Point[wayNodes.length];
        for (int i = 0; i < points.length; i++) {
            points[i] = extractPoint(wayNodes[i]);
        }
        return points;
    }

    @Override
    Point extractPoint(Node wayNode) {
        Node node = wayNode.getSingleRelationship(Relation.NODE, Direction.OUTGOING).getEndNode();
        return new Neo4jPoint(node);
    }

    @Override
    public Point getNextPoint() {
        super.traversing = true;
        pointer = getNextNode(pointer);
        Point point = extractPoint(pointer);
        return point;
    }
}
