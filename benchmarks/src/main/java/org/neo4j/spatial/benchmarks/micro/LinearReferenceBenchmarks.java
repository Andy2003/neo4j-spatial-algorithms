package org.neo4j.spatial.benchmarks.micro;

import org.neo4j.spatial.algo.Distance;
import org.neo4j.spatial.algo.DistanceCalculator;
import org.neo4j.spatial.algo.LinearReference;
import org.neo4j.spatial.algo.LinearReferenceCalculator;
import org.neo4j.spatial.benchmarks.JfrProfiler;
import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Random;

@State(Scope.Benchmark)
@Fork(1)
@BenchmarkMode(Mode.AverageTime)
public class LinearReferenceBenchmarks {

    private Polygon.SimplePolygon[] polygons;
    private double[] geoDistances;
    private double[] cartesianDistances;
    private final Distance geographicDistanceCalc= DistanceCalculator.getCalculator(CRS.WGS84);
    private final Distance cartesianDistanceCalc = DistanceCalculator.getCalculator(CRS.CARTESIAN);

    private final LinearReference geographicCalculator = LinearReferenceCalculator.getCalculator(CRS.WGS84);
    private final LinearReference cartesianCalculator = LinearReferenceCalculator.getCalculator(CRS.CARTESIAN);

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(LinearReferenceBenchmarks.class.getSimpleName())
                .forks(1)
                .addProfiler(JfrProfiler.class)
                .build();

        new Runner(opt).run();
    }

    @Setup
    public void setup() {
        int nUS = 1000;
        int nEU = 1000;
        int nOZ = 1000;

        Random random = new Random(0);
        polygons = new Polygon.SimplePolygon[nUS + nEU + nOZ];
        geoDistances = new double[nUS + nEU + nOZ];
        cartesianDistances = new double[nUS + nEU + nOZ];

        Point originUS = Point.point(CRS.WGS84, -122.31, 37.56);    // San Francisco
        for (int i = 0; i < nUS; i++) {
            polygons[i] = MicroBenchmarkUtil.createPolygon(random, originUS, 0.1, 1.0, 0.1, 1.1).first();
        }
        Point originEU = Point.point(CRS.WGS84, 12.99, 55.61);      // Malmo (Neo4j)
        for (int i = nUS; i < nUS + nEU; i++) {
            polygons[i] = MicroBenchmarkUtil.createPolygon(random, originEU, 0.1, 1.0, 0.1, 1.1).first();
        }
        Point originOZ = Point.point(CRS.WGS84, 151.17, -33.90);    // Sydney
        for (int i = nUS + nEU; i < nUS + nEU + nOZ; i++) {
            polygons[i] = MicroBenchmarkUtil.createPolygon(random, originOZ, 0.1, 1.0, 0.1, 1.1).first();
        }

        double cartesianDist = 0;
        for (int i = 0; i < polygons[0].getPoints().length - 1; i++) {
            cartesianDist += cartesianDistanceCalc.distance(polygons[0].getPoints()[i], polygons[0].getPoints()[i+1]);
        }

        double geographicDist = 0;
        for (int i = 0; i < polygons[0].getPoints().length - 1; i++) {
            geographicDist += geographicDistanceCalc.distance(polygons[0].getPoints()[i], polygons[0].getPoints()[i+1]);
        }

        for (int i = 0; i < geoDistances.length; i++) {
            double rand = random.nextDouble();
            geoDistances[i] = rand * geographicDist;
            cartesianDistances[i] = rand * cartesianDist;
        }
    }

    @Benchmark
    public void testGeographicLinearReference(Blackhole bh) {
        for (int i = 0; i < polygons.length; i++) {
            Point[] temp = geographicCalculator.reference(polygons[i], polygons[i].getPoints()[0], polygons[i].getPoints()[1], geoDistances[i]);
            bh.consume(temp);
        }
    }

    @Benchmark
    public void testCartesianLinearReference(Blackhole bh) {
        for (int i = 0; i < polygons.length; i++) {
            Point[] temp = cartesianCalculator.reference(polygons[i], polygons[i].getPoints()[0], polygons[i].getPoints()[1], cartesianDistances[i]);
            bh.consume(temp);
        }
    }
}
