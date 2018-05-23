package org.jenkinsci.plugins.nunit_runner;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Util;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class FilePatternTest {

    private FilePath workspace;
    private FilePath subfolder;

    @Before
    public void setUp() throws Exception {
        File parent = Util.createTempDir();
        workspace = new FilePath(parent);
        if (workspace.exists()) {
            workspace.deleteRecursive();
        }
        workspace.mkdirs();
        subfolder = workspace.child("subfolder");
        if (subfolder.exists()) {
            boolean delete = subfolder.delete();
            assertThat(delete, is(true));
        }
        subfolder.mkdirs();
    }

    @After
    public void tearDown() throws Exception {
        workspace.deleteRecursive();
    }

    private FilePath createFile(FilePath folder, String child) throws Exception {
        FilePath file = new FilePath(folder, child);
            File newFile = new File(file.getRemote());
        boolean createdNewFile = newFile.createNewFile();
        assertThat(createdNewFile, is(true));
        assertThat(file.exists(), is(true));
        return file;
    }

    // Bunch of tests from here:
    // https://github.com/jenkinsci/vstestrunner-plugin/blob/master/src/test/java/org/jenkinsci/plugins/vstest_runner/FilePatternTest.java
    // But this doesn't care about trx, so removed.
}
