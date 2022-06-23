## About Resilient HTTP

HTTP communication layer using Netflix Hystrix and NetFlix Ribbon for resilience.

[![Maven Central](https://img.shields.io/maven-central/v/io.wcm.caravan/io.wcm.caravan.io.http)](https://repo1.maven.org/maven2/io/wcm/caravan/io.wcm.caravan.io.http/)


### Documentation

* [Usage][usage]
* [API documentation][apidocs]
* [Changelog][changelog]


[usage]: usage.html
[apidocs]: apidocs/
[changelog]: changes-report.html


### Overview

Implements a resilient HTTP transport layer using:

* [Apache Http Client](http://hc.apache.org/httpcomponents-client-4.4.x/index.html) for communication
* [Netflix Hystrix](https://github.com/Netflix/Hystrix) for latency and fault tolerance
* [Netflix Ribbon](https://github.com/netflix/ribbon) for load balancing
* [Reactive Java](https://github.com/ReactiveX/RxJava) for asynchronous interfaces
* [RFC 6570 URI Template](https://tools.ietf.org/html/rfc6570) for URI templating

It's targeted for REST communication, but can be used for other protocols as well (e.g. SOAP).
