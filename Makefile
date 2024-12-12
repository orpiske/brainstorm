REGISTRY:=quay.io
ORGANIZATION=bstorm
VERSION_TAG:=latest
KUBECTL:=kubectl

build:
	mvn clean -Dquarkus.container-image.build=true -Dquarkus.container-image.registry=$(REGISTRY) -Dquarkus.container-image.group=$(ORGANIZATION) -Dquarkus.container-image.tag=$(VERSION_TAG) package
	podman build -f workers/sources/camel-source/Dockerfile -t $(REGISTRY)/$(ORGANIZATION)/camel-source:$(VERSION_TAG) ./workers/sources/camel-source
	podman build -f workers/transformers/runner-transformer/Dockerfile -t $(REGISTRY)/$(ORGANIZATION)/runner-transformer:$(VERSION_TAG) ./workers/transformers/runner-transformer

push:
# These are only needed if not using quay.io
ifneq ($(REGISTRY),quay.io)
	podman tag localhost/$(ORGANIZATION)/operator:$(VERSION_TAG) $(REGISTRY)/$(ORGANIZATION)/operator:$(VERSION_TAG)
	podman tag localhost/$(ORGANIZATION)/service:$(VERSION_TAG) $(REGISTRY)/$(ORGANIZATION)/service:$(VERSION_TAG)
endif
	podman push $(REGISTRY)/$(ORGANIZATION)/operator:$(VERSION_TAG)
	podman push $(REGISTRY)/$(ORGANIZATION)/service:$(VERSION_TAG)
	podman push $(REGISTRY)/$(ORGANIZATION)/camel-source:$(VERSION_TAG)
	podman push $(REGISTRY)/$(ORGANIZATION)/runner-transformer:$(VERSION_TAG)


prepare-cluster:
	$(KUBECTL) apply -f operator/samples/kind-pv-sample.yaml
