# Transformers - Camel

Run a Camel YAML route when a transformation event is received. 

# Writing YAML routes

When a new transformation event is received, it produces the data to `direct:start-transformation` endpoint. That should be the starting
route for your YAML routes.

The end route for the transformation should be `direct:end-transformation`. Whatever data is produced to this endpoint, will then 
be forward to the Kafka topic pointed to via `--produces-to` option (this is managed by the operator, and you don't need to worry with that).

**IMPORTANT**: when writing the project, the file _must_ be named `route.yaml`. It is possible to override, but prefer to stick to the convention.
