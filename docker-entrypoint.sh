#!/bin/sh
nohup java  -cp '/home/sunbird/learner/learning-service-1.0-SNAPSHOT/lib/*' play.core.server.ProdServerStart  /home/sunbird/learner/learning-service-1.0-SNAPSHOT &
ln -sf /dev/stdout /home/sunbird/learner/logs/learningServiceProject.log
