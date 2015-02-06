wcm.io Caravan JSON transform
=============================

Implements a streaming JSON manipulation library.


Sources
-------

Streaming pipeline starts with a source which transforms the input data into JSON elements. At this time JSON and XML is supported. Offering an iterator interface the elements can get requested by a Processor or Sink.


Processors
----------
Processors modify the JSON element stream. Extending the Source interface a further Processor or Sink can get appended.

The module offers the following Processors at this time:

* ArrayProcessor: converts a JSON object into a JSON array and all children into JSON objects without name
* NumericFieldsProcessor: converts the value of a JSON object to a BigDecimal
* RenameProcessor: changes the name of a JSON element


Sinks
-----
Sinks write/convert the JSON element stream to any format. At this time writing JSON into an output stream and Jackson Node conversion is supported.


Simple Example
--------------

	// define input source
	Source source = new JacksonStreamSource(input);
	// define a renaming processor
	Map<String, String> mapping = new HashMap<String, String>();
	mapping.put("oldname", "newname");
	Processor rename = new RenameProcessor(source, mapping);
	// write into output
	Sink sink = new JacksonStreamSink(output);	
	while(rename.hasNext()) {
	  sink.write(rename.next());
	}
