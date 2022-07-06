package ds4h.services;

import ds4h.image.model.Project;
import ds4h.image.model.ProjectImage;
import ds4h.image.model.ProjectImageRoi;
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
import java.util.*;

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
        for (ProjectImage projectImage : project.getProjectImages()) {
            Element childElement = doc.createElement(childName);
            rootElement.appendChild(childElement);
            childElement.setAttribute(FILEPATH, projectImage.getFilePath());
            childElement.setAttribute("id", String.valueOf(projectImage.getId()));
            childElement.setAttribute("imagesCounter", String.valueOf(projectImage.getImagesCounter()));
            childElements.add(childElement);
        }
        project.getProjectImages().stream().map(projectImage -> new Pair<>(projectImage.getId(), projectImage.getProjectImageRois())).sorted(Comparator.comparingInt(Pair::getFirst)).forEachOrdered(pair -> {
            for (ProjectImageRoi projectImageRoi : pair.getSecond()) {
                final Element roiIndexElement = doc.createElement("roiIndex");
                roiIndexElement.setAttribute("id", String.valueOf(projectImageRoi.getId()));
                roiIndexElement.setAttribute("imageIndex", String.valueOf(projectImageRoi.getImageIndex()));
                final Element x = doc.createElement("x");
                final Element y = doc.createElement("y");
                x.setTextContent(projectImageRoi.getPoint().getFirst().toString());
                y.setTextContent(projectImageRoi.getPoint().getSecond().toString());
                roiIndexElement.appendChild(x);
                roiIndexElement.appendChild(y);
                childElements.stream().filter(childElement -> childElement.getAttribute("id").equals(String.valueOf(pair.getFirst()))).findFirst().ifPresent(childElement -> childElement.appendChild(roiIndexElement));
            }
        });
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


    public static Project loadProject(String filePath, Set<String> files) {
        Project project = new Project();
        final List<ProjectImage> projectImages = new ArrayList<>();
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
                final Node imageNode = images.item(imageNodeIndex);
                if (imageNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element imageElement = (Element) imageNode;
                    final int id = Integer.parseInt(imageElement.getAttribute("id"));
                    final int imagesCounter = Integer.parseInt(imageElement.getAttribute("imagesCounter"));
                    final String imageFilePath = imageElement.getAttribute(FILEPATH);
                    final Optional<String> fileName = files.stream().filter(file -> getFilename(file).equals(getFilename(imageFilePath))).findFirst();
                    if (!fileName.isPresent()) {
                        continue;
                    }
                    final ProjectImage projectImage = new ProjectImage(imagesCounter, fileName.get());
                    final List<ProjectImageRoi> projectImageRois = new ArrayList<>();
                    final NodeList roiIndexes = imageElement.getElementsByTagName("roiIndex");
                    projectImage.setId(id);
                    for (int roiNodeIndex = 0; roiNodeIndex < roiIndexes.getLength(); roiNodeIndex++) {
                        Node roiNode = roiIndexes.item(roiNodeIndex);
                        if (roiNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element roiElement = (Element) roiNode;
                            BigDecimal x = BigDecimal.valueOf(Double.parseDouble(roiElement.getElementsByTagName("x").item(0).getTextContent()));
                            BigDecimal y = BigDecimal.valueOf(Double.parseDouble(roiElement.getElementsByTagName("y").item(0).getTextContent()));
                            final ProjectImageRoi projectImageRoi = new ProjectImageRoi();
                            projectImageRoi.setPoint(new Pair<>(x, y));
                            projectImageRoi.setImageIndex(Integer.parseInt(((Element) roiNode).getAttribute("imageIndex")));
                            projectImageRoi.setId(Integer.parseInt(((Element) roiNode).getAttribute("id")));
                            projectImageRois.add(projectImageRoi);
                        }
                    }
                    projectImage.setProjectImageRois(projectImageRois);
                    projectImages.add(projectImage);
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        project.setProjectImages(projectImages);
        return project;
    }

    private static String getFilename(String file) {
        return file.substring(file.lastIndexOf(File.separator) + 1);
    }
}
