properties([
    [$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', numToKeepStr: '100']]
])

throttle(['docker']) {
    node(label: 'docker') {
        stage('Checkout'){
            checkout scm
        }
        stage('Test'){
            sh './gradle-docker.sh test'
        }
    }
}
