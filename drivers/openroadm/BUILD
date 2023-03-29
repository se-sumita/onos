COMPILE_DEPS = CORE_DEPS + JACKSON + CLI + [
    "//drivers/utilities:onos-drivers-utilities",
    "//protocols/netconf/api:onos-protocols-netconf-api",
    "//protocols/netconf/ctl:onos-protocols-netconf-ctl",
    "//apps/optical-model:onos-apps-optical-model",
    "@velocity//jar",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//core/api:onos-api-tests",
    "//utils/osgi:onlab-osgi-tests",
]

BUNDLES = [
    "@velocity//jar",
    ":onos-drivers-openroadm",
    "//drivers/utilities:onos-drivers-utilities",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
    test_deps = TEST_DEPS,
    resources_root = 'src/main/resources',
    resources = glob(['src/main/resources/**']),
)

onos_app(
    app_name = "org.onosproject.drivers.openroadm",
    category = "Drivers",
    description = "Vendor specific drivers with OpenROADM adaptation.",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.netconf",
        "org.onosproject.optical-model",
        "org.onosproject.drivers.netconf",
    ],
    title = "Drivers with OpenROADM adaptation",
    url = "http://www.openroadm.org",
)
