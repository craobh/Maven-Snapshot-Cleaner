/**
 * Config
 */
class Configuration {
    private val homeFolder = System.getProperty("user.home")
    var path = homeFolder + "/.m2/repository"
    var dryRun = true
    var printDetails = true
    var maxAgeSnapshotsInDays = 60
    var maxAgeInDays = 90
    var snapshotsOnly = true
}
