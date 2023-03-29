package org.onosproject.models.openroadm2.service;

import com.google.common.collect.ImmutableMap;
import org.onosproject.yang.AbstractYangModelRegistrator;
import org.onosproject.yang.gen.v1.ietfinettypes.rev20130715.IetfInetTypes;
import org.onosproject.yang.gen.v1.ietfyangtypes.rev20130715.IetfYangTypes;
import org.onosproject.yang.gen.v1.orgopenroadmcommonservicetypes.rev20171215.OrgOpenroadmCommonServiceTypes;
import org.onosproject.yang.gen.v1.orgopenroadmcommontypes.rev20171215.OrgOpenroadmCommonTypes;
import org.onosproject.yang.gen.v1.orgopenroadmotncommontypes.rev20171215.OrgOpenroadmOtnCommonTypes;
import org.onosproject.yang.gen.v1.orgopenroadmresource.rev20171215.OrgOpenroadmResource;
import org.onosproject.yang.gen.v1.orgopenroadmresourcetypes.rev20171215.OrgOpenroadmResourceTypes;
import org.onosproject.yang.gen.v1.orgopenroadmroutingconstraints.rev20171215.OrgOpenroadmRoutingConstraints;
import org.onosproject.yang.gen.v1.orgopenroadmservice.rev20171215.OrgOpenroadmService;
import org.onosproject.yang.gen.v1.orgopenroadmtopology.rev20171215.OrgOpenroadmTopology;
import org.onosproject.yang.model.DefaultYangModuleId;
import org.onosproject.yang.model.YangModuleId;
import org.onosproject.yang.runtime.AppModuleInfo;
import org.onosproject.yang.runtime.DefaultAppModuleInfo;
import org.osgi.service.component.annotations.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Component to register the Open ROADM service model and its dependencies.
 */
@Component(immediate = true)
public class OpenRoadm2ServiceModelRegistrator extends AbstractYangModelRegistrator {
    public OpenRoadm2ServiceModelRegistrator() {
        super(OpenRoadm2ServiceModelRegistrator.class, getAppInfo());
    }

    private static Map<YangModuleId, AppModuleInfo> getAppInfo() {
        Map<YangModuleId, AppModuleInfo> appInfo = new HashMap<>();
        // Dependencies for org-openroadm-service
        appInfo.put(new DefaultYangModuleId("ietf-yang-types", "2013-07-15"),
                new DefaultAppModuleInfo(IetfYangTypes.class, null));
        appInfo.put(new DefaultYangModuleId("org-openroadm-routing-constraints", "2017-12-15"),
                new DefaultAppModuleInfo(OrgOpenroadmRoutingConstraints.class, null));
        appInfo.put(new DefaultYangModuleId("org-openroadm-common-types", "2017-12-15"),
                new DefaultAppModuleInfo(OrgOpenroadmCommonTypes.class, null));
        appInfo.put(new DefaultYangModuleId("org-openroadm-resource-types", "2017-12-15"),
                new DefaultAppModuleInfo(OrgOpenroadmResourceTypes.class, null));
        appInfo.put(new DefaultYangModuleId("org-openroadm-common-service-types", "2017-12-15"),
                new DefaultAppModuleInfo(OrgOpenroadmCommonServiceTypes.class, null));
        appInfo.put(new DefaultYangModuleId("ietf-inet-types", "2013-07-15"),
                new DefaultAppModuleInfo(IetfInetTypes.class, null));
        appInfo.put(new DefaultYangModuleId("org-openroadm-topology", "2017-12-15"),
                new DefaultAppModuleInfo(OrgOpenroadmTopology.class, null));
        appInfo.put(new DefaultYangModuleId("org-openroadm-otn-common-types", "2017-12-15"),
                new DefaultAppModuleInfo(OrgOpenroadmOtnCommonTypes.class, null));
        appInfo.put(new DefaultYangModuleId("org-openroadm-resource", "2017-12-15"),
                new DefaultAppModuleInfo(OrgOpenroadmResource.class, null));
        appInfo.put(new DefaultYangModuleId("org-openroadm-service", "2017-12-15"),
                new DefaultAppModuleInfo(OrgOpenroadmService.class, null));
        return ImmutableMap.copyOf(appInfo);
    }
}
