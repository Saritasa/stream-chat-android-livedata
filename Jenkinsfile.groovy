#!/usr/bin/env groovy

node('android') {
    try {
        GRADLE_HOME = tool(name: 'gradle-latest', type: 'gradle')
        JAVA_HOME = tool(name: 'jdk-latest', type: 'jdk')

        stage('clean up') {
            cleanWs()
        }

        stage('scm') {
            checkout(scm)
        }

        stage('build and publish') {
            withEnv(["PATH=$PATH:${GRADLE_HOME}/bin", "JAVA_HOME=${JAVA_HOME}"]) {
                sh("chmod +x gradlew")
                sh("./gradlew clean assemble --stacktrace")
                sh("./gradlew :livedata:uploadArchives --stacktrace")
            }
        }
    } catch (error) {
        currentBuild.result = 'FAILURE'
        println("ERROR: ${error}")
        emailext(
                subject: "Build - ${currentBuild.currentResult}: ${JOB_NAME}: ${BUILD_NUMBER}",
                body: "\
      <h1 style='text-align:center;color:#ecf0f1;background-color:#ff5252;border-color:#ff5252'>${currentBuild.currentResult}</h1>\
      <b>Job url</b>: ${BUILD_URL}<br>",
                recipientProviders: [requestor(), developers(), brokenBuildSuspects(), brokenTestsSuspects()]
        )
    }
}