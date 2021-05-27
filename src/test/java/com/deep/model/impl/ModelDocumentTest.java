package com.deep.model.impl;

import com.deep.util.ResourceUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelDocumentTest {

    private static String xml;
    private static ModelDocument doc;
    private static Map<String, String> namespaces = new HashMap<>();
    static {
        namespaces.put("xxx", "com.deep");
        namespaces.put("yyy", "/document/core/");
        namespaces.put("doc", "document");
    }

    @Before
    public void prepare() throws Exception {
        xml = ResourceUtil.loadResourceAsString("/dbTicket_PmryObj.xml");
        doc = new ModelDocument(xml.getBytes(), null, namespaces);
    }

    @Test
    public void getElementValueList() {
        List<String> list = doc.getElementValueList("/XXX/UsersCodes/Element");
        assertEquals(3, list.size());
        assertEquals("1", list.get(0));
        assertEquals("2", list.get(1));
        assertEquals("3", list.get(2));

        list = doc.getElementValueList("zone/Zone/Element");
        assertEquals(1, list.size());
        assertEquals("42", list.get(0));

    }

    @Test
    public void countNodes() {
        assertEquals(3, doc.getNodeCount("zone/Zone/Element"));
        assertEquals(4, doc.getNodeCount("/XXX/UsersCodes/Element"));
    }

    @Test
    public void testAttributes() {
        String xpath = "/XXX/UsersCodes/Element";
        String attribute = "Id";
        assertEquals("", doc.getAttributeFromXPath(xpath+"[1]", "@"+attribute));
        assertEquals("1123E01", doc.getAttributeFromXPath(xpath+"[2]", "@"+attribute));

        assertEquals("3", doc.getAttribute("@number"));
    }
}
