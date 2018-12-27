class test implements Serializable {
    private String repo
    private String credentialsId
    def setRepo(value) {
        repo = value
    }
    def getRepo() {
        repo
    }

    def setCredentialsId(value) {
        credentialsId = value
    }
    def getCredentialsId() {
        credentialsId
    }

    def setBuildType(value) {
        buildType = value
    }
    def getBuildType() {
        buildType
    }

    def setProjectName(value) {
        projectName = value
    }
    def getProjectName() {
        projectName
    }
    def setAppName(value) {
        appName = value
    }
    def getAppName() {
        appName
    }
}
