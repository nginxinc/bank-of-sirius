.PHONY: docker-base-images
docker-base-images: $(foreach dir,$(BASE_IMAGES),docker-image-build/$(dir)) ## Build base images inherited by other images

.PHONY: docker-java-images
docker-java-images: docker-image-build/base-images/java java-build $(foreach project,$(JAVA_DOCKER_PROJECTS),docker-image-build/src/$(project)) ## Build Java container images

.PHONY: docker-python-images
docker-python-images: docker-image-build/base-images/python $(foreach project,$(PYTHON_PROJECTS),docker-image-build/src/$(project)) ## Build Python container images

.PHONY: docker-python-dev-images
docker-python-dev-images: docker-image-build/base-images/python docker-image-build/base-images/python-dev

.PHONY: docker-db-images
docker-db-images: $(foreach project,$(DB_PROJECTS),docker-image-build/src/$(project)) ## Build database container images

.PHONY: docker-nginx-images
docker-nginx-images: docker-image-build/src/nginx ## Build NGINX container images

.PHONY: docker-all-images
docker-all-images: docker-db-images docker-java-images docker-python-images docker-nginx-images ## Build all container images

.PHONY: docker-image-build/%
.ONESHELL:
docker-image-build/%: ; $(info $(M) building container image for $(notdir $@)) $Q
	BASENAME="$(notdir $@)"
	SHORTNAME="$(IMAGE_NAME_PREFIX)$${BASENAME}"
	DIR="$(CURDIR)$(subst docker-image-build,,$@)"
	cd "$${DIR}"
	$(DOCKER_BUILD) --tag "$${SHORTNAME}" --tag "$(IMAGE_PREFIX)/$${SHORTNAME}:latest" --tag "$(IMAGE_PREFIX)/$${SHORTNAME}:v$(VERSION)" .
