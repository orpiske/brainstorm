# Brainstorm 

The Brainstorm project provides a Kubernetes-based platform that simplifies building data 
pipelines with traditional Middleware.

NOTE: this is pre-alpha software.

## Building 

Edit the `Makefile` file and set the `REGISTRY` and `ORGANIZATION` variables: 

```Makefile
REGISTRY:=quay.io
ORGANIZATION=bstorm
```

Then run: 

```shell
make build push
````

This will build the project and push the containers to the Docker registry of your choice.

## Running 

### Setting up pre-requisites

#### OpenShift / Kubernetes

The most important pre-requisite is a Kubernetes-based environment. At this moment, 
the project has been tested only with [Kind (Kubernetes In Docker)](https://kind.sigs.k8s.io/), but 
is very likely possible to work with OpenShift and other Kubernetes distributions. 

#### Kind 

The recommended way to create a Kubernetes environment is via the Kind extension for Podman Desktop. 
You can follow the steps described in the [Podman Desktop documentation](https://podman-desktop.io/docs/kind) to get it up and running.

Make sure you have the `kubectl` and `kind` commands installed. Podman Desktop can help you install both of them.

### Running the operator

The operator uses the [Java Operator SDK](https://javaoperatorsdk.io/). This SDK will resolve the 
Kubernetes cluster by inspecting the local configuration for the `kubectl` command. This process is 
described in more detail in the [Getting Started section of the documentation](https://javaoperatorsdk.io/docs/getting-started/#getting-started).

**NOTE**: Podman Desktop can manage the Kubernetes contexts for you. You can set it using the tray icon or the Dashboard.

With the Kind cluster up and running, the `kubectl` command installed and the Kubernetes context set. Then you can launch the 
operator. At this moment, the recommended way is by running the `main` method on BrainStormOperator class 
directly via the IDE or by running `mvn quarkus:dev`.


If everything is alright, you should see a message like this:

```
2024-12-11 11:14:04,154 INFO  [io.jav.ope.pro.Controller] (Controller Starter for: pipelinereconciler) 'pipelinereconciler' controller started
```

This means that the reconciliation loop of the operator has started.

#### Setting up the Persistent Volume 

The pipeline needs a permanent storage to share data between jobs. Currently, the jobs use a [Persistent Volume](https://kubernetes.io/docs/concepts/storage/persistent-volumes/)
for that. 

The code base already comes with a sample definition for that. You can run the following command (once per cluster) to setup 
the volume: 

```make prepare-cluster```

#### Wrapping up

For now, that's all it's needed. The second part is to create or run a pipeline.

#### Creating a new project

You can create a new data pipeline project using the CLI with following command: 

```shell
java -jar cli/target/quarkus-app/quarkus-run.jar project new --destination /path/to/project
```

This will create a new project with the following structure:

* `/path/to/project/acquisition` -> this is where acquisition artifacts go
  * `/path/to/project/acquisition/route.yaml` -> this is the YAML route for Camel. Design it with [Kaoto](https://kaoto.io/).
* `/path/to/project/k8s` -> this is where the kubernetes artifacts go
  * `/path/to/project/k8s/pipeline.yaml` -> this is the pipeline that is deployed
* `/path/to/project/transformation` -> this is where transformation artifacts go.
  * `/path/to/project/transformation/01` -> Every step is numbered (from 01 to 99)
  * `/path/to/project/transformation/01/transform.sh` -> this is the launch script for the step
  * `/path/to/project/transformation/01/Dockerfile` -> this is the `Dockefile` that packages the step

Once you have adjusted/developed the step, you can run the following commands to create the images:

1. First, create the package for the acquisition:

```shell
java -jar cli/target/quarkus-app/quarkus-run.jar package acquisition --ingestion /path/to/project/acquisition/route.yaml --output-image quay.io/myorg/camel-source-runner-01:latest --username ${ORGANIZATION_USER} --password ${REGISTRY_PASSWD}
```

2. Package any step(s) you might have: 

```shell
java -jar cli/target/quarkus-app/quarkus-run.jar package runner --base-dir /path/to/project/transformation/01/ --output-image quay.io/myorg/runner-transformation-step-01
```

**NOTE**: repeat this for as many steps as you have, making sure to adjust the command and the name of the container.

3. Push the transformation containers: 

```shell
podman push quay.io/myorg/runner-transformation-step-01
```

**NOTE**: eventually, this will be handled by the CLI tool. 

4. Adjust the pipeline definition:

```yaml
apiVersion: "pipeline.brainstorm.org/v1alpha1"
kind: Pipeline
metadata:
  name: my-test-pipeline
  namespace: default
spec:
  pipelineInfra:
    bootstrapServer: 'my-kafka-server'
    port: 9092
  acquisitionStep:
    image: quay.io/my-org/camel-source-runner-01:latest
    producesTo: data.acquired
  transformationSteps:
    steps:
      - image: quay.io/my-org/runner-transformation-step-01
        consumesFrom: data.acquired
        producesTo: data.step.01
        name: step-01
```

Make sure to: 
1. Set the address of the Kafka cluster
2. Set the images to the ones you have created with the CLI
3. Add more steps if you have created them

After the `k8s/pipeline.yaml` file is adjusted, it apply it to the cluster:

```shell
kubectl apply -f k8s/pipeline.yaml
```

Then you should be able to check that it was created:

```
[~]$ kubectl get pipelines
NAME                       AGE
camel-dataset-brainstorm   6m26s
```

You can check if the acquisition and transformation jobs were created using the `kubectl get jobs` command. 

After completion, you can verify their statuses using: 

```shell
[~]$ kubectl get jobs
NAME                       STATUS     COMPLETIONS   DURATION   AGE
my-sample-acquisition      Complete   1/1           47s        7m27s
step-01                    Complete   1/1           111s       7m27s
step-02                    Complete   1/1           113s       7m27s
```

**NOTE**: and, of course, you can read the pod logs for details and more. 

###  Service Backend

The project also comes with a service web backend that, eventually, should provide a Web UI for the 
pipeline, statuses and some quality-of-life features. At this moment, this is not fully developed. 

### Accessing the backend API

Find the port: 

```
kubectl get services
NAME               TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)          AGE
external-service   NodePort    10.96.155.176   <none>        8080:30781/TCP   15m
kubernetes         ClusterIP   10.96.0.1       <none>        443/TCP          24h
```

Then forward the port:

```k port-forward svc/external-service 30781:8080```


## Development 

The system is currently composed of the following components: 

* An API module: that contains API, events, etc
* A CLI module: to create new projects and package artifacts 
* Workers that actually execute the actual acquisition, transformation and sink. Currently consists of:
  *  Source Workers
     * Camel Source: A worker that can consume from multiple sources using a Camel route defined in YAML.
  *  Transformation Workers:
     * Runner Worker: a worker that runs a transformation task based provided in a script

## Developing the Operator

NOTE: if you have never written an operator, check this [blog post on Red Hat Developer's Blog](https://developers.redhat.com/articles/2022/02/15/write-kubernetes-java-java-operator-sdk).

## Creating new Transformers

You can use the transformer archetype to create new transformers.

```shell
 mvn -B archetype:generate -DarchetypeGroupId=org.brainstorm -DarchetypeArtifactId=archetype-worker-transformer -DarchetypeVersion=1.0-SNAPSHOT  -DgroupId=org.brainstorm -Dpackage=org.brainstorm.transformer.test -DartifactId=test-transformer -Dname=Test
```

**NOTE**: make sure to adapt the following parameters: 

* `-Dpackage=org.brainstorm.transformer.test` -> the package should follow the convention `org.brainstorm.transformer.THE_NAME`
* `-DartifactId=test-transformer` -> the artifact should follow the convention `THE-NAME-transformer`
* `-Dname=Test` -> the name should be capitalized  

This will ensure it matches the Brainstorm operator expectations. 

# Roadmap / TODO

- Manage the storage in the operator
- Create a project format and let it be handled by the CLI
- Handle pushes via the CLI tool
- (TBD) Resolve the Kafka cluster automatically (likely from Strimzi)
- Resolve/set step topics automatically
- (TBD) Integrate Kaoto 
- Kubeflow integration
- Implement other types of workers
  - Flink
  - Quarkus
  - Camel + YAML DSL (for transformation, not acquisition)
  - Python
  - Go
  - Rust
  - 
- Implement/Investigate (Camel-based) sinks:
  - [Apache Iceberg](https://iceberg.apache.org/)
  - [Apache Hudi](https://hudi.apache.org/)
  - [Apache Ozone](https://ozone.apache.org/)
  - [Trino](https://trino.io/)
  - [MinIO](https://min.io/)
  - Vector DBs
  - HuggingFace