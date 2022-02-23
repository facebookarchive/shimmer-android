load("//tools/build_defs:fb_native_wrapper.bzl", "fb_native")
load("//tools/build_defs/oss:shimmer_defs.bzl", "SHIMMER_SUPPORT_ANNOTATIONS", "fb_core_android_library")

fb_core_android_library(
    name = "shimmer",
    srcs = glob(["src/main/java/**/*.java"]),
    required_for_source_only_abi = True,
    visibility = ["PUBLIC"],
    deps = [
        ":res",
        SHIMMER_SUPPORT_ANNOTATIONS,
    ],
)

fb_native.android_resource(
    name = "res",
    package = "com.facebook.shimmer",
    res = "src/main/res",
    visibility = ["PUBLIC"],
)
