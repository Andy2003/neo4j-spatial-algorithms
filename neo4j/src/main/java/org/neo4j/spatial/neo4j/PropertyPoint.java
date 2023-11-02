package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.Node;
import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.Point;

import java.util.Arrays;

import static java.lang.String.format;

public class PropertyPoint implements Point{
    private final double[] coordinate;

    public PropertyPoint(Node node, String xProperty, String yProperty) {
        double x = (double) node.getProperty(xProperty);
        double y = (double) node.getProperty(yProperty);

        this.coordinate = new double[]{x, y};
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
        return Arrays.hashCode(coordinate);
    }

    @Override
    public CRS getCRS() {
        //TODO support different CRS
        return CRS.CARTESIAN;
    }

    @Override
    public double[] getCoordinate() {
        return coordinate;
    }

    public String toString() {
        return format("PropertyPoint%s", Arrays.toString(getCoordinate()));
    }
}
