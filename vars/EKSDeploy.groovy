def call (Map configMap) {
    pipeline {
        agent {
            node {
                label 'AGENT-1'
            }
        }
        environment { 
            appVersion = configMap.get("appVersion")
            deploy_to = configMap.get("deploy_to")
            ACC_ID = "634758830486"
            PROJECT = configMap.get("project")
            COMPONENT = configMap.get("component")
        }
        options {
            timeout(time: 30, unit: 'MINUTES') 
            disableConcurrentBuilds()
        }
        // parameters {
        //     string(name: 'APP_VERSION', description: 'Application version to deploy')
        //     choice(name: 'DEPLOY_TO', choices: ['dev', 'qa', 'prod'], description: 'Environment to deploy to')
        // }
        stages {
            stage('Deploy to EKS') {
                steps {
                    script {
                        withAWS(region: 'us-east-1', credentials: 'aws') {
                            sh """
                                aws eks update-kubeconfig --region us-east-1 --name ${PROJECT}-${deploy_to}
                                kubectl get nodes
                                sed -i "s/v1/${appVersion}/g" values.yaml
                                helm upgrade ${COMPONENT} . --install -f values.yaml -n ${PROJECT} --atomic --timeout 5m
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