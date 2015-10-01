/**
 * Copyright (C) 2015 Bernd Prager <http://prager.ws>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ws.prager.camel.consul;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Properties;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.log4j.Logger;
import org.junit.Test;

/**
 * 
 * @author Bernd Prager
 * Unit test class for Camel integration of Consul Registry plug-in
 *
 */
public class CamelRegistryTest extends CamelTestSupport implements Serializable {

	private static final long serialVersionUID = 2503932166836068033L;
	private static final Logger logger = Logger.getLogger(CamelRegistryTest.class);
	private static Properties prop;
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
		// read the Consul host address from property file
		prop = new Properties();
		String propFileName = "/config.properties";
		InputStream inputStream = null;;

		try {
			inputStream = ConsulRegistryTest.class.getResourceAsStream(propFileName);
			if (inputStream != null) {
				prop.load(inputStream);
			} else {
				throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
			}
		} catch (Exception e) {
			logger.error("Exception: " + e);
		} finally {
			inputStream.close();
		}
		// connect to Consul
		consulRegistry = new ConsulRegistry.Builder(prop.getProperty("consulHost")).build();
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
