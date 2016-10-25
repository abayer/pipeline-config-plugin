pipeline {
    agent none

    environment {
        MAVEN_OPTS = "-Xmx1024m"
    }

    parameters {
        stringParam(defaultValue: "install", description: "What Maven goal to call", name: "MAVEN_GOAL")
    }
    
    jobProperties {
        buildDiscarder(logRotator(numToKeepStr:'5'))
    }
    
    triggers {
        cron('@daily')
    }

    stages {
        stage("Build") {
            agent docker: "maven:3.3.9-jdk-8"

            steps {
                checkout scm
                sh "mvn clean ${env.MAVEN_GOAL} -DskipTests"
            }

            post {
                success {
                    archive "**/target/**/*.jar"
                }
            }
        }

        stage("Test") {
            agent label:"agent1"
            
            steps {
                sh "mvn clean ${env.MAVEN_GOAL} -B -Dmaven.test.failure.ignore=true"
            }

            post {
                success {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
    }

    postBuild {
        always {
            echo "Build done"
        }
    }
    
    notifications {
        success {
            mail to: "abayer@cloudbees.com", subject: "Build Successful", body: "So there"
        }
        failure {
            mail to: "abayer@cloudbees.com", subject: "Build Failed", body: "So there"
        }
        unstable {
            mail to: "abayer@cloudbees.com", subject: "Build Unstable", body: "So there"
        }
    }

}
