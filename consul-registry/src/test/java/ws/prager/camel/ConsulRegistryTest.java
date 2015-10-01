package ws.prager.camel;

import java.io.Serializable;
import java.util.Set;

import org.apache.camel.NoSuchBeanException;
import org.junit.Test;

import junit.framework.TestCase;

/**
 * Unit test for Camel Registry implementation for Consul
 */
public class ConsulRegistryTest extends TestCase implements Serializable {

	private static final long serialVersionUID = -3482971969351609265L;
	static ConsulRegistry registry;

	@Override
	public void setUp() {
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
		assertEquals("stringValue", result);
	}
	
	@Test(expected = NoSuchBeanException.class)
	public void removeString() {
		registry.put("stringTestKey", "stringValue");
		registry.remove("stringTestKey");
		@SuppressWarnings("unused")
		String result = (String) registry.lookup("stringTestKey");
	}

	@Test
	public void overrideExistingKey() {
		registry.put("uniqueKey", "stringValueOne");
		registry.put("uniqueKey", "stringValueTwo");
		String result = (String) registry.lookup("uniqueKey");
		registry.remove("uniqueKey");
		assertEquals("stringValueTwo", result);
	}

	@Test
	public void testLookupByName() {
		registry.put("namedKey", "namedValue");
		String result = (String) registry.lookupByName("namedKey");
		registry.remove("namedKey");
		assertEquals("namedValue", result);
	}

	@Test(expected = NoSuchBeanException.class)
	public void testFailedLookupByName() {
		registry.put("namedKey", "namedValue");
		registry.remove("namedKey");
		@SuppressWarnings("unused")
		String result = (String) registry.lookupByName("namedKey");
	}

	public void testLookupByNameAndType() {

	}

	public void testFailedLookupByNameAndType() {

	}

	public void testFindByTypeWithName() {

	}

	public void testFailedFindByTypeWithName() {

	}

	public void testFindByType() {

	}

	public void testFailedFindByType() {

	}

	@Test
	public void deleteString() {
		registry.remove("stringTestKey");
		assertNull(registry.lookup("stringTestKey"));
	}

	@Test
	public void storeObject() {

		class TestClass implements Serializable {
			private static final long serialVersionUID = 8859556554364125104L;

			@Override
			public String toString() {
				return "hello";
			}
		}

		TestClass testObject = new TestClass();
		registry.put("objectTestClass", testObject);
		TestClass clone = (TestClass) registry.lookup("objectTestClass");
		assertEquals(clone.toString(), "hello");

		registry.remove("objectTestClass");
	}

	@Test
	public void findByType() {

		class ConsulTestClass implements Serializable {
			private static final long serialVersionUID = 8859556554364125104L;
		}

		ConsulTestClass class1 = new ConsulTestClass();
		registry.put("class1", class1);
		ConsulTestClass class2 = new ConsulTestClass();
		registry.put("class2", class2);

		Set<? extends ConsulTestClass> results = registry.findByType(class1.getClass());
		assertNotNull(results);
		assertEquals(2, results.size());

		registry.remove("class1");
		registry.remove("class2");

	}

	public void notFindByType() {

	}

	@Test(expected = NoSuchBeanException.class)
	public void deleteNonExisting() {
		registry.remove("nonExisting");
	}
}
