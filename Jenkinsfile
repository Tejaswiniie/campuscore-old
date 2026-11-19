// CampusCore CI/CD pipeline: checkout, build, test, package, Docker image, Kubernetes deploy, verify.
//
// Trigger: this file deliberately has NO triggers block. Keep the SCM polling you already configured
// on the Jenkins job so the pipeline is not polled twice.
//
// Tools expected on the Jenkins agent: JDK 21, Maven, Docker, kubectl.
pipeline {
    agent any

    options {
        disableConcurrentBuilds()      // one build at a time: there is a single H2-backed pod
        buildDiscarder(logRotator(numToKeepStr: '15'))
    }

    environment {
        // Jenkins on macOS starts with a short PATH. Adjust these two lines if your tools live elsewhere.
        PATH = "/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home/bin:/Users/heidi/.docker/bin:/Applications/Docker.app/Contents/Resources/bin:/usr/local/bin:/opt/homebrew/bin:/Applications/apache-maven-3.10.0/bin:${env.PATH}"
        JAVA_HOME = '/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home'
        IMAGE_NAME    = 'campuscore'
        IMAGE_TAG     = "${env.BUILD_NUMBER}"
        DEPLOYMENT    = 'campuscore'
        K8S_NAMESPACE = 'default'
        // Optional. Id of a Jenkins "Secret file" credential that holds a kubeconfig.
        // Leave empty when the Jenkins user can already run kubectl.
        KUBECONFIG_CREDENTIALS_ID = ''
        // Optional. Command that makes the image visible to your cluster, for example:
        //   minikube:  'minikube image load'      kind:  'kind load docker-image'
        // Leave empty for Docker Desktop Kubernetes, which shares the local Docker images.
        IMAGE_LOAD_COMMAND = ''
    }

    stages {
        stage('Checkout') {
            steps {
                // Uses the repository and branch configured on the Jenkins job.
                checkout scm
                script {
                    env.GIT_SHORT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                }
                echo "Building commit ${env.GIT_SHORT} as build ${env.BUILD_NUMBER}"
            }
        }

        stage('Verify Java and Maven') {
            steps {
                sh '''
                    java -version
                    mvn -version
                    java -version 2>&1 | grep -q '"21' || { echo "Java 21 is required"; exit 1; }
                '''
            }
        }

        stage('Clean') {
            steps { sh 'mvn -B clean' }
        }

        stage('Compile and Test') {
            steps { sh 'mvn -B test' }          // fails the build if compilation or any test fails
            post {
                always { junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml' }
            }
        }

        stage('Package') {
            steps { sh 'mvn -B package' }       // tests run again here; they are never skipped
            post {
                success { archiveArtifacts artifacts: 'target/campuscore.jar', fingerprint: true }
            }
        }

        stage('Build Docker Image') {
            steps { sh 'docker build -t ${IMAGE_NAME}:${IMAGE_TAG} .' }
        }

        stage('Tag Docker Image') {
            steps {
                sh '''
                    docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${IMAGE_NAME}:${GIT_SHORT}
                    docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${IMAGE_NAME}:latest
                    docker images ${IMAGE_NAME}
                '''
            }
        }

        stage('Push Docker Image') {
            // Publishes the image built in this run to Docker Hub. Needs a Jenkins credential with id "dockerhub"
            // (kind: Username with password; the password is a Docker Hub access token). Nothing secret is in this file.
            // If the credential is missing or Docker Hub is unreachable, this stage turns red and the build
            // becomes UNSTABLE (yellow), but the deployment below still runs. Fix the credential and rebuild.
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    withCredentials([usernamePassword(credentialsId: 'dockerhub',
                                                      usernameVariable: 'DH_USER', passwordVariable: 'DH_PASS')]) {
                        sh '''
                            echo "$DH_PASS" | docker login -u "$DH_USER" --password-stdin
                            docker tag ${IMAGE_NAME}:${IMAGE_TAG} "$DH_USER/${IMAGE_NAME}:${IMAGE_TAG}"
                            docker tag ${IMAGE_NAME}:${IMAGE_TAG} "$DH_USER/${IMAGE_NAME}:latest"
                            docker push "$DH_USER/${IMAGE_NAME}:${IMAGE_TAG}"
                            docker push "$DH_USER/${IMAGE_NAME}:latest"
                            docker logout
                        '''
                    }
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                script {
                    if (env.IMAGE_LOAD_COMMAND?.trim()) {
                        sh '${IMAGE_LOAD_COMMAND} ${IMAGE_NAME}:${IMAGE_TAG}'
                    }
                    // Deploy exactly the image built by this run (campuscore:<build number>).
                    kube '''
                        kubectl apply -n ${K8S_NAMESPACE} -f k8s/pvc.yaml -f k8s/service.yaml
                        sed "s|image: campuscore:.*|image: ${IMAGE_NAME}:${IMAGE_TAG}|" k8s/deployment.yaml \
                            | kubectl apply -n ${K8S_NAMESPACE} -f -
                    '''
                }
            }
        }

        stage('Verify Deployment') {
            steps {
                script {
                    kube '''
                        kubectl rollout status deployment/${DEPLOYMENT} -n ${K8S_NAMESPACE} --timeout=240s
                        kubectl get deployments -n ${K8S_NAMESPACE}
                        kubectl get pods -n ${K8S_NAMESPACE} -l app=campuscore
                        kubectl get services -n ${K8S_NAMESPACE}
                        DEPLOYED=$(kubectl get deployment ${DEPLOYMENT} -n ${K8S_NAMESPACE} -o jsonpath='{.spec.template.spec.containers[0].image}')
                        echo "Deployed image: ${DEPLOYED}"
                        [ "${DEPLOYED}" = "${IMAGE_NAME}:${IMAGE_TAG}" ] || { echo "Unexpected image deployed"; exit 1; }
                    '''
                }
            }
        }
    }

    post {
        success { echo "CampusCore ${IMAGE_NAME}:${IMAGE_TAG} is deployed and verified." }
        failure { echo 'Pipeline failed. Check the stage that turned red; later stages did not run.' }
    }
}

// Runs a shell block with kubectl access: through a stored kubeconfig credential when one is set,
// otherwise with whatever kubectl access the Jenkins user already has. No secrets appear in this file.
def kube(String commands) {
    if (env.KUBECONFIG_CREDENTIALS_ID?.trim()) {
        withCredentials([file(credentialsId: env.KUBECONFIG_CREDENTIALS_ID, variable: 'KUBECONFIG')]) {
            sh commands
        }
    } else {
        sh commands
    }
}
