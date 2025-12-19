pipeline {
    agent any

    environment {
        APP_NAME        = "user-service"
        NAMESPACE       = "next-me"

        // GHCR 레지스트리 정보
        REGISTRY        = "ghcr.io"
        GH_OWNER        = "sparta-next-me"
        IMAGE_REPO      = "user-service"
        FULL_IMAGE      = "${REGISTRY}/${GH_OWNER}/${IMAGE_REPO}:latest"

        // 시간대 설정 (한국 시간)
        TZ              = "Asia/Seoul"
    }

    stages {
        stage('Checkout') {
            steps {
                // 소스 코드 가져오기
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                withCredentials([
                    file(credentialsId: 'user-service-env-file', variable: 'ENV_FILE')
                ]) {
                    sh '''
                      # .env 파일을 환경변수로 로드하여 빌드 및 테스트 수행
                      set -a
                      . "$ENV_FILE"
                      set +a
                      ./gradlew clean bootJar --no-daemon
                    '''
                }
            }
        }

        stage('Docker Build & Push') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'ghcr-credential',
                        usernameVariable: 'USER',
                        passwordVariable: 'TOKEN'
                    )
                ]) {
                    sh """
                      # 이미지 빌드
                      docker build -t ${FULL_IMAGE} .

                      # GHCR 로그인 및 푸시
                      echo "${TOKEN}" | docker login ${REGISTRY} -u "${USER}" --password-stdin
                      docker push ${FULL_IMAGE}
                    """
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                withCredentials([
                    file(credentialsId: 'k3s-kubeconfig', variable: 'KUBECONFIG_FILE'),
                    file(credentialsId: 'user-service-env-file', variable: 'ENV_FILE')
                ]) {
                    sh '''
                      export KUBECONFIG=${KUBECONFIG_FILE}

                      # 1. 기존 시크릿 삭제 후 .env 파일 기반으로 새로 생성
                      echo "Updating K8s Secret: user-service-env..."
                      kubectl delete secret user-service-env -n ${NAMESPACE} --ignore-not-found
                      kubectl create secret generic user-service-env --from-env-file=${ENV_FILE} -n ${NAMESPACE}

                      # 2. 쿠버네티스 매니페스트(Deployment, Service) 적용
                      echo "Applying manifests from user-service.yaml..."
                      kubectl apply -f user-service.yaml -n ${NAMESPACE}

                      # 3. 무중단 배포(Rolling Update) 상태 모니터링
                      # 신규 파드가 Ready 상태가 될 때까지 대기하며, 실패 시 빌드 에러 처리
                      echo "Monitoring rollout status..."
                      kubectl rollout status deployment/user-service -n ${NAMESPACE}

                      # 4. 배포 결과 확인
                      kubectl get pods -n ${NAMESPACE} -l app=user-service
                    '''
                }
            }
        }
    }

    // 모든 작업이 끝난 후 실행되는 섹션
    post {
        always {
            // 빌드 서버(Jenkins 서버)의 디스크 용량 관리를 위해 로컬 이미지는 삭제
            // 배포는 이미 GHCR에 푸시된 이미지로 K8s가 수행하므로 로컬 이미지는 지워도 무방함
            echo "Cleaning up local docker image..."
            sh "docker rmi ${FULL_IMAGE} || true"
        }
        success {
            echo "Successfully deployed ${APP_NAME} to Kubernetes Cluster!"
        }
        failure {
            echo "Deployment failed. Please check the Jenkins console logs."
        }
    }
}