COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + CLI + ONOS_YANG + [
    "@onos_yang_runtime//jar",
    "//core/store/serializers:onos-core-serializers",
    "//models/openroadm2:onos-models-openroadm2",
    "//apps/yang:onos-apps-yang",
    "//apps/config:onos-apps-config",
    "//apps/optical-model:onos-apps-optical-model",
    '//apps/opticalpathstore:onos-apps-opticalpathstore',
    '//apps/openroadmprovider:onos-apps-openroadmprovider',
]

TEST_DEPS = TEST_ADAPTERS + TEST_REST + [
    "//core/api:onos-api-tests",
    "//utils/osgi:onlab-osgi-tests",
]

APPS = [
    "org.onosproject.config",
    "org.onosproject.optical-model",
    "org.onosproject.models.openroadm2",
    "org.onosproject.opticalpathstore",
    "org.onosproject.openroadmprovider"
]

osgi_jar_with_tests(
    karaf_command_packages = [
        "org.onosproject.opticalpathoptimizer.cli",
        "org.onosproject.opticalpathoptimizer.cli.completer"
    ],
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.opticalpathoptimizer",
    category = "Optical",
    description = "Automatic setting of the optical path for optimum OSNR.",
    required_apps = APPS,
    title = "Optical Path Optimizer",
    url = "http://onosproject.org",
)
