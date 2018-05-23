package org.jenkinsci.plugins.nunit_runner;

import java.io.File;
import java.io.IOException;

import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.EnvVars;
import hudson.Extension;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.model.TaskListener;
import hudson.model.Node;
import hudson.slaves.NodeSpecific;
import hudson.model.EnvironmentSpecific;
import jenkins.model.Jenkins;

/**
 * @author Yasuyuki Saito
 */
public class NUnitInstallation extends ToolInstallation implements NodeSpecific<NUnitInstallation>, EnvironmentSpecific<NUnitInstallation> {


    public static transient final String DEFAULT = "Default";

    private static final long serialVersionUID = 1;

    /** */
    @Deprecated
    private transient String pathToNUnit;

    /**
     * @param name Name of install
     * @param home Path of install
     */
    @DataBoundConstructor
    public NUnitInstallation(String name, String home) {
        super(name, home, null);
    }

    /**
     *
     */
    public NUnitInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
        return new NUnitInstallation(getName(), translateFor(node, log));
    }

    /**
     * @param environment Environment
     * @return new NUnitInstallation instance
     */
    public NUnitInstallation forEnvironment(EnvVars environment) {
        return new NUnitInstallation(getName(), environment.expand(getHome()));
    }

    /**
     * Used for backward compatibility
     *
     * @return the new object, an instance of MsBuildInstallation
     */
    protected Object readResolve() {
        if (this.pathToNUnit != null) {
            return new NUnitInstallation(this.getName(), this.pathToNUnit);
        }
        return this;
    }

    public String getNUnitExe() {
        return getHome();
    }

    public static NUnitInstallation getDefaultInstallation() {
        DescriptorImpl nunitsTools = Jenkins.getInstance().getDescriptorByType(NUnitInstallation.DescriptorImpl.class);
        NUnitInstallation tool = nunitsTools.getInstallation(NUnitInstallation.DEFAULT);
        if (tool != null) {
            return tool;
        } else {
            NUnitInstallation[] installations = nunitsTools.getInstallations();
            if (installations.length > 0) {
                return installations[0];
            } else {
                onLoaded();
                return nunitsTools.getInstallations()[0];
            }
        }
    }

    @Initializer(after = InitMilestone.EXTENSIONS_AUGMENTED)
    public static void onLoaded() {
        DescriptorImpl descriptor = (DescriptorImpl) Jenkins.getInstance().getDescriptor(NUnitInstallation.class);
        NUnitInstallation[] installations = getInstallations(descriptor);

        if (installations != null && installations.length > 0) {
            // No need to initialize if there's already something
            return;
        }


        String defaultNUnitExe = isWindows() ? "nunit3-console.exe" : "nunit-console";
        NUnitInstallation tool = new NUnitInstallation(DEFAULT, defaultNUnitExe);
        descriptor.setInstallations(tool);
        descriptor.save();
    }

    private static NUnitInstallation[] getInstallations(DescriptorImpl descriptor) {
        NUnitInstallation[] installations = null;
        try {
            installations = descriptor.getInstallations();
        } catch (NullPointerException e) {
            installations = new NUnitInstallation[0];
        }
        return installations;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

    private static boolean isWindows() {
        return File.pathSeparatorChar == ';';
    }

    /**
     */
    @Extension
    public static class DescriptorImpl extends ToolDescriptor<NUnitInstallation> {

        public String getDisplayName() {
            return Messages.NUnitInstallation_DisplayName();
        }

        @Nullable
        public NUnitInstallation getInstallation(String name) {
            for (NUnitInstallation i : getInstallations()) {
                if (i.getName().equals(name)) {
                    return i;
                }
            }
            return null;
        }

    }
}
