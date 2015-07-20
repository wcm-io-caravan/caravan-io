## JSON Transformation Usage

### Simple Example

```java
// define input source
Source source = new JacksonStreamSource(input);

// define a renaming processor
Map<String, String> mapping = new HashMap<String, String>();
mapping.put("oldname", "newname");
Processor rename = new RenameProcessor(source, mapping);

// write into output
Sink sink = new JacksonStreamSink(output);
while (rename.hasNext()) {
  sink.write(rename.next());
}
```
