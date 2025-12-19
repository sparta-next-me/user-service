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

        // 시간대(한국) 및 프로필 설정
        TZ              = "Asia/Seoul"
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
                      # 환경 파일 존재 확인
                      if [ ! -f "$ENV_FILE" ]; then
                        echo "Error: ENV_FILE not found at $ENV_FILE"
                        exit 1
                      fi

                      # 1. .env 파일 로드 (JWT_SECRET_KEY 등을 쉘 변수로 가져옴)
                      set -a
                      . "$ENV_FILE"
                      set +a

                      # 2. 빌드 및 테스트 수행
                      # -Dspring.profiles.active=prod 를 주어 application-prod.yml의 config import 설정을 읽게 함
                      # -DJWT_SECRET_KEY=${JWT_SECRET_KEY} 를 주어 테스트 시 Null 에러 방지
                      ./gradlew clean test bootJar --no-daemon \
                        -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} \
                        -DJWT_SECRET_KEY=${JWT_SECRET_KEY}
                    '''
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh "docker build -t ${FULL_IMAGE} ."
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
                      set -e
                      echo "\$REGISTRY_TOKEN" | docker login ${REGISTRY} -u "\$REGISTRY_USER" --password-stdin
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
                      # 기존 컨테이너 및 이미지 정리
                      if [ \$(docker ps -aq -f name=${CONTAINER_NAME}) ]; then
                        echo "Stopping and removing existing container..."
                        docker stop ${CONTAINER_NAME} || true
                        docker rm ${CONTAINER_NAME} || true
                        docker rmi ${FULL_IMAGE} || true
                      fi

                      echo "Starting new user-service container with prod profile..."
                      # 1. SPRING_PROFILES_ACTIVE 주입
                      # 2. 컨테이너 내부 통신을 위해 컨테이너 이름 기반으로 Eureka/Config 연결
                      docker run -d --name ${CONTAINER_NAME} \\
                        -e SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE} \\
                        -e EUREKA_INSTANCE_HOSTNAME='10.178.0.4' \\
                        --env-file \${ENV_FILE} \\
                        -p ${HOST_PORT}:${CONTAINER_PORT} \\
                        ${FULL_IMAGE}
                    """
                }
            }
        }
    }

    post {
        always {
            // 빌드 서버 용량 관리
            sh "docker rmi ${FULL_IMAGE} || true"
        }
    }
}