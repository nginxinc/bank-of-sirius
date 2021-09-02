# Development Guide

This document describes how to develop and add features to the Bank of Sirius application in your local environment. 

## Prerequisites 

1. A clone of this repository.
2. A linux build environment.

## Install Tools 

You can use MacOS or Linux as your dev environment - all these languages and tools support both. 

1. [Docker](https://www.docker.com/products/docker-desktop) 
2. [GNU Make 4.x](https://www.gnu.org/software/make/)
5. [JDK **8**](https://www.azul.com/downloads/?package=jdk) (newer versions might cause issues)
6. [Maven **3.6**](https://downloads.apache.org/maven/maven-3/) (newer versions might cause issues)

## Makefile Targets

The following code block shows the valid targets for the make process. Most of these options are self-explanatory, but to completely build the application one can run `make docker-all-images`. To push the new versions to the defined registry, you can run `make release`. Note that this assumes you have the appropriate permissions to push to the defined registry.

```
checkstyle                   Run all code style checks
clean                        Cleanup everything
docker-all-images            Build all container images
docker-base-images           Build base images inherited by other images
docker-db-images             Build database container images
docker-java-images           Build Java container images
docker-python-images         Build Python container images
java-build                   Builds all Java applications
java-checkstyle              Java code style check
java-test                    Runs unit tests for all Java applications
java-test-coverage           Creates test coverage reports for all Java applications
java-test-coverage-summary   Shows a summary of the test coverage reports for all Java applications
python-checkstyle            Python code style check
python-preproc-requirements  Run pip-compile for all requirements.in files
python-test                  Runs unit tests for all Python applications
python-test-coverage         Creates test coverage reports for all Python applications
release                      Release container images to registry
test                         Run all automated tests
update-manifests             Updates Kubernetes manifest files to use the current deployment version
version                      Outputs the current version
version-update               Prompts for a new version
```

## Adding External Packages 

### Python 

If you're adding a new feature that requires a new external Python package in one or more services (`frontend`, `contacts`, `userservice`), you must regenerate the `requirements.txt` file using `piptools`. This is what the Python Dockerfiles use to install external packages inside the containers.

To add a package: 

1. Add the package name to `requirements.in` within the `src/<service>` directory:

2. Then run the make target `python-preproc-requirements`. This will transform all Python projects' `requirements.in` files to `requirements.txt` if there have been changes to the `requirements.in` file.

3. Re-run `make` with the appropriate target for your build. To build just the python packages you can run `make docker-python-images`


### Java 

If you're adding a new feature to one or more of the Java services (`ledgerwriter`, `transactionhistory`, `balancereader`) and require a new third-party package, do the following:  

1. Add the package to the `pom.xml` file in the `src/<service>` directory, under `<dependencies>`. You can find specific package info in [Maven Central](https://search.maven.org/) ([example](https://search.maven.org/artifact/org.postgresql/postgresql/42.2.16.jre7/jar)). Example: 

```
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>
```
3. Re-run `make` with the appropriate target for your build. To build just the java packages you can run `make docker-java-images`


## Continuous Integration

Github actions are used for continuous integration (CI) for this project. Due to time constrains we have been unable to reimplement the UI tests and deployment tests in the fork.
