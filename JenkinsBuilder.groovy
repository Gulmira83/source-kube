def k8slabel = "jenkins-pipeline-${UUID.randomUUID().toString()}"
def gitCommitHash = ""
def branch = "${scm.branches[0].name}".replaceAll(/^\*\//, '')
def slavePodTemplate = """
      metadata:
        labels:
          k8s-label: ${k8slabel}
        annotations:
          jenkinsjoblabel: ${env.JOB_NAME}-${env.BUILD_NUMBER}
      spec:
        affinity:
          podAntiAffinity:
            requiredDuringSchedulingIgnoredDuringExecution:
            - labelSelector:
                matchExpressions:
                - key: component
                  operator: In
                  values:
                  - jenkins-jenkins-master
              topologyKey: "kubernetes.io/hostname"
        containers:
        - name: ansible-container
          image: ansibleplaybookbundle/s2i-apb
          imagePullPolicy: IfNotPresent
          command:
          - cat
          tty: true
        - name: python-container
          image: python:latest
          imagePullPolicy: IfNotPresent
          command:
          - cat
          tty: true
        - name: terraform-container
          image: hashicorp/terraform:0.11.14
          imagePullPolicy: IfNotPresent
          command:
          - cat
          tty: true
        - name: docker
          image: docker:latest
          imagePullPolicy: IfNotPresent
          command:
          - cat
          tty: true
          volumeMounts:
            - mountPath: /var/run/docker.sock
              name: docker-sock
            - mountPath: /etc/secrets/service-account/
              name: google-service-account
        - name: fuchicorptools
          image: fuchicorp/buildtools
          imagePullPolicy: Always
          command:
          - cat
          tty: true
          volumeMounts:
            - mountPath: /var/run/docker.sock
              name: docker-sock
            - mountPath: /etc/secrets/service-account/
              name: google-service-account
        serviceAccountName: common-service-account
        securityContext:
          runAsUser: 0
          fsGroup: 0
        volumes:
          - name: google-service-account
            secret:
              secretName: google-service-account
          - name: docker-sock
            hostPath:
              path: /var/run/docker.sock
    """

properties([
    parameters([
        booleanParam(defaultValue: false, description: 'Please select debug mode true to be able to turn on ', name: 'DebugMode'),
        
        ])
    ])


if (branch.contains('dev-feature')) {
    environment = "dev"
} else if (branch.contains('qa-feature')) {
    environment = "qa"
}


podTemplate(name: k8slabel, label: k8slabel, yaml: slavePodTemplate, showRawYaml: params.DebugMode) {
    node(k8slabel) {
        dir("${WORKSPACE}/") {

            container('fuchicorptools') {
                stage("Pull Source Code") {
                    checkout scm
                    gitCommitHash = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
                }
            }
            
            stage("Docker build") {
                dir("${WORKSPACE}/deployments/docker/") {
                    container("docker") {
                        sh '''
                        docker build -t source-kube .
                        '''
                    }
                }
            }

            stage("Docker Login") {
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId:'nexus-docker-creds',
                    usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']
                    ]) {
                        container("docker") {
                            sh """
                            docker login --username $USERNAME --password $PASSWORD docker.fuchicorp.com/source-kube:${gitCommitHash}
                            """
                        }
                    }
            }

            stage("Docker Push") {
                container("docker") {
                    sh """
                    docker tag source-kube:latest docker.fuchicorp.com/source-kube:${gitCommitHash}
                    docker push docker.fuchicorp.com/source-kube:${gitCommitHash}
                    """
                }
            }

            stage("Clean up") {
                container("docker") {
                    sh "docker rmi --no-prune docker.fuchicorp.com/source-kube:${gitCommitHash}"
                }
            }

            stage("Trigger Deploy") {
                build job: "source-kube-deploy", 
                parameters: [
                    [$class: 'StringParameterValue', name: 'environment', value: "${environment}"],
                    [$class: 'StringParameterValue', name: 'dockerImage', value: "docker.fuchicorp.com/source-kube:${gitCommitHash}"]
                    ]
            }
        }
    }
}