wcm.io Dromas Resilient Http
============================

Implements a resilient HTTP transport layer using:

* [Apache Http Async Client](http://hc.apache.org/httpcomponents-asyncclient-4.1.x/index.html) for communication
* [Netflix Hystrix](https://github.com/Netflix/Hystrix) for latency and fault tolerance
* [Netflix Ribbon](https://github.com/netflix/ribbon) for load balancing
* [Reactive Java](https://github.com/ReactiveX/RxJava) for asynchronous interfaces

Its target for REST communication, but can be used for other protocols as well (e.g. SOAP).
