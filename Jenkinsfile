pipeline {
    agent any

    environment {
        APP_NAME       = "user-service"          // 그냥 읽기용 이름
        IMAGE_NAME     = "user-service"          // Docker 이미지 이름
        IMAGE_TAG      = "latest"                // 태그
        FULL_IMAGE     = "${IMAGE_NAME}:${IMAGE_TAG}"

        CONTAINER_NAME = "user-service"          // 컨테이너 이름
        HOST_PORT      = "12000"                 // 호스트에서 열 포트
        CONTAINER_PORT = "12000"                 // 스프링 서버 포트 (server.port)

        // Jenkins 컨테이너 안에 만들어 놓은 env 파일 경로
        ENV_FILE       = "/var/jenkins_home/envs/user-service.env"
    }

    stages {

        stage('Build & Test') {
            steps {
                sh './gradlew bootJar'
            }
        }

        stage('Docker Build') {
            steps {
                sh """
                  docker build -t ${FULL_IMAGE} .
                """
            }
        }

        stage('Deploy') {
            steps {
                sh """
                  # 기존 컨테이너 있으면 정지/삭제
                  if [ \$(docker ps -aq -f name=${CONTAINER_NAME}) ]; then
                    echo "Stopping existing container..."
                    docker stop ${CONTAINER_NAME} || true
                    docker rm ${CONTAINER_NAME} || true
                  fi

                  echo "Starting new user-service container..."
                  docker run -d --name ${CONTAINER_NAME} \\
                    --env-file ${ENV_FILE} \\
                    -p ${HOST_PORT}:${CONTAINER_PORT} \\
                    ${FULL_IMAGE}
                """
            }
        }
    }
}
