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
package org.apache.juneau.rest.mock;

import static org.apache.juneau.rest.testutils.TestUtils.*;
import static org.junit.Assert.*;

import org.apache.juneau.rest.mock2.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests the {@link MockServletRequest} class.
 */
@SuppressWarnings({"javadoc"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MockServletRequestTest {

	//-----------------------------------------------------------------------------------------------------------------
	// URIs
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_uris_basic() {
		MockServletRequest req = MockServletRequest.create("GET", "/foo");

		assertEquals("", req.getContextPath());
		assertEquals("/foo", req.getPathInfo());
		assertEquals("/mock-path/foo", req.getPathTranslated());
		assertEquals(null, req.getQueryString());
		assertEquals("/foo", req.getRequestURI());
		assertEquals("/foo", req.getRequestURL().toString());
		assertEquals("", req.getServletPath());
	}

	@Test
	public void a02_uris_full() {
		MockServletRequest req = MockServletRequest.create("GET", "http://localhost:8080/foo?bar=baz#quz");

		assertEquals("", req.getContextPath());
		assertEquals("/foo", req.getPathInfo());
		assertEquals("/mock-path/foo", req.getPathTranslated());
		assertEquals("bar=baz", req.getQueryString());
		assertEquals("/foo", req.getRequestURI());
		assertEquals("http://localhost:8080/foo", req.getRequestURL().toString());
		assertEquals("", req.getServletPath());
	}

	@Test
	public void a03_uris_full2() {
		MockServletRequest req = MockServletRequest.create("GET", "http://localhost:8080/foo/bar/baz?bar=baz#quz");

		assertEquals("", req.getContextPath());
		assertEquals("/foo/bar/baz", req.getPathInfo());
		assertEquals("/mock-path/foo/bar/baz", req.getPathTranslated());
		assertEquals("bar=baz", req.getQueryString());
		assertEquals("/foo/bar/baz", req.getRequestURI());
		assertEquals("http://localhost:8080/foo/bar/baz", req.getRequestURL().toString());
		assertEquals("", req.getServletPath());
	}

	@Test
	public void a04_uris_contextPath() {
		MockServletRequest req = MockServletRequest.create("GET", "http://localhost:8080/foo/bar/baz?bar=baz#quz").contextPath("/foo");

		assertEquals("/foo", req.getContextPath());
		assertEquals("/bar/baz", req.getPathInfo());
		assertEquals("/mock-path/bar/baz", req.getPathTranslated());
		assertEquals("bar=baz", req.getQueryString());
		assertEquals("/foo/bar/baz", req.getRequestURI());
		assertEquals("http://localhost:8080/foo/bar/baz", req.getRequestURL().toString());
		assertEquals("", req.getServletPath());
	}

	@Test
	public void a05_uris_servletPath() {
		MockServletRequest req = MockServletRequest.create("GET", "http://localhost:8080/foo/bar/baz?bar=baz#quz").servletPath("/foo");

		assertEquals("", req.getContextPath());
		assertEquals("/bar/baz", req.getPathInfo());
		assertEquals("/mock-path/bar/baz", req.getPathTranslated());
		assertEquals("bar=baz", req.getQueryString());
		assertEquals("/foo/bar/baz", req.getRequestURI());
		assertEquals("http://localhost:8080/foo/bar/baz", req.getRequestURL().toString());
		assertEquals("/foo", req.getServletPath());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Query strings
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void b01_query_basic() {
		MockServletRequest req = MockServletRequest.create("GET", "/foo?bar=baz&bing=qux");

		assertEquals("bar=baz&bing=qux", req.getQueryString());
		assertObjectEquals("{bar:['baz'],bing:['qux']}", req.getParameterMap());
		assertObjectEquals("'baz'", req.getParameter("bar"));
		assertObjectEquals("['bar','bing']", req.getParameterNames());
		assertObjectEquals("['baz']", req.getParameterValues("bar"));
	}

	@Test
	public void b02_query_multivalues() {
		MockServletRequest req = MockServletRequest.create("GET", "/foo?bar=baz&bar=bing");

		assertEquals("bar=baz&bar=bing", req.getQueryString());
		assertObjectEquals("{bar:['baz','bing']}", req.getParameterMap());
		assertObjectEquals("'baz'", req.getParameter("bar"));
		assertObjectEquals("['bar']", req.getParameterNames());
		assertObjectEquals("['baz','bing']", req.getParameterValues("bar"));
	}
}
