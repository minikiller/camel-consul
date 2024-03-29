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
import org.apache.log4j.Logger;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetBinaryValue;
import com.ecwid.consul.v1.session.model.NewSession;

/**
 * 
 * @author Bernd Prager 
 * Apache Camel Plug-in for Consul Registry 
 * (Objects stored under kv/key 
 * as well as bookmarked under kv/[type]/key 
 * to avoid iteration over types)
 * 
 */
public class ConsulRegistry implements Registry {

	private static final Logger logger = Logger.getLogger(ConsulRegistry.class);
	private final String hostname;
	private final int port;
	private ConsulClient client;

	private ConsulRegistry(Builder builder) {
		this.hostname = builder.hostname;
		this.port = builder.port;
		logger.debug("get consul client for: " + hostname + ":" + port);
		this.client = new ConsulClient(hostname + ":" + port);
	}

	@Override
	public Object lookupByName(String name) {
		logger.debug("lookup by name: " + name);
		GetBinaryValue result = this.client.getKVBinaryValue(name).getValue();
		if (result == null) {
			return null;
		}
		InputStream inputStream = new ByteArrayInputStream(result.getValue());
		return SerializationUtils.deserialize(inputStream);
	}

	@Override
	public <T> T lookupByNameAndType(String name, Class<T> type) {
		logger.debug("lookup by name: " + name + " and type: " + type);
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
		logger.debug("find by type with name: " + type);
		Object object = null;
		Map<String, T> result = new HashMap<String, T>();
		// encode $ signs as they occur in subclass types
		String keyPrefix = type.getName().replace("$", "%24");
		Response<List<String>> response = client.getKVKeysOnly(keyPrefix);
		if (response != null && response.getValue() != null) {
			for (String key : response.getValue()) {
				// change bookmark back into actual key
				key = key.substring((key.lastIndexOf('/') + 1 ));
				object = lookupByName(key.replace("$", "%24"));
				if (type.isInstance(object)) {
					result.put(key, type.cast(object));
				}
			}
		}
		return result;
	}

	@Override
	public <T> Set<T> findByType(Class<T> type) {
		logger.debug("find by type: " + type);
		Object object = null;
		Set<T> result = new HashSet<T>();
		String keyPrefix = type.getName().replace("$", "%24");
		Response<List<String>> response = client.getKVKeysOnly(keyPrefix);
		if (response != null && response.getValue() != null) {
			for (String key : response.getValue()) {
				// change bookmark back into actual key
				key = key.substring((key.lastIndexOf('/') + 1 ));
				object = lookupByName(key.replace("$", "%24"));
				if (type.isInstance(object)) {
					result.add(type.cast(object));
				}
			}
		}
		return result;
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

	public void remove(String key) {
		// create session to avoid conflicts (not sure if that is safe enough)
		NewSession newSession = new NewSession();
		String session = client.sessionCreate(newSession, null).getValue();
		Object object = lookupByName(key);
		if (object == null) {
			String msg = "Bean with key '" + key + "' did not exist in Consul Registry.";
			throw new NoSuchBeanException(msg);
		}
		client.deleteKVValue(key);
		client.deleteKVValue(object.getClass().getName() + "/" + key);
		client.sessionDestroy(session, null);
	}

	public void put(String key, Object object) {
		// create session to avoid conflicts 
		// (not sure if that is safe enough, again)
		NewSession newSession = new NewSession();
		String session = client.sessionCreate(newSession, null).getValue();
		// Allow only unique keys, last one wins
		if (lookupByName(key) != null) {
			remove(key);
		}
		Object clone = SerializationUtils.clone((Serializable) object);
		byte[] value = SerializationUtils.serialize((Serializable) clone);
		// store the actual class
		client.setKVBinaryValue(key, value);
		// store just as a bookmark
		client.setKVValue(object.getClass().getName() + "/" + key, "1");
		client.sessionDestroy(session, null);
	}

	public static class Builder {
		// required parameter
		String hostname;
		// optional parameter
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

	@Deprecated
	@Override
	public Object lookup(String name) {
		return lookupByName(name);
	}

	@Deprecated
	@Override
	public <T> T lookup(String name, Class<T> type) {
		return lookupByNameAndType(name, type);
	}

	@Deprecated
	@Override
	public <T> Map<String, T> lookupByType(Class<T> type) {
		return findByTypeWithName(type);
	}

}
