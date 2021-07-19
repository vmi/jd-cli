git submodule update --init
cd jd-core
.\gradlew --console plain -x check -x test build publishToMavenLocal
cd ..
mvn clean package
