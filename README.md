# Tracer: Distributed system tracing

[![Highway at Night](docs/highway.jpg)](https://pixabay.com/en/highway-at-night-long-long-exposure-371009/)

[![Build Status](https://img.shields.io/travis/zalando/tracer.svg)](https://travis-ci.org/zalando/tracer)
[![Coverage Status](https://img.shields.io/coveralls/zalando/tracer.svg)](https://coveralls.io/r/zalando/tracer)
[![Release](https://img.shields.io/github/release/zalando/tracer.svg)](https://github.com/zalando/tracer/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/tracer-parent.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/tracer-parent)

*Tracer* is a library that manages custom trace identifiers and carries them through distributed systems. Traditionally traces are transported as custom HTTP headers, usually generated by clients on the very first request. Traces are added  to any subsequent requests and responses, especially to transitive dependencies. Having a consistent trace across different services in a system allows to correlate requests and responses beyond the traditional single client-server communication. 

This library historically originates from a closed-source implementation called *Flow-ID*. The goal was to create a clean open source version in which we could get rid of all the drawbacks of the old implementation, e.g. strong-coupling to internal libraries, single hard-coded header and limited testability.

- **Status**: Under development and used in production

## Features

-  **Tracing** of HTTP requests and responses
-  **Customization** by having a pluggable trace format and lifecycle listeners for easy integration
-  **Support** for Servlet containers, Apache’s HTTP client, and (via its elegant API) other frameworks
-  Convenient [Spring Boot](http://projects.spring.io/spring-boot/) Auto Configuration
-  Sensible defaults

## Dependencies

- Java 8
- Any build tool using Maven Central, or direct download
- Servlet Container (optional)
- Apache HTTP Client (optional)
- Spring Boot (optional)

## Installation

Selectively add the following dependencies to your project:

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>tracer-core</artifactId>
    <version>${tracer.version}</version>
</dependency>
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>tracer-servlet</artifactId>
    <version>${tracer.version}</version>
</dependency>
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>tracer-httpclient</artifactId>
    <version>${tracer.version}</version>
</dependency>
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>tracer-hystrix</artifactId>
    <version>${tracer.version}</version>
</dependency>
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>tracer-aspectj</artifactId>
    <version>${tracer.version}</version>
</dependency>
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>tracer-spring-boot-starter</artifactId>
    <version>${tracer.version}</version>
</dependency>
```

## Usage

After adding the dependency, create a `Tracer` and specify the name of the traces you want to manage:

```java
Tracer tracer = Tracer.create("X-Trace-ID");
```

If you need access to the current trace's value, call `getValue()` on it:

```java
Trace trace = tracer.get("X-Trace-ID"); // this is a live-view that can be a shared as a singleton
entity.setLastModifiedBy(trace.getValue());
```

### Generators

When starting a new trace, *Tracer* will create a time-based UUID by default. You can override this on a per-trace level by adding the following to your setup:

```java
Tracer tracer = Tracer.builder()
        .trace("X-Trace-ID", new CustomGenerator())
        .build();
```

For legacy reasons we use a different generator internally, the `FlowIDGenerator`. It basically renders a UUID as a base64-encoded byte array, e.g. `REcCvlqMSReeo7adheiYFA`.

### Listeners

For some use cases, e.g. integration with other frameworks and libraries, it might be useful to register a listener that gets notified every time a trace is either started or stopped.

```java
Tracer tracer = Tracer.builder()
        .trace("X-Trace-ID")
        .listener(new CustomTraceListener())
        .build();
```

*Tracer* comes with a very useful listener by default, the `MDCTraceListener`:

```java
Tracer tracer = Tracer.builder()
        .trace("X-Trace-ID")
        .listener(new MDCTraceListener())
        .build();
```

It allows you to add the trace id to every log line:

```xml
<PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} [%X{X-Trace-ID}] - %msg%n"/>
```

Another built-in listener is the `LoggingTraceListener` which logs the start and end of every trace.

## Servlet

On the server side is a single filter that you must be register in your filter chain. Make sure it runs very early — otherwise you might miss some crucial information when debugging.

You have to register the `TracerFilter` as a `Filter` in your filter chain:

```java
context.addFilter("TracerFilter", new TracerFilter(tracer))
    .addMappingForUrlPatterns(EnumSet.of(REQUEST, ASYNC, ERROR), true, "/*"); 
```

## Apache HTTP Client

Many client-side HTTP libraries on the JVM use the Apache HTTPClient, which is why *Tracer* comes with a request interceptor:

```java
DefaultHttpClient client = new DefaultHttpClient();
client.addRequestInterceptor(new TracerHttpRequestInterceptor(tracer));
```

## Hystrix

*Tracer* comes with built-in Hystrix support in form of a custom `HystrixConcurrencyStrategy`:

```java
final HystrixPlugins plugins = HystrixPlugins.getInstance();
final HystrixConcurrencyStrategy delegate = HystrixConcurrencyStrategyDefault.getInstance(); // or another
plugins.registerConcurrencyStrategy(new TracerConcurrencyStrategy(tracer, delegate));
```

## AspectJ

For background jobs and tests, you can use the built-in aspect:

```java
@Traced
public void performBackgroundJob() {
    // do work
}
```

or you can manage the lifecycle yourself:

```java
tracer.start();

try {
    // do work
} finally {
    tracer.stop();
}
```

## Spring Boot Starter

Tracer comes with a convenient auto configuration for Spring Boot users that sets up aspect, servlet filter and MDC support automatically with sensible defaults:

| Configuration               | Description                                                                        | Default                     |
|-----------------------------|------------------------------------------------------------------------------------|-----------------------------|
| `tracer.aspect.enabled`     | Enables the [`TracedAspect`](#aspect)                                              | `true`                      |
| `tracer.async.enabled`      | Enables for asynchronous tasks, i.e. `@Async`                                      | `true`                      |
| `tracer.filter.enabled`     | Enables the [`TracerFilter`](#servlet)                                             | `true`                      |
| `tracer.logging.enabled`    | Enables the [`LoggingTraceListener`](#logging)                                     | `false`                     |
| `tracer.logging.category`   | Changes the category of the [`LoggingTraceListener`](#logging)                     | `org.zalando.tracer.Tracer` |
| `tracer.mdc.enabled`        | Enables the [`MdcTraceListener`](#logging)                                         | `true`                      |
| `tracer.scheduling.enabled` | Enables support for Task Scheduling, i.e. `@Scheduled`                             | `true`                      |
| `tracer.traces`             | Configures actual traces, mapping from name to generator type (`uuid` or`flow-id`) |                             |

```yaml
tracer:
    aspect.enabled: true
    async.enabled: true
    filter.enabled: true
    logging:
        enabled: false
        category: org.zalando.tracer.Tracer
    mdc.enabled: true
    scheduling.enabled: true
    traces:
        X-Trace-ID: uuid
        X-Flow-ID: flow-id
```

The `TracerAutoConfiguration` will automatically pick up any `TraceListener` bound in the application context.

**Be aware**: The TracerAutoConfiguration will, as long as `tracer.async.enabled` is set to `true`, register a 
`AsyncConfigurer` and Spring only allows one per Application Context. If this turns out to be a problem please disable
the async support and configure it manually. Let us know if you think this needs to be improved.

## Getting Help with Tracer

If you have questions, concerns, bug reports, etc., please file an issue in this repository's [Issue Tracker](issues).

## Getting Involved/Contributing

To contribute, simply make a pull request and add a brief description (1-2 sentences) of your addition or change. For
more details, check the [contribution guidelines](CONTRIBUTING.md).

## Open Issues

* [Hystrix Support](issues/20)
* [Zipkin Support](issues/18) (*debatable*)
