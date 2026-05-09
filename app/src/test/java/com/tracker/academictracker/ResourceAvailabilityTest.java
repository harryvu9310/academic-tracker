package com.tracker.academictracker;

import org.junit.jupiter.api.Test;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ResourceAvailabilityTest {
    private static final List<String> REQUIRED_RESOURCES = List.of(
            "/styles/common.css",
            "/styles/apple-light.css",
            "/styles/apple-dark.css",
            "/com/tracker/academictracker/Dashboard.fxml",
            "/com/tracker/academictracker/CourseRoster.fxml",
            "/com/tracker/academictracker/CourseDetails.fxml",
            "/com/tracker/academictracker/Semesters.fxml",
            "/com/tracker/academictracker/Settings.fxml",
            "/com/tracker/academictracker/Welcome.fxml"
    );

    @Test
    void requiredStylesheetsAndFxmlResourcesExistOnClasspath() {
        for (String resource : REQUIRED_RESOURCES) {
            URL url = ResourceAvailabilityTest.class.getResource(resource);
            assertNotNull(url, "Missing classpath resource: " + resource);
        }
    }

    @Test
    void fxmlResourcesAreWellFormedXml() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        REQUIRED_RESOURCES.stream()
                .filter(resource -> resource.endsWith(".fxml"))
                .forEach(resource -> assertDoesNotThrow(() -> {
                    try (InputStream inputStream = ResourceAvailabilityTest.class.getResourceAsStream(resource)) {
                        assertNotNull(inputStream, "Missing classpath resource: " + resource);
                        factory.newDocumentBuilder().parse(inputStream);
                    }
                }, "FXML should be well-formed XML: " + resource));
    }
}
