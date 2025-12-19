pipeline {
    agent any

    environment {
        APP_NAME        = "user-service"
        REGISTRY        = "ghcr.io"
        GH_OWNER        = "sparta-next-me"
        IMAGE_REPO      = "user-service"
        FULL_IMAGE      = "${REGISTRY}/${GH_OWNER}/${IMAGE_REPO}:latest"

        CONTAINER_NAME  = "user-service"
        HOST_PORT       = "12000"
        CONTAINER_PORT  = "12000"

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
                      set -a
                      . "$ENV_FILE"
                      set +a

                      # 테스트 및 빌드 시 prod 프로필과 환경 변수 주입
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
                    usernamePassword(credentialsId: 'ghcr-credential', usernameVariable: 'U', passwordVariable: 'P')
                ]) {
                    sh """
                      echo "$P" | docker login ${REGISTRY} -u "$U" --password-stdin
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

                      echo "Starting new user-service container..."
                      # 컨테이너 간 통신을 위해 프로필 명시 및 환경 변수 파일 적용
                      docker run -d --name ${CONTAINER_NAME} \\
                        -e SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE} \\
                        --env-file \${ENV_FILE} \\
                        -p ${HOST_PORT}:${CONTAINER_PORT} \\
                        ${FULL_IMAGE}
                    """
                }
            }
        }
    } // stages 종료

    post {
        always {
            // 빌드 서버 용량 관리
            sh "docker rmi ${FULL_IMAGE} || true"
        }
    }
} // pipeline 종료