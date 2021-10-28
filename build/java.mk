.PHONY: java-build
java-build: ## Builds all Java applications
	$Q $(MVN) --update-snapshots --file $(CURDIR)/pom.xml install -DskipTests

.PHONY: java-test
java-test: $(foreach project,$(JAVA_PROJECTS),src/$(project)/target/surefire-reports) ## Runs unit tests for all Java applications

.PHONY: java-test-coverage
java-test-coverage: $(foreach project,$(JAVA_PROJECTS),src/$(project)/target/site/jacoco) ## Creates test coverage reports for all Java applications

.PRECIOUS: %.jar
%.jar: ; $(info $(M) building Java distribution archive file: $(@))	@
# $(dir $(@D)) - in this context will return src/<project dir>
	$Q $(MVN) -pl $(dir $(@D)) package -DskipTests

.PRECIOUS: %/target/surefire-reports
.ONESHELL: %/target/surefire-reports
%/target/surefire-reports: ; $Q
	MODULE="$$(basename $(dir $(@D)))"
	POM_PATH='$(dir $(@D))pom.xml'
	POM_NS='http://maven.apache.org/POM/4.0.0'
	POM_SEL="/pom:project/pom:packaging[text()='pom']"
# Skip modules that are pom only - if we have a tool to parse xml
	if command -v xmlstarlet > /dev/null && xmlstarlet sel -Q -N "pom=$${POM_NS}" -t -v "$${POM_SEL}" "$${POM_PATH}"; then \
		echo "$(M) skipping unit tests for pom only module: $${MODULE}"; \
	else \
	  	echo "$(M) running unit tests for module: $${MODULE}"; \
		$(MVN) -pl $(dir $(@D)) test; \
	fi

.PRECIOUS: %/target/site/jacoco
%/target/site/jacoco: %/target/surefire-reports
	$(info $(M) creating test coverage report for project: $(dir $(<D))) @
# $(dir $(<D)) - in this context will return src/<project dir>
	$Q $(MVN) -pl $(dir $(<D)) jacoco:report

COVERAGE_SUMMARY := $(AWK) -F, '{ instructions += $$4 + $$5; covered += $$5 } END { print covered, "/", instructions, " instructions covered"; print int(100*covered/instructions), "% covered" }'

.PHONY: java-test-coverage-summary
.ONESHELL: java-test-coverage-summary
java-test-coverage-summary: java-test-coverage ## Shows a summary of the test coverage reports for all Java applications
	$Q for project in $(JAVA_PROJECTS); do \
		echo "Coverage for $${project}:"; \
		$(COVERAGE_SUMMARY) "$(CURDIR)/src/$$project/target/site/jacoco/jacoco.csv"; \
		echo; \
	done

.PHONY: java-checkstyle
java-checkstyle: ## Java code style check
	$Q $(MVN) checkstyle:check
