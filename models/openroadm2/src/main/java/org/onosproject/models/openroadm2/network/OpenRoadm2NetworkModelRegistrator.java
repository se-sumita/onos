package org.onosproject.models.openroadm2.network;

import com.google.common.collect.ImmutableMap;
import org.onosproject.yang.AbstractYangModelRegistrator;
import org.onosproject.yang.gen.v1.ietfinettypes.rev20130715.IetfInetTypes;
import org.onosproject.yang.gen.v1.ietfnetwork.rev20150608.IetfNetwork;
import org.onosproject.yang.gen.v1.ietfyangtypes.rev20130715.IetfYangTypes;
import org.onosproject.yang.gen.v1.orgopenroadmcommontypes.rev20171215.OrgOpenroadmCommonTypes;
import org.onosproject.yang.gen.v1.orgopenroadmexternalpluggable.rev20171215.OrgOpenroadmExternalPluggable;
import org.onosproject.yang.gen.v1.orgopenroadmnetwork.rev20171215.OrgOpenroadmNetwork;
import org.onosproject.yang.gen.v1.orgopenroadmnetworktypes.rev20171215.OrgOpenroadmNetworkTypes;
import org.onosproject.yang.gen.v1.orgopenroadmotncommontypes.rev20171215.OrgOpenroadmOtnCommonTypes;
import org.onosproject.yang.gen.v1.orgopenroadmporttypes.rev20171215.OrgOpenroadmPortTypes;
import org.onosproject.yang.gen.v1.orgopenroadmroadm.rev20171215.OrgOpenroadmRoadm;
import org.onosproject.yang.gen.v1.orgopenroadmxponder.rev20171215.OrgOpenroadmXponder;
import org.onosproject.yang.model.DefaultYangModuleId;
import org.onosproject.yang.model.YangModuleId;
import org.onosproject.yang.runtime.AppModuleInfo;
import org.onosproject.yang.runtime.DefaultAppModuleInfo;
import org.osgi.service.component.annotations.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Component to register the Open ROADM network model and its dependencies.
 */
@Component(immediate = true)
public class OpenRoadm2NetworkModelRegistrator extends AbstractYangModelRegistrator {
    public OpenRoadm2NetworkModelRegistrator() {
        super(OpenRoadm2NetworkModelRegistrator.class, getAppInfo());
    }

    private static Map<YangModuleId, AppModuleInfo> getAppInfo() {
        Map<YangModuleId, AppModuleInfo> appInfo = new HashMap<>();
        // Dependencies for org-openroadm-network
        appInfo.put(new DefaultYangModuleId("ietf-network", "2015-06-08"),
                new DefaultAppModuleInfo(IetfNetwork.class, null));
        appInfo.put(new DefaultYangModuleId("org-openroadm-network-types", "2017-12-15"),
                new DefaultAppModuleInfo(OrgOpenroadmNetworkTypes.class, null));
        appInfo.put(new DefaultYangModuleId("org-openroadm-roadm", "2017-12-15"),
                new DefaultAppModuleInfo(OrgOpenroadmRoadm.class, null));
        appInfo.put(new DefaultYangModuleId("org-openroadm-external-pluggable", "2017-12-15"),
                new DefaultAppModuleInfo(OrgOpenroadmExternalPluggable.class, null));
        appInfo.put(new DefaultYangModuleId("org-openroadm-xponder", "2017-12-15"),
                new DefaultAppModuleInfo(OrgOpenroadmXponder.class, null));
        appInfo.put(new DefaultYangModuleId("ietf-inet-types", "2013-07-15"),
                new DefaultAppModuleInfo(IetfInetTypes.class, null));
        appInfo.put(new DefaultYangModuleId("ietf-yang-types", "2013-07-15"),
                new DefaultAppModuleInfo(IetfYangTypes.class, null));
        appInfo.put(new DefaultYangModuleId("org-openroadm-common-types", "2017-12-15"),
                new DefaultAppModuleInfo(OrgOpenroadmCommonTypes.class, null));
        appInfo.put(new DefaultYangModuleId("org-openroadm-port-types", "2017-12-15"),
                new DefaultAppModuleInfo(OrgOpenroadmPortTypes.class, null));
        appInfo.put(new DefaultYangModuleId("org-openroadm-otn-common-types", "2017-12-15"),
                new DefaultAppModuleInfo(OrgOpenroadmOtnCommonTypes.class, null));

        appInfo.put(new DefaultYangModuleId("org-openroadm-network", "2017-12-15"),
                new DefaultAppModuleInfo(OrgOpenroadmNetwork.class, null));
        return ImmutableMap.copyOf(appInfo);
    }
}
