package de.adesso.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Objects;

/**
 * This service helps managing repositories with the help of JGit.
 */
@Service
public class MarkdownTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarkdownTransformer.class);

    private final ConfigService configService;

    @Autowired
    public MarkdownTransformer(ConfigService configService) {
        this.configService = configService;
    }

    /**
     * The jekyll-build process generates html and xml files to the corresponding markdown file.
     * The xml output has to be copied to an intended folder.
     */
    public void copyGeneratedXmlFiles() {
        try {
            File generatedXmlFolder = new File(configService.getLOCAL_HTML_POSTS());
            if (fileIsDirectory(generatedXmlFolder)) {
                // for all folders in LOCAL_HTML_POSTS
                for (File file : Objects.requireNonNull(generatedXmlFolder.listFiles())) {
                    // save the date from the file name of the blog post
                    String datePart = getLastPartOfPath(file.getPath());
                    if (canConvertToDate(datePart)) {
                        // if the folders date is not in the future
                        if (isNotInFuture(LocalDate.parse(datePart))) {
                            // get the xml files from this folder and copy them to the destination
                            Files.walk(file.toPath()).filter(sourcePath -> sourcePath.toFile().getName().endsWith(".xml"))
                                    .forEach(sourcePath -> copyFile(sourcePath, datePart));
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("An error occurred while assembling all files to copy.", e);
            LOGGER.error("Exiting jekyll2cms.");
            System.exit(30);
        }
    }

    private String getLastPartOfPath(String path) {
        return path.substring(path.lastIndexOf("/") + 1);
    }

    private void copyFile(Path sourcePath, String datePart) {
        Path destinationPath = new File(configService.getFIRSTSPIRIT_XML_PATH() + "/" + datePart + "/" + datePart + "-" + sourcePath.getFileName().toString()).toPath();
        try {
            LOGGER.debug("Copy file from " + sourcePath + " to " + destinationPath);
            if (pathDoesNotExist(destinationPath)) {
                Files.createDirectories(destinationPath);
            }
            Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.error("An error occurred while copying generated XML files to destination.", e);
            LOGGER.error("Exiting jekyll2cms.");
            System.exit(36);
        }
    }

    private boolean pathDoesNotExist(Path path) {
        return !path.getParent().toFile().exists();
    }

    private boolean fileIsDirectory(File file) {
        return file.isDirectory();
    }

    private boolean canConvertToDate(String string) {
        return string.matches("\\d{4}-\\d{2}-\\d{2}");
    }

    private boolean isNotInFuture(LocalDate date) {
        return !date.isAfter(LocalDate.now());
    }
}
