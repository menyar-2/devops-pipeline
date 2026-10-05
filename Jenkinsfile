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
        withCredentials([
            string(
                credentialsId: 'vault-token',
                variable: 'VAULT_TOKEN'
            )
        ]) {
            sh '''
                set -e

                echo "========================================="
                echo " DOCKER COMPOSE DEPLOY WITH VAULT SECRET"
                echo "========================================="

                mkdir -p secrets         
                if [ -d secrets/app_secret.txt ]; then
    rm -rf secrets/app_secret.txt
fi
                set +x

                APP_SECRET=$(curl -fsS \
                  -H "X-Vault-Token: $VAULT_TOKEN" \
                  http://127.0.0.1:8200/v1/secret/data/devops-pipeline \
                  | python3 -c 'import sys,json; print(json.load(sys.stdin)["data"]["data"]["app_secret"])')

                printf '%s' "$APP_SECRET" > secrets/app_secret.txt
                chmod 444 secrets/app_secret.txt

                unset APP_SECRET

                set -x

                echo "Secret file prepared securely."

                docker compose down
                docker compose pull
                docker compose up -d --force-recreate

                docker compose ps

                echo "Checking Docker secret mount..."

                docker compose exec -T app \
                  test -f /run/secrets/app_secret

                docker compose exec -T app \
                  ls -l /run/secrets/app_secret

                echo "Docker secret mounted successfully."
            '''
        }
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

stage('Production - ZAP Baseline Scan') {
    steps {
        sh '''
            set -e

            echo "========================================="
            echo " PRODUCTION - ZAP BASELINE SCAN"
            echo "========================================="

            rm -rf zap-reports
            mkdir -p zap-reports
            chmod 777 zap-reports

            echo "Starting Kubernetes port-forward..."

            kubectl port-forward \
              --address 0.0.0.0 \
              service/devops-pipeline-service \
              8088:8085 \
              > /tmp/zap-port-forward.log 2>&1 &

            PF_PID=$!

            cleanup() {
                echo "Stopping port-forward..."
                kill $PF_PID 2>/dev/null || true
            }

            trap cleanup EXIT

            echo "Waiting for application..."

            READY=0

            for i in $(seq 1 20); do
                if curl -fsS http://127.0.0.1:8088/health >/dev/null; then
                    READY=1
                    echo "Application is ready."
                    break
                fi

                echo "Application not ready - attempt $i/20"
                sleep 2
            done

            if [ "$READY" -ne 1 ]; then
                echo "ERROR: application unavailable for ZAP scan"
                cat /tmp/zap-port-forward.log || true
                exit 1
            fi

            HOST_IP=$(hostname -I | awk '{print $1}')

            echo "WSL host IP: $HOST_IP"
            echo "ZAP target: http://$HOST_IP:8088"

            echo "Testing target from ZAP container..."

            docker run --rm \
              ghcr.io/zaproxy/zaproxy:stable \
              curl -I "http://$HOST_IP:8088/"

            echo "Running ZAP Baseline Scan..."

            set +e

            docker run --rm \
              -v "$(pwd)/zap-reports:/zap/wrk/:rw" \
              ghcr.io/zaproxy/zaproxy:stable \
              zap-baseline.py \
              -t "http://$HOST_IP:8088" \
              -r zap-baseline-report.html \
              -J zap-baseline-report.json

            ZAP_STATUS=$?

            set -e

            echo "ZAP exit code: $ZAP_STATUS"

            echo "Checking generated reports..."

            if [ ! -f zap-reports/zap-baseline-report.html ]; then
                echo "ERROR: zap-baseline-report.html not generated"
                ls -lh zap-reports || true
                exit 1
            fi

            if [ ! -f zap-reports/zap-baseline-report.json ]; then
                echo "ERROR: zap-baseline-report.json not generated"
                ls -lh zap-reports || true
                exit 1
            fi

            cp zap-reports/zap-baseline-report.html zap-baseline-report.html
            cp zap-reports/zap-baseline-report.json zap-baseline-report.json

            echo "Generated ZAP artifacts:"
            ls -lh zap-baseline-report.html zap-baseline-report.json

            if [ "$ZAP_STATUS" -eq 1 ]; then
                echo "ZAP detected blocking security findings."
                exit 1

            elif [ "$ZAP_STATUS" -eq 3 ]; then
                echo "ZAP execution error."
                exit 1

            elif [ "$ZAP_STATUS" -eq 2 ]; then
                echo "ZAP Baseline completed with warnings."
                exit 0
            fi

            echo "ZAP Baseline Scan completed successfully."
            exit 0
        '''
    }

    post {
        always {
            publishHTML(target: [
                reportDir: '.',
                reportFiles: 'zap-baseline-report.html',
                reportName: 'ZAP Baseline Security Report',
                keepAll: true,
                alwaysLinkToLastBuild: true,
                allowMissing: false
            ])

            archiveArtifacts(
                artifacts: 'zap-baseline-report.html,zap-baseline-report.json',
                allowEmptyArchive: false
            )
        }
    }
}

stage('Production - Nmap Security Smoke Test') {
    steps {
        sh '''
            set -e

            echo "========================================="
            echo " PRODUCTION - NMAP SECURITY SMOKE TEST"
            echo "========================================="

            mkdir -p reports/nmap

            echo "Starting Kubernetes port-forward..."

            kubectl port-forward \
              --address 0.0.0.0 \
              service/devops-pipeline-service \
              8089:8085 \
              > /tmp/nmap-port-forward.log 2>&1 &

            PF_PID=$!

            cleanup() {
                echo "Stopping Nmap port-forward..."
                kill $PF_PID 2>/dev/null || true
            }

            trap cleanup EXIT

            echo "Waiting for application..."

            READY=0

            for i in $(seq 1 20); do
                if curl -fsS http://127.0.0.1:8089/health >/dev/null; then
                    READY=1
                    echo "Application is ready."
                    break
                fi

                echo "Application not ready - attempt $i/20"
                sleep 2
            done

            if [ "$READY" -ne 1 ]; then
                echo "ERROR: application unavailable for Nmap scan"
                cat /tmp/nmap-port-forward.log || true
                exit 1
            fi

            HOST_IP=$(hostname -I | awk '{print $1}')

            echo "Nmap target: $HOST_IP:8089"

            nmap \
              -sV \
              -Pn \
              -p 8089 \
              "$HOST_IP" \
              -oN reports/nmap/nmap-production-report.txt

            cp reports/nmap/nmap-production-report.txt \
               nmap-production-report.txt

            cat > reports/nmap/nmap-production-report.html <<EOF
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Nmap Production Security Report</title>
</head>
<body>

<h1>Nmap Production Security Smoke Test</h1>

<p><strong>Security phase:</strong> Production</p>
<p><strong>Tool:</strong> Nmap</p>
<p><strong>Target:</strong> ${HOST_IP}:8089</p>
<p><strong>Build:</strong> ${BUILD_NUMBER}</p>
<p><strong>Git Commit:</strong> ${GIT_COMMIT}</p>

<h2>Nmap Scan Output</h2>

<pre>
$(cat reports/nmap/nmap-production-report.txt)
</pre>

</body>
</html>
EOF

            cp reports/nmap/nmap-production-report.html \
               nmap-production-report.html

            echo "Generated Nmap artifacts:"
            ls -lh nmap-production-report.html \
                   nmap-production-report.txt

            if ! grep -q "8089/tcp open" \
                 reports/nmap/nmap-production-report.txt; then

                echo "ERROR: expected application port 8089 is not open."
                exit 1
            fi

            echo "Nmap Production Security Smoke Test passed."
        '''
    }

    post {
        always {
            publishHTML(target: [
                reportDir: '.',
                reportFiles: 'nmap-production-report.html',
                reportName: 'Nmap Production Security Report',
                keepAll: true,
                alwaysLinkToLastBuild: true,
                allowMissing: false
            ])

            archiveArtifacts(
                artifacts: 'nmap-production-report.html,nmap-production-report.txt',
                allowEmptyArchive: false
            )
        }
    }
}

stage('Production - Vault Secrets Check') {
    steps {
        withCredentials([
            string(
                credentialsId: 'vault-token',
                variable: 'VAULT_TOKEN'
            )
        ]) {
            sh '''
                set -e

                echo "========================================="
                echo " PRODUCTION - VAULT SECRETS CHECK"
                echo "========================================="

                mkdir -p reports/vault

                VAULT_ADDR="http://127.0.0.1:8200"

                echo "Checking Vault health..."

                curl -fsS \
                  "$VAULT_ADDR/v1/sys/health" \
                  > reports/vault/vault-health.json

                echo "Reading secret metadata..."

                SECRET_RESPONSE=$(curl -fsS \
                  -H "X-Vault-Token: $VAULT_TOKEN" \
                  "$VAULT_ADDR/v1/secret/data/devops-pipeline")

                echo "$SECRET_RESPONSE" \
                  > reports/vault/vault-secret-response.json

                MYSQL_USER_PRESENT=$(echo "$SECRET_RESPONSE" | grep -q '"mysql_user"' && echo YES || echo NO)
                MYSQL_PASSWORD_PRESENT=$(echo "$SECRET_RESPONSE" | grep -q '"mysql_password"' && echo YES || echo NO)
                DOCKER_USERNAME_PRESENT=$(echo "$SECRET_RESPONSE" | grep -q '"docker_username"' && echo YES || echo NO)

                if [ "$MYSQL_USER_PRESENT" = "YES" ] && \
                   [ "$MYSQL_PASSWORD_PRESENT" = "YES" ] && \
                   [ "$DOCKER_USERNAME_PRESENT" = "YES" ]; then
                    VAULT_RESULT="PASS"
                else
                    VAULT_RESULT="FAIL"
                fi

                cat > reports/vault/vault-security-report.html <<EOF
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>HashiCorp Vault Security Report</title>
</head>
<body>

<h1>HashiCorp Vault Security Report</h1>

<p><strong>Phase:</strong> Production - Secrets Management</p>
<p><strong>Tool:</strong> HashiCorp Vault</p>
<p><strong>Vault Address:</strong> ${VAULT_ADDR}</p>
<p><strong>Secret Path:</strong> secret/devops-pipeline</p>
<p><strong>Build:</strong> ${BUILD_NUMBER}</p>
<p><strong>Git Commit:</strong> ${GIT_COMMIT}</p>

<h2>Secret Validation</h2>

<table border="1" cellpadding="8">
<tr>
    <th>Secret Field</th>
    <th>Status</th>
</tr>
<tr>
    <td>mysql_user</td>
    <td>${MYSQL_USER_PRESENT}</td>
</tr>
<tr>
    <td>mysql_password</td>
    <td>${MYSQL_PASSWORD_PRESENT}</td>
</tr>
<tr>
    <td>docker_username</td>
    <td>${DOCKER_USERNAME_PRESENT}</td>
</tr>
</table>

<h2>Result</h2>
<p><strong>${VAULT_RESULT}</strong></p>

<p>No secret values are displayed in this report.</p>

</body>
</html>
EOF

                cp reports/vault/vault-security-report.html \
                   vault-security-report.html

                echo "Generated Vault artifact:"
                ls -lh vault-security-report.html

                if [ "$VAULT_RESULT" != "PASS" ]; then
                    echo "Vault secret validation failed."
                    exit 1
                fi

                echo "Vault secret validation passed."
            '''
        }
    }

    post {
        always {
            publishHTML(target: [
                reportDir: '.',
                reportFiles: 'vault-security-report.html',
                reportName: 'Vault Security Report',
                keepAll: true,
                alwaysLinkToLastBuild: true,
                allowMissing: false
            ])

            archiveArtifacts(
                artifacts: 'vault-security-report.html',
                allowEmptyArchive: false
            )
        }
    }
}

stage('Production - Docker Secrets Validation') {
    steps {
        sh '''
            set -e

            echo "========================================="
            echo " PRODUCTION - DOCKER SECRETS VALIDATION"
            echo "========================================="

            mkdir -p reports/docker-secrets

            SECRET_PATH="/run/secrets/app_secret"

            echo "Checking Docker secret inside application container..."

            if docker compose exec -T app test -f "$SECRET_PATH"; then
                SECRET_PRESENT="YES"
            else
                SECRET_PRESENT="NO"
            fi

            if docker compose exec -T app test -r "$SECRET_PATH"; then
                SECRET_READABLE="YES"
            else
                SECRET_READABLE="NO"
            fi

            SECRET_PERMS=$(docker compose exec -T app \
                stat -c "%a" "$SECRET_PATH" 2>/dev/null || echo "UNKNOWN")

            if [ "$SECRET_PRESENT" = "YES" ] && \
               [ "$SECRET_READABLE" = "YES" ]; then
                DOCKER_SECRET_RESULT="PASS"
            else
                DOCKER_SECRET_RESULT="FAIL"
            fi

            cat > reports/docker-secrets/docker-secrets-report.html <<EOF
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Docker Secrets Security Report</title>
</head>
<body>

<h1>Docker Secrets Security Report</h1>

<p><strong>Phase:</strong> Production - Secrets Management</p>
<p><strong>Tool:</strong> Docker Secrets / Docker Compose Secrets</p>
<p><strong>Container:</strong> devops-pipeline-app</p>
<p><strong>Secret Path:</strong> /run/secrets/app_secret</p>
<p><strong>Build:</strong> ${BUILD_NUMBER}</p>
<p><strong>Git Commit:</strong> ${GIT_COMMIT}</p>

<h2>Validation</h2>

<table border="1" cellpadding="8">
<tr>
    <th>Check</th>
    <th>Result</th>
</tr>

<tr>
    <td>Secret file present</td>
    <td>${SECRET_PRESENT}</td>
</tr>

<tr>
    <td>Secret file readable</td>
    <td>${SECRET_READABLE}</td>
</tr>

<tr>
    <td>File permissions</td>
    <td>${SECRET_PERMS}</td>
</tr>

</table>

<h2>Overall Result</h2>
<p><strong>${DOCKER_SECRET_RESULT}</strong></p>

<p>The secret value is intentionally not displayed.</p>

</body>
</html>
EOF

            cp reports/docker-secrets/docker-secrets-report.html \
               docker-secrets-report.html

            echo "Generated Docker Secrets artifact:"
            ls -lh docker-secrets-report.html

            if [ "$DOCKER_SECRET_RESULT" != "PASS" ]; then
                echo "Docker Secrets validation failed."
                exit 1
            fi

            echo "Docker Secrets validation passed."
        '''
    }

    post {
        always {
            publishHTML(target: [
                reportDir: '.',
                reportFiles: 'docker-secrets-report.html',
                reportName: 'Docker Secrets Security Report',
                keepAll: true,
                alwaysLinkToLastBuild: true,
                allowMissing: false
            ])

            archiveArtifacts(
                artifacts: 'docker-secrets-report.html',
                allowEmptyArchive: false
            )
        }
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
