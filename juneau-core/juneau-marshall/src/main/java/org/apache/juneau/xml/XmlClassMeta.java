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

import static org.apache.juneau.internal.StringUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Metadata on classes specific to the XML serializers and parsers pulled from the {@link Xml @Xml} annotation on the
 * class.
 */
public class XmlClassMeta extends ClassMetaExtended {

	private final Namespace namespace;
	private final Xml xml;
	private final XmlFormat format;
	private final String childName;

	/**
	 * Constructor.
	 *
	 * @param cm The class that this annotation is defined on.
	 */
	public XmlClassMeta(ClassMeta<?> cm) {
		super(cm);
		this.namespace = findNamespace(cm);
		this.xml = cm.getClassInfo().getAnnotation(Xml.class);
		if (xml != null) {
			this.format = xml.format();
			this.childName = nullIfEmpty(xml.childName());

		} else {
			this.format = XmlFormat.DEFAULT;
			this.childName = null;
		}
	}

	/**
	 * Returns the {@link Xml @Xml} annotation defined on the class.
	 *
	 * @return
	 * 	The value of the annotation defined on the class, or <jk>null</jk> if annotation is not specified.
	 */
	protected Xml getAnnotation() {
		return xml;
	}

	/**
	 * Returns the {@link Xml#format() @Xml(format)} annotation defined on the class.
	 *
	 * @return The value of the annotation, or {@link XmlFormat#DEFAULT} if not specified.
	 */
	protected XmlFormat getFormat() {
		return format;
	}

	/**
	 * Returns the {@link Xml#childName() @Xml(childName)} annotation defined on the class.
	 *
	 * @return The value of the annotation, or <jk>null</jk> if not specified.
	 */
	protected String getChildName() {
		return childName;
	}

	/**
	 * Returns the XML namespace associated with this class.
	 *
	 * <p>
	 * Namespace is determined in the following order of {@link Xml#prefix() @Xml(prefix)} annotation:
	 * <ol>
	 * 	<li>Class.
	 * 	<li>Package.
	 * 	<li>Superclasses.
	 * 	<li>Superclass packages.
	 * 	<li>Interfaces.
	 * 	<li>Interface packages.
	 * </ol>
	 *
	 * @return The namespace associated with this class, or <jk>null</jk> if no namespace is associated with it.
	 */
	public Namespace getNamespace() {
		return namespace;
	}

	private static Namespace findNamespace(ClassMeta<?> cm) {
		if (cm == null)
			return null;
		ClassInfo ci = cm.getClassInfo();
		List<Xml> xmls = ci.getAnnotations(Xml.class);
		List<XmlSchema> schemas = ci.getAnnotations(XmlSchema.class);
		return XmlUtils.findNamespace(xmls, schemas);
	}
}
