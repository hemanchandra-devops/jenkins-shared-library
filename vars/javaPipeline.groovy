def call(Map configMap) {
    pipeline {
        agent {
            node {
                label 'AGENT-1'
            }
        }
        environment { 
            Course = 'Jenkins'
            appVersion = ""
            ACC_ID = "634758830486"
            PROJECT = configMap.get("project")
            COMPONENT = configMap.get("component")
        }
        options {
            timeout(time: 30, unit: 'MINUTES') 
            disableConcurrentBuilds()
        }
        stages {
            stage('App Version') {
                steps {
                    script {
                        def pom = readFile 'pom.xml'
                        appVersion = pom.version
                        echo "AppVersion: ${appVersion}"
                    }
                }
            }
            stage('mvn clean package') {
                steps {
                    script {
                        sh """
                            mvn clean package
                        """
                    }
                }
            }
            stage('ECR') {
                steps {
                    script {
                        withAWS(region: 'us-east-1', credentials: 'aws') {
                            sh """
                                aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin ${ACC_ID}.dkr.ecr.us-east-1.amazonaws.com
                                docker build -t ${ACC_ID}.dkr.ecr.us-east-1.amazonaws.com/${PROJECT}/${COMPONENT}:${appVersion} .
                                docker push ${ACC_ID}.dkr.ecr.us-east-1.amazonaws.com/${PROJECT}/${COMPONENT}:${appVersion}
                            """
                        }
                    }
                }
            }
            stage('Trigger Deploy') {
                steps {
                    build job: "${COMPONENT}-deploy",
                        wait: false,
                        propagate: false,
                        parameters: [
                            string(name: 'APP_VERSION', value: "${appVersion}"),
                            string(name: 'DEPLOY_TO', value: "dev")
                        ]            
                }
            }
        
        

        }
        post { 
            always { 
                echo 'I will always say Hello again!'
                cleanWs()

            }
            success {
                echo 'I will run if pipeline sucess'
            }
            failure {
                echo 'I will run if pipeline failed'
            }
            aborted {
                echo 'pipeline is aborted'
            }
        }
    }
}