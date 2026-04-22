@echo off
set MAVEN_ARGS=-Dmaven.repo.local=d:\code\live\.m2repo
set SPRING_PROFILES_ACTIVE=local
cd /d d:\code\live\backend
mvn spring-boot:run
