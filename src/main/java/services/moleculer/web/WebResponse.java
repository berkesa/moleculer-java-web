/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2019 Andras Berkes [andras.berkes@programmer.net]<br>
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
package services.moleculer.web;

import java.io.IOException;

public interface WebResponse {

	/**
	 * Sets the status code for this response. This method is used to set the
	 * return status code when there is no error (for example, for the 200 or
	 * 404 status codes). This method preserves any cookies and other response
	 * headers. Valid status codes are those in the 2XX, 3XX, 4XX, and 5XX
	 * ranges. Other status codes are treated as container specific.
	 * 
	 * @param code
	 *            the status code
	 */
	public void setStatus(int code);

	/**
	 * Gets the current status code of this response.
	 * 
	 * @return the status code
	 */
	public int getStatus();

	/**
	 * Sets a response header with the given name and value. If the header had
	 * already been set, the new value overwrites the previous one.
	 * 
	 * @param name
	 *            the name of the header
	 * @param value
	 *            the header value If it contains octet string, it should be
	 *            encoded according to RFC 2047
	 */
	public void setHeader(String name, String value);

	/**
	 * Returns the value of the specified response header as a String. If the
	 * response did not include a header of the specified name, this method
	 * returns null. If there are multiple headers with the same name, this
	 * method returns the first head in the response.
	 * 
	 * @param name
	 *            name a String specifying the header name
	 * 
	 * @return a String containing the value of the response header, or null if
	 *         the response does not have a header of that name
	 */
	public String getHeader(String name);

	/**
	 * Writes b.length bytes of body from the specified byte array to the output
	 * stream.
	 * 
	 * @param bytes
	 *            the data
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public void send(byte[] bytes) throws IOException;

	/**
	 * Completes the asynchronous operation that was started on the request.
	 * 
	 * @return first call of this method returns with true
	 */
	public boolean end();

	// --- CUSTOM PROPERTIES ---

	/**
	 * Associates the specified value with the specified "name" in this
	 * WebResponse. If the WebResponse previously contained a mapping for the
	 * "name", the old value is replaced.
	 * 
	 * @param name
	 *            a "name" with which the specified value is to be associated
	 * @param value
	 *            value to be associated with the specified "name"
	 */
	public void setProperty(String name, Object value);

	/**
	 * Returns the value to which the specified "name" is mapped, or null if
	 * this WebResponse contains no mapping for the "name".
	 * 
	 * @param name
	 *            the "name" whose associated value is to be returned
	 * 
	 * @return the value to which the specified "name" is mapped, or null if
	 *         this WebResponse contains no mapping for the "name"
	 */
	public Object getProperty(String name);

	// --- ACCESS TO INTERNAL OBJECT ---
	
	/**
	 * Returns the internal object of this WebResponse.
	 * 
	 * @return internal object (HttpServletResponse or Netty Context)
	 */
	public Object getInternalObject();
	
}