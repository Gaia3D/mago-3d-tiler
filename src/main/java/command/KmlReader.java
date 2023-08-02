package command;

import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class KmlReader {

    //read kml file
    public static KmlInfo read(File file) {
        KmlInfo kmlInfo = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = factory.newDocumentBuilder();
            Document document = documentBuilder.parse(file);
            Element root = document.getDocumentElement();
            List<Element> elements = getAllElements(root);

            Vector3d position = new Vector3d(Double.parseDouble(findContent(elements, "longitude")), Double.parseDouble(findContent(elements, "latitude")), Double.parseDouble(findContent(elements, "altitude")));

            kmlInfo = KmlInfo.builder()
                    .name(findContent(elements, "name"))
                    .position(position)
                    .altitudeMode(findContent(elements, "altitudeMode"))
                    .heading(Double.parseDouble(findContent(elements, "heading")))
                    .tilt(Double.parseDouble(findContent(elements, "tilt")))
                    .roll(Double.parseDouble(findContent(elements, "roll")))
                    .href(findContent(elements, "href"))
                    .scaleX(Double.parseDouble(findContent(elements, "x")))
                    .scaleY(Double.parseDouble(findContent(elements, "y")))
                    .scaleZ(Double.parseDouble(findContent(elements, "z")))
                    .build();

            factory = null;
            documentBuilder = null;
            document = null;
            root = null;
            elements = null;
        } catch (ParserConfigurationException | IOException | SAXException e) {
            log.error(e.getMessage());
        }
        return kmlInfo;
    }

    private static List<Element> getAllElements(Element parent) {
        List<Element> elements = new ArrayList<>();
        getElements(elements, parent);
        return elements;
    }

    private static void getElements(List<Element> elements, Element parent) {
        NodeList children = parent.getChildNodes();
        int length = children.getLength();
        for (int i = 0; i < length; i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE){
                Element child = (Element)node;
                getElements(elements, child);
                elements.add(child);
            }
        }
    }

    private static String findContent(List<Element> elements, String name) {
        Element result = elements.stream().filter((element) -> {
            return element.getNodeName().equals(name);
        }).findFirst().orElseThrow();
        return result.getTextContent();
    }
}
