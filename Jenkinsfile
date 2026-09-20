pipeline {
  agent any

  parameters {
    string(name: 'REPO_URL',    defaultValue: 'https://github.com/wangyong-fy/learn-k8s-backend.git', description: '后端仓库地址')
    string(name: 'REPO_BRANCH', defaultValue: 'main',                                                 description: '分支')
    string(name: 'IMAGE_TAG',   defaultValue: '',                                                     description: '留空则用构建号')
  }

  environment {
    REGISTRY = 'wanyongdoker'
    APP_NS   = 'app'
    KANIKO   = 'gcr.io/kaniko-project/executor:v1.23.2'
    CACHE    = 'wanyongdoker/kaniko-cache'
  }

  options {
    disableConcurrentBuilds()
  }

  stages {
    stage('Checkout') {
      steps {
        git branch: "${params.REPO_BRANCH}", url: "${params.REPO_URL}"
        script {
          env.TAG = params.IMAGE_TAG?.trim() ? params.IMAGE_TAG.trim() : env.BUILD_NUMBER
          echo "后端本次构建 TAG = ${env.TAG}"
        }
      }
    }

    stage('Build & Push Backend') {
      steps {
        sh '''
          set -e
          HOSTPATH="${REPO_URL#*://}"
          CTX="git://${HOSTPATH}#refs/heads/${REPO_BRANCH}"
          POD=kaniko-backend-${TAG}
          echo "backend context = ${CTX}"

          cat > /tmp/kaniko-be.yaml <<EOF
apiVersion: v1
kind: Pod
metadata:
  name: ${POD}
  namespace: jenkins
spec:
  restartPolicy: Never
  containers:
    - name: kaniko
      image: ${KANIKO}
      args:
        - "--context=${CTX}"
        - "--dockerfile=Dockerfile"
        - "--destination=${REGISTRY}/learn-k8s-backend:${TAG}"
        - "--snapshot-mode=time"
        - "--compressed-caching=false"
        - "--cache=true"
        - "--cache-repo=${CACHE}"
        - "--verbosity=info"
      resources:
        requests:
          cpu: "500m"
          memory: "1Gi"
        limits:
          cpu: "2"
          memory: "3Gi"
      volumeMounts:
        - name: docker-config
          mountPath: /kaniko/.docker
  volumes:
    - name: docker-config
      secret:
        secretName: dockerhub
        items:
          - key: .dockerconfigjson
            path: config.json
EOF

          kubectl -n jenkins delete pod ${POD} --ignore-not-found
          kubectl -n jenkins apply -f /tmp/kaniko-be.yaml

          i=0
          while [ $i -lt 180 ]; do
            PH=$(kubectl -n jenkins get pod ${POD} -o jsonpath='{.status.phase}' 2>/dev/null || echo "")
            if [ "$PH" = "Succeeded" ]; then echo "backend 镜像推送成功"; break; fi
            if [ "$PH" = "Failed" ]; then
              echo "backend 构建失败，日志："
              kubectl -n jenkins logs ${POD} --tail=200
              exit 1
            fi
            if [ $((i % 6)) -eq 0 ]; then
              echo "[$((i*10))s] 状态=${PH:-Pending} 最近日志:"
              kubectl -n jenkins logs ${POD} --tail=3 2>/dev/null | sed 's/^/    /' || true
            fi
            sleep 10
            i=$((i+1))
          done
          [ "$PH" = "Succeeded" ] || { echo "backend 构建超时"; kubectl -n jenkins logs ${POD} --tail=200; exit 1; }
          kubectl -n jenkins delete pod ${POD} --ignore-not-found
        '''
      }
    }

    stage('Deploy Backend to K8s') {
      steps {
        sh '''
          set -e
          kubectl -n "$APP_NS" apply -f k8s/backend.yaml
          kubectl -n "$APP_NS" set image deployment/backend backend=${REGISTRY}/learn-k8s-backend:${TAG}
          kubectl -n "$APP_NS" rollout status deployment/backend --timeout=300s
        '''
      }
    }
  }

  post {
    success {
      echo "后端 CI/CD 完成：${REGISTRY}/learn-k8s-backend:${TAG}"
    }
    failure {
      echo "后端 CI/CD 失败，请查看上方日志"
    }
  }
}
