REGISTRY:=quay.io
ORGANIZATION=bstorm
VERSION_TAG:=latest

build:
	mvn package -Dquarkus.container-image.build=true
	podman build -f workers/camel-worker/Dockerfile -t $(REGISTRY)/$(ORGANIZATION)/camel-worker:$(VERSION_TAG) ./workers/camel-worker
	podman build -f workers/runner-worker/Dockerfile -t $(REGISTRY)/$(ORGANIZATION)/runner-worker:$(VERSION_TAG) ./workers/runner-worker

push:
	podman push $(REGISTRY)/$(ORGANIZATION)/operator:$(VERSION_TAG)
	podman push $(REGISTRY)/$(ORGANIZATION)/service:$(VERSION_TAG)
	podman push $(REGISTRY)/$(ORGANIZATION)/camel-worker:$(VERSION_TAG)
	podman push $(REGISTRY)/$(ORGANIZATION)/runner-worker:$(VERSION_TAG)

