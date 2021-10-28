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

MAKE_MAJOR_VER    := $(shell echo $(MAKE_VERSION) | cut -d'.' -f1)

ifneq ($(shell test $(MAKE_MAJOR_VER) -gt 3; echo $$?),0)
$(error Make version $(MAKE_VERSION) is not supported, please install GNU Make 4.x)
endif

PACKAGE_ROOT      := github.com/nginxinc
PACKAGE           := $(PACKAGE_ROOT)/bank-of-sirius
DATE              ?= $(shell date -u +%FT%T%z)
VERSION           ?= $(shell cat $(CURDIR)/.version 2> /dev/null || echo 0.0.0)
GITHASH           ?= $(shell git rev-parse HEAD)

SED               ?= $(shell command -v gsed 2> /dev/null || command -v sed 2> /dev/null)
AWK               ?= $(shell command -v gawk 2> /dev/null || command -v awk 2> /dev/null)
GREP              ?= $(shell command -v ggrep 2> /dev/null || command -v grep 2> /dev/null)
FIND              ?= $(shell command -v gfind 2> /dev/null || command -v find 2> /dev/null)
SORT              ?= $(shell command -v sort 2> /dev/null || command -v sort 2> /dev/null)
TEE               ?= $(shell command -v gtee 2> /dev/null || command -v tee 2> /dev/null)
ARCH              := $(shell uname -m | $(SED) -e 's/x86_64/amd64/g' -e 's/i686/i386/g')
PLATFORM          := $(shell uname | tr '[:upper:]' '[:lower:]')
SHELL             := bash

JAVA_PROJECTS        := java-common/telemetry-common java-common/security-common java-common/spring-boot-parent \
                        java-common/ledger-common balancereader ledgerwriter transactionhistory
JAVA_DOCKER_PROJECTS := balancereader ledgerwriter transactionhistory
PYTHON_PROJECTS      := contacts frontend loadgenerator userservice
DB_PROJECTS          := accounts-db ledger-db

MVN               := $(CURDIR)/mvnw -q
JARS              := $(foreach project,$(JAVA_PROJECTS),src/$(project)/target/$(project).jar)

DOCKER            := docker
DOCKER_BUILD_OPTS := --quiet
DOCKER_BUILD      := $(DOCKER) build $(DOCKER_BUILD_OPTS)
IMAGE_PREFIX      := ghcr.io/nginxinc
IMAGE_NAME_PREFIX := bos-

# Directories containing Dockerfile file that should be published
IMAGES_TO_PUBLISH := $(realpath $(dir $(shell find $(CURDIR)/base-images/ $(CURDIR)/src/ \
					   -maxdepth 2 -type f -name Dockerfile \
					   -exec grep -l '^LABEL org.opencontainers.image.source' '{}' \; | xargs)))

# Directories containing Dockerfile file that is used as a base for other images
# Note: the processing of these images is sort-order dependent
BASE_IMAGES       := $(shell find base-images/ -mindepth 1 -maxdepth 1 -type d | sort)

.SUFFIXES:
.SUFFIXES: .jar

TIMEOUT = 45
V = 0
Q = $(if $(filter 1,$V),,@)
M = $(shell printf "\033[34;1m▶\033[0m")

.PHONY: help
help:
	@grep --no-filename -E '^[ a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		$(AWK) 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-28s\033[0m %s\n", $$1, $$2}' | sort

include build/docker.mk
include build/java.mk
include build/python.mk
include build/release.mk
include build/tools.mk

.PHONY: clean
clean: ; $(info $(M) cleaning...)	@ ## Cleanup everything
	$Q rm -rf $(foreach project,$(JAVA_PROJECTS),src/$(project)/target)

.PHONY: test
test: java-test python-test ## Run all automated tests

.PHONY: all
all: java-build docker-all-images

.PHONY: checkstyle
checkstyle: java-checkstyle python-checkstyle ## Run all code style checks
