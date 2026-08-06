package pl.diplomat.infrastructure.appinfo

data class AppBuildInfo(
    val versionName: String,
    val gitCommitHash: String,
    val apkBuiltAt: String,
)
