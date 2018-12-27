#!groovy

import com.cloudbees.groovy.cps.NonCPS
import java.io.File
import java.util.UUID
import groovy.json.JsonOutput;
import groovy.json.JsonSlurperClassic;

def call(body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    print "microserviceBuilderPipeline : config = ${config}"

    def jenkinsProject = "basic-spring-boot-build"
    def projectName = config.projectName
    def appName = config.appName
    def checkProjectName = ""
    def devTag  = "0.0-" + "${BUILD_NUMBER}"

    node("${config.buildType}") {

        stage('Checkout Source') {
            git credentialsId: "${config.credentialsId}", url: "${config.repo}"
        }

        createProject(projectName, appName, jenkinsProject) 

	if ( "${config.buildType}" == "maven" ) {
            mavenCICD(projectName,appName,devTag)
        }
        else {
           echo "${config.buildType} is not supported"
	}

    }

}

def createProject(String appProject, String appName, String jenkinsProjectName) {
    stage('create project'){
        def jenkinsProject = jenkinsProjectName
        def flag = ""
        sh 'oc login https://172.30.0.1:443 --insecure-skip-tls-verify -u admin -p admin'
        sh 'oc projects -q >> 123456.txt'
        
        echo "${jenkinsProject}   -- ${appProject}  -- ${appName}" 

        script{
	    def projects = readFile('123456.txt').trim().tokenize('\n')
            for (x in projects) {
                def project = x
                if ( "${project}" == "${appProject}" ){
                    flag = "matched"
                }
            }
	}
        if ( "${flag}" != "matched") {
            sh "oc new-project ${appProject}"
            sh "oc policy add-role-to-user edit system:serviceaccount:${jenkinsProject}:jenkins -n ${appProject}"
            sh 'oc create -f ./tasks.template'
            sh "oc new-app --template=tasks --param=PROJECT_NAME=${appProject} --param=APP_NAME=${appName}"
        }
    }
}

def mavenCICD(String projectName, String appName, String tag){

    stage('BuildWar') {
        sh 'mvn --version'
        sh "mvn clean package -DskipTests"
    }

    stage('Unit Tests') {
        echo "Running Unit Tests"
        sh "mvn test"
    }

    stage('Build and Tag OpenShift Image') {
        echo "${projectName}  ${appName}   ${tag}"

        echo "Building OpenShift container image ${appName}:${tag}"
        sh "oc start-build ${appName} --follow --from-file=./target/helloworld-0.0.1-SNAPSHOT.jar -n ${projectName}"

        openshiftTag alias: 'false', destStream: "${appName}", destTag: tag, destinationNamespace: "${projectName}", namespace: "${projectName}", srcStream: "${appName}", srcTag: 'latest', verbose: 'false'

    }

    stage('Deploy') {
        echo "Deploying container image to Development Project"
        sh "oc set image dc/${appName} ${appName}=docker-registry.default.svc:5000/${projectName}/${appName}:${tag} -n ${projectName}"
        openshiftDeploy depCfg: appName, namespace: projectName, verbose: 'false', waitTime: '', waitUnit: 'sec'

        openshiftVerifyDeployment depCfg: appName, namespace: projectName, replicaCount: '1', verbose: 'false', verifyReplicaCount: 'false', waitTime: '', waitUnit: 'sec'

        openshiftVerifyService namespace: projectName, svcName: appName, verbose: 'false'
    }
}
