package com.deep;

import com.deep.model.impl.ModelDocument;
import com.deep.util.ResourceUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.charset.StandardCharsets;

@Slf4j
@SpringBootApplication(scanBasePackages = {"com.deep"})
public class XmlParserApp implements CommandLineRunner {
    private static String xml;
    private static ModelDocument doc;

    public static void main(String[] args) {
        SpringApplication.run(XmlParserApp.class, args);
    }


    @Override
    public void run(String... args) throws Exception {
        String xmlDataFile = "/resources/sampleData.xml";
        xml = ResourceUtil.loadResourceAsString(xmlDataFile);
        doc = new ModelDocument(xml.getBytes(StandardCharsets.UTF_8));

        String id = xml.getElementValue("/XMLDOC/documentHdr/Id");
        String owner = xml.getElementValue("/XMLDOC/documentHdr/Owner");
        int nodeCount = xml.getNodeCount("/XXX/Zone/Element");

    }
}
