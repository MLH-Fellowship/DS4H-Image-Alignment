package ds4h.services;

import ds4h.image.model.Project;
import ds4h.image.model.ProjectRoi;
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
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static javax.xml.parsers.DocumentBuilderFactory.newInstance;


public class XMLService {
    private static final String FILEPATH = "filePath";

    private XMLService() {
    }

    /**
     * I'm sorry, but I don't have time now to create a Builder, something like that would be a better solution
     *
     * @param rootElementName
     * @param childName
     * @param project
     * @param fileOutputPath
     */
    public static void create(String rootElementName, String childName, Project project, String fileOutputPath) {
        DocumentBuilderFactory docFactory = newInstance();
        DocumentBuilder docBuilder = null;
        try {
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        if (docBuilder == null) {
            return;
        }
        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElement(rootElementName);
        doc.appendChild(rootElement);
        final List<Element> childElements = new ArrayList<>();
        for (String filePath : project.getFilePaths()) {
            Element childElement = doc.createElement(childName);
            rootElement.appendChild(childElement);
            childElement.setAttribute(FILEPATH, filePath);
            childElement.setAttribute("id", String.valueOf(project.getFilePaths().indexOf(filePath)));
            childElements.add(childElement);
        }
        final List<Pair<String, List<ProjectRoi>>> projectRoisList = project.getProjectRois().stream().collect(Collectors.groupingBy(ProjectRoi::getPathFile)).entrySet().stream().map(entry -> new Pair<>(entry.getKey(), entry.getValue())).collect(Collectors.toList());
        for (Pair<String, List<ProjectRoi>> pair : projectRoisList) {
            List<ProjectRoi> projectRoiList = pair.getSecond();
            for (ProjectRoi projectRoi : projectRoiList) {
                Element roiIndexElement = doc.createElement("roiIndex");
                roiIndexElement.setAttribute("id", String.valueOf(projectRoi.getRoiIndex()));
                Element x = doc.createElement("x");
                Element y = doc.createElement("y");
                x.setTextContent(projectRoi.getPoint().getFirst().toString());
                y.setTextContent(projectRoi.getPoint().getSecond().toString());
                roiIndexElement.appendChild(x);
                roiIndexElement.appendChild(y);
                childElements.stream().filter(childElement -> childElement.getAttribute(FILEPATH).equals(pair.getFirst())).findFirst().ifPresent(childElement -> childElement.appendChild(roiIndexElement));
            }
        }
        try (FileOutputStream output = new FileOutputStream(fileOutputPath)) {
            writeXml(doc, output);
        } catch (IOException | TransformerException e) {
            e.printStackTrace();
        }
    }


    /**
     * @param doc    write to
     * @param output stream
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
        List<Pair<Integer, String>> filePaths = new ArrayList<>();
        List<ProjectRoi> projectRois = new ArrayList<>();
        DocumentBuilderFactory docFactory = newInstance();
        // to be compliant, completely disable DOCTYPE declaration:
        try {
            docFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            // or completely disable external entities declarations:
            docFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            docFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            // or disable entity expansion but keep in mind that this doesn't prevent fetching external entities
            // and this solution is not correct for OpenJDK < 13 due to a bug: https://bugs.openjdk.java.net/browse/JDK-8206132
            docFactory.setExpandEntityReferences(false);
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
                    String imageFilePath = imageElement.getAttribute(FILEPATH);
                    int imageIndex = Integer.parseInt(imageElement.getAttribute("id"));
                    filePaths.add(new Pair<>(imageIndex, imageFilePath));
                    NodeList roiIndexes = imageElement.getElementsByTagName("roiIndex");
                    for (int roiNodeIndex = 0; roiNodeIndex < roiIndexes.getLength(); roiNodeIndex++) {
                        Node roiNode = roiIndexes.item(roiNodeIndex);
                        if (roiNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element roiElement = (Element) roiNode;
                            BigDecimal x = BigDecimal.valueOf(Double.parseDouble(roiElement.getElementsByTagName("x").item(0).getTextContent()));
                            BigDecimal y = BigDecimal.valueOf(Double.parseDouble(roiElement.getElementsByTagName("y").item(0).getTextContent()));
                            ProjectRoi projectRoi = new ProjectRoi();
                            projectRoi.setPoint(new Pair<>(x, y));
                            projectRoi.setFilePath(imageFilePath);
                            projectRoi.setRoiIndex(Integer.parseInt(((Element) roiNode).getAttribute("id")));
                            projectRois.add(projectRoi);
                        }
                    }
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        project.setFilePaths(filePaths.stream().sorted(Comparator.comparingInt(Pair::getFirst)).map(Pair::getSecond).collect(Collectors.toList()));
        project.setProjectRois(projectRois);
        return project;
    }
}
