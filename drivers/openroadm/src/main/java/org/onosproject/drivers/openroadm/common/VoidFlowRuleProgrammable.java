package org.onosproject.drivers.openroadm.common;

import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleProgrammable;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Flow rule that do nothing.
 */
public class VoidFlowRuleProgrammable extends AbstractHandlerBehaviour implements FlowRuleProgrammable {
    private final Logger log = getLogger(getClass());

    @Override
    public Collection<FlowEntry> getFlowEntries() {
        log.debug("getting flow entries for device {}", handler().data().deviceId());
        return Collections.emptyList();
    }

    @Override
    public Collection<FlowRule> applyFlowRules(Collection<FlowRule> rules) {
        log.debug("applying flow rules for device {}: {}", handler().data().deviceId(), rules);
        return rules;
    }

    @Override
    public Collection<FlowRule> removeFlowRules(Collection<FlowRule> rules) {
        log.debug("removing flow rules for device {}: {}", handler().data().deviceId(), rules);
        return rules;
    }
}
