REGISTRY:=quay.io
ORGANIZATION=bstorm

build:
	mvn package -Dquarkus.container-image.build=true
	podman build -f workers/camel-worker/Dockerfile -t $(REGISTRY)/$(ORGANIZATION)/camel-worker ./workers/camel-worker
	podman build -f workers/runner-worker/Dockerfile -t $(REGISTRY)/$(ORGANIZATION)/runner-worker ./workers/runner-worker

push:
	podman push $(REGISTRY)/$(ORGANIZATION)/operator
	podman push $(REGISTRY)/$(ORGANIZATION)/camel-worker
	podman push $(REGISTRY)/$(ORGANIZATION)/runner-worker

