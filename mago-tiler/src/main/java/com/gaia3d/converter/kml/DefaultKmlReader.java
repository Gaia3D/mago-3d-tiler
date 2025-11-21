package com.gaia3d.converter.kml;

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

/**
 * KmlReader is a class that reads kml files.
 * It reads kml files and returns the information of the kml file.
 */
@Slf4j
public class DefaultKmlReader implements AttributeReader {

    //read kml file
    @Override
    public TileTransformInfo read(File file) {
        TileTransformInfo tileTransformInfo = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(file);
            Element root = document.getDocumentElement();
            List<Element> elements = getAllElements(root);
            Vector3d position = new Vector3d(Double.parseDouble(findContent(elements, "longitude")), Double.parseDouble(findContent(elements, "latitude")), Double.parseDouble(findContent(elements, "altitude")));

            tileTransformInfo = TileTransformInfo.builder()
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
            document = null;
            builder = null;
            factory = null;
        } catch (IOException e) {
            log.error("[ERROR] :", e);
            throw new RuntimeException(e);
        } catch (SAXException | ParserConfigurationException e) {
            log.error("[ERROR] SAXException: {}", e.getMessage());
        }
        return tileTransformInfo;
    }

    @Override
    public List<TileTransformInfo> readAll(File file) {
        return null;
    }

    private List<Element> getAllElements(Element parent) {
        List<Element> elements = new ArrayList<>();
        getElements(elements, parent);
        return elements;
    }

    private void getElements(List<Element> elements, Element parent) {
        NodeList children = parent.getChildNodes();
        int length = children.getLength();
        for (int i = 0; i < length; i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element child = (Element) node;
                getElements(elements, child);
                elements.add(child);
            }
        }
    }

    private String findContent(List<Element> elements, String name) {
        Element result = elements.stream().filter((element) -> element.getNodeName().equals(name)).findFirst().orElseThrow();
        return result.getTextContent();
    }
}
