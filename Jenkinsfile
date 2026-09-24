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
        sh 'docker build -t devops-pipeline:1.0 .'
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
