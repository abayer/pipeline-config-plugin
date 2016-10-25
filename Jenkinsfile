pipeline {
    agent none

    environment {
        MAVEN_OPTS = "-Xmx1024m"
        // These are needed because the docker image doesn't have a usable .gitconfig yaaaay
        GIT_COMMITER_NAME = "abayer"
        GIT_COMMITTER_EMAIL = "abayer@cloudbees.com"
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

    tools {
        maven "Maven 3.3.9"
        jdk "Oracle JDK 8u40"
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
            agent label:"any-executor"

            steps {
                checkout scm
                sh "mvn clean test -B -Dmaven.test.failure.ignore=true"
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
