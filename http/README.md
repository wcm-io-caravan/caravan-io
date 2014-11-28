wcm.io Dromas Resilient Http
============================

Implements a resilient HTTP transport layer using:

* Apache Http Async Client for communication
* Netflix Hystrix for latency and fault tolerance
* Netflix Ribbon for load balancing
* Reactive Java for asynchronous interfaces

Its target for REST communication, but can be used for other protocols as well (e.g. SOAP).
