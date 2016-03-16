## About JSON Transformation

A stream-based JSON manipulation library.

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.wcm.caravan/io.wcm.caravan.io.json-transform/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.wcm.caravan/io.wcm.caravan.io.json-transform)


### Documentation

* [Usage][usage]
* [API documentation][apidocs]
* [Changelog][changelog]


[usage]: usage.html
[apidocs]: apidocs/
[changelog]: changes-report.html


### Overview

#### Sources

Streaming pipeline starts with a source which transforms the input data into JSON elements. At this time JSON and XML is supported. Offering an iterator interface the elements can get requested by a Processor or Sink.

#### Processors

Processors modify the JSON element stream. Extending the Source interface a further Processor or Sink can get appended.

The module offers the following Processors at this time:

* ArrayProcessor: converts a JSON object into a JSON array and all children into JSON objects without name
* NumericFieldsProcessor: converts the value of a JSON object to a BigDecimal
* RenameProcessor: changes the name of a JSON element

#### Sinks

Sinks write/convert the JSON element stream to any format. At this time writing JSON into an output stream and Jackson Node conversion is supported.
