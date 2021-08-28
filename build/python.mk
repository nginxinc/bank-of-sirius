.PHONY: python-checkstyle
python-checkstyle: ## Python code style check
	$Q $(DOCKER) run --tty --rm -v "$(CURDIR):/project" --workdir "/project" \
		$(IMAGE_NAME_PREFIX)python3-dev \
		pylint --exit-zero --rcfile=./.pylintrc ./src/*/*.py ./build/bin/*.py

.PHONY: python-preproc-requirements
.ONESHELL: python-preproc-requirements
python-preproc-requirements: docker-image-build/base-images/python3-dev ## Run pip-compile for all requirements.in files
	$Q $(info $(M) transforming requirements.in -> requirements.txt)
	REQS_TO_PREPROC="$$($(FIND) $(CURDIR)/src -mindepth 2 -maxdepth 2 -name requirements.in \
						-exec $(CURDIR)/build/bin/find_changed_requirements.sh '{}' \; | xargs)"

	for req in $${REQS_TO_PREPROC}; do \
		dir="$$(dirname "$${req}")"; \
		workdir="$$(basename "$${dir}")"; \
		cd "$${dir}"; \
		echo "Transforming $${req} into $${dir}/requirements.txt"
		$(DOCKER) run --rm -v "$${dir}:/$${workdir}" --workdir "/$${workdir}" \
			$(IMAGE_NAME_PREFIX)python3-dev \
			pip-compile --quiet --output-file=- --pip-args '--no-color --prefer-binary' requirements.in | $(TEE) --output-error=exit requirements.txt > /dev/null; \
		checksum="$$($(GREP) -v '^# requirements checksum:' requirements.in | openssl md5 | cut -d' ' -f2)"; \
		$(SED) -i '/^# requirements checksum:/d' requirements.in; \
		echo "# requirements checksum: $${checksum}" >> requirements.in; \
	done

.PHONY: python-test
.ONESHELL: python-test
python-test: TEST_CMD=python3 -m pytest -v -p no:warnings
python-test: run-python-tests
python-test: ## Runs unit tests for all Python applications

.PHONY: python-test-coverage
python-test-coverage: TEST_CMD=python -m pytest -p no:warnings --cov=./ tests/
python-test-coverage: run-python-tests
python-test-coverage: ## Creates test coverage reports for all Python applications

.PHONY: run-python-tests
.ONESHELL: run-python-tests
run-python-tests:
	$Q $(info $(M) running python unit tests)
	install_pytest_cmd="pip3 install /env/wheels/pytest/*"

	for project in $(PYTHON_PROJECTS); do \
		if [ ! -d "$(CURDIR)/src/$${project}/tests" ]; then \
		    echo "no tests folder in: src/$${project}"
		    continue; \
		fi; \

		echo "testing src/$${project}"; \
		$(DOCKER) run --tty --rm -v "$(CURDIR)/src/$${project}:/$${project}" --workdir "/$${project}" \
			"$(IMAGE_NAME_PREFIX)$${project}" \
			bash -c "$${install_pytest_cmd}; $(TEST_CMD)"; \
	done
