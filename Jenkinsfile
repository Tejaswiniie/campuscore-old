pipeline {
  agent any

  parameters {
    booleanParam(name: 'PUSH_TO_DOCKERHUB', defaultValue: false,
                 description: 'Also push the image to Docker Hub (needs a "dockerhub" credential)')
  }

  environment {
    // Jenkins on macOS often has a short PATH, so add the tool folders explicitly
    PATH = "/Users/heidi/.docker/bin:/Applications/Docker.app/Contents/Resources/bin:/usr/local/bin:/opt/homebrew/bin:/Applications/apache-maven-3.10.0/bin:${env.PATH}"
    JAVA_HOME = "/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home"
    APP = "campuscore"
  }

  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Build') {
      steps { sh 'mvn -B clean package -DskipTests' }
    }

    stage('Test') {
      steps { sh 'mvn -B test' }
      post { always { junit 'target/surefire-reports/*.xml' } }
    }

    stage('Docker Build') {
      steps { sh 'docker build -t $APP:$BUILD_NUMBER -t $APP:latest .' }
    }

    stage('Docker Push') {
      when { expression { return params.PUSH_TO_DOCKERHUB } }
      steps {
        withCredentials([usernamePassword(credentialsId: 'dockerhub',
                         usernameVariable: 'DH_USER', passwordVariable: 'DH_PASS')]) {
          sh '''
            echo "$DH_PASS" | docker login -u "$DH_USER" --password-stdin
            docker tag $APP:$BUILD_NUMBER $DH_USER/$APP:$BUILD_NUMBER
            docker push $DH_USER/$APP:$BUILD_NUMBER
          '''
        }
      }
    }

    stage('Deploy to Kubernetes') {
      steps {
        sh '''
          kubectl apply -f k8s/
          kubectl set image deployment/campuscore campuscore=$APP:$BUILD_NUMBER
          kubectl rollout status deployment/campuscore --timeout=180s
        '''
      }
    }
  }

  post {
    success { echo 'Pipeline succeeded: app is deployed.' }
    failure { echo 'Pipeline failed: check the console output of the failing stage.' }
    always  { echo "Build ${env.BUILD_NUMBER} finished." }
  }
}
