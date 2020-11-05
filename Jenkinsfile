import com.anaplan.buildtools.jenkins_pipelines.ContainerTemplates
import com.anaplan.buildtools.jenkins_pipelines.DefaultConfig

@Library('Anaplan_Pipeline')

def BUILD_LABEL = "vdm-gradle-plugin.${UUID.randomUUID().toString()}"

pipeline {
    agent {
        kubernetes {
            label BUILD_LABEL
            yaml pod([ContainerTemplates.gradle([:], "8-jdk")])
        }
    }

    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(daysToKeepStr: '60'))
        timeout(time: 30, unit: 'MINUTES')
    }

    stages {
        stage('Build & Test') {
            steps {
                script {
                    container('gradle') {
                        try {
                            sh "./gradlew check -x functionalTest"
                        } finally {
                            junit '**/build/test-results/**/*.xml'
                        }
                    }
                }
            }
        }

        stage('Publish') {
            steps {
                container('gradle') {
                    withCredentials([usernamePassword(
                            credentialsId: DefaultConfig.ARTIFACTORY_UNSTABLE_CREDENTIAL_ID,
                            passwordVariable: 'ART_PASSWORD',
                            usernameVariable: 'ART_USERNAME'
                    )]) {
                        sh "./gradlew -PART_URL=https://artifacts.anaplan-np.net/artifactory -PART_REPO=anaplan-develop artifactoryPublish --info"
                    }
                }
            }
        }
    }

}
