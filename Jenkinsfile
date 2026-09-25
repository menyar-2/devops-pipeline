pipeline {
    agent any


    stages {

        stage('Checkout') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/menyar-2/devops-pipeline.git'
            }
        }

        stage('Clean') {
            steps {
                sh 'mvn clean'
            }
        }

        stage('Compile') {
            steps {
                sh 'mvn compile'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn test'
            }
        }

        stage('Package') {
            steps {
                sh 'mvn package -DskipTests'
            }
        } 
stage('Docker Build') {
    steps {
        sh 'docker build -t m221jft4043/devops-pipeline:1.0 .'
    }
}
stage('Docker Push') {
    steps {
        withCredentials([
            usernamePassword(
                credentialsId: 'dockerhub-credentials',
                usernameVariable: 'DOCKER_USER',
                passwordVariable: 'DOCKER_PASSWORD'
            )
        ]) {
            sh '''
                echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USER" --password-stdin
                docker push m221jft4043/devops-pipeline:1.0
            '''
        }
    }
}   
    }

    post {
        success {
            echo 'PIPELINE SUCCESS'
        }

        failure {
            echo 'PIPELINE FAILED'
        }
    }
}
