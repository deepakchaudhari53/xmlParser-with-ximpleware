package com.deep.model.impl;

import com.deep.model.api.Document;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.ximpleware.AutoPilot;
import com.ximpleware.FastLongBuffer;
import com.ximpleware.NavException;
import com.ximpleware.ParseException;
import com.ximpleware.VTDGen;
import com.ximpleware.VTDNav;
import com.ximpleware.XPathEvalException;
import com.ximpleware.XPathParseException;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class ModelDocument implements Document {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ByteBuffer buffer;
    private final VTDNav vn;
    private final AutoPilot ap;
    private final Collection<String> fieldsWithRawValues;

    public ModelDocument(final byte[] xml) {
        this(xml, Collections.emptyList());
    }

    public ModelDocument(final byte[] xml, Map<String, String> namespaces) {
        this(xml, Collections.emptyList(), namespaces);
    }

    public ModelDocument(final byte[] xml, Collection<String> fieldsWithRawValues) {
        this(xml, fieldsWithRawValues, null);
    }

    public ModelDocument(final byte[] xml, Collection<String> fieldsWithRawValues, Map<String, String> namespaces) {
        this.fieldsWithRawValues = fieldsWithRawValues;

        try {
            buffer = ByteBuffer.wrap(xml);
            VTDGen vtd = new VTDGen();
            vtd.setDoc_BR(buffer.array());
            if (namespaces != null) vtd.parse(true);
            else vtd.parse(false);
            vn = vtd.getNav();
            ap = new AutoPilot(vn);
            if (namespaces != null) {
                for (Map.Entry<String, String> entry : namespaces.entrySet()) {
                    ap.declareXPathNameSpace(entry.getKey(), entry.getValue());
                }
            }
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * String representation of VtdDocument
     *
     * @return String representation of VtdDocument
     */
    @Override
    public String toString() {
        return new String(buffer.array());
    }


    @Override
    public long getTimestamp(final String xpath) {
        String ts = getElementValue(xpath);
        return Instant.parse(ts).toEpochMilli();
    }

    @Override
    public ByteBuffer bytes() {
        return buffer;
    }

    /**
     * Get string value of an XPath that resolves to a single element (unpredictable results if not)
     *
     * @param xpath XPath that resolves to a single element
     * @return String contents of value
     */
    @Override
    public String getElementValue(String xpath) {
        return resolveXPath(xpath, true);
    }

    @Override
    public String getElementBlock(String xpath) {
        return resolveXPath(xpath, false);
    }

    private String resolveXPath(String xpath, Boolean getSingleElementValue) {
        String ret = "";

        try {
            ap.resetXPath();
            ap.selectXPath(xpath);

            int result = ap.evalXPath();
            if (result != -1) {
                long lc = vn.getContentFragment();

                if (lc < 0) {
                    if (vn.getTokenType(result) == VTDNav.TOKEN_ATTR_NAME) {
                        ret = vn.toRawString(result + 1);
                    } else if (vn.getTokenType(result) == VTDNav.TOKEN_STARTING_TAG) {
                        //Might need to revisit here. For now, if it is an tag without value, we just return empty String.
                        //ret = '<' + vn.toRawString(result) + "/>";
                        return ret;
                    } else {
                        ret = vn.toRawString(result);
                    }
                } else {
                    long le = vn.getElementFragment();
                    String ret1 = vn.toRawString((int) le, (int) (le >> 32));
                    ret = getSingleElementValue ? extractValueFromElement(ret1) : ret1;
                }
            }
        } catch (XPathParseException | XPathEvalException | NavException e) {
            logger.error("Failed to get string from eval Xpath: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }

        return ret.trim();
    }

    /**
     * Get list of string values of an XPath that resolves to one or more elements
     *
     * @param xpath XPath that resolves to one or more elements
     * @return List of string values
     */
    @Override
    public List<String> getElementValueList(String xpath) {
        List<String> ret = new ArrayList<>();

        try {
            ap.resetXPath();
            ap.selectXPath(xpath);

            FastLongBuffer flb = new FastLongBuffer(4, 3); // TODO - Configurable page size/initial capacity

            while (ap.evalXPath() != -1) {
                if (vn.getText() > 0) {
                    flb.append(vn.getElementFragment());
                }
            }

            for (int i = 0; i < flb.size(); i++) {
                ret.add(extractValueFromElement(new String(buffer.array(), flb.lower32At(i), flb.upper32At(i))));
            }
        } catch (XPathParseException | XPathEvalException | NavException e) {
            logger.error("Failed to get string from eval Xpath: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }

        return ret;
    }

    /**
     * Extracts a value from an element, assuming that there is only one element in the string.
     * Designed to work for elements with single tags.
     * <p>
     * e.g. "<tag>some value here</tag>" => "some value here"
     *
     * @param s Element with single xml tag
     * @return Value of element with tags removed
     */
    private static String extractValueFromElement(String s) {
        return StringEscapeUtils.unescapeXml(s.substring((s.indexOf('>') + 1), s.indexOf("</")));
    }

    public Map<String, String> toMap() {
        Map<String, String> result = new LinkedHashMap<>();
        populate(result::put);
        return result;
    }

    public Multimap<String, String> toMultimap() {
        Multimap<String, String> result = LinkedHashMultimap.create();
        populate(result::put);
        return result;
    }

    private void populate(BiConsumer<String, String> consumer) {
        try {
            ap.selectElement("*"); // select every element (* matches all strings)
            while (ap.iterate()) // iterate will iterate thru all elements
            {
                String elementName = vn.toString(vn.getCurrentIndex());
                String value = null;
                int t = vn.getText();
                if (t != -1)
                    value = fieldsWithRawValues.contains(elementName) ? vn.toRawString(t) : vn.toNormalizedString(t);

                if (value != null) {
                    consumer.accept(elementName, value);
                }
            }
            vn.toElement(VTDNav.ROOT); // reset the cursor to point to the root element
        } catch (NavException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public int getNodeCount(String xpath) {
        try {
            ap.selectXPath("count(" + xpath + ")");
            return (int) ap.evalXPathToNumber();
        } catch (XPathParseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets an attribute given a name (NOT an XPath).
     * Allows access to attribute values in the root of the document.
     *
     * @param attributeName Attribute name, preceded with "@"
     * @return Value of attribute
     */
    @Override
    public String getAttribute(final String attributeName) {
        return getAttributeFromXPath("/", attributeName);
    }

    @Override
    public String getAttributeFromXPath(String xpath, String attributeName) {
        try {
            ap.resetXPath();
            ap.selectXPath(xpath + "/" + attributeName);
            return ap.evalXPathToString().trim();
        } catch (XPathParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Only to be used in the circumstance when:
     *
     * <a>
     * <b>Some</b>
     * <c>Text</c>
     * </a>
     * <p>
     * The XPath "/a" should result in output "SomeText"
     *
     * @param xpath XPath
     * @return Concatenated output of XPath
     */
    @Override
    public String getConcatenatedString(final String xpath) {
        try {
            ap.resetXPath();
            ap.selectXPath(xpath);
            return ap.evalXPathToString().trim();
        } catch (XPathParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Resolves an XPath that results in a Boolean value
     *
     * @param xpath XPath (that resolves to a boolean value)
     * @return true or false
     */
    @Override
    public Boolean getBooleanValue(final String xpath) {
        try {
            ap.resetXPath();
            ap.selectXPath(xpath);
            return ap.evalXPathToBoolean();
        } catch (XPathParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Checks whether the given node exists in the document (whether it contains a value or not)
     *
     * @param nodePath XPath to a node
     * @return Does node exist in document (whether it contains a value or not)
     */
    @Override
    public Boolean exists(final String nodePath) {
        return getBooleanValue("boolean(" + nodePath + ")");
    }


/*
    public List<VtdDocument> getSubDocuments(String xPath) {
        List<VtdDocument> results = null;
        try {
            ap.selectXPath(xPath);
            FastLongBuffer flb = new FastLongBuffer(4);
            byte[] bytes = vn.getXML().getBytes();

            while ( ap.evalXPath() != -1 ) {
                flb.append(vn.getElementFragment());
            }
            results = new ArrayList<>(flb.size());

            for(int i=0; i< flb.size(); i++) {
                results.add(new VtdDocument(Arrays.copyOfRange(bytes, flb.lower32At(i), flb.lower32At(i)+flb.upper32At(i))));
                System.out.println("Result[i]: " + results.get(i));
            }
            return results;
        } catch (XPathParseException | XPathEvalException | NavException e) {
            logger.error("Failed to spawn documents for Xpath: {}", xPath, e);
            throw new RuntimeException(e);
        }
    }
*/
}