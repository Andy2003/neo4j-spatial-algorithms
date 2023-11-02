package org.neo4j.spatial.neo4j;

import org.neo4j.internal.helpers.collection.Iterators;
import org.neo4j.internal.kernel.api.NodeCursor;
import org.neo4j.internal.kernel.api.PropertyCursor;
import org.neo4j.io.pagecache.context.CursorContext;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.kernel.impl.api.TokenAccess;
import org.neo4j.memory.EmptyMemoryTracker;
import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.Point;

import java.util.Arrays;
import java.util.Objects;

import static java.lang.String.format;

class Neo4jIDPoint implements Point {
    private final long nodeId;
    private final KernelTransaction ktx;

    private static int propertyId = -1;

    public Neo4jIDPoint(long nodeId, KernelTransaction ktx) {
        this.nodeId = nodeId;
        this.ktx = ktx;

        if (propertyId == -1) {
            getPropertyId();
        }
    }

    @Override
    public boolean equals(Point other) {
        return Arrays.equals(this.getCoordinate(), other.getCoordinate());
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Point point && this.equals(point);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId);
    }

    @Override
    public double[] getCoordinate() {
        double[] coordinates = new double[2];
        try (NodeCursor nodeCursor = ktx.cursors().allocateNodeCursor(CursorContext.NULL_CONTEXT);
             PropertyCursor propertyCursor = ktx.cursors().allocatePropertyCursor(CursorContext.NULL_CONTEXT, EmptyMemoryTracker.INSTANCE)) {
            ktx.dataRead().singleNode(nodeId, nodeCursor);
            outer:
            while (nodeCursor.next()) {
                nodeCursor.properties(propertyCursor);
                while (propertyCursor.next()) {
                    if (propertyCursor.propertyKey() == propertyId) {
                        org.neo4j.graphdb.spatial.Point point = (org.neo4j.graphdb.spatial.Point) propertyCursor.propertyValue();
                        coordinates = point.getCoordinate().getCoordinate().clone();
                        break outer;
                    }
                }
            }
        }
        return coordinates;
    }

    @Override
    public CRS getCRS() {
        CRS crs = CRS.CARTESIAN;
        try ( NodeCursor nodeCursor = ktx.cursors().allocateNodeCursor(CursorContext.NULL_CONTEXT);
              PropertyCursor propertyCursor = ktx.cursors().allocatePropertyCursor(CursorContext.NULL_CONTEXT, EmptyMemoryTracker.INSTANCE) ) {
            ktx.dataRead().singleNode(nodeId, nodeCursor);
            outer:
            while (nodeCursor.next()) {
                nodeCursor.properties(propertyCursor);
                while (propertyCursor.next()) {
                    if (propertyCursor.propertyKey() == propertyId) {
                        org.neo4j.graphdb.spatial.Point point = (org.neo4j.graphdb.spatial.Point) propertyCursor.propertyValue();
                        org.neo4j.graphdb.spatial.CRS neo4jCRS = point.getCRS();
                        crs = CRSConverter.toInMemoryCRS(neo4jCRS);
                        break outer;
                    }
                }
            }
        }
        return crs;
    }

    private void getPropertyId() {
        String[] properties = Iterators.stream(TokenAccess.PROPERTY_KEYS.inUse(ktx.dataRead(), ktx.schemaRead(), ktx.tokenRead())).toArray(String[]::new);
        for (int i = 0; i < properties.length; i++) {
            if (properties[i].equals(PROPERTY)) {
                propertyId = i;
                return;
            }
        }
    }

    public String toString() {
        return format("Neo4jPoint%s", Arrays.toString(getCoordinate()));
    }
}
