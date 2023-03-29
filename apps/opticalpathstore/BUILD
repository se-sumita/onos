COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + [
    "//core/store/serializers:onos-core-serializers",
]

TEST_DEPS = TEST_ADAPTERS + TEST_REST + [
    "//core/api:onos-api-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.opticalpathstore",
    category = "Optical",
    description = "Optical Path store.",
    required_apps = [],
    title = "Optical Path Store",
    url = "http://onosproject.org",
)
