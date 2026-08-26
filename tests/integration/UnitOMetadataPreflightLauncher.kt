package parker.core.runtime

/** Acceptance-only one-shot entry point. It constructs no Parker production composition. */
fun main() {
    check(System.getProperty("user.name") == "parker") { "Unit O.3 must run as the Parker principal" }
    UnitOMetadataPreflightAcceptanceTest()
        .`exact locked manifests pass metadata-only preflight and emit bounded record`()
}
