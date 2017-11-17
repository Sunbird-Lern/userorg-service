#!/bin/sh
nohup java  -cp '/home/sunbird/learner/learning-service-1.0-SNAPSHOT/lib/*' play.core.server.ProdServerStart  /home/sunbird/learner/learning-service-1.0-SNAPSHOT &
sleep 60
ln -sf /dev/stdout /home/sunbird/learner/logs/learningServiceProject.log
