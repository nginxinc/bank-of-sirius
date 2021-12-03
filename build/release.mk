LAST_VERSION       = $(shell git tag -l | $(GREP) -E '^v[0-9]+\.[0-9]+\.[0-9]+$$' | $(SORT) --version-sort --field-separator=. --reverse | head -n1)
LAST_VERSION_HASH  = $(shell git show --format=%H $(LAST_VERSION) | head -n1)

.PHONY: update-manifest-image-versions
.ONESHELL: update-manifest-image-versions
update-manifest-image-versions: docker-python-dev-images ## Updates Kubernetes manifest files to use the current deployment version
	$Q $(info $(M) changing kubernetes-manifests/*.yaml to use image v$(VERSION))
	files="$$($(FIND) kubernetes-manifests/ -maxdepth 1 -mindepth 1 -type f -iname '*.yml' -or -iname '*.yaml' | xargs)"
	$(DOCKER) run --tty --rm -v "$(CURDIR):/project" --workdir "/project" \
		$(IMAGE_NAME_PREFIX)python-dev \
		python3 build/bin/update_manifest_image_versions.py 'v$(VERSION)' $${files}

.PHONY: update-manifest-versions
.ONESHELL: update-manifest-versions
update-manifest-versions:
	$(Q) $(info $(M) updating application versions in kubernetes manifests to v$(VERSION))
	files="$$($(FIND) kubernetes-manifests/ dev-kubernetes-manifests -maxdepth 1 -mindepth 1 -type f -iname '*.yml' -or -iname '*.yaml' | xargs)"
	$(DOCKER) run --tty --rm -v "$(CURDIR):/project" --workdir "/project" \
		$(IMAGE_NAME_PREFIX)python-dev \
		python3 build/bin/update_manifest_versions.py 'v$(VERSION)' $${files}

.PHONY: update-maven-versions
update-maven-versions: ## Updates maven projects to the latest version
	$Q $(info $(M) updating versions in maven projects to v$(VERSION))
	$Q $(MVN) versions:set -DnewVersion=$(VERSION)

.PHONY: version
version: ## Outputs the current version
	$Q echo "Version: $(VERSION)"
	$Q echo "Commit : $(GITHASH)"

.PHONY: version-set
.ONESHELL: version-set
version-set: ## Prompts for a new version
	$(info $(M) updating repository to new version) @
	$Q echo "  last committed version: $(LAST_VERSION)"
	$Q echo "  .version file version : $(VERSION)"
	read -p "  Enter new version in the format (MAJOR.MINOR.PATCH): " version
	$Q echo "$$version" | $(GREP) -qE '^[0-9]+\.[0-9]+\.[0-9]+$$' || \
		(echo "invalid version identifier: $$version" && exit 1) && \
	echo -n $$version > $(CURDIR)/.version

.PHONY: version-update
version-update: validate-manifests version-set update-manifest-image-versions update-manifest-versions update-maven-versions

.PHONY: release
.ONESHELL: release
release: ## Release container images to registry
	$Q $(info $(M) pushing container images)
	latest_tag="$$(git tag -l | tr -d ' ' | $(SED) 's/^v//' | $(SORT) --version-sort --reverse | head -n1)"
	echo "latest tag: $${latest_tag}"
	latest_release="$$(gh release list | $(GREP) -Eo '^v?[0-9]+\.[0-9]+\.[0-9]+' | $(SED) 's/^v//' | $(SORT) --version-sort --reverse | head -n1)"
	echo "latest release: $${latest_release}"

	for dir in $(IMAGES_TO_PUBLISH); do \
		dir_basename="$$(basename "$${dir}")"; \
		short_name="$(IMAGE_NAME_PREFIX)$${dir_basename}"; \
		image_name="$(IMAGE_PREFIX)/$${short_name}"; \
		echo "Publishing $${image_name}"; \
		$(DOCKER) push "$${image_name}:v$(VERSION)"; \
		if [[ "$${latest_tag}" == "$(VERSION)" ]] && [[ "$${latest_release}" == "$(VERSION)" ]]; then \
			echo "tagging v$(VERSION) as latest"; \
			$(DOCKER) tag "$${image_name}:v$(VERSION)" "$${image_name}:latest"; \
			echo "pushing v$(VERSION) as latest"; \
			$(DOCKER) push "$${image_name}:latest"; \
		fi; \
	done

.PHONY: changelog
.ONESHELL: changelog
changelog: ## Outputs the changes since the last version committed
	$Q echo 'Changes since $(LAST_VERSION):'
	git log --format="%s	(%h)" "$(LAST_VERSION_HASH)..HEAD" | \
		$(GREP) -Ev '^(ci|chore|docs|build): .*' | \
		$(SED) 's/: /:\t/g1' | \
		column -s "	" -t | \
		$(SED) -e 's/^/ * /'
