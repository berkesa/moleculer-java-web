/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2018 Andras Berkes [andras.berkes@programmer.net]<br>
 * Based on Moleculer Framework for NodeJS [https://moleculer.services].
 * <br><br>
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:<br>
 * <br>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.<br>
 * <br>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package services.moleculer.web.template.thymeleaf;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.thymeleaf.context.IContext;

import io.datatree.Tree;

public class TreeContext implements IContext {

	// --- VARIABLES ---
	
	private final Tree data;
	
	private Set<String> cachedVariableNames;
	
	// --- CONSTRUCTOR ---
	
	public TreeContext(Tree data) {
		this.data = data;
	}
	
	// --- CONTEXT IMPLEMENTATION ---
	
	@Override
	public Locale getLocale() {
		Tree meta = data.getMeta(false);
		if (meta != null) {
			Tree locale = meta.get("$locale");
			if (locale != null) {
				return new Locale(locale.asString());
			}
		}
		return Locale.getDefault();
	}

	@Override
	public boolean containsVariable(String name) {
		return getVariableNames().contains(name);
	}

	@Override
	public Set<String> getVariableNames() {
		if (cachedVariableNames == null) {
			Set<String> set = new HashSet<>(); 
			collectVariables(set, data);
			cachedVariableNames = Collections.unmodifiableSet(set);
		}
		return cachedVariableNames;
	}

	@Override
	public Object getVariable(String name) {
		Tree child = data.get(name);
		if (child == null) {
			return null;
		}
		return child.asObject();
	}

	// --- UTILITIES ---
	
	protected void collectVariables(Set<String> set, Tree root) {
		if (root != null) {
			for (Tree child: root) {
				if (child.isStructure()) {
					collectVariables(set, child);
				} else {
					set.add(child.getPath());
				}
			}
		}
	}
	
}