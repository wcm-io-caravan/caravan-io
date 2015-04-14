wcm.io Caravan Resilient Http
============================

Implements a resilient HTTP transport layer using:

* [Apache Http Async Client](http://hc.apache.org/httpcomponents-asyncclient-4.1.x/index.html) for communication
* [Netflix Hystrix](https://github.com/Netflix/Hystrix) for latency and fault tolerance
* [Netflix Ribbon](https://github.com/netflix/ribbon) for load balancing
* [Reactive Java](https://github.com/ReactiveX/RxJava) for asynchronous interfaces
* [RFC 6570 URI Template](https://tools.ietf.org/html/rfc6570) for URI templating

It's targeted for REST communication, but can be used for other protocols as well (e.g. SOAP).

Simple example
--------------

To execute a resilient HTTP request it needs to load the HTTP client by dependency injection, create the request with help of the builder and execute it by the client.
As the client works in an asynchron fashion, you will receive an Observable. It is possible to execute the request with a fallback getting returned if it fails.

```java
// create a new HTTP request with the builder
CaravanHttpRequest request = new CaravanHttpRequestBuilder("my-service")
  // default HTTP method is GET
  .method("GET")
  // add your URL in RFC 6570 URI Template format
  .append("/path{?param}")
  // pass template values at building time
  .build(ImmutableMap.of("param", "test-value"));

// HTTP client gets injected by OSGI
CaravanHttpClient client = ... 

// execute the request
Observable<CaravanHttpResponse> rxResponse = client.execute(request);

// wait for the response
CaravanHttpResponse response = rxResponse.toBlocking().single();

// check the status
if (HttpServletResponse.SC_OK.equals(response.status())) {
  // extract the body
  String body = response.body().asString();
}
```
