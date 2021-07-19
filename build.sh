#!/bin/bash

set -x

cd jd-core
bash ./gradlew --console plain -x check -x test build publishToMavenLocal
cd ..
mvn clean package
