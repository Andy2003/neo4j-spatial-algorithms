package org.neo4j.spatial.neo4j.api.osm;

import java.util.List;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.spatial.Point;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;
import org.neo4j.spatial.neo4j.api.GeoUtils;
import org.neo4j.spatial.neo4j.api.osm.utils.OSMUtils;

import static org.neo4j.spatial.neo4j.api.osm.utils.CRSConverter.toNeo4jCRS;

public class OSMFunctions {


    // TODO write tests
    @UserFunction(name = "spatial.osm.graph.polygonAsWKT")
    public String getGraphPolygonWKT(@Name("main") Node main) {
        return OSMUtils.getGraphNodePolygon(main).toWKT();
    }

    // TODO write tests
    @UserFunction(name = "spatial.osm.property.polygonAsWKT")
    public String getArrayPolygonWKT(@Name("main") Node main) {
        return OSMUtils.getArrayPolygon(main).toWKT();
    }

    // TODO write tests
    @UserFunction(name = "spatial.osm.property.polygonShell")
    public List<Point> getArrayPolygonShell(@Name("main") Node main) {
        org.neo4j.spatial.core.Point[] mainPoints = OSMUtils.getArrayPolygon(main).getShell().getPoints();
        return GeoUtils.asNeo4jPoints(toNeo4jCRS(mainPoints[0].getCRS()), mainPoints);
    }

    // TODO write tests
    @UserFunction(name = "spatial.osm.graph.polygonShell")
    public List<Point> getGraphPolygonShell(@Name("main") Node main) {
        org.neo4j.spatial.core.Point[] mainPoints = OSMUtils.getGraphNodePolygon(main).getShell().getPoints();
        return GeoUtils.asNeo4jPoints(toNeo4jCRS(mainPoints[0].getCRS()), mainPoints);
    }

    @UserFunction(name = "spatial.osm.graph.polylineAsWKT")
    public String getGraphPolylineWKT(@Name("main") Node main) {
        return OSMUtils.getGraphNodePolyline(main).toWKT();
    }
}
