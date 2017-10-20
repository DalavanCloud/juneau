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
package org.apache.juneau.xml;

import javax.xml.stream.*;
import javax.xml.stream.util.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;

/**
 * Parses text generated by the {@link XmlSerializer} class back into a POJO model.
 *
 * <h5 class='section'>Media types:</h5>
 *
 * Handles <code>Content-Type</code> types: <code>text/xml</code>
 *
 * <h5 class='section'>Description:</h5>
 *
 * See the {@link XmlSerializer} class for a description of Juneau-generated XML.
 */
public class XmlParser extends ReaderParser {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	private static final String PREFIX = "XmlParser.";

	/**
	 * <b>Configuration property:</b>  Enable validation.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"XmlParser.validating"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * If <jk>true</jk>, XML document will be validated.
	 * See {@link XMLInputFactory#IS_VALIDATING} for more info.
	 */
	public static final String XML_validating = PREFIX + "validating";

	/**
	 * <b>Configuration property:</b>  XML reporter.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"XmlParser.reporter"</js>
	 * 	<li><b>Data type:</b> {@link XMLReporter}
	 * 	<li><b>Default:</b> <jk>null</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * Associates an {@link XMLReporter} with this parser.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>Reporters are not copied to new parsers during a clone.
	 * </ul>
	 */
	public static final String XML_reporter = PREFIX + "reporter";

	/**
	 * <b>Configuration property:</b>  XML resolver.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"XmlParser.resolver"</js>
	 * 	<li><b>Data type:</b> {@link XMLResolver}
	 * 	<li><b>Default:</b> <jk>null</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * Associates an {@link XMLResolver} with this parser.
	 */
	public static final String XML_resolver = PREFIX + "resolver";

	/**
	 * <b>Configuration property:</b>  XML event allocator.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"XmlParser.eventAllocator"</js>
	 * 	<li><b>Data type:</b> {@link XMLEventAllocator}
	 * 	<li><b>Default:</b> <jk>null</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * Associates an {@link XMLEventAllocator} with this parser.
	 */
	public static final String XML_eventAllocator = PREFIX + "eventAllocator";

	/**
	 * <b>Configuration property:</b>  Preserve root element during generalized parsing.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"XmlParser.preserveRootElement"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * If <jk>true</jk>, when parsing into a generic {@link ObjectMap}, the map will contain a single entry whose key
	 * is the root element name.
	 *
	 * <p>
	 * Example:
	 * <table class='styled'>
	 * 	<tr>
	 * 		<td>XML</td>
	 * 		<td>ObjectMap.toString(), preserveRootElement==false</td>
	 * 		<td>ObjectMap.toString(), preserveRootElement==true</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td><code><xt>&lt;root&gt;&lt;a&gt;</xt>foobar<xt>&lt;/a&gt;&lt;/root&gt;</xt></code></td>
	 * 		<td><code>{ a:<js>'foobar'</js> }</code></td>
	 * 		<td><code>{ root: { a:<js>'foobar'</js> }}</code></td>
	 * 	</tr>
	 * </table>
	 */
	public static final String XML_preserveRootElement = PREFIX + "preserveRootElement";


	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Default parser, all default settings.*/
	public static final XmlParser DEFAULT = new XmlParser(PropertyStore.create());


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final XmlParserContext ctx;

	/**
	 * Constructor.
	 *
	 * @param propertyStore
	 * 	The property store containing all the settings for this object.
	 */
	public XmlParser(PropertyStore propertyStore) {
		this(propertyStore, "text/xml", "application/xml");
	}

	/**
	 * Constructor.
	 *
	 * @param propertyStore
	 * 	The property store containing all the settings for this object.
	 * @param consumes
	 * 	The list of media types that this parser consumes (e.g. <js>"application/json"</js>, <js>"*&#8203;/json"</js>).
	 */
	public XmlParser(PropertyStore propertyStore, String...consumes) {
		super(propertyStore, consumes);
		this.ctx = createContext(XmlParserContext.class);
	}

	@Override /* CoreObject */
	public XmlParserBuilder builder() {
		return new XmlParserBuilder(propertyStore);
	}

	@Override /* Parser */
	public ReaderParserSession createSession(ParserSessionArgs args) {
		return new XmlParserSession(ctx, args);
	}
}
