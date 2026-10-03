pipeline {
    agent any

    stages {

        stage('Checkout') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/menyar-2/devops-pipeline.git'
            }
        }
    stage('Pre-Commit Security - Talisman') {
    steps {
        sh '''
            mkdir -p reports/talisman

            echo "========================================="
            echo " TALISMAN SECURITY SCAN"
            echo "========================================="

            set +e

            talisman --scan > reports/talisman/talisman.txt 2>&1
            TALISMAN_STATUS=$?

            set -e

            cat > reports/talisman/report.html <<EOF
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Talisman Security Report</title>
</head>
<body>

<h1>ThoughtWorks Talisman Security Report</h1>

<p><strong>Security phase:</strong> Pre-Commit Security</p>
<p><strong>Tool:</strong> ThoughtWorks Talisman</p>
<p><strong>Build:</strong> ${BUILD_NUMBER}</p>
<p><strong>Git Commit:</strong> ${GIT_COMMIT}</p>

EOF

if [ "$TALISMAN_STATUS" -eq 0 ]; then
   echo '<p><strong>Status:</strong> CLEAN</p>' >> reports/talisman/report.html
else
    echo '<p><strong>Status:</strong> FINDINGS DETECTED</p>' >> reports/talisman/report.html
fi

cat >> reports/talisman/report.html <<EOF

<h2>Scan Output</h2>

<pre>
$(cat reports/talisman/talisman.txt)
</pre>

</body>
</html>
EOF
            cp reports/talisman/talisman.txt talisman-report.txt
            cp reports/talisman/report.html talisman-report.html

            echo "Generated artifacts:"
            ls -lh talisman-report.*

            if [ "$TALISMAN_STATUS" -ne 0 ]; then
                echo "Talisman found potential security issues."
                echo "Report generated and archived."
            else
                echo "Talisman scan passed."
            fi

            exit 0
        '''
    }

    post {
        always {
            publishHTML(target: [
                reportDir: '.',
                reportFiles: 'talisman-report.html',
                reportName: 'Talisman Security Report',
                keepAll: true,
                alwaysLinkToLastBuild: true,
                allowMissing: true
            ])

            archiveArtifacts(
                artifacts: 'talisman-report.txt,talisman-report.html',
                allowEmptyArchive: true
            )
        }
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
          
stage('OWASP Dependency-Check') {
    steps {
        withCredentials([string(credentialsId: 'nvd-api-key', variable: 'NVD_API_KEY')]) {
            sh '''
                mvn org.owasp:dependency-check-maven:13.0.0:check \
                  -DnvdApiKeyEnvironmentVariable=NVD_API_KEY
            '''
        }
    }

    post {
        always {
            sh '''
                if [ -f target/dependency-check-report.html ]; then
                    cp target/dependency-check-report.html dependency-check-report.html
                fi
            '''

            publishHTML(target: [
                reportDir: '.',
                reportFiles: 'dependency-check-report.html',
                reportName: 'OWASP Dependency-Check Report',
                keepAll: true,
                alwaysLinkToLastBuild: true,
                allowMissing: true
            ])

            archiveArtifacts(
                artifacts: 'dependency-check-report.html',
                allowEmptyArchive: true
            )
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
                mkdir -p reports/sonarqube

                mvn org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
                  -Dsonar.projectKey=devops-pipeline \
                  -Dsonar.projectName=devops-pipeline \
                  -Dsonar.host.url=http://localhost:9000 \
                  -Dsonar.token=$SONAR_TOKEN \
                  > reports/sonarqube/sonar-output.txt 2>&1

                SONAR_STATUS=$?

                if [ "$SONAR_STATUS" -eq 0 ]; then
                    SONAR_RESULT="SUCCESS"
                else
                    SONAR_RESULT="FAILED"
                fi

                cat > reports/sonarqube/report.html <<EOF
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>SonarQube Analysis Report</title>
</head>
<body>

<h1>SonarQube Static Analysis Report</h1>

<p><strong>Security phase:</strong> Static Analysis</p>
<p><strong>Tool:</strong> SonarQube</p>
<p><strong>Build:</strong> ${BUILD_NUMBER}</p>
<p><strong>Git Commit:</strong> ${GIT_COMMIT}</p>
<p><strong>Status:</strong> ${SONAR_RESULT}</p>

<p>
<strong>Dashboard:</strong>
<a href="http://localhost:9000/dashboard?id=devops-pipeline">
Open SonarQube Dashboard
</a>
</p>

<h2>Scanner Output</h2>

<pre>
$(cat reports/sonarqube/sonar-output.txt)
</pre>

</body>
</html>
EOF

                cp reports/sonarqube/report.html sonarqube-report.html
                cp reports/sonarqube/sonar-output.txt sonarqube-output.txt

                exit $SONAR_STATUS
            '''
        }
    }

    post {
        always {
            publishHTML(target: [
                reportDir: '.',
                reportFiles: 'sonarqube-report.html',
                reportName: 'SonarQube Analysis Report',
                keepAll: true,
                alwaysLinkToLastBuild: true,
                allowMissing: true
            ])

            archiveArtifacts(
                artifacts: 'sonarqube-report.html,sonarqube-output.txt',
                allowEmptyArchive: true
            )
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
            mkdir -p reports/trivy-iac

            echo "========================================="
            echo " TRIVY IaC SECURITY SCAN"
            echo "========================================="

            # Rapport JSON
            trivy config \
              --severity HIGH,CRITICAL \
              --format json \
              --output reports/trivy-iac/trivy-iac-report.json \
              .

            # Rapport HTML
            trivy config \
              --severity HIGH,CRITICAL \
              --format template \
              --template "@contrib/html.tpl" \
              --output reports/trivy-iac/trivy-iac-report.html \
              .

            # Scan bloquant du pipeline
            trivy config \
              --severity HIGH,CRITICAL \
              --exit-code 1 \
              .

            # Copies à la racine pour les Artefacts Jenkins
            cp reports/trivy-iac/trivy-iac-report.json trivy-iac-report.json
            cp reports/trivy-iac/trivy-iac-report.html trivy-iac-report.html

            echo "Generated Trivy IaC artifacts:"
            ls -lh trivy-iac-report.*
        '''
    }

    post {
        always {
            publishHTML(target: [
                reportDir: '.',
                reportFiles: 'trivy-iac-report.html',
                reportName: 'Trivy IaC Security Report',
                keepAll: true,
                alwaysLinkToLastBuild: true,
                allowMissing: true
            ])

            archiveArtifacts(
                artifacts: 'trivy-iac-report.html,trivy-iac-report.json',
                allowEmptyArchive: true
            )
        }
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
            
stage('Security Scanning - SQLMap') {
    steps {
        sh '''
            set -e

            kubectl port-forward deployment/devops-pipeline 8087:8085 \
              > /tmp/sqlmap-port-forward.log 2>&1 &

            PF_PID=$!

            cleanup() {
                kill $PF_PID 2>/dev/null || true
            }

            trap cleanup EXIT

            echo "Waiting for SQLMap target..."

            for i in $(seq 1 20); do
                if curl -fsS "http://127.0.0.1:8087/item?id=1" >/dev/null; then
                    break
                fi
                sleep 2
            done

            echo "Running SQLMap acceptance scan..."

            sqlmap \
              -u "http://127.0.0.1:8087/item?id=1" \
              --batch \
              --level=1 \
              --risk=1 \
              --dbms=SQLite

            echo "SQLMap scan completed."
        '''
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
