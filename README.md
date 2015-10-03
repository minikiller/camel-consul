# Camel registry plugin for Consul #

[Consul](https://www.consul.io/ "Consul") is a service registry. Some interesting features make it unique, such as Service Discovery, Health Checking, Key/Value Store and Multi-Datacenter support.

[Apache Camel](http://camel.apache.org/ "Apache Camel") is a Java integration framework implementing standard Enterprise Integration Patterns (EIP).

Camel allows to plugin custom service registries, which makes it ideal for using Consul. This projects provides this plugin implementation.

## Example ##

    // connect to Consul
    ConsulRegistry consulRegistry = new ConsulRegistry.Builder("localhost").build();
    // register our HelloBean under the name helloBean
    consulRegistry.put("helloBean", new HelloBean());
    // tell Camel to use our registry
    CamelContext camelContext = new DefaultCamelContext(consulRegistry);
    // add the route
    camelContext.addRoutes(new RouteBuilder() { public void configure() {
    	from("direct:hello").beanRef("helloBean");
    	}
    });
    // create a producer template for testing
    ProducerTemplate producerTemplate = camelContext.createProducerTemplate();
    // start Camel
    camelContext.start();

## Using ##

	<dependency>
	  <groupId>ws.prager.camel</groupId>
	  <artifactId>camel-consul</artifactId>
	  <version>1.1.0</version>
	</dependency>

## License ##
This component is distributed under [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0 "Apache License 2.0")