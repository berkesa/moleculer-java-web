/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2017 Andras Berkes [andras.berkes@programmer.net]<br>
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
package services.moleculer.web.router;

public class Alias {

	// --- HTTP METHODS ---

	public static final String ALL = "ALL";
	public static final String REST = "REST";

	public static final String CONNECT = "CONNECT";
	public static final String DELETE = "DELETE";
	public static final String GET = "GET";
	public static final String HEAD = "HEAD";
	public static final String OPTIONS = "OPTIONS";
	public static final String POST = "POST";
	public static final String PUT = "PUT";
	public static final String TRACE = "TRACE";

	// --- PROPERTIES ---

	protected final String httpMethod;
	protected final String pathPattern;
	protected final String actionName;
	protected final int hashCode;

	// --- CONSTRUCTOR ---

	public Alias(String httpMethod, String pathPattern, String actionName) {
		this.httpMethod = httpMethod.trim().toUpperCase();
		String pattern = pathPattern.trim();
		if (!pattern.startsWith("/")) {
			pattern = '/' + pattern;
		}
		this.pathPattern = pattern;
		String name = actionName.trim().replace('/', '.');
		while (name.startsWith(".")) {
			name = name.substring(1);
		}
		while (name.endsWith(".")) {
			name = name.substring(0, name.length() - 1);
		}
		this.actionName = name;

		// Generate hashcode
		final int prime = 31;
		int result = 1;
		result = prime * result + name.hashCode();
		result = prime * result + httpMethod.hashCode();
		result = prime * result + pathPattern.hashCode();
		hashCode = result;
	}

	// --- PROPERTY GETTERS ---
	
	public String getHttpMethod() {
		return httpMethod;
	}

	public String getPathPattern() {
		return pathPattern;
	}

	public String getActionName() {
		return actionName;
	}
	
	// --- COLLECTION HELPERS ---

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (Alias.class != obj.getClass()) {
			return false;
		}
		Alias other = (Alias) obj;
		return hashCode == other.hashCode && actionName.equals(other.actionName) && httpMethod.equals(other.httpMethod)
				&& pathPattern.equals(other.pathPattern);
	}

}