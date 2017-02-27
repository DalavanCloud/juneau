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
package org.apache.juneau.svl.vars;

import static org.apache.juneau.internal.ThrowableUtils.*;

import java.util.regex.*;

import org.apache.juneau.svl.*;

/**
 * A basic switch/case logic variable resolver.
 * <p>
 * The format for this var is one of the following:
 * <ul>
 * 	<li><js>"$SWITCH{stringArg,pattern,thenValue}"</js>
 * 	<li><js>"$SWITCH{stringArg,pattern,thenValue,elseValue}"</js>
 * 	<li><js>"$SWITCH{stringArg,pattern,thenValue,pattern,thenValue}"</js>
 * 	<li><js>"$SWITCH{stringArg,pattern,thenValue,pattern,thenValue,elsePattern}"</js>
 * 	<li>...
 * </ul>
 * <p>
 * The pattern can be any string optionally containing <js>'*'</js> or <js>'?'</js> representing any or one character respectively.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Create a variable resolver that resolves system properties and $SWITCH vars.</jc>
 * 	VarResolver r = <jk>new</jk> VarResolver().addVars(SwitchVar.<jk>class</jk>, SystemPropertiesVar.<jk>class</jk>);
 *
 * 	<jc>// Use it!</jc>
 * 	System.<jsf>out</jsf>.println(r.resolve(<js>"We are running on $SWITCH{$P{os.name},*win*,Windows,Something else}!"</js>));
 * </p>
 * <p>
 * Since this is a {@link MultipartVar}, any variables contained in the result will be recursively resolved.
 * Likewise, if the arguments contain any variables, those will be resolved before they are passed to this var.
 */
public class SwitchVar extends MultipartVar {

	/** The name of this variable. */
	public static final String NAME = "SWITCH";

	/**
	 * Constructor.
	 */
	public SwitchVar() {
		super(NAME);
	}

	@Override /* MultipartVar */
	public String resolve(VarResolverSession session, String[] args) {
		if (args.length < 3)
			illegalArg("Invalid number of arguments passed to $SWITCH var.  Must have 3 or more arguments.");

		String stringArg = args[0];
		for (int i = 1; i < args.length;) {
			String pattern = args[i++];
			if (args.length == i)
				return pattern;  // We've reached an else.

			Pattern p = Pattern.compile(pattern.replace("*", ".*").replace("?", "."));
			if (p.matcher(stringArg).matches())
				return args[i];
			i++;
		}

		// Nothing matched and no else clause.
		return "";
	}
}
