#!groovy

node('build-slave') {

   currentBuild.result = "SUCCESS"
   cleanWs()

   try {

      stage('Checkout') {

         checkout scm
      }

      stage('Build') {

        env.NODE_ENV = "build"
        print "Environment will be : ${env.NODE_ENV}"
        sh('git submodule update --init')
        sh('git submodule update --init --recursive --remote')
        sh 'git log -1'
        sh 'cat service/conf/routes | grep v2'
        sh 'sudo mvn clean install -DskipTests=true '

      }

      stage('Unit Tests') {

        sh "sudo mvn test '-Dtest=!%regex[io.opensaber.registry.client.*]' -DfailIfNoTests=false"

      }

      stage('Package') {

        dir ('service') {
          sh 'mvn play2:dist'
        }
        sh('chmod 777 ./build.sh')
        sh('./build.sh')

      }

      stage('Publish') {

        echo 'Push to Repo'
        sh 'ls -al ~/'
        sh('chmod 777 ./dockerPushToRepo.sh')
        sh 'ARTIFACT_LABEL=bronze ./dockerPushToRepo.sh'
        sh './metadata.sh > metadata.json'
        sh 'cat metadata.json'
        archive includes: "metadata.json"

      }

    }
    catch (err) {
        currentBuild.result = "FAILURE"
        throw err
    }

}
