# ODE-X 

## Purpose
  
ODE-X is a prototype business process management (BPM) framework for executing persistent processes. The intent of a project is to provide common, reusable, and scalable infrastructure to rapidly create, extend, manage, execute, and monitor process definitions.    

   
## Key Concepts
  
### Persistent Process
A long running process that has a beginning and end that can span multiple systems. The process can be activated and passivated multiple times during its lifecycle. 

### Cloud Native Architecture
A Cloud Native Application is one that is designed from the ground up to operate in a scalable web base environment. An excellent overview of Cloud Native attributes can be found on this blog [Yet Another Look at Cloud-Native Apps Architecture](http://blog.grapeup.com/read/yet-another-look-at-cloud-native-apps-architecture-33).
  
### Tenant
A tenant is a single [isolated cluster](https://apacheignite.readme.io/v2.6/docs/tcpip-discovery) of ODE-X operated for a specific purpose with constrained access in accordance with the principle of [Multitenancy](https://en.wikipedia.org/wiki/Multitenancy).

### Node
ODE-X nodes Correspond directly to an Apache Ignite [cluster nodes](https://apacheignite.readme.io/docs/clustering). Server and node are used interchangeably in ODE-X.

### Microservices
[Microservices](https://en.wikipedia.org/wiki/Microservices) is a design philosophy of building many specific purpose stateless REST based services that are fault tolerant and continuously improved which can react quickly to changes in business and technological requirements.  

### Composite Application
A [Composite Application](https://en.wikipedia.org/wiki/Composite_application) is an application that combines existing applications and services into a new one. Composite applications are used to define and orchestrate communication between Microservices both instead and outside of ODE-X. 

### Assembly
A unit of deployment in a JAR format. It may contain Java code and libraries, static resources such as single page application HTML files, or dialect representations like executable BPMN xml files. Assemblies may contain a YAML descriptor file or necessary metadata is extracted through annotations and CDI inspection. Assemblies may have dependencies on other assemblies and have isolated classpaths in the ODE-X runtime.

### Composite
A configuration of an assembly. Concrete bindings are provided for all of the registered assembly endpoints. All required assembly configuration is also provided. Composites are environment specific so there may be dev/qa/prod composite variants. Composites can also be instrumented at runtime for monitoring or diagnostic purposes.

### Identifier
Everything in ODE-X is addressed using a Qname (namespace and local part) or generated GUID which can be used interchangeably. 

### Endpoints
Addresses for message communication that contains metadata such as type/protocol Authentication.

### Endpoint Registry
Every process interacts with other processes or external systems. Managing which endpoints are bound to each other is a complex task and central registry should be maintained. The registry can map a logical endpoint address (Qname) to a physical URL. 

### Exchange
ODE 2.0 processes will interact with external entities. Exchanges are mediation points were external requests and responses, like HTTP requests, are converted into Ignite computing requests that in turn are processed by ODE or module implementations. 

### Routing
route messages between endpoints using registry. - dynamic, microservices come and go. 

### Compiler
A dialect specific lexical analyzer that converts a dialect file such as BPMN into a sequence of instructions. As the compiler parses the dialect file extensions can indicate or override how the instructions are ordered and configured. 

### Instruction
A CDI annotated Java class that interacts through Ignite with ODE or assembly services to create, mutate, or destroy persisted state in Ignite. Instructions should be single purpose so that they can be rearanged in different execution order. Instructions will be provided contextual references through CDI or Ignite lookups to perform discrete processing tasks. Instructions should be annotated with source line and column numbers to easily correlate them back dialect source files for troubleshooting or diagnostic purposes. 

### Executable
A namespace aware XML representation of a sequence of instructions arranged in blocks. EXI or FI may be used to compress the representation for performance purposes.  

### Processor
A dialect specific interpreter that is activated on demand that executes instructions in sequence. The processor sets up the executational context the instructions are invoked in and acts on directives issued by the instructions, like create a process, destroy it, activate, passivate, etc.

### Repository
registry of artifacts, blob storage. WSDL, single page application zip or path, Java class zip/path, 
	

### Dialect
A representation of process orchestration, such as BPEL, BPMN, SCXML, etc. Dialects typically need to be interpreted for execution. 

### Compiler
convert target artifact int executable

### Module
An Apache ODE 2.0 extension that is bundled as a single JAR achive in the server classpath. A module may contain a dialect compiler, processor, numerous instructions, exchanges, CLI commands, etc. The ODE runtime will discover implementations and register them accordingly. A module may have dependencies on other modules and can include it’s own CDI extension to support it’s own extensions. 


## Major Features

### Virtual Processor
Implements a primitive metaphor everyone in computing is familiar with. A dialect implementer is free to [fracture](http://ode.apache.org/developerguide/jacob.html) source lines through a dialect specific compiler into a sequence of instruction blocks. Upon activation through an endpoint the dialect specific processor is activated and executes each instruction in sequence. There is no ODE 2.0 standard for process storage in ignite, common instructions, or executable XML file format. Instead ODE 2.0 provides optional utilities for assisting with these tasks that can be extended or replaced. For instance a StAX xml parser that supports registered extensions for Qnames, Ignite storage functions for creating/updating/destroying processes, etc. 

### Cloud Scale
Apache Ignite provides all the necessary capabilities and more. 

### Modules
ODE or other module extensions that are added to the server classpath and are discovered and registered through CDI.  

### Assembly
A bundle of executable code or content that is intended to be deployed as a service to ignite. Assemblies are jar files akin to Tomcat web applications. They are stored in Ignite as binary objects and are lazy loaded and extracted to a temp file on Ignite nodes after they are deployed for easy and efficient access. As dynamic deployable unit’s assemblies have their own classloader and CDI BeanManager. The assembly’s bean manager may delegate to the parent server classloader and bean manager to inject select API instances but in general the server classpath is [shielded from the assembly](http://mail-archives.apache.org/mod_mbox/openwebbeans-user/201810.mbox/browser). Assemblies with dependencies will be loaded in the same classpath. This can help with deploying a single shared dependency like a jar file or WSDL file and have it shared across multiple assemblies. Assemblies are deployed, undeployed, released, and recalled. If the assembly contains a dialect source file it also supports build, clean, and verify.  

### Composite
Composites are the configuration for one or more assemblies. They contain environment specific settings such as bindings for physical addresses and compiler preferences. Composites are created, updated, deleted, activated, and deactivated. 

### Management
All management is performed through CLI like other Cloud service providers. The CLI bootstrap entry point creates a custom classloader and bean manager so only CLI specific instances are loaded. Any necessary configuration files are in the YAML format. Activated composites can be instrumented with diagnostic configuration such as state logging or break points. 

 
## Technologies
 
### Apache Ignite 
While Apache Ignite offers a variety of distributed computing functionality significant capabilities include persistence [7], indexing (ACID SQL 99 support) [8], and eventing [9]. 

### OpenWebBeans
CDI scopes, extensions 

### Apache Tomcat
HTTP server 

### Apache CXF
REST, OIDC support 

### STaX and FastInfoset
 or Apache Avro for process executable format 

### YAML
Application configuration descriptor

## Platform Layers

### JVM
ODE-X is run as standalone JVM process. Each ODE-X node has it's own ODE_HOME directory where the runtime libraries and working directories reside. Classpath isolation is handled at the classloader level for now. Once Java modules are supported in Apache Ignite Java module isolation will be implemented. When a ODE-X server is started it creates a dedicated classloader for OpenWebbeans and Ignite so that multiple isolated servers can be run in the same JVM without any cross interference. Currently there is not a dedicated classloader for Modules, that should be handled in the future by Java modules, so for now there is a single Server classpath where all runtime and module libraries exist. 

Peer class loading is disabled in Ignite so all server runtime classes including module libraries and dependencies should be consistently provisioned on all ODE-X nodes. Basic runtime checks are performed to ensure modules are consistently deployed across nodes. 

### CDI
OpenWebbeans is used as the CDI container. CDI is reliant on classloading so all CDI injections and events occur in the isolated server runtime. CDI is intended to be used for automatic implementation discovery, injection, and eventing inside a single ODE-X node.

### Ignite
Ignite services such as compute, service grid, message queues, and caches should be used for all internal and external node-to-node interactions.  

### Application
An assembly may contain executable code which when activated in a running composite will have access to the classpath of peer assemblies and services defined in the API library. 

  