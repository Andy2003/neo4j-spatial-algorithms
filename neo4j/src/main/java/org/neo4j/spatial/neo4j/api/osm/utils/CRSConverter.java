package org.neo4j.spatial.neo4j.api.osm.utils;

import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.HasCRS;
import org.neo4j.values.storable.CoordinateReferenceSystem;

public class CRSConverter {

    private CRSConverter() {
    }

    public static org.neo4j.spatial.core.CRS toInMemoryCRS(org.neo4j.graphdb.spatial.CRS neo4jCRS) {
        if (neo4jCRS == CoordinateReferenceSystem.CARTESIAN) {
            return org.neo4j.spatial.core.CRS.CARTESIAN;
        } else if (neo4jCRS == CoordinateReferenceSystem.WGS_84) {
            return org.neo4j.spatial.core.CRS.WGS84;
        } else {
            throw new IllegalArgumentException("Unsupported Coordinate Reference System");
        }
    }

    public static org.neo4j.graphdb.spatial.CRS toNeo4jCRS(org.neo4j.spatial.core.CRS memCRS) {
        if (memCRS == CRS.CARTESIAN) {
            return CoordinateReferenceSystem.CARTESIAN;
        } else if (memCRS == CRS.WGS84) {
            return CoordinateReferenceSystem.WGS_84;
        } else {
            throw new IllegalArgumentException("Unsupported Coordinate Reference System");
        }
    }

    public static CoordinateReferenceSystem toNeo4jCRS(HasCRS hasCRS) {
        if (hasCRS.getCRS() == org.neo4j.spatial.core.CRS.CARTESIAN) {
            if (hasCRS.dimension() == 2) {
                return CoordinateReferenceSystem.CARTESIAN;
            } else if (hasCRS.dimension() == 3) {
                return CoordinateReferenceSystem.CARTESIAN_3D;
            }
        } else if (hasCRS.getCRS() == org.neo4j.spatial.core.CRS.WGS84) {
            if (hasCRS.dimension() == 2) {
                return CoordinateReferenceSystem.WGS_84;
            } else if (hasCRS.dimension() == 3) {
                return CoordinateReferenceSystem.WGS_84_3D;
            }
        }
        throw new IllegalArgumentException("Unsupported CRS: " + hasCRS.getCRS() + " with dimension: " + hasCRS.dimension());
    }
}
