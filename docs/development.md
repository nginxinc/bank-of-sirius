# Development Guide

This document describes how to develop and add features to the Bank of Sirius application in your local environment. 

## Prerequisites 

1. A clone of this repository.
2. A linux build environment.

## Install Tools 

You can use MacOS or Linux as your dev environment - all these languages and tools support both. 

1. [Docker](https://www.docker.com/products/docker-desktop) 
2. [GNU Make](https://www.gnu.org/software/make/)
5. [JDK **14**](https://www.oracle.com/java/technologies/javase/jdk14-archive-downloads.html) (newer versions might cause issues)
6. [Maven **3.6**](https://downloads.apache.org/maven/maven-3/) (newer versions might cause issues)
7. [Python3](https://www.python.org/downloads/)  
8. [piptools](https://pypi.org/project/pip-tools/)


## Adding External Packages 

### Python 

If you're adding a new feature that requires a new external Python package in one or more services (`frontend`, `contacts`, `userservice`), you must regenerate the `requirements.txt` file using `piptools`. This is what the Python Dockerfiles use to install external packages inside the containers.

To add a package: 

1. Add the package name to `requirements.in` within the `src/<service>` directory:

2. From inside that directory, run: 

```
python3 -m pip install pip-tools
python3 -m piptools compile --output-file=requirements.txt requirements.in
```

3. Re-run `make` with the appropriate target for your build.


### Java 

If you're adding a new feature to one or more of the Java services (`ledgerwriter`, `transactionhistory`, `balancereader`) and require a new third-party package, do the following:  

1. Add the package to the `pom.xml` file in the `src/<service>` directory, under `<dependencies>`. You can find specific package info in [Maven Central](https://search.maven.org/) ([example](https://search.maven.org/artifact/org.postgresql/postgresql/42.2.16.jre7/jar)). Example: 

```
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>
```
3. Re-run `make` with the appropriate target for your build.


## Generating your own JWT public key. 

The [extras](/extras/jwt) directory provides the RSA key/pair secret used for demos. To create your own: 

```
openssl genrsa -out jwtRS256.key 4096
openssl rsa -in jwtRS256.key -outform PEM -pubout -out jwtRS256.key.pub
kubectl create secret generic jwt-key --from-file=./jwtRS256.key --from-file=./jwtRS256.key.pub
```

## Continuous Integration

To be added.
