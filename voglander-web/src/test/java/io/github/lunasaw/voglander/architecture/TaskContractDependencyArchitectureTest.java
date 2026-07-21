package io.github.lunasaw.voglander.architecture;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskContractDependencyArchitectureTest {

    private static final List<String> FORBIDDEN_REFERENCES = List.of(
        "io.github.lunasaw.voglander.repository.",
        "io.github.lunasaw.voglander.manager.",
        "io.github.lunasaw.voglander.intergration.",
        "io.github.lunasaw.voglander.service.",
        "io.github.lunasaw.voglander.web.");

    @Test
    void clientModuleDependsOnlyOnCommon() throws Exception {
        Path root = repositoryRoot();
        Element project = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(root.resolve("voglander-client/pom.xml").toFile()).getDocumentElement();
        NodeList dependencies = project.getElementsByTagName("dependency");
        List<String> coordinates = new ArrayList<>();
        for (int index = 0; index < dependencies.getLength(); index++) {
            Element dependency = (Element) dependencies.item(index);
            coordinates.add(text(dependency, "groupId") + ":" + text(dependency, "artifactId"));
        }

        assertEquals(List.of("io.github.lunasaw:voglander-common"), coordinates);
    }

    @Test
    void taskContractsContainNoUpwardOrDomainHandlerReferences() throws Exception {
        Path taskRoot = repositoryRoot().resolve("voglander-client/src/main/java/io/github/lunasaw/voglander/client");
        try (var sources = Files.walk(taskRoot)) {
            for (Path source : sources.filter(path -> path.toString().endsWith(".java")).toList()) {
                if (!source.toString().contains("/task/")) {
                    continue;
                }
                String content = Files.readString(source);
                for (String forbidden : FORBIDDEN_REFERENCES) {
                    assertFalse(content.contains(forbidden),
                        () -> repositoryRoot().relativize(source) + " contains forbidden dependency " + forbidden);
                }
            }
        }
    }

    @Test
    void architectureDocumentsBusinessTaskPlacement() throws Exception {
        String architecture = Files.readString(repositoryRoot()
            .resolve("doc/architecture/current/ARCHITECTURE-OVERVIEW.md"));
        assertTrue(architecture.contains("## 业务长任务内核分层"));
        assertTrue(architecture.contains("LongTaskHandler"));
        assertTrue(architecture.contains("技术调度器"));
    }

    private String text(Element parent, String tagName) {
        return parent.getElementsByTagName(tagName).item(0).getTextContent().trim();
    }

    private Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("voglander-client/pom.xml"))) {
            current = current.getParent();
        }
        if (current == null) {
            throw new IllegalStateException("Cannot locate voglander repository root");
        }
        return current;
    }
}
