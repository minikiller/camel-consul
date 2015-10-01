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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.camel.NoSuchBeanException;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 * @author Bernd Prager 
 * Unit test for Camel Registry implementation for Consul
 */
public class ConsulRegistryTest implements Serializable {

	private static final long serialVersionUID = -3482971969351609265L;
	private static ConsulRegistry registry;
	private static Properties prop;

	private static final Logger logger = Logger.getLogger(ConsulRegistryTest.class);

	public class ConsulTestClass implements Serializable {
		private static final long serialVersionUID = -4815945688487114891L;

		public String hello(String name) {
			return "Hello " + name;
		}
	}

	@BeforeClass
	public static void setUp() throws IOException {
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
		registry = new ConsulRegistry.Builder(prop.getProperty("consulHost")).build();
	}

	/**
	 * Rigorous Test :-)
	 */
	@Test
	public void basics() {
		logger.info("testing with consul host: " + prop.getProperty("consulHost"));
		assertNotNull(prop.getProperty("consulHost"));
	}

	@Test
	public void storeString() {
		registry.put("stringTestKey", "stringValue");
		String result = (String) registry.lookup("stringTestKey");
		registry.remove("stringTestKey");
		assertNotNull(result);
		assertEquals("stringValue", result);
	}

	@Test
	public void overrideExistingKey() {
		registry.put("uniqueKey", "stringValueOne");
		registry.put("uniqueKey", "stringValueTwo");
		String result = (String) registry.lookup("uniqueKey");
		registry.remove("uniqueKey");
		assertNotNull(result);
		assertEquals("stringValueTwo", result);
	}

	@Test
	public void checkLookupByName() {
		registry.put("namedKey", "namedValue");
		String result = (String) registry.lookupByName("namedKey");
		registry.remove("namedKey");
		assertNotNull(result);
		assertEquals("namedValue", result);
	}

	@Test
	public void checkFailedLookupByName() {
		registry.put("namedKey", "namedValue");
		registry.remove("namedKey");
		String result = (String) registry.lookupByName("namedKey");
		assertNull(result);
	}

	@Test
	public void checkLookupByNameAndType() {
		ConsulTestClass consulTestClass = new ConsulTestClass();
		registry.put("testClass", consulTestClass);
		ConsulTestClass consulTestClassClone = registry.lookupByNameAndType("testClass", consulTestClass.getClass());
		registry.remove("testClass");
		assertNotNull(consulTestClassClone);
		assertEquals(consulTestClass.getClass(), consulTestClassClone.getClass());
	}

	@Test
	public void checkFailedLookupByNameAndType() {
		ConsulTestClass consulTestClass = new ConsulTestClass();
		registry.put("testClass", consulTestClass);
		registry.remove("testClass");
		ConsulTestClass consulTestClassClone = registry.lookupByNameAndType("testClass", consulTestClass.getClass());
		assertNull(consulTestClassClone);
	}

	@Test
	public void checkFindByTypeWithName() {
		ConsulTestClass consulTestClassOne = new ConsulTestClass();
		ConsulTestClass consulTestClassTwo = new ConsulTestClass();
		registry.put("testClassOne", consulTestClassOne);
		registry.put("testClassTwo", consulTestClassTwo);
		Map<String, ? extends ConsulTestClass> consulTestClassMap = registry
				.findByTypeWithName(consulTestClassOne.getClass());
		registry.remove("testClassOne");
		registry.remove("testClassTwo");
		HashMap<String, ConsulTestClass> hm = new HashMap<String, ConsulTestClass>();
		assertNotNull(consulTestClassMap);
		assertEquals(consulTestClassMap.getClass(), hm.getClass());
		assertEquals(2, consulTestClassMap.size());
	}

	public void checkFailedFindByTypeWithName() {

	}

	@Test
	public void storeObject() {
		ConsulTestClass testObject = new ConsulTestClass();
		registry.put("objectTestClass", testObject);
		ConsulTestClass clone = (ConsulTestClass) registry.lookup("objectTestClass");
		assertEquals(clone.hello("World"), "Hello World");
		registry.remove("objectTestClass");
	}

	@Test
	public void findByType() {
		ConsulTestClass classOne = new ConsulTestClass();
		registry.put("classOne", classOne);
		ConsulTestClass classTwo = new ConsulTestClass();
		registry.put("classTwo", classTwo);
		Set<? extends ConsulTestClass> results = registry.findByType(classOne.getClass());
		assertNotNull(results);
		HashSet<ConsulTestClass> hashSet = new HashSet<ConsulTestClass>();
		registry.remove("classOne");
		registry.remove("classTwo");
		assertEquals(results.getClass(), hashSet.getClass());
		assertEquals(2, results.size());
	}

	public void notFindByType() {

	}

	@Test(expected = NoSuchBeanException.class)
	public void deleteNonExisting() {
		registry.remove("nonExisting");
	}
}
