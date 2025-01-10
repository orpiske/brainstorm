# Sinks - Camel

Run a Camel YAML route when the pipeline is completed. 

# Writing YAML routes

The start endpoint for the SINK should be `direct:end-transformation`. The data is consumed from that endpoint.

**IMPORTANT**: when writing the project, the file _must_ be named `route.yaml`. It is possible to override, but prefer to stick to the convention.
