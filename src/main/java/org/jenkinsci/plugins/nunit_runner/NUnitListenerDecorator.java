package org.jenkinsci.plugins.nunit_runner;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hudson.console.LineTransformationOutputStream;
import hudson.model.TaskListener;

/**
 * @author a.filatov
 * 31.03.2014.
 */
public class NUnitListenerDecorator extends LineTransformationOutputStream {

    private final static String ATTACHMENTS_PATTERN = "^Attachments:\\s*$";

    private final static String COVERAGE_PATTERN = "^\\s*(.*\\.coverage)$";
    private final static int COVERAGE_GROUP = 1;

    private final OutputStream listener;

    private final Pattern attachmentsPattern;
    private final Pattern coveragePattern;

    private boolean attachmentsSection;

    private String coverageFile;

    public NUnitListenerDecorator(TaskListener listener) throws FileNotFoundException {
        this.listener = listener != null ? listener.getLogger() : null;

        coverageFile = null;

        this.attachmentsSection = false;
        this.attachmentsPattern = Pattern.compile(ATTACHMENTS_PATTERN);
        this.coveragePattern = Pattern.compile(COVERAGE_PATTERN);
    }

    public String getCoverageFile() {
        return this.coverageFile;
    }

    @Override
    protected void eol(byte[] bytes, int len) throws IOException {

        if (this.listener == null) {
            return;
        }

        String line = new String(bytes, 0, len, Charset.defaultCharset());

        if (!this.attachmentsSection) {
            Matcher attachmentsMatcher = this.attachmentsPattern.matcher(line);

            if (attachmentsMatcher.matches()) {
                this.attachmentsSection = true;
            }
        } else {
            Matcher coverageMatcher = this.coveragePattern.matcher(line);
            if (coverageMatcher.find()) {
                this.coverageFile = coverageMatcher.group(COVERAGE_GROUP);
            }
        }

        this.listener.write(line.getBytes(Charset.defaultCharset()));
    }
}
