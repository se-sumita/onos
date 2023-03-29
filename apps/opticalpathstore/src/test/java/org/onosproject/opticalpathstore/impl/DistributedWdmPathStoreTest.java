package org.onosproject.opticalpathstore.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.openroadm.model.ModulationFormat;
import org.onosproject.net.openroadm.model.OchParam;
import org.onosproject.net.openroadm.model.OsnrMap;
import org.onosproject.net.openroadm.model.Rate;
import org.onosproject.net.openroadm.model.WdmPath;
import org.onosproject.store.service.TestStorageService;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.onosproject.opticalpathstore.impl.PathTestTool.*;

public class DistributedWdmPathStoreTest {
    private DistributedWdmPathStore pathStoreImpl;

    @Before
    public void setUp() {
        pathStoreImpl = new DistributedWdmPathStore();
        pathStoreImpl.coreService = new CoreServiceAdapter();
        pathStoreImpl.storageService = new TestStorageService();
        pathStoreImpl.eventDispatcher = new TestEventDispatcher();

        pathStoreImpl.activate();

        TestUtils.setField(pathStoreImpl.pathMap, "map", new ConcurrentHashMap<>());
    }

    @After
    public void tearDown() {
        pathStoreImpl.deactivate();
    }

    @Test(expected = NullPointerException.class)
    public void testAddAllNullPointerException() {
        pathStoreImpl.addAll(null);
    }

    //CHECKSTYLE:OFF
    private static ConnectPoint src1 = point("dev1/1");
    private static ConnectPoint src2 = point("dev2/1");
    private static ConnectPoint dst3 = point("dev3/1");
    private static ConnectPoint dst4 = point("dev4/1");

    private static Link link1_2 = link("dev1/2", "dev2/2");
    private static Link link2_3 = link("dev2/3", "dev3/2");
    private static Link link3 = link("dev1/2", "dev2/2");
    private static Link link2_4 = link("dev2/2", "dev4/2");
    //CHECKSTYLE:ON

    @Test
    public void testAddAll() {
        // 1 path
        List<WdmPath> paths = buildWdmPath1();

        pathStoreImpl.addAll(paths);

        assertEquals(1, pathStoreImpl.stream().count());

        // check cache
        assertEquals(1, pathStoreImpl.ingressMap.size());
        assertEquals(1, pathStoreImpl.egressMap.size());
        assertEquals(1, pathStoreImpl.pairMap.size());

        // 2 paths
        paths = buildWdmPath2();

        pathStoreImpl.addAll(paths); // including path duplicated (path1)

        assertEquals(2, pathStoreImpl.stream().count());

        // check cache
        assertEquals(2, pathStoreImpl.ingressMap.size());
        assertEquals(2, pathStoreImpl.ingressMap.get(paths.get(0).src()).size());
        assertEquals(2, pathStoreImpl.egressMap.size());
        assertEquals(1, pathStoreImpl.egressMap.get(paths.get(0).dst()).size());
        assertEquals(1, pathStoreImpl.egressMap.get(paths.get(1).dst()).size());
        assertEquals(2, pathStoreImpl.pairMap.size());
    }

    @Test
    public void testClear() {
        List<WdmPath> paths = buildWdmPath3();

        pathStoreImpl.addAll(paths);
        pathStoreImpl.clear();

        assertEquals(0, pathStoreImpl.stream().count());

        // check cache
        assertEquals(0, pathStoreImpl.ingressMap.size());
        assertEquals(0, pathStoreImpl.egressMap.size());
        assertEquals(0, pathStoreImpl.pairMap.size());

        // use remove()
        pathStoreImpl.addAll(paths);
        pathStoreImpl.remove(null, null);
        assertEquals(0, pathStoreImpl.stream().count());
    }

    @Test
    public void testGetPaths() {
        List<WdmPath> paths = buildWdmPath3();

        pathStoreImpl.addAll(paths);

        Collection<WdmPath> actual = pathStoreImpl.getPaths();
        assertEquals(4, actual.size());
        assertEquals(ImmutableSet.copyOf(paths), ImmutableSet.copyOf(actual));
    }

    @Test
    public void testGetPathsByIngressPort() {
        // dev1/1 --- dev3/1
        // dev1/1 --- dev4/1
        // dev1/1 --...-- dev3/1
        // dev2/1 --- dev3/1
        List<WdmPath> paths = buildWdmPath3();

        pathStoreImpl.addAll(paths);

        ConnectPoint p1 = paths.get(0).src(); // dev1/1
        Collection<WdmPath> actual = pathStoreImpl.getPathsByIngressPort(p1);
        assertEquals(3, actual.size());
        assertEquals(ImmutableSet.of(paths.get(0), paths.get(1), paths.get(2)),
                ImmutableSet.copyOf(actual));

        ConnectPoint p2 = paths.get(3).src(); // dev2/1
        actual = pathStoreImpl.getPathsByIngressPort(p2);
        assertEquals(1, actual.size());
        assertEquals(ImmutableList.of(paths.get(3)), actual);

        // use getPaths()
        actual = pathStoreImpl.getPaths(p2, null);
        assertEquals(1, actual.size());
        assertEquals(ImmutableList.of(paths.get(3)), actual);
    }

    @Test
    public void testGetPathsByEgressPort() {
        // dev1/1 --- dev3/1
        // dev1/1 --- dev4/1
        // dev1/1 --...-- dev3/1
        // dev2/1 --- dev3/1
        List<WdmPath> paths = buildWdmPath3();

        pathStoreImpl.addAll(paths);

        ConnectPoint p1 = paths.get(0).dst(); // dev3/1
        Collection<WdmPath> actual = pathStoreImpl.getPathsByEgressPort(p1);
        assertEquals(3, actual.size());
        assertEquals(ImmutableList.of(paths.get(0), paths.get(2), paths.get(3)), actual);

        ConnectPoint p2 = paths.get(1).dst(); // dev4/1
        actual = pathStoreImpl.getPathsByEgressPort(p2);
        assertEquals(1, actual.size());
        assertEquals(ImmutableList.of(paths.get(1)), actual);

        // use getPaths()
        actual = pathStoreImpl.getPaths(null, p2);
        assertEquals(1, actual.size());
        assertEquals(ImmutableList.of(paths.get(1)), actual);
    }

    @Test
    public void testGetPathsWithPorts() {
        // dev1/1 --- dev3/1
        // dev1/1 --- dev4/1
        // dev1/1 --...-- dev3/1
        // dev2/1 --- dev3/1
        List<WdmPath> paths = buildWdmPath3();

        pathStoreImpl.addAll(paths);

        ConnectPoint p1src = paths.get(0).src(); // dev1/1
        ConnectPoint p1dst = paths.get(0).dst(); // dev3/1
        Collection<WdmPath> actual = pathStoreImpl.getPaths(p1src, p1dst);
        assertEquals(2, actual.size());
        assertEquals(ImmutableList.of(paths.get(0), paths.get(2)), actual);

        ConnectPoint p2src = paths.get(1).src(); // dev1/1
        ConnectPoint p2dst = paths.get(1).dst(); // dev4/1
        actual = pathStoreImpl.getPaths(p2src, p2dst);
        assertEquals(1, actual.size());
        assertEquals(ImmutableList.of(paths.get(1)), actual);

        ConnectPoint p3src = paths.get(3).src(); // dev2/1
        ConnectPoint p3dst = paths.get(3).dst(); // dev3/1
        actual = pathStoreImpl.getPaths(p3src, p3dst);
        assertEquals(1, actual.size());
        assertEquals(ImmutableList.of(paths.get(3)), actual);
    }

    @Test
    public void testRemoveByIngressPort() {
        // dev1/1 --- dev3/1
        // dev1/1 --- dev4/1
        // dev1/1 --...-- dev3/1
        // dev2/1 --- dev3/1
        List<WdmPath> paths = buildWdmPath3();

        pathStoreImpl.addAll(paths);

        ConnectPoint p1 = paths.get(0).src(); // dev1/1
        pathStoreImpl.removeByIngressPort(p1);
        assertEquals(1, pathStoreImpl.stream().count());
        assertEquals(paths.get(3), pathStoreImpl.stream().toArray()[0]);

        // use remove()
        pathStoreImpl.addAll(buildWdmPath3());
        pathStoreImpl.remove(p1, null);
        assertEquals(1, pathStoreImpl.stream().count());
        assertEquals(paths.get(3), pathStoreImpl.stream().toArray()[0]);
    }

    @Test
    public void testRemoveByEgressPort() {
        // dev1/1 --- dev3/1
        // dev1/1 --- dev4/1
        // dev1/1 --...-- dev3/1
        // dev2/1 --- dev3/1
        List<WdmPath> paths = buildWdmPath3();

        pathStoreImpl.addAll(paths);

        ConnectPoint p1 = paths.get(0).dst(); // dev3/1
        pathStoreImpl.removeByEgressPort(p1);
        assertEquals(1, pathStoreImpl.stream().count());
        assertEquals(paths.get(1), pathStoreImpl.stream().toArray()[0]);

        // use remove()
        pathStoreImpl.addAll(buildWdmPath3());
        pathStoreImpl.remove(null, p1);
        assertEquals(1, pathStoreImpl.stream().count());
        assertEquals(paths.get(1), pathStoreImpl.stream().toArray()[0]);
    }

    @Test
    public void testRemove() {
        // dev1/1 --- dev3/1
        // dev1/1 --- dev4/1
        // dev1/1 --...-- dev3/1
        // dev2/1 --- dev3/1
        List<WdmPath> paths = buildWdmPath3();

        pathStoreImpl.addAll(paths);

        ConnectPoint p1src = paths.get(0).src(); // dev1/1
        ConnectPoint p1dst = paths.get(0).dst(); // dev3/1
        pathStoreImpl.remove(p1src, p1dst);
        assertEquals(2, pathStoreImpl.stream().count());
        assertEquals(
                ImmutableSet.of(paths.get(1), paths.get(3)),
                ImmutableSet.copyOf(pathStoreImpl.stream().toArray()));
    }

    @Test
    public void testGetReversePath() {
        List<WdmPath> paths = Lists.newArrayList(buildWdmPath3());
        WdmPath reversedPath = buildReversePath(paths.get(0));
        paths.add(reversedPath);
        pathStoreImpl.addAll(paths);

        WdmPath r = pathStoreImpl.getReversePath(paths.get(0));
        WdmPath f = pathStoreImpl.getReversePath(r);

        assertEquals(paths.get(0), f);
        assertEquals(reversedPath, r);

        WdmPath notFound = pathStoreImpl.getReversePath(paths.get(1));
        assertNull(notFound);
    }

    private List<WdmPath> buildWdmPath1() {
        OsnrMap osnr = new OsnrMap();
        osnr.put(OchParam.of(Rate.R100G, ModulationFormat.DP_QPSK), 1.0);
        WdmPath path1 = new WdmPath(
                src1, dst3,
                path(link1_2, link2_3),
                osnr
        );
        // dev1/1 --- dev3/1
        return ImmutableList.of(path1);
    }

    private List<WdmPath> buildWdmPath2() {
        OsnrMap osnr = new OsnrMap();
        osnr.put(OchParam.of(Rate.R100G, ModulationFormat.DP_QPSK), 2.0);
        List<WdmPath> paths = Lists.newArrayList(buildWdmPath1());
        paths.add(
            new WdmPath(
                    src1, dst4,
                    path(link3, link2_4),
                    osnr
            )
        );
        // dev1/1 --- dev3/1
        // dev1/1 --- dev4/1
        return ImmutableList.copyOf(paths);
    }

    private List<WdmPath> buildWdmPath3() {
        List<WdmPath> paths = Lists.newArrayList(buildWdmPath2());
        OsnrMap osnr = new OsnrMap();
        osnr.put(OchParam.of(Rate.R100G, ModulationFormat.DP_QPSK), 3.0);
        paths.add(
                new WdmPath(
                        src1, dst3,
                        path(link1_2, link3, link2_3),
                        osnr
                )
        );
        osnr = new OsnrMap();
        osnr.put(OchParam.of(Rate.R100G, ModulationFormat.DP_QPSK), 4.0);
        paths.add(
                new WdmPath(
                        src2, dst3,
                        path(link2_4, link2_3),
                        osnr
                )
        );
        // dev1/1 --- dev3/1
        // dev1/1 --- dev4/1
        // dev1/1 --...-- dev3/1
        // dev2/1 --- dev3/1
        return ImmutableList.copyOf(paths);
    }

    private WdmPath buildReversePath(WdmPath wdmPath) {
        return new WdmPath(wdmPath.dst(), wdmPath.src(),
                PathTestTool.buildReversePath(wdmPath.path()), wdmPath.osnr());
    }
}
