#REGISTRY:=quay.io
#ORGANIZATION=bstorm
REGISTRY:=docker.io
ORGANIZATION=otavio021
VERSION_TAG:=latest
KUBECTL:=kubectl

build:
	mvn -Dquarkus.container-image.build=true -Dquarkus.container-image.registry=$(REGISTRY) -Dquarkus.container-image.group=$(ORGANIZATION) -Dquarkus.container-image.tag=$(VERSION_TAG) package
	podman build -f workers/camel-worker/Dockerfile -t $(REGISTRY)/$(ORGANIZATION)/camel-worker:$(VERSION_TAG) ./workers/camel-worker
	podman build -f workers/runner-worker/Dockerfile -t $(REGISTRY)/$(ORGANIZATION)/runner-worker:$(VERSION_TAG) ./workers/runner-worker

push:
	podman tag localhost/$(ORGANIZATION)/operator:$(VERSION_TAG) $(REGISTRY)/$(ORGANIZATION)/operator:$(VERSION_TAG)
	podman tag localhost/$(ORGANIZATION)/service:$(VERSION_TAG) $(REGISTRY)/$(ORGANIZATION)/service:$(VERSION_TAG)
	podman push $(REGISTRY)/$(ORGANIZATION)/operator:$(VERSION_TAG)
	podman push $(REGISTRY)/$(ORGANIZATION)/service:$(VERSION_TAG)
	podman push $(REGISTRY)/$(ORGANIZATION)/camel-worker:$(VERSION_TAG)
	podman push $(REGISTRY)/$(ORGANIZATION)/runner-worker:$(VERSION_TAG)


prepare-cluster:
	$(KUBECTL) apply -f operator/samples/kind-pv-sample.yaml
