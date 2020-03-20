package de.adesso.service;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.diff.DiffEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This service helps managing repositories with the help of JGit.
 */
@Service
public class MarkdownTransformer {

    private Environment environment;

    @Value("${repository.local.path}")
    private String LOCAL_REPO_PATH;

    @Value("#{environment.REPOSITORY_REMOTE_URL}")
    private String REPOSITORY_REMOTE_URL;

    @Value("${repository.local.htmlposts.path}")
    private String LOCAL_HTML_POSTS;

    @Value("${repository.local.firstspirit-xml.path}")
    private String FIRSTSPIRIT_XML_PATH;

    @Value("${repository.local.JSON.path}")
    private String JSON_PATH;

    private FileTransfer fileTransfer;

    @Autowired
    public MarkdownTransformer(Environment environment, FileTransfer fileTransfer) {
        this.environment = environment;
        this.fileTransfer = fileTransfer;
    }

    /**
     * After a change in a markdown post was detected, the jekyll-build process
     * generates html and xml files to the corresponding markdown file. The xml
     * output has to be copied to an intended folder
     *
     * @param entries List with all changed files
     */
    protected void copyGeneratedXmlFiles(List<DiffEntry> entries) {

        entries.forEach((entry) -> {
            /*
			 * Assumption: every-blog- post-file with ending "markdown" has the following
			 * structure: _posts/2017-08-01-new-post-for-netlify-test.markdown
			 */
            String filePath;

            if (entry.getChangeType() == DiffEntry.ChangeType.DELETE) {
                filePath = entry.getOldPath();
            } else {
                filePath = entry.getNewPath();
            }

            /*
			 * RegEx to separate the filepath into
			 * the folders, filedate, filename and the markdown suffix
			 */
            String regex = "(((/.+/)|())(((\\d+-){3})(([^/\\.]+))))";

            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(filePath);

            if(filePath.startsWith("_posts") && matcher.find()){
                String folderStructure = matcher.group(2);
                String fullFileName = matcher.group(5);
                String fileName = matcher.group(8);
                String fileDate = matcher.group(5).substring(0,10);

                String xmlFileName = fileName + ".xml";

				/*
				 * The Jeykyll xml built is located at
				 * "/_site/blog-posts/2017-08-01/new-post-title/2017-08-01-new-post-title.xml
				 */
                File source = new File(String.format("%s%s/%s/%s/%s", LOCAL_HTML_POSTS, folderStructure, fileDate, fileName, xmlFileName));
                File dest = new File(String.format("%s%s/%s/%s.xml", FIRSTSPIRIT_XML_PATH, folderStructure, fileDate, fullFileName));
                Path dirPath = new File(String.format("%s%s/%s", FIRSTSPIRIT_XML_PATH, folderStructure, fileDate)).toPath();

                if (entry.getChangeType() == DiffEntry.ChangeType.DELETE) {
                    try {
                        Files.delete(dest.toPath());
						/* Checking if the directory of the File is empty
						 * Then delete it
						 */
                        if (!Files.newDirectoryStream(dirPath).iterator().hasNext()) {
                            Files.delete(dirPath);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    fileTransfer.copyFile(source, dest);
                }
            }
        });
        fileTransfer.deleteXmlFromSiteFolder();
    }

    /**
     * Copy all XML files that were generated by jekyll.
     * This method is useful after starting and initializing the application
     */
    public void copyAllGeneratedXmlFiles() {
        Collection<File> allFiles = new ArrayList<File>();
        File file = new File(LOCAL_HTML_POSTS);
        scanDirectory(file, allFiles);
		/*
		 * Filter: take only XML-files - other files will be ignored
		 */
        allFiles.stream().filter(File::isFile).filter((f) -> {
            return FilenameUtils.getExtension(f.getAbsolutePath()).equals("xml");
        }).forEach((f) -> {
            String fileDate = FilenameUtils.getBaseName(new File(f.getParent()).getParent());
            File dest = new File(
					/*
					 * XML File located at
					 * "_site/blog-posts/2016-05-12/welcome-to-jekyll/welcome-to-jekyll.xml" and is
					 * desired to be copied to
					 * "assets/first-spirit-xml/2016-05-12-welcome-to-jekyll"
					 */
                    FIRSTSPIRIT_XML_PATH + "/" + fileDate + "/" + fileDate + "-"
                            + FilenameUtils.getBaseName(f.getAbsolutePath() + ".xml"));
            fileTransfer.copyFile(f, dest);
        });
        fileTransfer.deleteXmlFromSiteFolder();
    }

    /**
     * Auxiliary-method for copyAllGeneratedXmlFiles() - a file directory will be
     * scanned and all files are collected
     *
     * @param file root directory - scan starts here
     * @param all  Collection where results are added to
     */
    private void scanDirectory(File file, Collection<File> all) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                all.add(child);
                scanDirectory(child, all);
            }
        }
    }
}
