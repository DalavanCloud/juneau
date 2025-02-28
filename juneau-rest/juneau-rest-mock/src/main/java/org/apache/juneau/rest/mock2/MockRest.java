// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.rest.mock2;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.utils.*;

/**
 * Creates a mocked interface against a REST resource class.
 *
 * <p>
 * Allows you to test your REST resource classes without a running servlet container.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bcode w800'>
 *  <jk>public class</jk> MockTest {
 *
 *  	<jc>// Our REST resource to test.</jc>
 *  	<ja>@RestResource</ja>(serializers=JsonSerializer.Simple.<jk>class</jk>, parsers=JsonParser.<jk>class</jk>)
 *  	<jk>public static class</jk> MyRest {
 *
 *  		<ja>@RestMethod</ja>(name=<jsf>PUT</jsf>, path=<js>"/String"</js>)
 *  		<jk>public</jk> String echo(<ja>@Body</ja> String b) {
 *  			<jk>return</jk> b;
 *  		}
 *  	}
 *
 *  <ja>@Test</ja>
 *  <jk>public void</jk> testEcho() <jk>throws</jk> Exception {
 *  	MockRest
 *  		.<jsf>create</jsf>(MyRest.<jk>class</jk>)
 *  		.put(<js>"/String"</js>, <js>"'foo'"</js>)
 *  		.execute()
 *  		.assertStatus(200)
 *  		.assertBody(<js>"'foo'"</js>);
 *  }
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'>{@doc juneau-rest-mock.MockRest}
 * </ul>
 */
public class MockRest implements MockHttpConnection {
	private static Map<Class<?>,RestContext> CONTEXTS_DEBUG = new ConcurrentHashMap<>(), CONTEXTS_NORMAL = new ConcurrentHashMap<>();

	private final RestContext ctx;

	/** Requests headers to add to every request. */
	protected final Map<String,Object> headers;

	/** Debug mode enabled. */
	protected final boolean debug;

	/**
	 * Constructor.
	 *
	 * @param b Builder.
	 */
	protected MockRest(Builder b) {
		try {
			debug = b.debug;
			Class<?> c = b.impl instanceof Class ? (Class<?>)b.impl : b.impl.getClass();
			Map<Class<?>,RestContext> contexts = debug ? CONTEXTS_DEBUG : CONTEXTS_NORMAL;
			if (! contexts.containsKey(c)) {
				Object o = b.impl instanceof Class ? ((Class<?>)b.impl).newInstance() : b.impl;
				RestContext rc = RestContext.create(o).logger(b.debug ? BasicRestLogger.class : NoOpRestLogger.class).build();
				if (o instanceof RestServlet) {
					((RestServlet)o).setContext(rc);
				} else {
					rc.postInit();
				}
				rc.postInitChildFirst();
				contexts.put(c, rc);
			}
			ctx = contexts.get(c);
			headers = new LinkedHashMap<>(b.headers);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a new builder with the specified REST implementation bean or bean class.
	 *
	 * <p>
	 * No <code>Accept</code> or <code>Content-Type</code> header is specified by default.
	 *
	 * @param impl
	 * 	The REST bean or bean class annotated with {@link RestResource @RestResource}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @return A new builder.
	 */
	public static Builder create(Object impl) {
		return new Builder(impl);
	}

	/**
	 * Convenience method for creating a MockRest over the specified REST implementation bean or bean class.
	 *
	 * <p>
	 * <code>Accept</code> header is set to <code>"application/json+simple"</code> by default.
	 * <code>Content-Type</code> header is set to <code>"application/json"</code> by default.
	 *
	 * <p>
	 * Equivalent to calling:
	 * <p class='bpcode w800'>
	 * 	MockRest.create(impl, SimpleJson.<jsf>DEFAULT</jsf>).build();
	 * </p>
	 *
	 * @param impl
	 * 	The REST bean or bean class annotated with {@link RestResource @RestResource}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @return A new {@link MockRest} object.
	 */
	public static MockRest build(Object impl) {
		return build(impl, SimpleJson.DEFAULT);
	}

	/**
	 * Convenience method for creating a MockRest over the specified REST implementation bean or bean class.
	 *
	 * <p>
	 * <code>Accept</code> and <code>Content-Type</code> headers are set to the primary media types on the specified marshall.
	 *
	 * <p>
	 * Note that the marshall itself is not involved in any serialization or parsing.
	 *
	 * <p>
	 * Equivalent to calling:
	 * <p class='bpcode w800'>
	 * 	MockRest.create(impl, SimpleJson.<jsf>DEFAULT</jsf>).marshall(m).build();
	 * </p>
	 *
	 * @param impl
	 * 	The REST bean or bean class annotated with {@link RestResource @RestResource}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @param m
	 * 	The marshall to use for specifying the <code>Accept</code> and <code>Content-Type</code> headers.
	 * 	<br>If <jk>null</jk>, headers will be reset.
	 * @return A new {@link MockRest} object.
	 */
	public static MockRest build(Object impl, Marshall m) {
		return create(impl).marshall(m).build();
	}

	/**
	 * Convenience method for creating a MockRest over the specified REST implementation bean or bean class.
	 *
	 * <p>
	 * <code>Accept</code> and <code>Content-Type</code> headers are set to the primary media types on the specified serializer and parser.
	 *
	 * <p>
	 * Note that the marshall itself is not involved in any serialization or parsing.
	 *
	 * <p>
	 * Equivalent to calling:
	 * <p class='bpcode w800'>
	 * 	MockRest.create(impl, SimpleJson.<jsf>DEFAULT</jsf>).serializer(s).parser(p).build();
	 * </p>
	 *
	 * @param impl
	 * 	The REST bean or bean class annotated with {@link RestResource @RestResource}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @param s
	 * 	The serializer to use for specifying the <code>Content-Type</code> header.
	 * 	<br>If <jk>null</jk>, header will be reset.
	 * @param p
	 * 	The parser to use for specifying the <code>Accept</code> header.
	 * 	<br>If <jk>null</jk>, header will be reset.
	 * @return A new {@link MockRest} object.
	 */
	public static MockRest build(Object impl, Serializer s, Parser p) {
		return create(impl).serializer(s).parser(p).build();
	}

	/**
	 * Builder class.
	 */
	public static class Builder {
		Object impl;
		boolean debug;
		Map<String,Object> headers = new LinkedHashMap<>();

		Builder(Object impl) {
			this.impl = impl;
		}

		/**
		 * Enable debug mode.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder debug() {
			this.debug = true;
			return this;
		}

		/**
		 * Adds a header to every request.
		 *
		 * @param name The header name.
		 * @param value
		 * 	The header value.
		 * 	<br>Can be <jk>null</jk> (will be skipped).
		 * @return This object (for method chaining).
		 */
		public Builder header(String name, Object value) {
			this.headers.put(name, value);
			return this;
		}

		/**
		 * Adds the specified headers to every request.
		 *
		 * @param value
		 * 	The header values.
		 * 	<br>Can be <jk>null</jk> (existing values will be cleared).
		 * 	<br><jk>null</jk> null map values will be ignored.
		 * @return This object (for method chaining).
		 */
		public Builder headers(Map<String,Object> value) {
			if (value != null)
				this.headers.putAll(value);
			else
				this.headers.clear();
			return this;
		}

		/**
		 * Specifies the <code>Accept</code> header to every request.
		 *
		 * @param value The <code>Accept/code> header value.
		 * @return This object (for method chaining).
		 */
		public Builder accept(String value) {
			return header("Accept", value);
		}

		/**
		 * Specifies the  <code>Content-Type</code> header to every request.
		 *
		 * @param value The <code>Content-Type</code> header value.
		 * @return This object (for method chaining).
		 */
		public Builder contentType(String value) {
			return header("Content-Type", value);
		}

		/**
		 * Convenience method for setting the <code>Accept</code> and <code>Content-Type</code> headers to <js>"application/json"</js>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder json() {
			return accept("application/json").contentType("application/json");
		}

		/**
		 * Convenience method for setting the <code>Accept</code> and <code>Content-Type</code> headers to <js>"application/json+simple"</js>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder simpleJson() {
			return accept("application/json+simple").contentType("application/json+simple");
		}

		/**
		 * Convenience method for setting the <code>Accept</code> and <code>Content-Type</code> headers to <js>"text/xml"</js>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder xml() {
			return accept("text/xml").contentType("text/xml");
		}

		/**
		 * Convenience method for setting the <code>Accept</code> and <code>Content-Type</code> headers to <js>"text/html"</js>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder html() {
			return accept("text/html").contentType("text/html");
		}

		/**
		 * Convenience method for setting the <code>Accept</code> and <code>Content-Type</code> headers to <js>"text/plain"</js>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder plainText() {
			return accept("text/plain").contentType("text/plain");
		}

		/**
		 * Convenience method for setting the <code>Accept</code> and <code>Content-Type</code> headers to <js>"octal/msgpack"</js>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder msgpack() {
			return accept("octal/msgpack").contentType("octal/msgpack");
		}

		/**
		 * Convenience method for setting the <code>Accept</code> and <code>Content-Type</code> headers to <js>"text/uon"</js>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder uon() {
			return accept("text/uon").contentType("text/uon");
		}

		/**
		 * Convenience method for setting the <code>Accept</code> and <code>Content-Type</code> headers to <js>"application/x-www-form-urlencoded"</js>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder urlEnc() {
			return accept("application/x-www-form-urlencoded").contentType("application/x-www-form-urlencoded");
		}

		/**
		 * Convenience method for setting the <code>Accept</code> and <code>Content-Type</code> headers to <js>"text/yaml"</js>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder yaml() {
			return accept("text/yaml").contentType("text/yaml");
		}

		/**
		 * Convenience method for setting the <code>Accept</code> and <code>Content-Type</code> headers to <js>"text/openapi"</js>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder openapi() {
			return accept("text/openapi").contentType("text/openapi");
		}

		/**
		 * Convenience method for setting the <code>Content-Type</code> header to the primary media type on the specified serializer.
		 *
		 * @param value
		 * 	The serializer to get the media type from.
		 * 	<br>If <jk>null</jk>, header will be reset.
		 * @return This object (for method chaining).
		 */
		public Builder serializer(Serializer value) {
			return contentType(value == null ? null : value.getPrimaryMediaType().toString());
		}

		/**
		 * Convenience method for setting the <code>Accept</code> header to the primary media type on the specified parser.
		 *
		 * @param value
		 * 	The parser to get the media type from.
		 * 	<br>If <jk>null</jk>, header will be reset.
		 * @return This object (for method chaining).
		 */
		public Builder parser(Parser value) {
			return accept(value == null ? null : value.getPrimaryMediaType().toString());
		}

		/**
		 * Convenience method for setting the <code>Accept</code> and <code>Content-Type</code> headers to the primary media types on the specified marshall.
		 *
		 * @param value
		 * 	The marshall to get the media types from.
		 * 	<br>If <jk>null</jk>, headers will be reset.
		 * @return This object (for method chaining).
		 */
		public Builder marshall(Marshall value) {
			contentType(value == null ? null : value.getSerializer().getPrimaryMediaType().toString());
			accept(value == null ? null : value.getParser().getPrimaryMediaType().toString());
			return this;
		}

		/**
		 * Create a new {@link MockRest} object based on the settings on this builder.
		 *
		 * @return A new {@link MockRest} object.
		 */
		public MockRest build() {
			return new MockRest(this);
		}
	}

	/**
	 * Performs a REST request against the REST interface.
	 *
	 * @param method The HTTP method
	 * @param path The URI path.
	 * @param headers Optional headers to include in the request.
	 * @param body
	 * 	The body of the request.
	 * 	<br>Can be any of the following data types:
	 * 	<ul>
	 * 		<li><code><jk>byte</jk>[]</code>
	 * 		<li>{@link Reader}
	 * 		<li>{@link InputStream}
	 * 		<li>{@link CharSequence}
	 * 	</ul>
	 * 	Any other types are converted to a string using the <code>toString()</code> method.
	 * @return A new servlet request.
	 * @throws Exception
	 */
	@Override /* MockHttpConnection */
	public MockServletRequest request(String method, String path, Map<String,Object> headers, Object body) throws Exception {
		String p = RestUtils.trimContextPath(ctx.getPath(), path);
		return MockServletRequest.create(method, p).body(body).headers(this.headers).headers(headers).debug(debug).restContext(ctx);
	}

	/**
	 * Performs a REST request against the REST interface.
	 *
	 * @param method The HTTP method
	 * @param path The URI path.
	 * @return A new servlet request.
	 * @throws Exception
	 */
	public MockServletRequest request(String method, String path) throws Exception {
		return request(method, path, null, null);
	}

	/**
	 * Performs a REST request against the REST interface.
	 *
	 * @param method The HTTP method
	 * @param path The URI path.
	 * @param body
	 * 	The body of the request.
	 * 	<br>Can be any of the following data types:
	 * 	<ul>
	 * 		<li><code><jk>byte</jk>[]</code>
	 * 		<li>{@link Reader}
	 * 		<li>{@link InputStream}
	 * 		<li>{@link CharSequence}
	 * 	</ul>
	 * 	Any other types are converted to a string using the <code>toString()</code> method.
	 * @return A new servlet request.
	 * @throws Exception
	 */
	public MockServletRequest request(String method, String path, Object body) throws Exception {
		return request(method, path, null, body);
	}

	/**
	 * Performs a REST request against the REST interface.
	 *
	 * @param method The HTTP method
	 * @param headers Optional headers to include in the request.
	 * @param path The URI path.
	 * @return A new servlet request.
	 * @throws Exception
	 */
	public MockServletRequest request(String method, Map<String,Object> headers, String path) throws Exception {
		return request(method, path, headers, null);
	}

	/**
	 * Perform a GET request.
	 *
	 * @param path The URI path.
	 * @return A new servlet request.
	 * @throws Exception
	 */
	public MockServletRequest get(String path) throws Exception {
		return request("GET", path, null, null);
	}

	/**
	 * Perform a PUT request.
	 *
	 * @param path The URI path.
	 * @param body
	 * 	The body of the request.
	 * 	<br>Can be any of the following data types:
	 * 	<ul>
	 * 		<li><code><jk>byte</jk>[]</code>
	 * 		<li>{@link Reader}
	 * 		<li>{@link InputStream}
	 * 		<li>{@link CharSequence}
	 * 	</ul>
	 * 	Any other types are converted to a string using the <code>toString()</code> method.
	 * @return A new servlet request.
	 * @throws Exception
	 */
	public MockServletRequest put(String path, Object body) throws Exception {
		return request("PUT", path, null, body);
	}

	/**
	 * Perform a POST request.
	 *
	 * @param path The URI path.
	 * @param body
	 * 	The body of the request.
	 * 	<br>Can be any of the following data types:
	 * 	<ul>
	 * 		<li><code><jk>byte</jk>[]</code>
	 * 		<li>{@link Reader}
	 * 		<li>{@link InputStream}
	 * 		<li>{@link CharSequence}
	 * 	</ul>
	 * 	Any other types are converted to a string using the <code>toString()</code> method.
	 * @return A new servlet request.
	 * @throws Exception
	 */
	public MockServletRequest post(String path, Object body) throws Exception {
		return request("POST", path, null, body);
	}

	/**
	 * Perform a DELETE request.
	 *
	 * @param path The URI path.
	 * @return A new servlet request.
	 * @throws Exception
	 */
	public MockServletRequest delete(String path) throws Exception {
		return request("DELETE", path, null, null);
	}

	/**
	 * Perform an OPTIONS request.
	 *
	 * @param path The URI path.
	 * @return A new servlet request.
	 * @throws Exception
	 */
	public MockServletRequest options(String path) throws Exception {
		return request("OPTIONS", path, null, null);
	}

	/**
	 * Perform a PATCH request.
	 *
	 * @param path The URI path.
	 * @param body
	 * 	The body of the request.
	 * 	<br>Can be any of the following data types:
	 * 	<ul>
	 * 		<li><code><jk>byte</jk>[]</code>
	 * 		<li>{@link Reader}
	 * 		<li>{@link InputStream}
	 * 		<li>{@link CharSequence}
	 * 	</ul>
	 * 	Any other types are converted to a string using the <code>toString()</code> method.
	 * @return A new servlet request.
	 * @throws Exception
	 */
	public MockServletRequest patch(String path, Object body) throws Exception {
		return request("PATCH", path, null, body);
	}

	/**
	 * Returns the headers that were defined in this class.
	 *
	 * @return
	 * 	The headers that were defined in this class.
	 * 	<br>Never <jk>null</jk>.
	 */
	public Map<String,Object> getHeaders() {
		return headers;
	}
}
