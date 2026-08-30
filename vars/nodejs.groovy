def call(configMap) {
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
            PROJECT = "${configMap.project}"
            COMPONENT = "${configMap.component}"
        }
        options {
            timeout(time: 60, unit: 'MINUTES') 
            disableConcurrentBuilds()
        }
        
        stages {
            stage('App Version') {
                steps {
                    script {
                        def packageJson = readJSON file: 'package.json'
                        appVersion = packageJson.version
                        echo "AppVersion: ${appVersion}"
                    }
                }
            }
            stage('npm Install') {
                steps {
                    script {
                        sh """
                            npm install
                        """
                    }
                }
            }
            stage('Unit Test') {
                steps {
                    script {
                        sh """
                            npm test
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