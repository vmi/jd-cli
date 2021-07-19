#!/bin/bash

set -x

git submodule update --init
cd jd-core
bash ./gradlew --console plain -x check -x test build publishToMavenLocal
cd ..
mvn clean package
