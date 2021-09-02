COMMITSAR_DOCKER	:= docker run --tty --rm --workdir /src -v "$(CURDIR):/src" aevea/commitsar
COMMITSAR			?= $(shell command -v commitsar 2> /dev/null)

# Use docker based commitsar if it isn't in the path
ifeq ($(COMMITSAR),)
	COMMITSAR = $(COMMITSAR_DOCKER)
endif

.PHONY: commitsar
commitsar: ## Run git commit linter
	$Q $(info $(M) running commitsar...)
	$(COMMITSAR)
