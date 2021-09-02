# Running in Kubernetes Environments

## NGINX Reference Architectures

This application is designed to be deployed via the [NGINX KIC Reference Architectures](https://github.com/nginxinc/kic-reference-architectures) project. That said, this application can be deployed to any Kubernetes cluster via the manifests located in the [kubernetes-manifests](../kubernetes-manifests) directory.

## GKE Metrics
This application was designed to send tracing to **Google Cloud Operations**. This fork is removing that code in favor of moving to a more generic OTEL model. This can still be used with Google's metrics/tracing infrastructure, but the primary goal is to be as agnostic as possible in terms of telemetry.


