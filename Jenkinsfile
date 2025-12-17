// pipeline {
//     agent any
//
//     environment {
//         APP_NAME        = "user-service"
//
//         // GHCR 레지스트리 정보
//         REGISTRY        = "ghcr.io"
//         GH_OWNER        = "sparta-next-me"
//         IMAGE_REPO      = "user-service"
//         FULL_IMAGE      = "${REGISTRY}/${GH_OWNER}/${IMAGE_REPO}:latest"
//
//         CONTAINER_NAME  = "user-service"
//         HOST_PORT       = "12000"
//         CONTAINER_PORT  = "12000"
//     }
//
//     stages {
//
//         stage('Checkout') {
//             steps {
//                 checkout scm
//             }
//         }
//
//         stage('Build & Test') {
//             steps {
//                 withCredentials([
//                     file(credentialsId: 'user-service-env-file', variable: 'ENV_FILE')
//                 ]) {
//                     sh '''
//                       # 환경 파일 존재 확인
//                       if [ ! -f "$ENV_FILE" ]; then
//                         echo "Error: ENV_FILE not found at $ENV_FILE"
//                         exit 1
//                       fi
//                       set -a
//                       . "$ENV_FILE"       # DB_URL, DB_USERNAME, DB_PASSWORD, REDIS_HOST, OAUTH 키들 export
//                       set +a
//
//                       ./gradlew clean test --no-daemon
//                       ./gradlew bootJar --no-daemon
//                     '''
//                 }
//             }
//         }
//
//         stage('Docker Build') {
//             steps {
//                 sh """
//                   docker build -t ${FULL_IMAGE} .
//                 """
//             }
//         }
//
//         stage('Push Image') {
//             steps {
//                 withCredentials([
//                     usernamePassword(
//                         credentialsId: 'ghcr-credential',
//                         usernameVariable: 'REGISTRY_USER',
//                         passwordVariable: 'REGISTRY_TOKEN'
//                     )
//                 ]) {
//                     sh """
//                       set -e  # 아래 명령 중 하나라도 실패하면 즉시 종료
//
//                       echo "\$REGISTRY_TOKEN" | docker login ghcr.io -u "\$REGISTRY_USER" --password-stdin
//                       docker push ${FULL_IMAGE}
//                     """
//                 }
//             }
//         }
//
//        stage('Deploy to K8s') {
//            steps {
//                // 1) kubectl 설치 (없으면)
//                sh '''
//                  if ! command -v kubectl >/dev/null 2>&1; then
//                    echo "kubectl not found. installing..."
//                    curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
//                    chmod +x kubectl
//                    mv kubectl /usr/local/bin/kubectl
//                  fi
//                '''
//
//                // 2) kubeconfig + env 파일을 함께 사용
//                withCredentials([
//                    file(credentialsId: 'k3s-kubeconfig',      variable: 'KUBECONFIG_FILE'),
//                    file(credentialsId: 'user-service-env-file', variable: 'ENV_FILE')
//                ]) {
//                    sh '''
//                      export KUBECONFIG=${KUBECONFIG_FILE}
//
//                      echo "Syncing env file to K8s Secret..."
//
//                      # 기존 Secret 있으면 삭제 (없으면 무시)
//                      kubectl delete secret user-service-env -n next-me || true
//
//                      # .env 파일로부터 Secret 생성
//                      kubectl create secret generic user-service-env \
//                        --from-env-file=${ENV_FILE} \
//                        -n next-me
//
//                      echo "Applying user-service manifest to k3s..."
//                      kubectl apply -f user-service.yaml -n next-me
//
//                      echo "Rollout status for user-service:"
//                      kubectl rollout status deployment/user-service -n next-me || true
//
//                      echo "Current user-service pods:"
//                      kubectl get pods -n next-me -l app=user-service
//                    '''
//                }
//            }
//        }
//
//     }
// }

pipeline {
    agent any

    environment {
        APP_NAME        = "user-service"

        // GHCR 레지스트리 정보
        REGISTRY        = "ghcr.io"
        GH_OWNER        = "sparta-next-me"
        IMAGE_REPO      = "user-service"
        FULL_IMAGE      = "${REGISTRY}/${GH_OWNER}/${IMAGE_REPO}:latest"

        CONTAINER_NAME  = "user-service"
        HOST_PORT       = "12000"
        CONTAINER_PORT  = "12000"
        // 시간대(한국)
        TZ              = "Asia/Seoul"
        // (선택) JVM 타임존까지 고정하고 싶으면 사용
        JAVA_TZ_OPTS    = "-Duser.timezone=Asia/Seoul"
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
                      # 환경 파일 존재 확인
                      if [ ! -f "$ENV_FILE" ]; then
                        echo "Error: ENV_FILE not found at $ENV_FILE"
                        exit 1
                      fi
                      set -a
                      . "$ENV_FILE"       # DB_URL, DB_USERNAME, DB_PASSWORD, REDIS_HOST, OAUTH 키들 export
                      set +a

                      ./gradlew clean test --no-daemon
                      ./gradlew bootJar --no-daemon
                    '''
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh """
                  docker build -t ${FULL_IMAGE} .
                """
            }
        }

        stage('Push Image') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'ghcr-credential',
                        usernameVariable: 'REGISTRY_USER',
                        passwordVariable: 'REGISTRY_TOKEN'
                    )
                ]) {
                    sh """
                      set -e  # 아래 명령 중 하나라도 실패하면 즉시 종료

                      echo "\$REGISTRY_TOKEN" | docker login ghcr.io -u "\$REGISTRY_USER" --password-stdin
                      docker push ${FULL_IMAGE}
                    """
                }
            }
        }

        stage('Deploy') {
            steps {
                withCredentials([
                    file(credentialsId: 'user-service-env-file', variable: 'ENV_FILE')
                ]) {
                    sh """
                      # 기존 컨테이너 있으면 정지/삭제
                      if [ \$(docker ps -aq -f name=${CONTAINER_NAME}) ]; then
                        echo "Stopping existing container..."
                        docker stop ${CONTAINER_NAME} || true
                        docker rm ${CONTAINER_NAME} || true
                        docker rmi ${FULL_IMAGE} || true
                      fi

                      echo "Starting new user-service container..."
                      docker run -d --name ${CONTAINER_NAME} \\
                        -e EUREKA_INSTANCE_HOSTNAME='10.178.0.4' \\
                        --env-file \${ENV_FILE} \\
                        -e TZ=${TZ} \\
                        -e JAVA_TOOL_OPTIONS="${JAVA_TZ_OPTS}" \\
                        -v /etc/localtime:/etc/localtime:ro \\
                        -v /etc/timezone:/etc/timezone:ro \\
                        -p ${HOST_PORT}:${CONTAINER_PORT} \\
                        ${FULL_IMAGE}
                    """
                }
            }
        }
    }
}
