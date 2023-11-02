package org.neo4j.spatial.algo.wgs84.intersect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import org.neo4j.spatial.algo.AlgoUtil;
import org.neo4j.spatial.algo.wgs84.WGSUtil;
import org.neo4j.spatial.core.*;

public class WGS84MCSweepLineIntersect extends WGS84Intersect {
    private List<MonotoneChain> activeChainList;
    private List<MonotoneChain> sweepingChainList;
    private List<Point> outputList;

    //This variable is used to determine the origin of the monotone chains
    private long splitId;

    public WGS84MCSweepLineIntersect() {
        initialize();
    }

    private void initialize() {
        MonotoneChain.resetId();
        this.activeChainList = new ArrayList<>();
        this.sweepingChainList = new ArrayList<>();
        this.outputList = new ArrayList<>();
    }

    @Override
    public boolean doesIntersect(Polygon a, Polygon b) {
        initialize();
        Polygon.SimplePolygon[] aPolygons = getSimplePolygons(a);
        Polygon.SimplePolygon[] bPolygons = getSimplePolygons(b);

        if (!validate(aPolygons) || !validate(bPolygons)) {
            return new WGS84NaiveIntersect().doesIntersect(a, b);
        }

        Pair<List<MonotoneChain>, List<LineSegment>> aPair = getMonotoneChains(aPolygons, true);
        List<MonotoneChain> inputList = new ArrayList<>(aPair.first());
        Pair<List<MonotoneChain>, List<LineSegment>> bPair = getMonotoneChains(bPolygons, false);
        inputList.addAll(bPair.first());

        //Check the vertical intersections
        checkVerticals(aPair.other(), bPair.first());
        checkVerticals(bPair.other(), aPair.first());

        return intersect(inputList, true).length > 0;
    }

    @Override
    public Point[] intersect(Polygon a, Polygon b) {
        initialize();
        Polygon.SimplePolygon[] aPolygons = getSimplePolygons(a);
        Polygon.SimplePolygon[] bPolygons = getSimplePolygons(b);

        if (!validate(aPolygons) || !validate(bPolygons)) {
            return new WGS84NaiveIntersect().intersect(a, b);
        }

        Pair<List<MonotoneChain>, List<LineSegment>> aPair = getMonotoneChains(aPolygons, true);
        List<MonotoneChain> inputList = new ArrayList<>(aPair.first());
        Pair<List<MonotoneChain>, List<LineSegment>> bPair = getMonotoneChains(bPolygons, false);
        inputList.addAll(bPair.first());

        //Check the vertical intersections
        checkVerticals(aPair.other(), bPair.first());
        checkVerticals(bPair.other(), aPair.first());

        return intersect(inputList, false);
    }

    @Override
    public boolean doesIntersect(Polygon a, MultiPolyline b) {
        initialize();
        Polygon.SimplePolygon[] aPolygons = getSimplePolygons(a);
        Polyline[] bPolylines = b.getChildren();

        if (!validate(aPolygons) || !validate(bPolylines)) {
            return new WGS84NaiveIntersect().doesIntersect(a, b);
        }

        Pair<List<MonotoneChain>, List<LineSegment>> aPair = getMonotoneChains(aPolygons, true);
        List<MonotoneChain> inputList = new ArrayList<>(aPair.first());
        Pair<List<MonotoneChain>, List<LineSegment>> bPair = getMonotoneChains(bPolylines, false);
        inputList.addAll(bPair.first());

        //Check the vertical intersections
        checkVerticals(aPair.other(), bPair.first());
        checkVerticals(bPair.other(), aPair.first());

        return intersect(inputList, true).length > 0;
    }

    @Override
    public Point[] intersect(Polygon a, MultiPolyline b) {
        initialize();
        Polygon.SimplePolygon[] aPolygons = getSimplePolygons(a);
        Polyline[] bPolylines = b.getChildren();

        if (!validate(aPolygons) || !validate(bPolylines)) {
            return new WGS84NaiveIntersect().intersect(a, b);
        }

        Pair<List<MonotoneChain>, List<LineSegment>> aPair = getMonotoneChains(aPolygons, true);
        List<MonotoneChain> inputList = new ArrayList<>(aPair.first());
        Pair<List<MonotoneChain>, List<LineSegment>> bPair = getMonotoneChains(bPolylines, false);
        inputList.addAll(bPair.first());

        //Check the vertical intersections
        checkVerticals(aPair.other(), bPair.first());
        checkVerticals(bPair.other(), aPair.first());

        return intersect(inputList, false);
    }

    @Override
    public boolean doesIntersect(Polygon polygon, Polyline polyline) {
        initialize();
        Polygon.SimplePolygon[] aPolygons = getSimplePolygons(polygon);

        if (!validate(aPolygons) || !validate(new Polyline[]{ polyline })) {
            return new WGS84NaiveIntersect().doesIntersect(polygon, polyline);
        }

        Pair<List<MonotoneChain>, List<LineSegment>> aPair = getMonotoneChains(aPolygons, true);
        List<MonotoneChain> inputList = new ArrayList<>(aPair.first());
        Pair<List<MonotoneChain>, List<LineSegment>> bPair = getMonotoneChains(new Polyline[]{ polyline }, false);
        inputList.addAll(bPair.first());

        //Check the vertical intersections
        checkVerticals(aPair.other(), bPair.first());
        checkVerticals(bPair.other(), aPair.first());

        return intersect(inputList, true).length > 0;
    }

    @Override
    public Point[] intersect(Polygon a, Polyline b) {
        initialize();
        Polygon.SimplePolygon[] aPolygons = getSimplePolygons(a);

        if (!validate(aPolygons) || !validate(new Polyline[]{ b })) {
            return new WGS84NaiveIntersect().intersect(a, b);
        }

        Pair<List<MonotoneChain>, List<LineSegment>> aPair = getMonotoneChains(aPolygons, true);
        List<MonotoneChain> inputList = new ArrayList<>(aPair.first());
        Pair<List<MonotoneChain>, List<LineSegment>> bPair = getMonotoneChains(new Polyline[]{ b }, false);
        inputList.addAll(bPair.first());

        //Check the vertical intersections
        checkVerticals(aPair.other(), bPair.first());
        checkVerticals(bPair.other(), aPair.first());

        return intersect(inputList, false);
    }

    @Override
    public Point[] intersect(MultiPolyline a, MultiPolyline b) {
        initialize();
        Polyline[] aPolylines = a.getChildren();
        Polyline[] bPolylines = b.getChildren();

        if (!validate(aPolylines) || !validate(bPolylines)) {
            return new WGS84NaiveIntersect().intersect(a, b);
        }

        Pair<List<MonotoneChain>, List<LineSegment>> aPair = getMonotoneChains(aPolylines, true);
        List<MonotoneChain> inputList = new ArrayList<>(aPair.first());
        Pair<List<MonotoneChain>, List<LineSegment>> bPair = getMonotoneChains(bPolylines, false);
        inputList.addAll(bPair.first());

        //Check the vertical intersections
        checkVerticals(aPair.other(), bPair.first());
        checkVerticals(bPair.other(), aPair.first());

        return intersect(inputList, false);
    }

    @Override
    public Point[] intersect(MultiPolyline a, Polyline b) {
        initialize();
        Polyline[] aPolylines = a.getChildren();

        if (!validate(aPolylines) || !validate(new Polyline[]{ b })) {
            return new WGS84NaiveIntersect().intersect(a, b);
        }

        Pair<List<MonotoneChain>, List<LineSegment>> aPair = getMonotoneChains(aPolylines, true);
        List<MonotoneChain> inputList = new ArrayList<>(aPair.first());
        Pair<List<MonotoneChain>, List<LineSegment>> bPair = getMonotoneChains(new Polyline[]{ b }, false);
        inputList.addAll(bPair.first());

        //Check the vertical intersections
        checkVerticals(aPair.other(), bPair.first());
        checkVerticals(bPair.other(), aPair.first());

        return intersect(inputList, false);
    }

    @Override
    public Point[] intersect(MultiPolyline a, LineSegment b) {
        initialize();
        Polyline[] aPolylines = a.getChildren();

        Pair<List<MonotoneChain>, List<LineSegment>> aPair = getMonotoneChains(aPolylines, true);
        List<MonotoneChain> inputList = new ArrayList<>(aPair.first());

        if (WGS84MonotoneChainPartitioner.getXDirection(b) == 0) {
            ArrayList<LineSegment> verticals = new ArrayList<>();
            verticals.add(b);
            checkVerticals(verticals, aPair.first());
            return outputList.toArray(new Point[0]);
        }

        MonotoneChain bChain = new MonotoneChain();
        bChain.add(b);
        bChain.initialize();
        inputList.add(bChain);
        return intersect(inputList, false);
    }

    @Override
    public Point[] intersect(Polyline a, Polyline b) {
        initialize();

        if (!validate(new Polyline[]{ a }) || !validate(new Polyline[]{ b })) {
            return new WGS84NaiveIntersect().intersect(a, b);
        }

        Pair<List<MonotoneChain>, List<LineSegment>> aPair = getMonotoneChains(new Polyline[]{ a }, true);
        List<MonotoneChain> inputList = new ArrayList<>(aPair.first());
        Pair<List<MonotoneChain>, List<LineSegment>> bPair = getMonotoneChains(new Polyline[]{ b }, false);
        inputList.addAll(bPair.first());

        //Check the vertical intersections
        checkVerticals(aPair.other(), bPair.first());
        checkVerticals(bPair.other(), aPair.first());

        return intersect(inputList, false);
    }

    @Override
    public Point[] intersect(Polyline a, LineSegment b) {
        initialize();

        Pair<List<MonotoneChain>, List<LineSegment>> aPair = getMonotoneChains(new Polyline[]{ a }, true);
        List<MonotoneChain> inputList = new ArrayList<>(aPair.first());

        if (WGS84MonotoneChainPartitioner.getXDirection(b) == 0) {
            ArrayList<LineSegment> verticals = new ArrayList<>();
            verticals.add(b);
            checkVerticals(verticals, aPair.first());
            return outputList.toArray(new Point[0]);
        }

        MonotoneChain bChain = new MonotoneChain();
        bChain.add(b);
        bChain.initialize();
        inputList.add(bChain);
        return intersect(inputList, false);
    }

    /**
     * Checks the validness of the polygons. A polygon is invalid if it is around a pole or crosses the Date line
     *
     * @param polygons The polygons to check
     * @return True iff none of the polygons are invalid
     */
    private boolean validate(Polygon.SimplePolygon[] polygons) {
        boolean outOfBounds = Arrays.stream(polygons).map(Polygon.SimplePolygon::getPoints).flatMap(Stream::of).anyMatch(p -> p.getCoordinate()[0] > 180 || p.getCoordinate()[0] < -180);
        if (outOfBounds) {
            //Polygon wraps around the Date line
            return false;
        }
        for (Polygon.SimplePolygon polygon : polygons) {
            double courseDelta = WGSUtil.courseDelta(polygon.getPoints());
            if (courseDelta < 270) {
                //Polygon is around a Pole
                return false;
            }
        }
        return true;
    }

    /**
     * Checks the validness of the polyline. A polyline is invalid if it crosses the Date line
     *
     * @param polylines The polyline to check
     * @return True if the polyline is valid
     */
    private boolean validate(Polyline[] polylines) {
        //Polygon wraps around the Date line
        return Arrays.stream(polylines)
                .map(Polyline::getPoints).flatMap(Stream::of)
                .noneMatch(p -> p.getCoordinate()[0] > 180 || p.getCoordinate()[0] < -180);
    }

    /**
     * @param polygons The polygons to check
     * @param first True if the polygons are the first set of polygons
     * @return The monotone chains that make up the polygons
     */
    private Pair<List<MonotoneChain>, List<LineSegment>> getMonotoneChains(Polygon.SimplePolygon[] polygons, boolean first) {
        List<MonotoneChain> result = new ArrayList<>();
        WGS84MonotoneChainPartitioner partitioner = new WGS84MonotoneChainPartitioner();
        for (Polygon.SimplePolygon polygon : polygons) {
            List<MonotoneChain> partitioned = partitioner.partition(polygon);
            result.addAll(partitioned);
        }

        if (first) {
            splitId = result.get(result.size() - 1).getId() + 1;
        }

        return Pair.of(result, partitioner.getVerticals());
    }

    /**
     * @param polylines The polylines to check
     * @param first True if the polylines are the first set of polylines
     * @return The monotone chains that make up the polyline
     */
    private Pair<List<MonotoneChain>, List<LineSegment>> getMonotoneChains(Polyline[] polylines, boolean first) {
        WGS84MonotoneChainPartitioner partitioner = new WGS84MonotoneChainPartitioner();

        List<MonotoneChain> result = new ArrayList<>();
        for (Polyline polyline : polylines) {
            List<MonotoneChain> partitioned = partitioner.partition(polyline);
            result.addAll(partitioned);
        }
        if (first) {
            splitId = result.get(result.size() - 1).getId() + 1;
        }

        return Pair.of(result, partitioner.getVerticals());
    }

    /**
     * @param polygon The polygon to check
     * @return Array of all the shells and holes of the input polygon as simple polygons
     */
    private Polygon.SimplePolygon[] getSimplePolygons(Polygon polygon) {
        Polygon.SimplePolygon[] aPolygons = Stream.concat(Arrays.stream(polygon.getShells()), Arrays.stream(polygon.getHoles()))
                .toArray(Polygon.SimplePolygon[]::new);
        for (int i = 0; i < aPolygons.length; i++) {
            aPolygons[i] = filterCollinear(aPolygons[i]);
        }
        return aPolygons;
    }

    /**
     * A monotone chain sweep line algorithm based on:
     * Park S.C., Shin H., Choi B.K. (2001) A sweep line algorithm for polygonal chain intersection and its applications.
     * In: Kimura F. (eds) Geometric Modelling. GEO 1998. IFIP — The International Federation for Information Processing, vol 75. Springer, Boston, MA
     *
     * @param inputList The list of monotone chains to check for intersections
     * @return An array of points at which the two input polygons distance
     */
    public Point[] intersect(List<MonotoneChain> inputList, boolean shortcut) {
        for (MonotoneChain monotoneChain : inputList) {
            insertMonotoneChainInACL(monotoneChain);
        }

        Vertex v;
        MonotoneChain mcA;
        while (!this.activeChainList.isEmpty()) {
            mcA = this.activeChainList.get(0);
            v = mcA.getFrontVertex();
            mcA.advance();
            insertMonotoneChainInACL(mcA);

            switch (v.getType()) {
                case LEFT_MOST:
                    insertInSCL(mcA, v.getPoint().getCoordinate()[0]);
                    findIntersection(mcA, getPrevious(this.sweepingChainList, mcA));
                    findIntersection(mcA, getNext(this.sweepingChainList, mcA));
                    break;
                case INTERNAL:
                    findIntersection(mcA, getPrevious(this.sweepingChainList, mcA));
                    findIntersection(mcA, getNext(this.sweepingChainList, mcA));
                    break;
                case RIGHT_MOST: {
                    MonotoneChain previous = getPrevious(this.sweepingChainList, mcA);
                    MonotoneChain next = getNext(this.sweepingChainList, mcA);
                    this.sweepingChainList.remove(mcA);
                    this.activeChainList.remove(mcA);
                    findIntersection(previous, next);
                    break;
                }
                case INTERSECTION: {
                    MonotoneChain finalMCa = mcA;
                    MonotoneChain mcB = v.getMonotoneChains().stream().filter(c -> !c.equals(finalMCa)).findFirst().orElseThrow();
                    mcB.advance();
                    insertMonotoneChainInACL(mcB);
                    swapAccordingToSCL(new ArrayList<>(Arrays.asList(mcA, mcB)), v.getPoint().getCoordinate()[0]);
                    MonotoneChain previous = getPrevious(sweepingChainList, mcB);
                    if (previous != null && mcA.equals(previous)) {
                        findIntersection(mcA, getPrevious(sweepingChainList, mcA));
                        findIntersection(mcB, getNext(sweepingChainList, mcB));
                    } else {
                        findIntersection(mcB, getPrevious(sweepingChainList, mcB));
                        findIntersection(mcA, getNext(sweepingChainList, mcA));
                    }
                    addToOutput(v.getPoint());
                    break;
                }
            }
            if (shortcut && !outputList.isEmpty()) {
                return outputList.toArray(new Point[0]);
            }
        }

        return outputList.toArray(new Point[0]);
    }

    /**
     * Check for intersections between the vertical line segments and the monotone chains
     *
     * @param verticals The vertical line segments
     * @param chains The monotone chains
     */
    private void checkVerticals(List<LineSegment> verticals, List<MonotoneChain> chains) {
        List<LineSegment> segments = new ArrayList<>();
        for (MonotoneChain chain : chains) {
            segments.addAll(chain.getLineSegments());
        }

        for (LineSegment vertical : verticals) {
            for (LineSegment segment : segments) {
                Point intersect = super.intersect(vertical, segment);
                if (intersect != null) {
                    addToOutput(intersect);
                }
            }
        }
    }

    private void addToOutput(Point point) {
        boolean flag = false;
        for (Point intersection : this.outputList) {
            if (AlgoUtil.isEqual(intersection.getCoordinate(), point.getCoordinate())) {
                flag = true;
                break;
            }
        }

        if (!flag) {
            this.outputList.add(point);
        }
    }

    /**
     * Removes all successive collinear points of the given polygon
     *
     * @param polygon The polygon to filter
     * @return New polygon without successive collinear points
     */
    private Polygon.SimplePolygon filterCollinear(Polygon.SimplePolygon polygon) {
        List<Point> filteredPoints = new ArrayList<>(Arrays.asList(polygon.getPoints()));

        List<Integer> toDelete = new LinkedList<>();
        for (int i = 1; i < filteredPoints.size() - 1; i++) {
            Point a = filteredPoints.get(i - 1);
            Point b = filteredPoints.get(i);
            Point c = filteredPoints.get(i + 1);

            if (AlgoUtil.ccw(a, b, c) == 0) {
                toDelete.add(i - toDelete.size());
            }
        }
        for (Integer index : toDelete) {
            filteredPoints.remove((int) index);
        }

        return Polygon.simple(filteredPoints.toArray(new Point[0]));
    }

    /**
     * Insert the monotone chain into the active chain list based on x-values of the front vertices.
     *
     * @param chain The monotone chain to be inserted
     */
    private void insertMonotoneChainInACL(MonotoneChain chain) {
        this.activeChainList.remove(chain);

        if (chain.getFrontVertex() == null || this.activeChainList.isEmpty()) {
            this.activeChainList.add(chain);
            return;
        }

        int i = 0;
        Vertex current = chain.getFrontVertex();
        Vertex other = this.activeChainList.get(i).getFrontVertex();
        while (other.getPoint().getCoordinate()[0] < current.getPoint().getCoordinate()[0]) {
            i++;
            if (i >= this.activeChainList.size()) {
                this.activeChainList.add(chain);
                return;
            }
            other = this.activeChainList.get(i).getFrontVertex();
        }

        this.activeChainList.add(i, chain);
    }

    /**
     * Find the intersection between two monotone chains (if it exists) and create a new INTERSECTION vertex
     * if the intersection point is not a shared point of the two chains.
     *
     * @param a The first monotone chain
     * @param b The second monotone chain
     */
    private void findIntersection(MonotoneChain a, MonotoneChain b) {
        if (a == null || b == null) {
            return;
        }

        LineSegment aSegment = LineSegment.lineSegment(a.getFrontVertex().getPoint(), a.getPrevious(a.getFrontVertex()).getPoint());
        LineSegment bSegment = LineSegment.lineSegment(b.getFrontVertex().getPoint(), b.getPrevious(b.getFrontVertex()).getPoint());

        Point sharedPoint = LineSegment.sharedPoint(aSegment, bSegment);
        if (sharedPoint != null) {
            //Check if point isn't already in the output and the two chains are from different polygons by comparing signs
            if (!outputList.contains(LineSegment.sharedPoint(aSegment, bSegment)) &&
                    ((a.getId() - splitId ^ b.getId() - splitId) < 0))
            {
                addToOutput(sharedPoint);
            }
            return;
        }

        Point intersect = super.intersect(aSegment, bSegment);

        if (intersect == null) {
            return;
        }

        Vertex intersectVertex = new Vertex(intersect);

        intersectVertex.setType(Vertex.Type.INTERSECTION);
        intersectVertex.setMonotoneChains(new ArrayList<>(Arrays.asList(a, b)));

        a.insertFrontVertex(intersectVertex);
        insertMonotoneChainInACL(a);
        b.insertFrontVertex(intersectVertex);
        insertMonotoneChainInACL(b);
    }

    /**
     * For all the elements in the input list, which currently distance the sweep line,
     * sort them in the sweeping chain list based on their angle at the sweep line.
     *
     * @param toSort The list of chains which will be re-sorted in the sweeping chain list
     * @param x The x-coordinate of the sweep line
     */
    private void swapAccordingToSCL(List<MonotoneChain> toSort, double x) {
        toSort.sort((a, b) -> {
            double angleA = a.getAngle(x);
            double angleB = b.getAngle(x);

            return Double.compare(angleA, angleB);
        });

        int index = this.sweepingChainList.size();
        for (MonotoneChain chain : toSort) {
            int i = this.sweepingChainList.indexOf(chain);
            if (i < index) {
                index = i;
            }
        }
        this.sweepingChainList.removeAll(toSort);
        for (int i = 0; i < toSort.size(); i++) {
            this.sweepingChainList.add(index + i, toSort.get(i));
        }

    }

    /**
     * Insert the monotone chain into the sweeping chain list based on its y-value at the sweep line.
     * If this y-value coincides with another chain in the list, sort them by their angle.
     *
     * @param chain The chain to be inserted.
     * @param x The x-coordinate of the sweep line
     */
    private void insertInSCL(MonotoneChain chain, double x) {
        this.sweepingChainList.add(chain);
        this.sweepingChainList.sort((a, b) -> {
            double aY = a.getY(x);
            double bY = b.getY(x);

            if (AlgoUtil.isEqual(aY, bY)) {
                double angleA = a.getAngle(x);
                double angleB = b.getAngle(x);

                return Double.compare(angleA, angleB);
            }

            return Double.compare(aY, bY);
        });
    }

    /**
     * Get the next monotone chain from the list based on the position of the input chain.
     *
     * @param list The list of monotone chains to search
     * @param chain The input chain
     * @return The next chain in the list based on the input chain, and null if no next chain exists.
     */
    private MonotoneChain getNext(List<MonotoneChain> list, MonotoneChain chain) {
        int index = list.indexOf(chain);

        if (index == -1 || index == list.size() - 1) {
            return null;
        }

        return list.get(index + 1);
    }

    /**
     * Get the previous monotone chain from the list based on the position of the input chain.
     *
     * @param list The list of monotone chains to search
     * @param chain The input chain
     * @return The previous chain in the list based on the input chain, and null if no previous chain exists.
     */
    private MonotoneChain getPrevious(List<MonotoneChain> list, MonotoneChain chain) {
        int index = list.indexOf(chain);

        if (index <= 0) {
            return null;
        }

        return list.get(index - 1);
    }

    record Pair<T, U>(T first, U other) {
        public static <T, U> Pair<T, U> of(T first, U other) {
            return new Pair<>(first, other);
        }
    }
}
