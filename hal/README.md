wcm.io Caravan HAL
==================

A JSON HAL (Hypertext Application Language) library to document JSON output. Further information is available on the [HAL specification].
Adds links, curies (documentation URIs), and embedded resources to a JSON representation.

Central part of the library is the HalResource object which carries the resource self and HAL specific attributes. There exists a HalResourceFactory which helps to create last one. Transformation from or into this object is done by writers and readers.

Creating a HAL resource
-----------------------

To augment any type of object (really any object, Map, JSON) it just needs to create a new HalResource and set the current payload as state of the resource. Further it is possible to add links and embedded resources, where embedded resources are HalResources too.

```java
HalResource resource = new HalResource()
  .setState(payload)
  .setLink("self", new Link(uri))
  .setLink("more", new Link(moreUri))
  .setEmbeddedResource("children", new EmbeddedResource(children));
```
	  
HalResourceWriter
-----------------

At this time there only exists a Jackson implementation for the writer. This works best with native JsonNode objects as payload.

```java
HalResource halResource = ...
OutputStream output = ...
HalResourceWriter<JsonNode> writer = new JacksonHalResourceWriter();
writer.write(output, halResouce);
```

HalResourceReader
-----------------

Same to the writer, there only exists a Jackson based reader implementation, which converts the JSON input to a HalResource with JsonNode as payload.

```java
InputStream input = ...
HalResourceReader<JsonNode> reader = new JacksonHalResourceReader();
HalResource halResource = reader.read(input);
```
	
HalResourceFactory and ResourceMapper
-------------------------------------

Creating embedded resources can be very struggling. Thats why the HalResourceFactory can work with any kind of input objects and a ResourceMapper to convert the input objects into another representation and create the corresponding link for them.

```java
Iterable<A> children = ...
ResourceMapper<A, B> mapper = ...
EmbeddedResource embeddedChildren = HalResourceFactory.createEmbeddedResources(children, mapper);
```
	
[HAL specification]:http://stateless.co/hal_specification.html