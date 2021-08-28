.PHONY: update-manifests
.ONESHELL: update-manifests
update-manifests: ## Updates Kubernetes manifest files to use the current deployment version
	$Q $(info $(M) changing kubernetes-manifests/*.yaml to use image v$(VERSION))
	files="$$($(FIND) kubernetes-manifests/ -maxdepth 1 -mindepth 1 -type f -iname '*.yml' -or -iname '*.yaml' | xargs)"
	$(DOCKER) run --tty --rm -v "$(CURDIR):/project" --workdir "/project" \
		$(IMAGE_NAME_PREFIX)python3-dev \
		python3 build/bin/update_manifest_image_versions.py 'v$(VERSION)' $${files}
