LAST_VERSION       = $(shell git tag -l | $(GREP) -E '^v[0-9]+\.[0-9]+\.[0-9]+$$' | $(SORT) --version-sort --field-separator=. --reverse | head -n1)
LAST_VERSION_HASH  = $(shell git show --format=%H $(LAST_VERSION) | head -n1)

.PHONY: update-manifests
.ONESHELL: update-manifests
update-manifests: ## Updates Kubernetes manifest files to use the current deployment version
	$Q $(info $(M) changing kubernetes-manifests/*.yaml to use image v$(VERSION))
	files="$$($(FIND) kubernetes-manifests/ -maxdepth 1 -mindepth 1 -type f -iname '*.yml' -or -iname '*.yaml' | xargs)"
	$(DOCKER) run --tty --rm -v "$(CURDIR):/project" --workdir "/project" \
		$(IMAGE_NAME_PREFIX)python3-dev \
		python3 build/bin/update_manifest_image_versions.py 'v$(VERSION)' $${files}

.PHONY: version
version: ## Outputs the current version
	$Q echo "Version: $(VERSION)"
	$Q echo "Commit : $(GITHASH)"

.PHONY: version-update
.ONESHELL: version-update
version-update: ## Prompts for a new version
	$(info $(M) updating repository to new version) @
	$Q echo "  last committed version: $(LAST_VERSION)"
	$Q echo "  .version file version : $(VERSION)"
	read -p "  Enter new version in the format (MAJOR.MINOR.PATCH): " version
	$Q echo "$$version" | $(GREP) -qE '^[0-9]+\.[0-9]+\.[0-9]+$$' || \
		(echo "invalid version identifier: $$version" && exit 1) && \
	echo -n $$version > $(CURDIR)/.version
