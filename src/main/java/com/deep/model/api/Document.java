package com.deep.model.api;

import java.nio.ByteBuffer;
import java.util.List;

public interface Document {

    /**
     * Get string value of an XPath that resolves to a single element (unpredictable results if not)
     *
     * @param xpath XPath that resolves to a single element
     * @return String contents of value
     */
    String getElementValue(String xpath);

    /**
     * Get string value of an XPath that resolves to an XML block
     *
     * @param xpath XPath that resolves to an XML block
     * @return String contents of value
     */
    String getElementBlock(String xpath);

    /**
     * Get list of string values of an XPath that resolves to one or more elements
     *
     * @param xpath XPath that resolves to one or more elements
     * @return List of string values
     */
    List<String> getElementValueList(String xpath);

    long getTimestamp(String xpath);

    int getNodeCount(String xpath);

    /**
     * Gets an attribute given a name (NOT an XPath).
     * Allows access to attribute values in the root of the document.
     *
     * @param attributeName Attribute name, preceded with "@"
     * @return Value of attribute
     */
    String getAttribute(String attributeName);

    String getAttributeFromXPath(String xpath, String attributeName);

    /**
     * Only to be used in the circumstance when:
     *
     * <a>
     *     <b>Some</b>
     *     <c>Text</c>
     * </a>
     *
     * The XPath "/a" should result in output "SomeText"
     *
     * @param xpath XPath
     * @return Concatenated output of XPath
     */
    String getConcatenatedString(String xpath);

    Boolean getBooleanValue(String xpath);

    Boolean exists(String xpath);

    ByteBuffer bytes();
}