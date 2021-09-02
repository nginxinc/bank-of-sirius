# Change Log

## v1.0.0

### Rewrite of Build System

The project [Makefile](GNUmakefile) has been completely rewritten with new
targets.

### Google Cloud Platform (GCP) Dependencies Removal

Release, deployment, build and test scripts that had GCP dependencies have been
removed from the project. Except UI tests and deployment tests, the 
functionality has been replaced with Makefile targets and additional container
images.

### Github Actions CI

Github Actions previously depended on CI systems running in GCP. Those CI
workflows have been removed and replace with CI workflows that can reside
solely upon Github.

### Locust Upgrade

Locust has been upgraded to version 2.1.0 in the [loadgenerator project](src/loadgenerator).

### Tracing and Metrics Off by Default

Tracing and metrics toggles within the manifest files are now off by default.

### Load Generation Scripts no Longer Verify TLS Certificates

TLS verification in the [loadgenerator project](src/loadgenerator) has been 
disabled in order to allow for load testing development clusters. 

### Monolith Example Removed

The Java monolith project has been removed because it is not relevant for
the mission the Bank of Sirius as a containerized example application. At some
point in the future, the monolith project could be re-introduced in order to
show off how to containerize monoliths.

## v0.5.1

Version v0.5.1 is how we refer to the latest commit on master when the project
was forked. All forked changes happen after this version.
