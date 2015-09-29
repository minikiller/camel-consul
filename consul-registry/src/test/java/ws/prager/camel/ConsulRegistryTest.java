package ws.prager.camel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.Serializable;
import java.util.Set;

import org.apache.camel.NoSuchBeanException;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class ConsulRegistryTest implements Serializable {
	
	

	private static final long serialVersionUID = -3482971969351609265L;
	static ConsulRegistry registry;

	@BeforeClass
	public static void setup() {
		registry = new ConsulRegistry.Builder("192.168.99.100").build();
	}

	/**
	 * Rigourous Test :-)
	 */
	@Test
	public void app() {
		assert(true);
	}

	@Test
	public void storeString() {
		registry.store("stringTestKey", "stringValue");
		String result = registry.lookup("stringTestKey").toString();
		assertEquals("stringValue", result);
	}
	
	@Test
	public void deleteString() {
		registry.delete("stringTestKey");
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
		registry.store("objectTestClass", testObject);
		TestClass clone = (TestClass) registry.lookup("objectTestClass");
		assertEquals(clone.toString(), "hello");
		
		registry.delete("objectTestClass");
	}

	@Test
	public void testFindByType() {
		
		class ConsulTestClass implements Serializable {
			private static final long serialVersionUID = 8859556554364125104L;
		}
		
		ConsulTestClass class1 = new ConsulTestClass();
		registry.store("class1", class1);
		ConsulTestClass class2 = new ConsulTestClass();
		registry.store("class2", class2);
		
		Set<? extends ConsulTestClass> results = registry.findByType(class1.getClass());
		assertNotNull(results);
		assertEquals(2, results.size());
		
		registry.delete("class1");
		registry.delete("class2");

	}
	
	@Test (expected = NoSuchBeanException.class)
	public void nonExisting() {
		registry.delete("nonExisting");
	}
}
