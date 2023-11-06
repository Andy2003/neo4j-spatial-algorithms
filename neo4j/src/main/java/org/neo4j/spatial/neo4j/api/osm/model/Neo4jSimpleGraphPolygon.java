package org.neo4j.spatial.neo4j.api.osm.model;

import java.util.Iterator;
import java.util.Objects;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.neo4j.internal.helpers.collection.Iterables;
import org.neo4j.internal.helpers.collection.Pair;
import org.neo4j.kernel.impl.traversal.MonoDirectionalTraversalDescription;
import org.neo4j.spatial.algo.Distance;
import org.neo4j.spatial.algo.DistanceCalculator;
import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;
import org.neo4j.spatial.neo4j.api.osm.Relation;

import static java.lang.String.format;

public abstract class Neo4jSimpleGraphPolygon implements Polygon.SimplePolygon {
    private final long osmRelationId;
    private final CRS crs;
    private Iterator<Node> nodeIterator;
    Node firstWayNode;

    protected Neo4jSimpleGraphPolygon(Node firstWayNode, long osmRelationId) {
        this.osmRelationId = osmRelationId;
        this.firstWayNode = firstWayNode;
        crs = extractPoint(firstWayNode).getCRS();
    }

    @Override
    public CRS getCRS() {
        return crs;
    }

    @Override
    public int dimension() {
        return extractPoint(this.firstWayNode).dimension();
    }

    @Override
    public boolean isSimple() {
        return true;
    }

    @Override
    public String toString() {
        return format("Neo4jSimpleGraphNodePolygon(%s)", this.firstWayNode);
    }

    private Traverser getNewTraverser(Node start) {
        return new MonoDirectionalTraversalDescription()
                .depthFirst()
                .relationships(Relation.NEXT, Direction.BOTH)
                .relationships(Relation.NEXT_IN_POLYGON, Direction.BOTH)
                .uniqueness(Uniqueness.NONE)
                .evaluator(new WayEvaluator(osmRelationId, null, null)).traverse(start);
    }

    private Traverser getNewTraverser(Node start, Direction nextDirection, Direction nextInPolygonDirection) {
        return new MonoDirectionalTraversalDescription()
                .depthFirst()
                .relationships(Relation.NEXT, Direction.BOTH)
                .relationships(Relation.NEXT_IN_POLYGON)
                .uniqueness(Uniqueness.NONE)
                .evaluator(new WayEvaluator(osmRelationId, nextDirection, nextInPolygonDirection)).traverse(start);
    }

    @Override
    public boolean fullyTraversed() {
        if (this.nodeIterator != null) {
            return !this.nodeIterator.hasNext();
        }
        return false;
    }

    @Override
    public void startTraversal(Point startPoint, Point directionPoint) {
        Iterator<Node> iterator = getNewTraverser(this.firstWayNode).nodes().iterator();

        Distance calculator = DistanceCalculator.getCalculator(startPoint);

        Point firstPoint = null;
        Node start = null;

        double minDistance = Double.MAX_VALUE;
        while (iterator.hasNext()) {
            Node next = iterator.next();
            Point extracted = extractPoint(next);
            if (firstPoint == null) {
                firstPoint = extracted;
            } else if (firstPoint.equals(extracted)) {
                // Having a ´break' in node.getRelationship causes a RelationshipTraversalCursor cleanup error, so we need to exhaust the iterator later to avoid this
                break;
            }

            double currentDistance = calculator.distance(extracted, startPoint);
            if (currentDistance <= minDistance) {
                minDistance = currentDistance;
                start = next;
            }
        }
        // Exhaust iterator to avoid transaction closing bug with RelationshipTraversalCursor
        while (iterator.hasNext()) {
            iterator.next();
        }
        Pair<Direction, Direction> directions = getClosestNeighborToDirection(start, directionPoint);
        this.nodeIterator = getNewTraverser(start, directions.first(), directions.other()).nodes().iterator();
    }

    private Pair<Direction, Direction> getClosestNeighborToDirection(Node wayNode, Point directionPoint) {
        double minDistance = Double.MAX_VALUE;
        Direction minDirection = null;
        boolean nextInPolygon = true;

        Distance calculator = DistanceCalculator.getCalculator(directionPoint);

        for (Relationship relationship : wayNode.getRelationships(Relation.NEXT_IN_POLYGON)) {
            if (WayEvaluator.nextInPolygon(relationship, osmRelationId)) {
                Node other = relationship.getOtherNode(wayNode);

                double currentDistance = calculator.distance(directionPoint, extractPoint(other));
                if (currentDistance < minDistance) {
                    minDistance = currentDistance;
                    minDirection = relationship.getStartNode().equals(wayNode) ? Direction.OUTGOING : Direction.INCOMING;
                }
            }
        }

        for (Relationship relationship : wayNode.getRelationships(Relation.NEXT)) {
            Node other = relationship.getOtherNode(wayNode);

            double currentDistance = calculator.distance(directionPoint, extractPoint(other));
            if (currentDistance < minDistance) {
                minDistance = currentDistance;
                minDirection = relationship.getStartNode().equals(wayNode) ? Direction.OUTGOING : Direction.INCOMING;
                nextInPolygon = false;
            }
        }

        if (nextInPolygon) {
            return Pair.of(null, minDirection);
        } else {
            return Pair.of(minDirection, null);
        }
    }

    @Override
    public void startTraversal() {
        this.nodeIterator = getNewTraverser(firstWayNode).nodes().iterator();
    }

    abstract Point extractPoint(Node node);

    Node getNextNode() {
        if (this.nodeIterator == null) {
            throw new TraversalException("No traversal is currently ongoing");
        }

        return this.nodeIterator.next();
    }

    protected Node[] traverseWholePolygon() {
        return Iterables.stream(getNewTraverser(firstWayNode).nodes()).toArray(Node[]::new);
    }

    private static class WayEvaluator implements Evaluator {
        private final long relationId;
        private final Direction nextDirection;
        private final Direction nextInPolygonDirection;
        private boolean firstWay;

        private String firstLocationNode = null;
        private String previousLocationNode = null;
        private boolean finished;
        private Direction lastNextDirection;

        public WayEvaluator(long relationId, Direction nextDirection, Direction nextInPolygonDirection) {
            this.relationId = relationId;
            this.nextDirection = nextDirection;
            this.nextInPolygonDirection = nextInPolygonDirection;
            this.firstWay = true;
            this.finished = false;
        }

        @Override
        public Evaluation evaluate(Path path) {
            if (finished) {
                return Evaluation.EXCLUDE_AND_PRUNE;
            }

            Relationship rel = path.lastRelationship();
            Node endNode = path.endNode();
            String locationNode = getLocationNode(endNode);

            if (path.length() == 1
                    && (rel.isType(Relation.NEXT) && nextInPolygonDirection != null
                    || rel.isType(Relation.NEXT_IN_POLYGON) && nextDirection != null))
            {
                return Evaluation.EXCLUDE_AND_PRUNE;

            }

            if (Objects.equals(locationNode, previousLocationNode)) {
                if (rel.isType(Relation.NEXT_IN_POLYGON)) {
                    if (!nextInPolygon(rel) || !validDirection(rel, endNode, nextInPolygonDirection)) {
                        return Evaluation.EXCLUDE_AND_PRUNE;
                    }
                    firstWay = false;
                    lastNextDirection = null;
                }
                return Evaluation.EXCLUDE_AND_CONTINUE;
            }

            if (Objects.equals(firstLocationNode, locationNode)) {
                finished = true;
                return Evaluation.INCLUDE_AND_PRUNE;
            }

            if (rel == null) {
                firstLocationNode = locationNode;
                previousLocationNode = locationNode;
                return Evaluation.INCLUDE_AND_CONTINUE;
            }

            if (rel.isType(Relation.NEXT_IN_POLYGON)) {
                if (!validDirection(rel, endNode, nextInPolygonDirection)) {
                    return Evaluation.EXCLUDE_AND_PRUNE;
                }

                if (nextInPolygon(rel)) {
                    firstWay = false;
                    lastNextDirection = null;
                    previousLocationNode = locationNode;
                    return Evaluation.INCLUDE_AND_CONTINUE;
                } else {
                    return Evaluation.EXCLUDE_AND_PRUNE;
                }
            }

            if (rel.isType(Relation.NEXT) && validDirection(rel, endNode, lastNextDirection)) {
                if (!validDirection(rel, endNode, nextDirection) && firstWay) {
                    return Evaluation.EXCLUDE_AND_PRUNE;
                }

                previousLocationNode = locationNode;
                lastNextDirection = rel.getEndNode().equals(endNode) ? Direction.OUTGOING : Direction.INCOMING;
                return Evaluation.INCLUDE_AND_CONTINUE;
            }
            return Evaluation.EXCLUDE_AND_PRUNE;
        }

        private String getLocationNode(Node node) {
            return node.getSingleRelationship(Relation.NODE, Direction.OUTGOING).getEndNode().getElementId();
        }

        private boolean validDirection(Relationship rel, Node endNode, Direction direction) {
            return direction == null
                    || !(direction == Direction.OUTGOING && endNode.equals(rel.getStartNode()))
                    && !(direction == Direction.INCOMING && endNode.equals(rel.getEndNode()));
        }

        private boolean nextInPolygon(Relationship rel) {
            return nextInPolygon(rel, relationId);
        }

        static boolean nextInPolygon(Relationship rel, long relationId) {
            long[] ids = (long[]) rel.getProperty(Polygon.RELATION_OSM_IDS);
            for (long id : ids) {
                if (id == relationId) {
                    return true;
                }
            }
            return false;
        }
    }

    private static class TraversalException extends RuntimeException {
        public TraversalException(String message) {
        }
    }
}
