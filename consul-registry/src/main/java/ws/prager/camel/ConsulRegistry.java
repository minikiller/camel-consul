package ws.prager.camel;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.camel.NoSuchBeanException;
import org.apache.camel.spi.Registry;
import org.apache.commons.lang.SerializationUtils;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetBinaryValue;
import com.ecwid.consul.v1.session.model.NewSession;

/**
 * Consul Registry Plug-in
 * (Objects stored twice; under kv/key and under kv/[type]/key
 *   to avoid iteration over types
 */
public class ConsulRegistry implements Registry {

	private final String hostname;
	private final int port;
	private ConsulClient client;

	private ConsulRegistry(Builder builder) {
		this.hostname = builder.hostname;
		this.port = builder.port;
		this.client = new ConsulClient(hostname + ":" + port);
		// this.client = new ConsulClient(hostname);
		}

	@Override
	public Object lookupByName(String name) {
		GetBinaryValue result = this.client.getKVBinaryValue(name).getValue();
		if (result == null) {
			return null;
		}
		InputStream inputStream = new ByteArrayInputStream(result.getValue());
		return SerializationUtils.deserialize(inputStream);
	}

	@Override
	public <T> T lookupByNameAndType(String name, Class<T> type) {
		Object object = lookupByName(name);
		if (object == null)
			return null;
		try {
			return type.cast(object);
		} catch (Throwable e) {
			String msg = "Found bean: " + name + " in Consul Registry: " + this + " of type: "
					+ object.getClass().getName() + "expected type was: " + type;
			throw new NoSuchBeanException(name, msg, e);
		}
	}
	
	@Override
	public <T> Map<String, T> findByTypeWithName(Class<T> type) {
		Object object = null;
		Map<String, T> result = new HashMap<String, T>();
		String keyPrefix = type.getName().replace("$", "%24");
		Response<List<String>> response = client.getKVKeysOnly(keyPrefix);
		if (response == null) return null;
		for (String key: response.getValue()) {
			object = lookup(key);
			if (type.isInstance(object)) {
				result.put(key, type.cast(object));
			}
		}
		return result;
	}
	
	@Override
	public <T> Set<T> findByType(Class<T> type) {
		Object object = null;
		Set<T> result = new HashSet<T>();
		String keyPrefix = type.getName().replace("$", "%24");
		Response<List<String>> response = client.getKVKeysOnly(keyPrefix);
		if (response.getValue() == null) return null;
		for (String key: response.getValue()) {
			object = lookup(key.replace("$", "%24"));
			if (type.isInstance(object)) {
				result.add(type.cast(object));
			}
		}
		return result;
	}

	@Override
	public Object lookup(String name) {
		return lookupByName(name);
	}

	@Override
	public <T> T lookup(String name, Class<T> type) {
		return lookupByNameAndType(name, type);
	}

	@Override
	public <T> Map<String, T> lookupByType(Class<T> type) {
		return findByTypeWithName(type);
	}

	public String getHostname() {
		return hostname;
	}

	public int getPort() {
		return port;
	}

	public ConsulClient getClient() {
		return client;
	}

	public void delete(String key) {
		// create session to avoid conflicts
		NewSession newSession = new NewSession();
		String session = client.sessionCreate(newSession, null).getValue();
		Object object = lookup(key);
		if (object == null) {
			String msg = "Bean with key '" + key + "' did not exist in Consul Registry.";
			throw new NoSuchBeanException(msg);
		}
		client.deleteKVValue(key);
		client.deleteKVValue(object.getClass().getName() + key);
		client.sessionDestroy(session, null);
	}

	public void store(String key, Object object) {
		NewSession newSession = new NewSession();
		String session = client.sessionCreate(newSession, null).getValue();
		// Allow only unique keys, last one wins
		if (lookup(key) != null) {
			delete(key);
		}
		Object clone = SerializationUtils.clone((Serializable) object);
		byte[] value = SerializationUtils.serialize((Serializable) clone);
		client.setKVBinaryValue(key, value);
		client.setKVBinaryValue(object.getClass().getName() + "/" + key, value);
		client.sessionDestroy(session, null);
	}

	public static class Builder {
		// required parameter
		String hostname;
		// optional parameter
		// Integer port = 53;
		Integer port = 8500;

		public Builder(String hostname) {
			this.hostname = hostname;
		}

		public Builder port(Integer port) {
			this.port = port;
			return this;
		}

		public ConsulRegistry build() {
			return new ConsulRegistry(this);
		}
	}

}
