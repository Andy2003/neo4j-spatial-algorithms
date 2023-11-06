package org.neo4j.spatial.benchmarks.macro;

import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.spatial.algo.cartesian.CartesianConvexHull;
import org.neo4j.spatial.algo.wgs84.WGS84ConvexHull;
import org.neo4j.spatial.benchmarks.JfrProfiler;
import org.neo4j.spatial.core.MultiPolygon;
import org.neo4j.spatial.core.Polygon;
import org.neo4j.spatial.neo4j.api.osm.utils.OSMUtils;
import org.neo4j.test.TestDatabaseManagementServiceBuilder;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.nio.file.Path;

import static org.neo4j.configuration.GraphDatabaseInternalSettings.databases_root_path;

@State(Scope.Benchmark)
@Fork(1)
@BenchmarkMode(Mode.AverageTime)
public class ConvexHullMacroBenchmarks {

    private Node[] nodes;
    private DatabaseManagementService databases;
    private GraphDatabaseService db;

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(ConvexHullMacroBenchmarks.class.getSimpleName())
                .forks(1)
                .addProfiler(JfrProfiler.class)
                .build();

        new Runner(opt).run();
    }

    @Setup
    public void setup() {
        databases = new TestDatabaseManagementServiceBuilder().setConfig(databases_root_path, Path.of("benchmarks/data")).build();
        db = databases.database("sweden");

        long[] ids = new long[]{
                54413,
                52834,
                941530,
                52832,
                54403,
                52826,
                54374,
                54417,
                54412,
                52824,
                54409,
                54391,
                54386,
                54220,
                54223,
                52825,
                52827,
                54221,
                54367,
                54222,
                940675
        };

        nodes = new Node[ids.length];
        Label label = Label.label("OSMRelation");

        try (Transaction tx = db.beginTx()) {
            for (int i = 0; i < ids.length; i++) {
                nodes[i] = tx.findNode(label, Polygon.RELATION_OSM_ID, ids[i]);

                if (nodes[i] == null) {
                    throw new IllegalStateException("OSMRelation not found for relation: " + ids[i]);
                }
            }
            tx.commit();
        }
    }

    @TearDown
    public void tearDown() {
        databases.shutdown();
    }

    @Benchmark
    public void testCartesianConvexHullGraph(Blackhole bh) {
        try (Transaction tx = db.beginTx()) {
            for (Node osmRelation : nodes) {
                MultiPolygon polygon = OSMUtils.getGraphNodePolygon(osmRelation);

                bh.consume(CartesianConvexHull.convexHull(polygon));

            }
            tx.commit();
        }
    }

    @Benchmark
    public void testGeographicConvexHullGraph(Blackhole bh) {
        try (Transaction tx = db.beginTx()) {
            for (Node osmRelation : nodes) {
                MultiPolygon polygon = OSMUtils.getGraphNodePolygon(osmRelation);

                bh.consume(WGS84ConvexHull.convexHull(polygon));
            }
            tx.commit();
        }
    }

    @Benchmark
    public void testCartesianConvexHullProperty(Blackhole bh) {
        try {
            try (Transaction tx = db.beginTx()) {
                for (Node osmRelation : nodes) {
                    MultiPolygon polygon = OSMUtils.getArrayPolygon(osmRelation);

                    bh.consume(CartesianConvexHull.convexHull(polygon));
                }
                tx.commit();
            }
        } catch (Exception e) {
            //ignore
            e.printStackTrace();
        }
    }

    @Benchmark
    public void testGeographicConvexHullProperty(Blackhole bh) {
        try (Transaction tx = db.beginTx()) {
            for (Node osmRelation : nodes) {
                MultiPolygon polygon = OSMUtils.getArrayPolygon(osmRelation);

                bh.consume(WGS84ConvexHull.convexHull(polygon));
            }
            tx.commit();
        }
    }
}
