package org.neo4j.spatial.neo4j;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.configuration.GraphDatabaseInternalSettings;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.exceptions.KernelException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.spatial.Point;
import org.neo4j.kernel.api.procedure.GlobalProcedures;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.neo4j.spatial.neo4j.api.SpatialFunctions;
import org.neo4j.spatial.neo4j.api.osm.OSMFunctions;
import org.neo4j.spatial.neo4j.api.osm.OSMProcedures;
import org.neo4j.test.TestDatabaseManagementServiceBuilder;
import org.neo4j.values.storable.CoordinateReferenceSystem;
import org.neo4j.values.storable.PointValue;
import org.neo4j.values.storable.Values;

import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

public class SpatialFunctionsTest {
    private DatabaseManagementService databases;
    private GraphDatabaseService db;

    @Before
    public void setUp() throws KernelException {
        List<String> unrestricted = List.of("neo4j.*");
        databases = new TestDatabaseManagementServiceBuilder()
                .setConfig(GraphDatabaseSettings.procedure_unrestricted, unrestricted)
                .setConfig(GraphDatabaseInternalSettings.trace_cursors, true)
                .impermanent()
                .build();
        db = databases.database(DEFAULT_DATABASE_NAME);
        GlobalProcedures procedures = ((GraphDatabaseAPI) db)
                .getDependencyResolver()
                .resolveDependency(GlobalProcedures.class);
        procedures.registerProcedure(OSMProcedures.class);
        procedures.registerFunction(OSMFunctions.class);
        procedures.registerFunction(SpatialFunctions.class);

        try (Transaction tx = db.beginTx()) {
            tx.execute("""
                    CREATE (root:ExampleRoot)
                    CREATE (shell1:Polygon {shape: [
                            point({srid:4326, x:-10, y: -10}),
                            point({srid:4326, x: 10, y: -10}),
                            point({srid:4326, x: 10, y:  10}),
                            point({srid:4326, x:  0, y:  20}),
                            point({srid:4326, x:-10, y:  10})
                        ]})
                    CREATE (shell2:Polygon {shape: [
                            point({srid:4326, x:-5, y: -20}),
                            point({srid:4326, x: 5, y: -20}),
                            point({srid:4326, x: 5, y:  20}),
                            point({srid:4326, x: 0, y:  40}),
                            point({srid:4326, x:-5, y:  20})
                        ]})
                    CREATE (hole1:Polygon {shape: [
                            point({srid:4326, x:-2, y:  2}),
                            point({srid:4326, x: 0, y:  2}),
                            point({srid:4326, x: 2, y:  2}),
                            point({srid:4326, x: 2, y: -2}),
                            point({srid:4326, x:-2, y: -2})
                        ]})
                    CREATE (root)-[:HAS_SHELL]->(shell1)-[:HAS_HOLE]->(hole1)
                    CREATE (root)-[:HAS_SHELL]->(shell2)
                    """);
            tx.commit();
        }
    }

    @After
    public void tearDown() {
        databases.shutdown();
    }

    @Test
    public void testBoundingBox() {
        try (Transaction tx = db.beginTx()) {
            var result = tx.execute("MATCH (root:ExampleRoot) RETURN spatial.boundingBox(spatial.parsePolygon(root)) as boundingBox")
                    .stream()
                    .map(map -> map.get("boundingBox"))
                    .toList();

            Assertions.assertThat(result)
                    .hasSize(1)
                    .first(InstanceOfAssertFactories.map(String.class, Point.class))
                    .containsExactly(
                            Map.entry("min", getPointValue(-10, -20)),
                            Map.entry("max", getPointValue(10, 40))
                    );
        }
    }

    @Test
    public void testIntersectsTrue() {
        try (Transaction tx = db.beginTx()) {
            Map<String, Object> params = Map.of("polygon", List.of(
                    getPointValue(-9, -9),
                    getPointValue(9, -9),
                    getPointValue(9, 9),
                    getPointValue(-9, -9)
            ));
            var result = tx.execute("MATCH (root:ExampleRoot) RETURN spatial.algo.intersects(spatial.parsePolygon(root), $polygon) as intersects", params)
                    .stream()
                    .map(map -> map.get("intersects"))
                    .toList();

            Assertions.assertThat(result)
                    .hasSize(1)
                    .first(InstanceOfAssertFactories.BOOLEAN)
                    .isTrue();
        }
    }

    @Test
    public void testIntersectsFalse() {
        try (Transaction tx = db.beginTx()) {
            Map<String, Object> params = Map.of("polygon", List.of(
                    getPointValue(-1, -1),
                    getPointValue(1, -1),
                    getPointValue(1, 1),
                    getPointValue(-1, -1)
            ));
            var result = tx.execute("MATCH (root:ExampleRoot) RETURN spatial.algo.intersects(spatial.parsePolygon(root), $polygon) as intersects", params)
                    .stream()
                    .map(map -> map.get("intersects"))
                    .toList();

            Assertions.assertThat(result)
                    .hasSize(1)
                    .first(InstanceOfAssertFactories.BOOLEAN)
                    .isFalse();
        }
    }

    @Test
    public void testIntersectsMultipolygon() {
        try (Transaction tx = db.beginTx()) {
            Map<String, Object> params = Map.of("polygon",
                    List.of(
                            List.of(
                                    getPointValue(-1, -1),
                                    getPointValue(1, -1),
                                    getPointValue(1, 1),
                                    getPointValue(-1, -1)
                            ),
                            List.of(
                                    getPointValue(-9, -9),
                                    getPointValue(9, -9),
                                    getPointValue(9, 9),
                                    getPointValue(-9, -9)
                            )
                    ));
            var result = tx.execute("MATCH (root:ExampleRoot) RETURN spatial.algo.intersects(spatial.parsePolygon(root), $polygon) as intersects", params)
                    .stream()
                    .map(map -> map.get("intersects"))
                    .toList();

            Assertions.assertThat(result)
                    .hasSize(1)
                    .first(InstanceOfAssertFactories.BOOLEAN)
                    .isTrue();
        }
    }

    private static PointValue getPointValue(double x, double y) {
        return Values.pointValue(CoordinateReferenceSystem.WGS_84, x, y);
    }

}
