#! /bin/bash
cd /var/grpcserver
export GOOGLE_APPLICATION_CREDENTIALS=key.json
export GOOGLE_MAPS_API_KEY=AIzaSyA4gkB64-3B8Q37ociZj_fdanREk8fiREA
export PROJECT_ID="cn2425-t1-g09"
java -jar /var/grpcserver/server.jar
