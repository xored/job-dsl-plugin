package javaposse.jobdsl.dsl.helpers.step

import javaposse.jobdsl.dsl.AbstractContext
import javaposse.jobdsl.dsl.ContextHelper
import javaposse.jobdsl.dsl.DslContext
import javaposse.jobdsl.dsl.Item
import javaposse.jobdsl.dsl.JobManagement
import javaposse.jobdsl.dsl.Preconditions
import javaposse.jobdsl.dsl.RequiresPlugin
import javaposse.jobdsl.dsl.SlaveFs
import javaposse.jobdsl.dsl.helpers.common.DownstreamTriggerParameterContext

class PhaseJobContext extends AbstractContext {
    private static final List<String> VALID_KILL_CONDITIONS = ['FAILURE', 'NEVER', 'UNSTABLE']

    private static final List<String> VALID_RESUME_CONDITIONS = ['SKIP', 'ALWAYS', 'EXPRESSION']

    String jobName
    boolean currentJobParameters = true
    boolean exposedScm = true
    DownstreamTriggerParameterContext paramTrigger
    boolean disableJob = false
    boolean abortAllJobs = false
    String killPhaseCondition = 'FAILURE'
    String resumeCondition = 'SKIP'
    Boolean enableJobScript = null
    boolean isUseScriptFile = false
    String jobScript
    String scriptPath
    Boolean isUseResumeScriptFile
    String resumeScriptText
    String resumeScriptPath
    String resumeBindings = ''
    String jobBindings = ''
    String resumeConditions = null
    boolean isJobScriptOnSlave = false
    boolean isResumeScriptOnSlave = false

    Closure configureClosure

    PhaseJobContext(JobManagement jobManagement, Item item, String jobName) {
        super(jobManagement)
        this.jobName = jobName
        this.paramTrigger = new DownstreamTriggerParameterContext(jobManagement, item)
    }

    @Deprecated
    PhaseJobContext(JobManagement jobManagement, Item item, String jobName, boolean currentJobParameters,
                    boolean exposedScm) {
        this(jobManagement, item, jobName)
        this.currentJobParameters = currentJobParameters
        this.exposedScm = exposedScm
    }

    /**
     * Defines the name of the job.
     */
    @Deprecated
    void jobName(String jobName) {
        jobManagement.logDeprecationWarning()

        this.jobName = jobName
    }

    SlaveFs slaveFs(String path) {
        new SlaveFs(path)
    }

    void enableGroovyScript(boolean enableJobScript) {
        this.enableJobScript = enableJobScript
    }

    void groovyScript(String type, String source) {
        if (null == enableJobScript) {
            enableJobScript = true
        }
        isUseScriptFile = false
        if ('FILE' == type) {
            isUseScriptFile = true
            scriptPath = source
            jobScript = ''
        } else if ('SCRIPT' == type) {
            jobScript = source
            isUseScriptFile = ''
        } else {
            enableJobScript = false
        }
    }

    void groovyScript(String type, SlaveFs slaveFs) {
        if (null == enableJobScript) {
            enableJobScript = true
        }
        isUseScriptFile = false
        if ('FILE' == type) {
            isUseScriptFile = true
            isJobScriptOnSlave = true
            scriptPath = slaveFs.path
            jobScript = ''
        } else {
            enableJobScript = false
            isJobScriptOnSlave = false
        }
    }

    void resumeCondition(String resumeCondition) {
        Preconditions.checkArgument(
                VALID_RESUME_CONDITIONS.contains(resumeCondition),
                "Resume condition needs to be one of these values: ${VALID_RESUME_CONDITIONS.join(',')}"
        )

        this.resumeCondition = resumeCondition
    }

    void resumeGroovyScript(String source, String value) {
        this.isUseResumeScriptFile = false
        if ('FILE' == source) {
            this.resumeScriptPath = value
            this.isUseResumeScriptFile = true
        } else if ('SCRIPT' == source) {
            this.resumeScriptText = value
        }
    }

    void resumeGroovyScript(String source, SlaveFs slaveFs) {
        this.isUseResumeScriptFile = false
        if ('FILE' == source) {
            this.resumeScriptPath = slaveFs.path
            this.isResumeScriptOnSlave = true
            this.isUseResumeScriptFile = true
        }
    }

    void bindVar(String source, String key, String value) {
        if ('RESUME' == source) {
            if (null == resumeBindings) {
                resumeBindings = ''
            }
            resumeBindings += key + '=' + value + '\n'
            //resumeBindings.concat(key).concat('=').concat(value).concat('\n')
        } else if ('JOB' == source) {
            if (null == jobBindings) {
                jobBindings = ''
            }
            jobBindings += key + '=' + value + '\n'
            //jobBindings.concat(key).concat('=').concat(value).concat('\n')
        }
    }

    void bindVarMap(String source, Map<String, String> map) {
        if ('RESUME' == source) {
            if (null == resumeBindings) {
                resumeBindings = ''
            }
            map.each { k, v ->
                this.resumeBindings.concat(k).concat('=').concat(v).concat('\n')
            }
        } else if ('JOB' == source) {
            if (null == jobBindings) {
                jobBindings = ''
            }
            map.each { k, v ->
                this.jobBindings.concat(k).concat('=').concat(v).concat('\n')
            }
        }
    }

    /**
     * Copies parameters from the current build, except for file parameters. Defaults to [@code true}.
     */
    void currentJobParameters(boolean currentJobParameters = true) {
        this.currentJobParameters = currentJobParameters
        paramTrigger.currentBuild()
    }

    /**
     * Defaults to {@code true}.
     */
    void exposedScm(boolean exposedScm = true) {
        this.exposedScm = exposedScm
    }

    /**
     * Adds parameter values for the job.
     *
     * @since 1.38
     */
    @RequiresPlugin(id = 'parameterized-trigger')
    void parameters(@DslContext(DownstreamTriggerParameterContext) Closure closure) {
        jobManagement.logPluginDeprecationWarning('parameterized-trigger', '2.26')

        ContextHelper.executeInContext(closure, paramTrigger)
    }

    /**
     * Adds a boolean parameter. Can be called multiple times to add more parameters.
     */
    @Deprecated
    void boolParam(String paramName, boolean defaultValue = false) {
        jobManagement.logDeprecationWarning()
        paramTrigger.boolParam(paramName, defaultValue)
    }

    /**
     * Reads parameters from a properties file.
     */
    @Deprecated
    void fileParam(String propertyFile, boolean failTriggerOnMissing = false) {
        jobManagement.logDeprecationWarning()
        paramTrigger.propertiesFile(propertyFile, failTriggerOnMissing)
    }

    /**
     * Uses the same node for the triggered builds that was used for this build.
     */
    @Deprecated
    void sameNode(boolean nodeParam = true) {
        jobManagement.logDeprecationWarning()
        paramTrigger.sameNode = nodeParam
    }

    /**
     * Specifies a Groovy filter expression that restricts the subset of combinations that the downstream project will
     * run.
     */
    @Deprecated
    void matrixParam(String filter) {
        jobManagement.logDeprecationWarning()
        paramTrigger.matrixSubset(filter)
    }

    /**
     * Passes the Subversion revisions that were used in this build to the downstream builds.
     */
    @Deprecated
    void subversionRevision(boolean includeUpstreamParameters = false) {
        jobManagement.logDeprecationWarning()
        paramTrigger.subversionRevision(includeUpstreamParameters)
    }

    /**
     * Passes the Git commit that was used in this build to the downstream builds.
     */
    @Deprecated
    void gitRevision(boolean combineQueuedCommits = false) {
        jobManagement.logDeprecationWarning()
        paramTrigger.gitRevision(combineQueuedCommits)
    }

    /**
     * Adds a parameter. Can be called multiple times to add more parameters.
     */
    @Deprecated
    void prop(Object key, Object value) {
        jobManagement.logDeprecationWarning()
        paramTrigger.predefinedProp(key, value)
    }

    /**
     * Adds parameters. Can be called multiple times to add more parameters.
     */
    @Deprecated
    void props(Map<String, String> map) {
        jobManagement.logDeprecationWarning()
        paramTrigger.predefinedProps(map)
    }

    /**
     * Defines where the target job should be executed, the value must match either a label or a node name.
     *
     * @since 1.26
     */
    @Deprecated
    void nodeLabel(String paramName, String nodeLabel) {
        jobManagement.logDeprecationWarning()
        paramTrigger.nodeLabel(paramName, nodeLabel)
    }

    /**
     * Disables the job. Defaults to {@code false}.
     *
     * @since 1.25
     */
    void disableJob(boolean disableJob = true) {
        this.disableJob = disableJob
    }

    /**
     * Kills all sub jobs and the phase job if this sub job is killed. Defaults to {@code false}.
     *
     * @since 1.37
     */
    void abortAllJobs(boolean abortAllJob = true) {
        this.abortAllJobs = abortAllJob
    }

    /**
     * Kills the phase when a condition is met.
     *
     * Must be one of {@code 'FAILURE'} (default), {@code 'NEVER'} or [@code 'UNSTABLE'}.
     * @since 1.25
     */
    void killPhaseCondition(String killPhaseCondition) {
        Preconditions.checkArgument(
                VALID_KILL_CONDITIONS.contains(killPhaseCondition),
                "Kill Phase on Job Result Condition needs to be one of these values: ${VALID_KILL_CONDITIONS.join(',')}"
        )

        this.killPhaseCondition = killPhaseCondition
    }

    /**
     * Allows direct manipulation of the generated XML. The {@code com.tikal.jenkins.plugins.multijob.PhaseJobsConfig}
     * node is passed into the configure block.
     *
     * @since 1.30
     * @see <a href="https://github.com/jenkinsci/job-dsl-plugin/wiki/The-Configure-Block">The Configure Block</a>
     */
    void configure(Closure configureClosure) {
        this.configureClosure = configureClosure
    }
}
