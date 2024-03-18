node('build-slave') {
    try {
        String ANSI_GREEN = "\u001B[32m"
        String ANSI_NORMAL = "\u001B[0m"
        String ANSI_BOLD = "\u001B[1m"
        String ANSI_RED = "\u001B[31m"
        String ANSI_YELLOW = "\u001B[33m"

        ansiColor('xterm') {
            withEnv(["JAVA_HOME=${JAVA11_HOME}"]) {
                stage('Checkout') {
                    if (!env.hub_org) {
                        println(ANSI_BOLD + ANSI_RED + "Uh Oh! Please set a Jenkins environment variable named hub_org with value as registery/sunbidrded" + ANSI_NORMAL)
                        error 'Please resolve the errors and rerun..'
                    } else
                        println(ANSI_BOLD + ANSI_GREEN + "Found environment variable named hub_org with value as: " + hub_org + ANSI_NORMAL)
                }
                
                cleanWs()
                checkout scm
                commit_hash = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                build_tag = sh(script: "echo " + params.github_release_tag.split('/')[-1] + "_" + commit_hash + "_" + env.BUILD_NUMBER, returnStdout: true).trim()
                echo "build_tag: " + build_tag

                stage('Build') {
                    env.NODE_ENV = "build"
                    print "Environment will be : ${env.NODE_ENV}"
                    sh 'git log -1'
                    sh "mvn clean install -DskipTests=true -DCLOUD_STORE_GROUP_ID=${params.cloud_store_group_id} -DCLOUD_STORE_ARTIFACT_ID=${params.cloud_store_artifact_id} -DCLOUD_STORE_VERSION=${params.cloud_store_version}"
                }

                stage('Unit Tests') {
                    sh "mvn clean install '-Dtest=!%regex[io.opensaber.registry.client.*]' -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -DCLOUD_STORE_GROUP_ID=org.sunbird -DCLOUD_STORE_ARTIFACT_ID=cloud-store-sdk_2.12 -DCLOUD_STORE_VERSION=1.4.7"
                }
                stage('Package') {
                    dir('controller') {
                        sh 'mvn play2:dist'
                    }
                    sh('chmod 777 ./build.sh')
                    sh("./build.sh ${build_tag} ${env.NODE_NAME} ${hub_org}")
                }
                stage('ArchiveArtifacts') {
                    archiveArtifacts "metadata.json"
                    currentBuild.description = "${build_tag}"
                }
            }
        }
    } catch (err) {
        currentBuild.result = "FAILURE"
        throw err
    }
}
