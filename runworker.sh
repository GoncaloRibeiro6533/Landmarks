#! /bin/bash
cd /var/landmarksApp
export GOOGLE_APPLICATION_CREDENTIALS=key.json
export PROJECT_ID="cn2425-t1-g09"
java -jar /var/landmarksApp/landmarksApp.jar
