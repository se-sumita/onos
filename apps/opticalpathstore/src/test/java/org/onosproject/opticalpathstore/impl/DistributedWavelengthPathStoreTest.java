package org.onosproject.opticalpathstore.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.OchSignal;
import org.onosproject.net.Path;
import org.onosproject.net.openroadm.model.ModulationFormat;
import org.onosproject.net.openroadm.model.Rate;
import org.onosproject.net.openroadm.model.WavelengthPath;
import org.onosproject.store.service.TestStorageService;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.onosproject.opticalpathstore.impl.PathTestTool.*;

public class DistributedWavelengthPathStoreTest {
    DistributedWavelengthPathStore pathStoreImpl;

    @Before
    public void setUp() {
        pathStoreImpl = new DistributedWavelengthPathStore();
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

    @Test
    public void testBuild() {
        WavelengthPath path1 = buildBasicPath1();
        OchSignal signal = OchSignal.newDwdmSlot(ChannelSpacing.CHL_50GHZ, 10);
        Path path = path(linkR1, linkR2);

        assertEquals(1, path1.id());
        assertEquals(2, path1.groupId());
        assertEquals(10, path1.frequencyId());
        assertEquals(signal, path1.signal());
        assertEquals(path, path1.path());
        assertEquals(Rate.R100G, path1.rate());
        assertEquals(ModulationFormat.DP_QPSK, path1.modulationFormat());
        assertEquals(3.0, path1.qValue(), 0.0);
        assertEquals(1.0, path1.qThreshold(), 0.0);
        assertEquals("NAME1", path1.name());
        assertFalse(path1.isSubmitted());

        // Use `create` method
        path1 = WavelengthPath.create(
                1, 2, 10, signal, linkT1, linkT2, path,
                Rate.R150G, ModulationFormat.DP_QAM8, 5.0, 4.0, "NAME");
        assertEquals(1, path1.id());
        assertEquals(2, path1.groupId());
        assertEquals(10, path1.frequencyId());
        assertEquals(signal, path1.signal());
        assertEquals(path, path1.path());
        assertEquals(Rate.R150G, path1.rate());
        assertEquals(ModulationFormat.DP_QAM8, path1.modulationFormat());
        assertEquals(5.0, path1.qValue(), 0.0);
        assertEquals(4.0, path1.qThreshold(), 0.0);
        assertEquals("NAME", path1.name());
        assertFalse(path1.isSubmitted());

        // Submit
        path1 = path1.cloneAsSubmitted();
        assertEquals(1, path1.id());
        assertEquals(2, path1.groupId());
        assertEquals(10, path1.frequencyId());
        assertEquals(signal, path1.signal());
        assertEquals(path, path1.path());
        assertEquals(Rate.R150G, path1.rate());
        assertEquals(ModulationFormat.DP_QAM8, path1.modulationFormat());
        assertEquals(5.0, path1.qValue(), 0.0);
        assertEquals(4.0, path1.qThreshold(), 0.0);
        assertTrue(path1.isSubmitted());
    }

    @Test
    public void testAdd() {
        // 1 path
        WavelengthPath path1 = buildBasicPath1();
        pathStoreImpl.add(path1);

        assertEquals(1, pathStoreImpl.stream().count());

        // check cache
        assertEquals(2, pathStoreImpl.terminationPointMap.size());
        assertEquals(1, pathStoreImpl.terminationPointMap.get(linkT1.src()).size());
        assertEquals(1, pathStoreImpl.terminationPointMap.get(linkT3.dst()).size());
        assertEquals(1, pathStoreImpl.pairMap.size());
        assertEquals(1, pathStoreImpl.groupMap.size());

        // 2 paths
        WavelengthPath path2 = buildBasicPath2();
        pathStoreImpl.add(path2);

        assertEquals(2, pathStoreImpl.getPaths().size());

        // check cache
        assertEquals(4, pathStoreImpl.terminationPointMap.size());
        assertEquals(1, pathStoreImpl.terminationPointMap.get(linkT1.src()).size());
        assertEquals(1, pathStoreImpl.terminationPointMap.get(linkT3.dst()).size());
        assertEquals(1, pathStoreImpl.terminationPointMap.get(linkT2.src()).size());
        assertEquals(1, pathStoreImpl.terminationPointMap.get(linkT4.dst()).size());
        assertEquals(2, pathStoreImpl.pairMap.size());
        assertEquals(2, pathStoreImpl.groupMap.size());
    }

    @Test
    public void testAddAll() {
        pathStoreImpl.addAll(buildBasicPaths());
        assertEquals(2, pathStoreImpl.getPaths().size());
    }

    @Test
    public void testClear() {
        List<WavelengthPath> paths = buildBasicPaths();
        pathStoreImpl.addAll(paths);
        pathStoreImpl.clear();

        assertEquals(0, pathStoreImpl.getPaths().size());

        // check cache
        assertEquals(0, pathStoreImpl.terminationPointMap.size());
        assertEquals(0, pathStoreImpl.pairMap.size());
        assertEquals(0, pathStoreImpl.groupMap.size());

        // use remove()
        pathStoreImpl.addAll(paths);
        pathStoreImpl.remove(null, null);
        assertEquals(0, pathStoreImpl.getPaths().size());

        // check cache
        assertEquals(0, pathStoreImpl.terminationPointMap.size());
        assertEquals(0, pathStoreImpl.pairMap.size());
        assertEquals(0, pathStoreImpl.groupMap.size());
    }

    @Test
    public void testGet() {
        List<WavelengthPath> paths = buildBasicPaths();
        pathStoreImpl.addAll(paths);

        long id = paths.get(0).id();
        WavelengthPath path = pathStoreImpl.get(id);
        assertEquals(path, paths.get(0));

        id = paths.get(1).id();
        path = pathStoreImpl.get(id);
        assertEquals(path, paths.get(1));
    }

    @Test
    public void testGetPaths() {
        List<WavelengthPath> paths = buildBasicPaths();
        pathStoreImpl.addAll(paths);

        Collection<WavelengthPath> actual = pathStoreImpl.getPaths();
        assertEquals(2, actual.size());
        assertEquals(ImmutableSet.copyOf(paths), ImmutableSet.copyOf(actual));
    }

    @Test
    public void testGetByGroup() {
        List<WavelengthPath> paths = Lists.newArrayList(buildBasicPaths()); // group 2, 3
        paths.add(buildBasicPath3()); // Add to group 3
        pathStoreImpl.addAll(paths);

        List<WavelengthPath> foundPaths = pathStoreImpl.findByGroupId(3);

        assertEquals(2, foundPaths.size());
        assertEquals(paths.subList(1, paths.size()), foundPaths);
    }

    @Test
    public void testGetPathsByPort() {
        List<WavelengthPath> paths = buildBasicPaths();
        pathStoreImpl.addAll(paths);

        ConnectPoint p1 = paths.get(0).src(); // devT1/1
        Collection<WavelengthPath> actual = pathStoreImpl.getPathsByPort(p1);
        assertEquals(1, actual.size());
        assertEquals(paths.get(0), Iterables.getFirst(actual, null));

        ConnectPoint p2 = paths.get(0).dst(); // devT1/1
        actual = pathStoreImpl.getPathsByPort(p2);
        assertEquals(1, actual.size());
        assertEquals(paths.get(0), Iterables.getFirst(actual, null));

        ConnectPoint p3 = paths.get(1).src(); // devT3/1
        actual = pathStoreImpl.getPathsByPort(p3);
        assertEquals(1, actual.size());
        assertEquals(paths.get(1), Iterables.getFirst(actual, null));

        // use getPaths()
        actual = pathStoreImpl.getPaths(p3, null);
        assertEquals(1, actual.size());
        assertEquals(paths.get(1), Iterables.getFirst(actual, null));

        ConnectPoint p4 = paths.get(1).src(); // devT3/1
        actual = pathStoreImpl.getPaths(null, p4);
        assertEquals(1, actual.size());
        assertEquals(paths.get(1), Iterables.getFirst(actual, null));
    }

    @Test
    public void testGetPathsWithPorts() {
        List<WavelengthPath> paths = buildBasicPaths();
        pathStoreImpl.addAll(paths);

        ConnectPoint p1src = paths.get(0).src(); // devT1/1
        ConnectPoint p1dst = paths.get(0).dst(); // devT3/1
        Collection<WavelengthPath> actual = pathStoreImpl.getPaths(p1src, p1dst);
        assertEquals(1, actual.size());
        assertEquals(paths.get(0), Iterables.getFirst(actual, null));

        ConnectPoint p2src = paths.get(1).src(); // devT2/1
        ConnectPoint p2dst = paths.get(1).dst(); // devT4/1
        actual = pathStoreImpl.getPaths(p2src, p2dst);
        assertEquals(1, actual.size());
        assertEquals(paths.get(1), Iterables.getFirst(actual, null));
    }

    @Test
    public void testFindByOmsPortAndLambda() {
        List<WavelengthPath> paths = Lists.newArrayList(buildBasicPaths());
        paths.add(buildBasicPath3()); // Add to group 3
        pathStoreImpl.addAll(paths);

        ConnectPoint p1src = paths.get(0).path().links().get(1).src(); // devT1/1
        OchSignal p1signal = paths.get(0).signal();
        WavelengthPath actual = pathStoreImpl.findByOmsPortAndLambda(p1src, p1signal);
        assertEquals(paths.get(0), actual);

        ConnectPoint p2src = paths.get(2).path().links().get(0).dst(); // devT2/1
        OchSignal p2signal = paths.get(2).signal();
        actual = pathStoreImpl.findByOmsPortAndLambda(p2src, p2signal);
        assertEquals(paths.get(2), actual);

        actual = pathStoreImpl.findByOmsPortAndLambda(p1src, p2signal);
        assertNull(actual);
    }

    @Test
    public void testRemoveByPort() {
        List<WavelengthPath> paths = buildBasicPaths();
        pathStoreImpl.addAll(paths);

        ConnectPoint p1 = paths.get(0).src(); // dev1/1
        pathStoreImpl.remove(p1);
        assertEquals(1, pathStoreImpl.getPaths().size());
        assertEquals(paths.get(1), Iterables.getFirst(pathStoreImpl.getPaths(), null));

        // use remove()
        pathStoreImpl.clear();
        pathStoreImpl.addAll(paths);
        pathStoreImpl.remove(p1, null);
        assertEquals(1, pathStoreImpl.getPaths().size());
        assertEquals(paths.get(1), Iterables.getFirst(pathStoreImpl.getPaths(), null));

        pathStoreImpl.clear();
        pathStoreImpl.addAll(paths);
        pathStoreImpl.remove(null, p1);
        assertEquals(1, pathStoreImpl.getPaths().size());
        assertEquals(paths.get(1), Iterables.getFirst(pathStoreImpl.getPaths(), null));
    }

    @Test
    public void testRemoveAll() {
        List<WavelengthPath> paths = Lists.newArrayList(buildBasicPaths()); // group 2, 3
        paths.add(buildBasicPath3()); // Add to group 3
        pathStoreImpl.addAll(paths);

        pathStoreImpl.removeAll(3);
        assertEquals(1, pathStoreImpl.getPaths().size());
        assertEquals(paths.get(0), Iterables.getFirst(pathStoreImpl.getPaths(), null));

        pathStoreImpl.removeAll(2);
        assertEquals(0, pathStoreImpl.getPaths().size());
    }

    @Test
    public void testRemove() {
        List<WavelengthPath> paths = buildBasicPaths();
        pathStoreImpl.addAll(paths);

        ConnectPoint p1src = paths.get(0).src(); // dev1/1
        ConnectPoint p1dst = paths.get(0).dst(); // dev3/1

        pathStoreImpl.remove(p1src, p1dst);
        assertEquals(1, pathStoreImpl.getPaths().size());
        assertEquals(paths.get(1), Iterables.getFirst(pathStoreImpl.getPaths(), null));
    }

    @Test
    public void testIssueGroupId() {
        long id = pathStoreImpl.issueGroupId();
        assertEquals(1, id);
        id = pathStoreImpl.issueGroupId();
        assertEquals(2, id);

        pathStoreImpl.releaseGroupIdIfPossible(id);
        id = pathStoreImpl.issueGroupId();
        assertEquals(2, id);
    }

    // ROADM-TO-ROADM
    //   devR1/1 <= link R1 => devR2/1
    //   devR2/2 <= link R2 => devR3/1
    //   devR1/2 <= link R3 => devR3/2
    // TRANSPONDER-TO-ROADM
    //   devT1/1 <= link T1 => devR1/3
    //   devT2/1 <= link T2 => devR1/4
    //   devT3/1 <= link T3 => devR3/3
    //   devT4/1 <= link T4 => devR3/4
    //   devT5/1 <= link T5 => devR1/5
    //   devT6/1 <= link T6 => devR3/5

    private static Link linkR1 = link("devR1/1", "devR2/1");
    private static Link linkR2 = link("devR2/2", "devR3/1");
    private static Link linkR3 = link("devR1/2", "devR3/2");

    private static Link linkT1 = link("devT1/1", "devR1/3");
    private static Link linkT2 = link("devT2/1", "devR1/4");
    private static Link linkT3 = link("devR3/4", "devT3/1");
    private static Link linkT4 = link("devR3/4", "devT4/1");
    private static Link linkT5 = link("devT5/1", "devR1/5");
    private static Link linkT6 = link("devR3/5", "devT6/1");

    private WavelengthPath buildBasicPath1() {
        return pathStoreImpl.build(2, linkT1, linkT3,
                path(linkR1, linkR2), 10, och(10),
                Rate.R100G, ModulationFormat.DP_QPSK, 3.0, 1.0, "NAME1");
    }

    private WavelengthPath buildBasicPath2() {
        return pathStoreImpl.build(3, linkT2, linkT4,
                path(linkR1, linkR2), 20, och(20),
                Rate.R150G, ModulationFormat.DP_QAM8, 4.0, 2.0, "NAME2");
    }

    private WavelengthPath buildBasicPath3() {
        return pathStoreImpl.build(3, linkT5, linkT6,
                path(linkR3), 21, och(21),
                Rate.R100G, ModulationFormat.DP_QPSK, 3.5, 1.0, "NAME3");
    }

    private List<WavelengthPath> buildBasicPaths() {
        return ImmutableList.of(
                        buildBasicPath1(),
                        buildBasicPath2());
    }
}
