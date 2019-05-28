# Design 

## Service Orientated 

Internally ODE-X provides BPM services that interact with with other internal and external services. The services can register extensions or be extended themselves. Interdependent modules are used to bundle a set of services together and are installed and activated at runtime. 
 
## Layers

Modularity and extensibility are key goals of the ODE-X prototype and to support these objectives several technologies are used. A layered approach is used to implement functionality and an understanding of how the technologies interact with each is required in order develop consistent and scalable features.
  
### Java

ODE-X is developed in Java and all core services and extensions are written in it. Module extensions should be archived into jar files 

#### Classloading

Ignite peer classloading is disabled and it is expected that ODE-X nodes be distributed and installed with identical classpaths. Currently there is a single node classpath that includes all runtime libraries including modules. Once Ignite supports Java modules then the classpath may be isolated at the JVM level. The ODE server creates a simple top level parent classloader instance that is used at startup for isolation purposes. This allows multiple nodes and OWB containers to run in the same JVM at the same time.
    
### CDI

CDI is utilized for runtime discovery and injection. Be sure to include a beans.xml file so that any annotated implementation classes are discovered by the CDI container and become eligible for injection. Be aware that CDI should only be used for processing inside of a single node. Any references to CDI services from inside an Ignite compute or service instance should be declared as transient so that these local references are not inadvertently serialized by the Ignite job framework.  

### Ignite

Ignite is the core framework that the ODE-X prototype is built on. Each ODE-X node starts an embedded Ignite server that can connect to other nodes on the same network.

#### Clustering

ODE-X relies on Ignite clustering to horizontally scale. Affinity keys are used to partition cache data across multiple nodes to evenly distribute the load. Default Ignite communication settings are utilized during startup but can be overwritten via YAML configuration or CDI events. 

#### Compute

Ignite compute may be used to execute jobs on the local or remote nodes.  

#### Persistent Cache 

Ignite persistent caches play a prominent role in the ODE-X platform. All configuration, process definitions, and process instances are stored and retrieved from persistent caches. These caches are replicated or partitioned across multiple nodes for scalability and high availability.

#### Query

Ignite provides a SQL compliant querying interface which is used to 

#### Services    

Ignite services are used for managing and exposing core BPM functionality. Ignite services can be deployed as node or cluster singletons and have direct access to all of Ignites functionality.  


## CDI vs Ignite Services

A microservices Ignite services should be used for all inter-node service access since these services are managed and scaled by Ignite. CDI can be used for single JVM service discovery and dependency injection. CDI injection points should be declared with the transient modifier so that they are not inadvertently serialized during Ignite service and compute operations.

### Reflection Invocation vs Events

CDI has an event feature that can be used to notify event observers. However event classes need to be created for each event type and qualifier annotations, possibly verbose, need to be set along with the observes annotation on the method event parameter. 

Alternatively, marker annotations can be used to identify implementations that should participate in particular processing flows. The implementations can have annotated methods for specific operations which have variable parameters based on requirements.       