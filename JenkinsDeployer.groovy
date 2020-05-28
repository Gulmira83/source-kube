def k8slabel = "jenkins-pipeline-${UUID.randomUUID().toString()}"
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
        choice(choices: ['dev', 'qa', 'prod', 'test'], description: 'Please select the environment to deploy', name: 'environment')
        
        ])
    ])


podTemplate(name: k8slabel, label: k8slabel, yaml: slavePodTemplate, showRawYaml: params.DebugMode) {
    node(k8slabel) {
        dir("${WORKSPACE}/") {

            stage("Pull Source Code") {
                git branch: 'dev-feature/fsadykov', url: 'https://github.com/fuchicorp/source-kube.git'
            }

            stage("Deployment Info") {
                println("""
                MODE: ${params.DebugMode}
                Environment: ${params.environment}
                """  
                )
            }

            container("terraform-container") {
                dir("${WORKSPACE}/deployments/terraform") {
                    sh 'terraform init'
                }
                
            }

            container("python-container") {
                sh 'python --version'
            }
        }
    }
}