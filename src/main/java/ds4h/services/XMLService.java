package ds4h.services;

import ds4h.image.model.Project;
import ds4h.utils.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class XMLService {
    private XMLService() {}

    /**
     * I'm sorry, but I don't have time now to create a Builder, something like that would be a better solution
     * @param rootElementName
     * @param childName
     * @param project
     * @param fileOutputPath
     */
    public static void create(String rootElementName, String childName, Project project, String fileOutputPath) {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
        try {
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElement(rootElementName);
        doc.appendChild(rootElement);
        project.getImagesIndexesWithRois()
            .entrySet()
            .stream()
            .collect(
                Collectors.groupingBy(
                    Map.Entry::getValue,
                    Collectors.mapping(Map.Entry::getKey, Collectors.toList())
                )
            )
            .forEach((index, listOfPoints) -> {
                Element childElement = doc.createElement(childName);
                rootElement.appendChild(childElement);
                childElement.setAttribute("id", index.toString());
                childElement.setAttribute("filePath", project.getFilePaths().get(index));
                for (int roiIndex = 0; roiIndex < listOfPoints.size(); roiIndex++) {
                    Pair<BigDecimal, BigDecimal> points = listOfPoints.get(roiIndex);
                    Element roiIndexElement = doc.createElement("roiIndex");
                    roiIndexElement.setAttribute("id", String.valueOf(roiIndex));
                    Element x = doc.createElement("x");
                    Element y = doc.createElement("y");
                    x.setTextContent(points.getX().toString());
                    y.setTextContent(points.getY().toString());
                    roiIndexElement.appendChild(x);
                    roiIndexElement.appendChild(y);
                    childElement.appendChild(roiIndexElement);
                }
        });
        try (FileOutputStream output = new FileOutputStream(fileOutputPath)) {
            writeXml(doc, output);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * write doc to output stream
     * @param doc
     * @param output
     * @throws TransformerException
     */
    private static void writeXml(Document doc, OutputStream output) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(output);
        transformer.transform(source, result);
    }


    public static Project loadProject(String filePath) {
        Project project = new Project();
        List<String> filePaths = new ArrayList<>();
        Map<Pair<BigDecimal, BigDecimal>, Integer> map = new HashMap<>();
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        try {
            docFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            // parse XML file
            DocumentBuilder db = docFactory.newDocumentBuilder();
            Document doc = db.parse(new File(filePath));
            doc.getDocumentElement().normalize();
            NodeList images = doc.getElementsByTagName("image");
            for (int imageNodeIndex = 0; imageNodeIndex < images.getLength(); imageNodeIndex++) {
                Node imageNode = images.item(imageNodeIndex);
                if (imageNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element imageElement = (Element) imageNode;
                    String imageIndex = imageElement.getAttribute("id");
                    String imageFilePath = imageElement.getAttribute("filePath");
                    filePaths.add(imageFilePath);
                    NodeList roiIndexes = imageElement.getElementsByTagName("roiIndex");
                    for (int roiNodeIndex = 0; roiNodeIndex < roiIndexes.getLength(); roiNodeIndex++) {
                        Node roiNode = roiIndexes.item(roiNodeIndex);
                        if (roiNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element roiElement = (Element) roiNode;
                            BigDecimal x = BigDecimal.valueOf(Double.parseDouble(roiElement.getElementsByTagName("x").item(0).getTextContent()));
                            BigDecimal y = BigDecimal.valueOf(Double.parseDouble(roiElement.getElementsByTagName("y").item(0).getTextContent()));
                            map.put(new Pair<>(x, y), Integer.valueOf(imageIndex));
                        }
                    }
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        project.setFilePaths(filePaths);
        project.setImagesIndexesWithRois(map);
        return project;
    }
}
