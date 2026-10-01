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
        stage('SCA - RetireJS') {
    steps {
        dir('lab-samples') {
            sh '''
                npm ci
                retire
            '''
        }
    }
}

        stage('OWASP Dependency-Check') {
    steps {
        withCredentials([
            string(
                credentialsId: 'nvd-api-key',
                variable: 'NVD_API_KEY'
            )
        ]) {
            sh '''
                mvn org.owasp:dependency-check-maven:13.0.0:check \
                  -DnvdApiKeyEnvironmentVariable=NVD_API_KEY
            '''
        }
    }
}
  
        stage('SonarQube Analysis') {
            steps {
                withCredentials([
                    string(
                        credentialsId: 'sonarqube-token',
                        variable: 'SONAR_TOKEN'
                    )
                ]) {
                    sh '''
                        mvn org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
                          -Dsonar.projectKey=devops-pipeline \
                          -Dsonar.projectName=devops-pipeline \
                          -Dsonar.host.url=http://localhost:9000 \
                          -Dsonar.token=$SONAR_TOKEN
                    '''
                }
            }
        }

        stage('Package') {
            steps {
                sh 'mvn package -DskipTests'
            }
        }

        stage('Nexus Deploy') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'nexus-credentials',
                        usernameVariable: 'NEXUS_USER',
                        passwordVariable: 'NEXUS_PASSWORD'
                    )
                ]) {
                    sh '''
                        mkdir -p "$WORKSPACE/.m2"

                        cat > "$WORKSPACE/.m2/settings.xml" <<EOF
<settings>
    <servers>
        <server>
            <id>deploymentRepo</id>
            <username>${NEXUS_USER}</username>
            <password>${NEXUS_PASSWORD}</password>
        </server>
    </servers>
</settings>
EOF

                        mvn deploy -DskipTests -s "$WORKSPACE/.m2/settings.xml"
                    '''
                }
            }
        }

stage('IaC Security Scan') {
    steps {
        sh '''
            trivy config \
              --severity HIGH,CRITICAL \
              --exit-code 1 \
              .
        '''
    }
}
        stage('Docker Build') {
            steps {
                sh '''
                    docker build -t m221jft4043/devops-pipeline:1.0 .
                '''
            }
        }

stage('Trivy Container Scan') {
    steps {
        sh '''
            trivy image \
              --timeout 30m \
              --scanners vuln \
              --severity HIGH,CRITICAL \
              --exit-code 1 \
              --skip-db-update \
              --skip-java-db-update \
              m221jft4043/devops-pipeline:1.0
        '''
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
                        echo "$DOCKER_PASSWORD" | docker login \
                          -u "$DOCKER_USER" \
                          --password-stdin

                        docker push m221jft4043/devops-pipeline:1.0
                    '''
                }
            }
        }

        stage('Docker Compose Deploy') {
            steps {
                sh '''
                    docker compose down || true
                    docker compose pull
                    docker compose up -d
                    docker compose ps
                '''
            }
        }
stage('Infrastructure as Code - Ansible') {
    steps {
        sh '''
            ansible-playbook ansible/deploy-k8s.yml
        '''
    }
}
        

stage('Kubernetes Verification') {
    steps {
        sh '''
            kubectl rollout status deployment/devops-pipeline --timeout=120s
            kubectl get pods
            kubectl get deployments
        '''
    }
}
stage('DAST HTTP Scan') {
    steps {
        sh '''
            set -e

            kubectl rollout status deployment/devops-pipeline --timeout=120s
            kubectl wait --for=condition=Ready pod \
              -l app=devops-pipeline \
              --timeout=120s

            kubectl port-forward deployment/devops-pipeline 8086:8085 \
              > /tmp/dast-port-forward.log 2>&1 &

            PF_PID=$!

            cleanup() {
                kill $PF_PID 2>/dev/null || true
            }

            trap cleanup EXIT

            echo "Waiting for application HTTP endpoint..."

            SUCCESS=0

            for i in $(seq 1 20); do
                if curl -fsS http://127.0.0.1:8086/health | grep -q "OK"; then
                    SUCCESS=1
                    break
                fi

                echo "Application not ready yet - attempt $i/20"
                sleep 2
            done

            if [ "$SUCCESS" -ne 1 ]; then
                echo "DAST target did not become ready."
                cat /tmp/dast-port-forward.log || true
                kubectl get pods -o wide
                exit 1
            fi

            echo "Testing root endpoint..."
            curl -fsS http://127.0.0.1:8086/

            echo
            echo "Testing health endpoint..."
            curl -fsS http://127.0.0.1:8086/health

            echo
            echo "DAST HTTP tests passed."
        '''
    }
}            
stage('Security Acceptance Test - Gauntlt') {
    steps {
        dir('acceptance-security') {
            sh '''
                gauntlt network.attack
            '''
        }
    }
}
        stage('Prometheus') {
    steps {
        sh '''
            echo "Checking Prometheus readiness..."
            curl -f http://localhost:9090/-/ready

            echo "Checking Jenkins target in Prometheus..."
            curl -f "http://localhost:9090/api/v1/query?query=up%7Bjob%3D%22jenkins%22%7D"
        '''
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
