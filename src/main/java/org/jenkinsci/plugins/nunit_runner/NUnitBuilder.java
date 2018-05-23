package org.jenkinsci.plugins.nunit_runner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.EnvironmentContributingAction;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.util.ArgumentListBuilder;
import hudson.util.ComboBoxModel;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * @author Yasuyuki Saito
 */
public class NUnitBuilder extends Builder implements SimpleBuildStep {

    private String nunitName;
    private String testFiles;
    private String settings;
    private String tests;
    private String testCaseFilter;
    private String platform;
    private String framework;
    private String cmdLineArgs;
    private boolean enablecodecoverage = DescriptorImpl.defaultEnableCodeCoverage;
    private boolean failBuild = DescriptorImpl.defaultFailBuild;

    @DataBoundConstructor
    public NUnitBuilder() {

    }

    protected Object readResolve() {
        return this;
    }

    public String getNUnitName() {
        return nunitName;
    }

    public String getTestFiles() {
        return testFiles;
    }

    public String getSettings() {
        return settings;
    }

    public String getTests() {
        return tests;
    }

    public boolean isEnablecodecoverage() {
        return enablecodecoverage;
    }

    public String getPlatform() {
        return platform;
    }

    public String getFramework() {
        return framework;
    }

    public String getTestCaseFilter() {
        return testCaseFilter;
    }

    public String getCmdLineArgs() {
        return cmdLineArgs;
    }

    public boolean isFailBuild() {
        return failBuild;
    }

    @DataBoundSetter
    public void setNUnitName(String nunitName) {
        this.nunitName = Util.fixEmptyAndTrim(nunitName);
    }

    @DataBoundSetter
    public void setTestFiles(String testFiles) {
        this.testFiles = Util.fixEmptyAndTrim(testFiles);
    }

    @DataBoundSetter
    public void setSettings(String settings) {
        this.settings = Util.fixEmptyAndTrim(settings);
    }

    @DataBoundSetter
    public void setTests(String tests) {
        this.tests = Util.fixEmptyAndTrim(tests);
    }

    @DataBoundSetter
    public void setTestCaseFilter(String testCaseFilter) {
        this.testCaseFilter = Util.fixEmptyAndTrim(testCaseFilter);
    }

    @DataBoundSetter
    public void setPlatform(String platform) {
        this.platform = Util.fixEmptyAndTrim(platform);
    }

    @DataBoundSetter
    public void setFramework(String framework) {
        this.framework = Util.fixEmptyAndTrim(framework);
    }

    @DataBoundSetter
    public void setCmdLineArgs(String cmdLineArgs) {
        this.cmdLineArgs = Util.fixEmptyAndTrim(cmdLineArgs);
    }

    @DataBoundSetter
    public void setEnablecodecoverage(boolean enablecodecoverage) {
        this.enablecodecoverage = enablecodecoverage;
    }

    @DataBoundSetter
    public void setFailBuild(boolean failBuild) {
        this.failBuild = failBuild;
    }

    @NonNull
    public NUnitInstallation getNUnit(TaskListener listener) {
        if (nunitName == null) return NUnitInstallation.getDefaultInstallation();
        NUnitInstallation.getDefaultInstallation();
        NUnitInstallation tool = Jenkins.getInstance().getDescriptorByType(NUnitInstallation.DescriptorImpl.class).getInstallation(nunitName);
        if (tool == null) {
            listener.getLogger().println("Selected NUnit installation does not exist. Using Default");
            tool = NUnitInstallation.getDefaultInstallation();
        }
        return tool;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     */
    @Extension
    @Symbol("nunit")
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public static final boolean defaultFailBuild = true;
        public static final boolean defaultEnableCodeCoverage = false;

        public DescriptorImpl() {
            super(NUnitBuilder.class);
            load();
        }

        public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        @NonNull
        public String getDisplayName() {
            return Messages.NUnitBuilder_DisplayName();
        }

        @SuppressWarnings("unused") // Used by Stapler
        public boolean showNUnitToolOptions() {
            return getNUnitToolDescriptor().getInstallations().length > 1;
        }

        private NUnitInstallation.DescriptorImpl getNUnitToolDescriptor() {
            return Jenkins.getInstance().getDescriptorByType(NUnitInstallation.DescriptorImpl.class);
        }

        public List<NUnitInstallation> getNUnitTools() {
            NUnitInstallation[] nunitInstallations = getNUnitToolDescriptor().getInstallations();
            return Arrays.asList(nunitInstallations);
        }

        @SuppressWarnings("unused") // Used by Stapler
        public ListBoxModel doFillNUnitNameItems() {
            ListBoxModel r = new ListBoxModel();
            for (NUnitInstallation nunitInstallation : getNUnitTools()) {
                r.add(nunitInstallation.getName());
            }
            return r;
        }

        @SuppressWarnings("unused") // Used by Stapler
        public ComboBoxModel doFillPlatformItems() {
            return fillComboBox(NUnitPlatform.class);
        }

        @SuppressWarnings("unused") // Used by Stapler
        public ComboBoxModel doFillFrameworkItems() {
            return fillComboBox(NUnitFramework.class);
        }

        private <E extends Enum<E>> ComboBoxModel fillComboBox(Class<E> clazz) {
            ComboBoxModel r = new ComboBoxModel();
            for (Enum<E> enumVal : clazz.getEnumConstants()) {
                r.add(enumVal.toString());
            }
            return r;
        }
    }

    /**
     *
     */
    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull Launcher launcher, @NonNull TaskListener listener) throws InterruptedException, IOException {
        ArrayList<String> args = new ArrayList<String>();

        EnvVars env = run.getEnvironment(listener);

        // nunit3-console.exe path.
        String pathToNUnit = getNUnitPath(workspaceToNode(workspace), listener, env);
        args.add(pathToNUnit);

        // Target dll path
        if (!StringUtils.isBlank(testFiles)) {
            List<String> targets = getTestFilesArguments(workspace, env);
            if (targets.size() == 0) {
                listener.getLogger().println("no files matching the pattern " + this.testFiles);
                if (this.failBuild) {
                    run.setResult(Result.FAILURE);
                    throw new AbortException("no files matching the pattern " + this.testFiles);
                }
            }
            args.addAll(targets);
        }

        // Run tests with additional settings such as data collectors.
        if (!StringUtils.isBlank(settings)) {
            args.add(convertArgumentWithQuote("Settings", replaceMacro(settings, env)));
        }

        // Run tests with names that match the provided values.
        if (!StringUtils.isBlank(tests)) {
            args.add(convertArgument("Tests", replaceMacro(tests, env)));
        }

        // Run tests that match the given expression.
        if (!StringUtils.isBlank(testCaseFilter)) {
            args.add(convertArgumentWithQuote("TestCaseFilter", replaceMacro(testCaseFilter, env)));
        }

        // Enables data diagnostic adapter CodeCoverage in the test run.
        if (enablecodecoverage) {
            args.add("/Enablecodecoverage");
        }

        // Target platform architecture to be used for test execution.
        String platformArg = getPlatformArgument(env);
        if (!StringUtils.isBlank(platformArg)) {
            args.add(convertArgument("Platform", platformArg));
        }

        // Target .NET Framework version to be used for test execution.
        String frameworkArg = getFrameworkArgument(env);
        if (!StringUtils.isBlank(frameworkArg)) {
            args.add(convertArgument("Framework", frameworkArg));
        }

        // Manual Command Line String
        if (!StringUtils.isBlank(cmdLineArgs)) {
            args.add(replaceMacro(cmdLineArgs, env));
        }

        // NUnit run.
        execNUnit(args, run, workspace, launcher, listener, env);
    }

    /**
     * @param value String value
     * @param env EnvVars
     * @return
     */
    private String replaceMacro(String value, EnvVars env) {
        String result = Util.replaceMacro(value, env);
        return result;
    }

    /**
     * @param builtOn Node build on
     * @param listener TaskListener
     * @param env EnvVars
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    @NonNull
    private String getNUnitPath(Node builtOn, TaskListener listener, EnvVars env) {
        NUnitInstallation installation = getNUnit(listener);
        if (builtOn != null) {
            try {
                installation = installation.forNode(builtOn, listener);
            } catch (IOException | InterruptedException e) {
                listener.getLogger().println("Failed to get NUnit executable");
            }
        }
        if (env != null) {
            installation = installation.forEnvironment(env);
        }

        String nunitExe = installation.getNUnitExe();

        listener.getLogger().println("Path To NUnit: " + nunitExe);

        return nunitExe;
    }

    /**
     * @param workspace
     * @param env
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    /* package */ List<String> getTestFilesArguments(FilePath workspace, EnvVars env) throws InterruptedException {
        Set<String> files = new HashSet<>();

        StringTokenizer testFilesTokenizer = new StringTokenizer(testFiles, " \t\r\n");

        while (testFilesTokenizer.hasMoreTokens()) {
            String testFile = testFilesTokenizer.nextToken();
            testFile = replaceMacro(testFile, env);

            if (!StringUtils.isBlank(testFile)) {
                try {
                    for (FilePath filePath : workspace.list(testFile)) {
                        files.add(appendQuote(relativize(workspace, filePath)));
                    }
                } catch (IOException ignored) {
                }
            }
        }

        return new ArrayList<>(files);
    }

    /**
     * @param env
     * @return
     */
    private String getPlatformArgument(EnvVars env) {
        return replaceMacro(platform, env);
    }

    /**
     * @param env
     * @return
     */
    private String getFrameworkArgument(EnvVars env) {
        String expanded = replaceMacro(framework, env);
        return expanded;
    }

    /**
     * @param base
     * @param path
     * @return the relative path of 'path'
     * @throws InterruptedException
     * @throws IOException
     */
    /* package */ String relativize(FilePath base, FilePath path) throws InterruptedException, IOException {
        return base.toURI().relativize(path.toURI()).getPath();
    }

    /**
     * @param args
     * @param run
     * @param workspace
     * @param launcher
     * @param listener
     * @param env
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    private void execNUnit(List<String> args, Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener, EnvVars env) throws InterruptedException, IOException {
        ArgumentListBuilder cmdExecArgs = new ArgumentListBuilder();
        FilePath tmpDir = null;

        if (!launcher.isUnix()) {
            tmpDir = workspace.createTextTempFile("nunit", ".bat", concatString(args), false);
            cmdExecArgs.add("cmd.exe", "/C", tmpDir.getRemote(), "&&", "exit", "%ERRORLEVEL%");
        } else {
            for (String arg : args) {
                cmdExecArgs.add(arg);
            }
        }

        listener.getLogger().println("Executing NUnit: " + cmdExecArgs.toStringWithQuote());

        try {
            NUnitListenerDecorator parserListener = new NUnitListenerDecorator(listener);
            int r = launcher.launch().cmds(cmdExecArgs).envs(env).stdout(parserListener).pwd(workspace).join();

            String coverageFullPath = parserListener.getCoverageFile();
            String coveragePathRelativeToWorkspace = null;

            if (coverageFullPath != null) {
                coveragePathRelativeToWorkspace = relativize(workspace, workspace.child(parserListener.getCoverageFile()));
            }

            if (r != 0) {
                if (failBuild) {
                    run.setResult(Result.FAILURE);
                    throw new AbortException("NUnit exited with " + r);
                } else {
                    run.setResult(Result.UNSTABLE);
                }
            }
        } catch (IOException e) {
            Util.displayIOException(e, listener);
            e.printStackTrace(listener.fatalError("NUnit command execution failed"));
        } finally {
            try {
                if (tmpDir != null) {
                    tmpDir.delete();
                }
            } catch (IOException e) {
                Util.displayIOException(e, listener);
                e.printStackTrace(listener.fatalError("temporary file delete failed"));
            }
        }
    }

    /**
     * @param option
     * @param param
     * @return
     */
    private String convertArgument(String option, String param) {
        return String.format("/%s:%s", option, param);
    }

    /**
     * @param option
     * @param param
     * @return
     */
    private String convertArgumentWithQuote(String option, String param) {
        return String.format("/%s:\"%s\"", option, param);
    }

    /**
     * @param value
     * @return
     */
    private String appendQuote(String value) {
        return String.format("\"%s\"", value);
    }

    /**
     * @param args
     * @return
     */
    private String concatString(List<String> args) {
        StringBuilder buf = new StringBuilder();
        for (String arg : args) {
            if (buf.length() > 0) {
                buf.append(' ');
            }
            buf.append(arg);
        }
        return buf.toString();
    }

    private static Node workspaceToNode(FilePath workspace) {
        Computer computer = workspace.toComputer();
        Node node = null;
        if (computer != null) node = computer.getNode();
        return node != null ? node : Jenkins.getInstance();
    }

    private static class AddNUnitEnvVarsAction implements EnvironmentContributingAction {

        private final static String COVERAGE_ENV = "NUNIT_RESULT_COVERAGE";

        private final String coverageEnv;

        public AddNUnitEnvVarsAction(String coverageEnv) {
            this.coverageEnv = coverageEnv;
        }

        public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
            if (coverageEnv != null) {
                env.put(COVERAGE_ENV, coverageEnv);
            }
        }

        public String getDisplayName() {
            return "Add NUnitRunner Environment Variables to Build Environment";
        }

        public String getIconFileName() {
            return null;
        }

        public String getUrlName() {
            return null;
        }
    }
}
