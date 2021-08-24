#
#  Copyright 2020 F5 Networks
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

PACKAGE_ROOT  := github.com/nginxinc
PACKAGE       := $(PACKAGE_ROOT)/bank-of-sirius
DATE          ?= $(shell date -u +%FT%T%z)
VERSION       ?= $(shell cat $(CURDIR)/.version 2> /dev/null || echo 0.0.0)
GITHASH       ?= $(shell git rev-parse HEAD)

SED           ?= $(shell which gsed 2> /dev/null || which sed 2> /dev/null)
ARCH          := $(shell uname -m | $(SED) -e 's/x86_64/amd64/g' -e 's/i686/i386/g')
PLATFORM      := $(shell uname | tr '[:upper:]' '[:lower:]')
SHELL         := bash

MVN           := $(CURDIR)/mvnw -q
JAVA_PROJECTS = balancereader ledgerwriter transactionhistory
JARS          = $(foreach project,$(JAVA_PROJECTS),src/$(project)/target/$(project).jar)

.SUFFIXES:
.SUFFIXES: .jar

TIMEOUT = 45
V = 0
Q = $(if $(filter 1,$V),,@)
M = $(shell printf "\033[34;1m▶\033[0m")

.PHONY: help
help:
	@grep --no-filename -E '^[ a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-24s\033[0m %s\n", $$1, $$2}' | sort

#include build/docker.mk
#include build/tools.mk
#include build/compile.mk
#include build/test.mk
#include build/release.mk

.PHONY: clean
clean: ; $(info $(M) cleaning...)	@ ## Cleanup everything
	$Q rm -rf $(foreach project,$(JAVA_PROJECTS),src/$(project)/target)

.PHONY: java
java: $(JARS) ## Builds all Java applications

.PHONY: test-java
test-java: $(foreach project,$(JAVA_PROJECTS),src/$(project)/target/surefire-reports) ## Runs unit tests for all Java applications

.PHONY: test-coverage-java
test-coverage-java: $(foreach project,$(JAVA_PROJECTS),src/$(project)/target/site/jacoco) ## Creates test coverage reports for all Java applications

.PRECIOUS: %.jar
%.jar: ; $(info $(M) building Java distribution archive file: $(@))	@
# $(dir $(@D)) - in this context will return src/<project dir>
	$Q $(MVN) -pl $(dir $(@D)) package -DskipTests

.PRECIOUS: %/target/surefire-reports
%/target/surefire-reports: ; $(info $(M) running unit tests for project: $(dir $(@D))) @
	$Q $(MVN) -pl $(dir $(@D)) test

.PRECIOUS: %/target/site/jacoco
%/target/site/jacoco: %/target/surefire-reports
	$(info $(M) creating test coverage report for project: $(dir $(<D))) @
# $(dir $(<D)) - in this context will return src/<project dir>
	$Q $(MVN) -pl $(dir $(<D)) jacoco:report

COVERAGE_SUMMARY := awk -F, '{ instructions += $$4 + $$5; covered += $$5 } END { print covered, "/", instructions, " instructions covered"; print int(100*covered/instructions), "% covered" }'

.PHONY: test-coverage-summary-java
.ONESHELL: test-coverage-summary-java
test-coverage-summary-java: ## Shows a summary of the test coverage reports for all Java applications
	$Q for project in $(JAVA_PROJECTS); do \
		echo "Coverage for $${project}:"; \
		$(COVERAGE_SUMMARY) "$(CURDIR)/src/$$project/target/site/jacoco/jacoco.csv"; \
		echo; \
	done
