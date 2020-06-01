/**
 *
 */
package hudson.plugins.build_publisher;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;

/**
 *
 *
 */
class PublishRequest {

    AbstractBuild build;
    AbstractProject project;

    public PublishRequest(AbstractBuild build, AbstractProject project) {
        this.build = build;
        this.project = project;
    }

}