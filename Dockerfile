FROM openjdk:11.0-jre
COPY . /usr/src/myapp
WORKDIR /usr/src/myapp
ENTRYPOINT [ "java", "-cp", "ElectricChargingStationOperations-assembly-0.1.jar", "com.alekslitvinenk.ecso.Main" ]
EXPOSE 8080/tcp
CMD [ "" ]