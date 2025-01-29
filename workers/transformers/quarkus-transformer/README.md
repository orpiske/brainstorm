# Transformers - Quarkus

The Quarkus Transformer allows creating transformers using Quarkus. They will consume events from
the pipeline bus:

```java
@ApplicationScoped
@Priority(1)
@Alternative
public class TestController implements EventController {
    private static final Logger LOG = Logger.getLogger(TestController.class);

    @Override
    public boolean handle(String event) {
        // Your pipeline code goes here
        LOG.debugf("Polled Record:(%s)\n", event);

        boolean ret = doSomething(); 
        if (!ret) {
            LOG.debugf("Failed to handle the event");
            
            return false;
        }
        
        return true;
    }
}
```

To create a new transformer, use the `archetype-quarkus-transformer`:

```shell
mvn -B archetype:generate -DarchetypeGroupId=org.brainstorm \
  -DarchetypeArtifactId=archetype-quarkus-transformer \
  -DarchetypeVersion=1.0-SNAPSHOT \
  -DgroupId=org.brainstorm \
  -Dpackage=org.brainstorm.transformer.test \
  -DartifactId=test-transformer \
  -Dname=TestTransformer -Dversion=1.0-SNAPSHOT
```