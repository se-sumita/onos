package org.onosproject.opticalpathoptimizer.model;

import org.onlab.graph.DefaultEdgeWeigher;
import org.onlab.graph.ScalarWeight;
import org.onlab.graph.Weight;
import org.onosproject.net.Link;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.topology.LinkWeigher;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyVertex;
import org.onosproject.opticalpathoptimizer.OpenRoadmConfigHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Get the data from DynamicConfig and calculate the Weight.
 */
public class FiberSpanWeigher extends DefaultEdgeWeigher<TopologyVertex, TopologyEdge> implements LinkWeigher {
    private final Logger log = LoggerFactory.getLogger(FiberSpanWeigher.class);

    private final DeviceService deviceService;
    private final OpenRoadmConfigHelper configHelper;

    public FiberSpanWeigher(DeviceService deviceService, OpenRoadmConfigHelper configHelper) {
        this.deviceService = deviceService;
        this.configHelper = configHelper;
    }

    @Override
    public Weight weight(TopologyEdge edge) {
        Link l = edge.link();
        double totalSpan = 0.0;

        // Avoid inactive links
        if (l.state() == Link.State.INACTIVE) {
            log.warn("{} is not active", l);
            return ScalarWeight.NON_VIABLE_WEIGHT;
        }

        // If the port type is not between ROADM or ROADM/AMP, it is not acquired
        Port srcPort = deviceService.getPort(l.src());
        Port dstPort = deviceService.getPort(l.dst());
        boolean roadmToRoadm;
        if (srcPort.type() == Port.Type.OMS) {
            roadmToRoadm = (dstPort.type() == Port.Type.OMS || dstPort.type() == Port.Type.FIBER);
        } else if (dstPort.type() == Port.Type.OMS) {
            roadmToRoadm = srcPort.type() == Port.Type.FIBER;
        } else {
            roadmToRoadm = srcPort.type() == Port.Type.FIBER && dstPort.type() == Port.Type.FIBER;
        }
        if (!roadmToRoadm) {
            return ScalarWeight.NON_VIABLE_WEIGHT;
        }

        OpenRoadmModelLink modelLink = configHelper.getLinkDetail(l.src(), l.dst());

        /* Get the total span length (km) */
        totalSpan = modelLink.getTotalSpan();

        /* If the total span length (km) is 0, do not select it. */
        if (totalSpan <= 0) {
            return ScalarWeight.NON_VIABLE_WEIGHT;
        }

        return new ScalarWeight(totalSpan);
    }
}
