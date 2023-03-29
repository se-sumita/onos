COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + ONOS_YANG + [
    "@onos_yang_runtime//jar",
    "//models/openroadm2:onos-models-openroadm2",
    "//apps/yang:onos-apps-yang",
    "//apps/config:onos-apps-config",
    "//apps/optical-model:onos-apps-optical-model",
    '//apps/opticalpathstore:onos-apps-opticalpathstore',
]

TEST_DEPS = TEST_ADAPTERS + [
    "//core/api:onos-api-tests",
]

APPS = [
    "org.onosproject.yang",
    "org.onosproject.config",
    "org.onosproject.optical-model",
    "org.onosproject.models.openroadm2",
    "org.onosproject.opticalpathstore",
    "org.onosproject.restconf",
    "org.onosproject.protocols.restconfserver"
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.openroadmprovider",
    category = "Provider",
    description = "Provides network/service information as OpenROADM.",
    required_apps = APPS,
    title = "OpenROADM Network/Service Provider",
    url = "http://onosproject.org",
)
