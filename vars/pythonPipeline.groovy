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
                        def appVersion = readFile('version').trim()
                        echo "AppVersion: ${appVersion}"
                    }
                }
            }
            stage('pip install') {
                steps {
                    script {
                        sh """
                            pip3 install -r requirements.txt
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