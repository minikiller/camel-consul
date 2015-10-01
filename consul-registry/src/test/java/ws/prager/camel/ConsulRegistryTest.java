package ws.prager.camel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.camel.NoSuchBeanException;
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

	public class ConsulTestClass implements Serializable {
		private static final long serialVersionUID = -4815945688487114891L;

		public String hello(String name) {
			return "Hello " + name;
		}
	}

	@BeforeClass
	public static void setUp() {
		registry = new ConsulRegistry.Builder("192.168.99.100").build();
	}

	/**
	 * Rigorous Test :-)
	 */
	@Test
	public void basics() {
		assert(true);
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
