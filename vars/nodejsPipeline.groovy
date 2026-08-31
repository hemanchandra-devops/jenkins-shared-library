def call(Map configMap) {
    pipeline {
        agent {
            node {
                label 'AGENT-1'
            }
        }

        environment {
            COURSE = 'Jenkins'
            ACC_ID = '634758830486'
            PROJECT = "${configMap.get('project')}"
            COMPONENT = "${configMap.get('component')}"
            APP_VERSION = ''
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
                        env.APP_VERSION = packageJson.version
                        echo "AppVersion: ${env.APP_VERSION}"
                    }
                }
            }

            stage('npm Install') {
                steps {
                    sh 'npm install'
                }
            }

            stage('Unit Test') {
                steps {
                    sh 'npm test'
                }
            }

            stage('ECR') {
                steps {
                    withAWS(
                        region: 'us-east-1',
                        credentials: 'aws'
                    ) {
                        sh """
                            aws ecr get-login-password --region us-east-1 | \
                            docker login \
                            --username AWS \
                            --password-stdin ${ACC_ID}.dkr.ecr.us-east-1.amazonaws.com

                            docker build \
                            -t ${ACC_ID}.dkr.ecr.us-east-1.amazonaws.com/${PROJECT}/${COMPONENT}:${APP_VERSION} .

                            docker push \
                            ${ACC_ID}.dkr.ecr.us-east-1.amazonaws.com/${PROJECT}/${COMPONENT}:${APP_VERSION}
                        """
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
                echo 'I will run if pipeline succeeds'
            }

            failure {
                echo 'I will run if pipeline fails'
            }

            aborted {
                echo 'Pipeline is aborted'
            }
        }
    }
}
