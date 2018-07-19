#!groovy

node('build-slave') {

   currentBuild.result = "SUCCESS"
   cleanWs()
   try {

      stage('Checkout'){

         checkout scm
       }

      stage('Build'){

        env.NODE_ENV = "build"
        print "Environment will be : ${env.NODE_ENV}"
        sh('git submodule foreach git pull origin open-saber-integration')
        sh 'mvn clean install -DskipTests=true '
        dir ('service') {
        sh 'mvn play2:dist'
         }
         sh('chmod 777 ./build.sh')
         sh('./build.sh')

      }

      stage('Publish'){

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
