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

        // 시간대(한국)
        TZ              = "Asia/Seoul"
        JAVA_TZ_OPTS    = "-Duser.timezone=Asia/Seoul"
        
        // [추가] 스프링 프로필 설정 (application-prod.yml 사용을 강제)
        SPRING_PROFILES_ACTIVE = "prod"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                withCredentials([
                    file(credentialsId: 'user-service-env-file', variable: 'ENV_FILE')
                ]) {
                    sh '''
                      if [ ! -f "$ENV_FILE" ]; then
                        echo "Error: ENV_FILE not found at $ENV_FILE"
                        exit 1
                      fi
                      set -a
                      . "$ENV_FILE"
                      set +a

                      # [핵심 수정] 빌드 및 테스트 시점에 prod 프로필을 명시하여 
                      # application-prod.yml의 config.import 설정을 읽게 함으로써 에러 방지
                      ./gradlew clean test bootJar --no-daemon -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}
                    '''
                }
            }
        }

        stage('Docker Build & Push') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'ghcr-credential',
                        usernameVariable: 'REGISTRY_USER',
                        passwordVariable: 'REGISTRY_TOKEN'
                    )
                ]) {
                    sh """
                      docker build -t ${FULL_IMAGE} .
                      echo "\$REGISTRY_TOKEN" | docker login ${REGISTRY} -u "\$REGISTRY_USER" --password-stdin
                      docker push ${FULL_IMAGE}
                    """
                }
            }
        }

        stage('Deploy to K8s') {
            steps {
                withCredentials([
                    file(credentialsId: 'k3s-kubeconfig',      variable: 'KUBECONFIG_FILE'),
                    file(credentialsId: 'user-service-env-file', variable: 'ENV_FILE')
                ]) {
                    sh '''
                      export KUBECONFIG=${KUBECONFIG_FILE}

                      echo "Syncing env file to K8s Secret..."
                      kubectl delete secret user-service-env -n ${NAMESPACE} --ignore-not-found
                      kubectl create secret generic user-service-env \
                        --from-env-file=${ENV_FILE} \
                        -n ${NAMESPACE}

                      # [참고] user-service.yaml 내부 env 설정에 SPRING_PROFILES_ACTIVE: prod가 
                      # 이미 있다면 자동으로 application-prod.yml을 읽습니다.
                      echo "Applying user-service manifest to k3s..."
                      kubectl apply -f user-service.yaml -n ${NAMESPACE}

                      echo "Rollout status for user-service:"
                      kubectl rollout status deployment/user-service -n ${NAMESPACE}
                    '''
                }
            }
        }
    }

    post {
        always {
            // 빌드 서버 용량 관리를 위해 빌드된 이미지 삭제
            echo "Cleaning up local images..."
            sh "docker rmi ${FULL_IMAGE} || true"
        }
        success {
            echo "Successfully deployed ${APP_NAME} to Kubernetes!"
        }
        failure {
            echo "Deployment failed. Check the logs."
        }
    }
}