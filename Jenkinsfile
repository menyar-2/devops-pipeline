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

            trivy config \
              --severity HIGH,CRITICAL \
              --format json \
              --output reports/trivy-iac/trivy-iac-report.json \
              .

            trivy config \
              --severity HIGH,CRITICAL \
              --exit-code 0 \
              . > reports/trivy-iac/trivy-iac-output.txt 2>&1

            cat > reports/trivy-iac/trivy-iac-report.html <<EOF
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Trivy IaC Security Report</title>
</head>
<body>

<h1>Trivy IaC Security Report</h1>

<p><strong>Security phase:</strong> Infrastructure as Code Security</p>
<p><strong>Tool:</strong> Trivy</p>
<p><strong>Build:</strong> ${BUILD_NUMBER}</p>
<p><strong>Git Commit:</strong> ${GIT_COMMIT}</p>

<h2>Scan Output</h2>

<pre>
$(cat reports/trivy-iac/trivy-iac-output.txt)
</pre>

</body>
</html>
EOF

            cp reports/trivy-iac/trivy-iac-report.json trivy-iac-report.json
            cp reports/trivy-iac/trivy-iac-report.html trivy-iac-report.html

            echo "Generated Trivy IaC artifacts:"
            ls -lh trivy-iac-report.*

            # Security gate
            trivy config \
              --severity HIGH,CRITICAL \
              --exit-code 1 \
              .
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
            mkdir -p reports/trivy-container

            echo "========================================="
            echo " TRIVY CONTAINER SECURITY SCAN"
            echo "========================================="

            # Rapport JSON
            trivy image \
              --timeout 30m \
              --scanners vuln \
              --severity HIGH,CRITICAL \
              --skip-db-update \
              --skip-java-db-update \
              --format json \
              --output reports/trivy-container/trivy-container-report.json \
              m221jft4043/devops-pipeline:1.0

            # Sortie lisible temporaire
            trivy image \
              --timeout 30m \
              --scanners vuln \
              --severity HIGH,CRITICAL \
              --skip-db-update \
              --skip-java-db-update \
              --exit-code 0 \
              m221jft4043/devops-pipeline:1.0 \
              > reports/trivy-container/trivy-container-output.txt 2>&1

            # Rapport HTML
            cat > reports/trivy-container/trivy-container-report.html <<EOF
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Trivy Container Security Report</title>
</head>
<body>

<h1>Trivy Container Security Report</h1>

<p><strong>Security phase:</strong> Container Security</p>
<p><strong>Tool:</strong> Trivy</p>
<p><strong>Image:</strong> m221jft4043/devops-pipeline:1.0</p>
<p><strong>Build:</strong> ${BUILD_NUMBER}</p>
<p><strong>Git Commit:</strong> ${GIT_COMMIT}</p>

<h2>Scan Output</h2>

<pre>
$(cat reports/trivy-container/trivy-container-output.txt)
</pre>

</body>
</html>
EOF

            cp reports/trivy-container/trivy-container-report.json trivy-container-report.json
            cp reports/trivy-container/trivy-container-report.html trivy-container-report.html

            echo "Generated Trivy Container artifacts:"
            ls -lh trivy-container-report.*

            # Security gate
            trivy image \
              --timeout 30m \
              --scanners vuln \
              --severity HIGH,CRITICAL \
              --skip-db-update \
              --skip-java-db-update \
              --exit-code 1 \
              m221jft4043/devops-pipeline:1.0
        '''
    }

    post {
        always {
            publishHTML(target: [
                reportDir: '.',
                reportFiles: 'trivy-container-report.html',
                reportName: 'Trivy Container Security Report',
                keepAll: true,
                alwaysLinkToLastBuild: true,
                allowMissing: true
            ])

            archiveArtifacts(
                artifacts: 'trivy-container-report.html,trivy-container-report.json',
                allowEmptyArchive: true
            )
        }
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
            mkdir -p reports/sqlmap

            echo "========================================="
            echo " SQLMAP DAST SECURITY SCAN"
            echo "========================================="

            kubectl port-forward deployment/devops-pipeline 8087:8085 \
              > /tmp/sqlmap-port-forward.log 2>&1 &

            PF_PID=$!

            cleanup() {
                kill $PF_PID 2>/dev/null || true
            }

            trap cleanup EXIT

            echo "Waiting for SQLMap target..."

            READY=0

            for i in $(seq 1 20); do
                if curl -fsS "http://127.0.0.1:8087/item?id=1" >/dev/null; then
                    READY=1
                    break
                fi

                echo "Target not ready - attempt $i/20"
                sleep 2
            done

            if [ "$READY" -ne 1 ]; then
                echo "ERROR: SQLMap target did not become ready."
                cat /tmp/sqlmap-port-forward.log || true
                exit 1
            fi

            echo "Running SQLMap scan..."

            set +e

            sqlmap \
              -u "http://127.0.0.1:8087/item?id=1" \
              --batch \
              --level=1 \
              --risk=1 \
              --dbms=SQLite \
              --flush-session \
              > reports/sqlmap/sqlmap-output.txt 2>&1

            SQLMAP_STATUS=$?

            set -e

            if grep -q "Parameter: id" reports/sqlmap/sqlmap-output.txt; then
                SQLMAP_RESULT="VULNERABILITY DETECTED"
            else
                SQLMAP_RESULT="NO SQL INJECTION DETECTED"
            fi

            cat > reports/sqlmap/sqlmap-report.html <<EOF
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>SQLMap Security Report</title>
</head>
<body>

<h1>SQLMap DAST Security Report</h1>

<p><strong>Security phase:</strong> DAST</p>
<p><strong>Tool:</strong> SQLMap</p>
<p><strong>Target:</strong> http://127.0.0.1:8087/item?id=1</p>
<p><strong>Database:</strong> SQLite</p>
<p><strong>Build:</strong> ${BUILD_NUMBER}</p>
<p><strong>Git Commit:</strong> ${GIT_COMMIT}</p>
<p><strong>Result:</strong> ${SQLMAP_RESULT}</p>

<h2>SQLMap Scan Output</h2>

<pre>
$(cat reports/sqlmap/sqlmap-output.txt)
</pre>

</body>
</html>
EOF

            cp reports/sqlmap/sqlmap-output.txt sqlmap-output.txt
            cp reports/sqlmap/sqlmap-report.html sqlmap-report.html

            echo "Generated SQLMap artifacts:"
            ls -lh sqlmap-report.html sqlmap-output.txt

            # On ne bloque pas le pipeline ici uniquement à cause
            # de la détection volontaire de la vulnérabilité du lab.
            exit 0
        '''
    }

    post {
        always {
            publishHTML(target: [
                reportDir: '.',
                reportFiles: 'sqlmap-report.html',
                reportName: 'SQLMap Security Report',
                keepAll: true,
                alwaysLinkToLastBuild: true,
                allowMissing: true
            ])

            archiveArtifacts(
                artifacts: 'sqlmap-report.html,sqlmap-output.txt',
                allowEmptyArchive: true
            )
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
