java_test(
    name = "QuotaEnforcerTest",
    srcs = [
        "QuotaEnforcerTest.java",
    ],
    deps = [
        "//3rdparty/jvm/ch/qos/logback:logback_classic",
        "//3rdparty/jvm/ch/qos/logback:logback_core",
        "//3rdparty/jvm/org/mockito:mockito_core",
        "//3rdparty/jvm/org/slf4j:slf4j_api",
        "//kafka_enforcer_common/src/main/java/com/tesla/data/enforcer",
        "//kafka_quota_enforcer/src/main/java/com/tesla/data/quota/enforcer",
    ],
)

java_test(
    name = "ValidateCommandTest",
    srcs = [
        "ValidateCommandTest.java",
    ],
    deps = [
        "//kafka_enforcer_common/src/main/java/com/tesla/data/enforcer",
        "//kafka_quota_enforcer/src/main/java/com/tesla/data/quota/enforcer",
    ],
)

java_test(
    name = "ConfiguredQuotaTest",
    srcs = [
        "ConfiguredQuotaTest.java",
    ],
    deps = [
        "//3rdparty/jvm/org/mockito:mockito_core",
        "//kafka_enforcer_common/src/main/java/com/tesla/data/enforcer",
        "//kafka_quota_enforcer/src/main/java/com/tesla/data/quota/enforcer",
    ],
)

java_test(
    name = "AdminClientQuotaServiceTest",
    srcs = [
        "AdminClientQuotaServiceTest.java",
    ],
    deps = [
        "//3rdparty/jvm/org/apache/kafka",
        "//3rdparty/jvm/org/apache/kafka:kafka_clients",
        "//3rdparty/jvm/org/mockito:mockito_core",
        "//3rdparty/jvm/org/scala_lang:scala_library",
        "//kafka_quota_enforcer/src/main/java/com/tesla/data/quota/enforcer",
    ],
)
