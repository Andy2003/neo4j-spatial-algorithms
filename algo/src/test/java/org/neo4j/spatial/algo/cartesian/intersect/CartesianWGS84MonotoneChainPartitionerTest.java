package org.neo4j.spatial.algo.cartesian.intersect;

import org.junit.Test;
import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.MonotoneChain;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

import java.util.List;

public class CartesianWGS84MonotoneChainPartitionerTest {
    @Test
    public void shouldPartitionPolygon() {
        Polygon.SimplePolygon testPolygon = makeTestPolygon();
        List<MonotoneChain> actual = CartesianMonotoneChainPartitioner.partition(testPolygon);
    }

    private Polygon.SimplePolygon makeTestPolygon() {
        return Polygon.simple(
                Point.point(CRS.CARTESIAN, -18,-12),
                Point.point(CRS.CARTESIAN, -3,-3),
                Point.point(CRS.CARTESIAN, 10,-15),
                Point.point(CRS.CARTESIAN, 18,3),
                Point.point(CRS.CARTESIAN, -2,14),
                Point.point(CRS.CARTESIAN, -11,8),
                Point.point(CRS.CARTESIAN, -0,1),
                Point.point(CRS.CARTESIAN, -17,2),
                Point.point(CRS.CARTESIAN, -21,12),
                Point.point(CRS.CARTESIAN, -25,4),
                Point.point(CRS.CARTESIAN, -29,-3),
                Point.point(CRS.CARTESIAN, -22,-9),
                Point.point(CRS.CARTESIAN, -17,-6),
                Point.point(CRS.CARTESIAN, -27,-14),
                Point.point(CRS.CARTESIAN, -18,-12)
        );
    }
}
