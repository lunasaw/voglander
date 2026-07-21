mvn install -DskipTests=true -Dgpg.skip=true
mvn spring-boot:run -pl voglander-web 
mvn spring-boot:run -pl voglander-web -Dserver.port=8181

 
mvn spring-boot:run -pl voglander-web -Dspring-boot.run.jvmArguments="-Dserver.port=8181"  -Dspring-boot.run.profiles=prod


 mvn spring-boot:run -pl voglander-web -Dspring-boot.run.profiles=dev