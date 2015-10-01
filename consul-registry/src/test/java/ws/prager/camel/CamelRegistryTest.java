package ws.prager.camel;

import java.io.Serializable;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

/**
 * 
 * @author Bernd Prager
 * Unit test class for Camel integration of Consul Registry plug-in
 *
 */
public class CamelRegistryTest extends CamelTestSupport implements Serializable {

	private static final long serialVersionUID = 2503932166836068033L;
	private CamelContext camelContext;
	private ProducerTemplate producerTemplate;
	private static ConsulRegistry consulRegistry;
	
	public class HelloBean implements Serializable {
		private static final long serialVersionUID = -1956587463595322854L;

		public String hello(String name) {
			return "Hello " + name;
		}
	}
	
	@Override
	public void setUp() throws Exception {
		// connect to Consul
		consulRegistry = new ConsulRegistry.Builder("192.168.99.100").build();
		// register our HelloBean under the name helloBean
		consulRegistry.put("helloBean", new HelloBean());
		// tell Camel to use our registry
		camelContext = new DefaultCamelContext(consulRegistry);
		// add the route
		camelContext.addRoutes(new RouteBuilder() {
			public void configure() {
				from("direct:hello").beanRef("helloBean");
			}
		});
		// create a producer template for testing
		producerTemplate = camelContext.createProducerTemplate();
		// start Camel
		camelContext.start();
	}
	
	@Override
	public void tearDown() throws Exception {
		// clean up
		consulRegistry.remove("helloBean");
		producerTemplate.stop();
		camelContext.stop();
	}
	
	@Test
	public void testHello() {
		// send 'World' into route and expect 'Hello World' back
		Object reply = producerTemplate.requestBody("direct:hello", "World");
		assertEquals("Hello World", reply);
	}

}
