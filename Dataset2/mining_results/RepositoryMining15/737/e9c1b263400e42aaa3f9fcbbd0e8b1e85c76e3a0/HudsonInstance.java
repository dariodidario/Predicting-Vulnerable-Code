/**
 * Hudson instance description.
 */
package hudson.plugins.build_publisher;

import hudson.ProxyConfiguration;
import hudson.Util;
import hudson.XmlFile;
import hudson.model.Item;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.Run;
import hudson.model.listeners.ItemListener;
import hudson.plugins.build_publisher.StatusInfo.State;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.util.Secret;
import jenkins.model.Jenkins;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.params.HttpClientParams;

/**
 * Represents remote public Hudson instance.
 *
 * @author dvrzalik@redhat.com
 */
public final class HudsonInstance {

    static final Logger LOGGER = Logger.getLogger(Hudson.class.getName());

    private String url;
    private String name;
    private String login;
    private Secret secret;

    @Deprecated
    private transient String password;

    // Builds to be published
    private transient LinkedHashSet<AbstractBuild> publishRequestQueue = new LinkedHashSet<AbstractBuild>();

    private transient PublisherThread publisherThread;
    transient BuildTransmitter buildTransmitter;
    private transient HttpClient client;

    public String getLogin() {
        return login;
    }

    /**
     * Get plaintext password.
     */
    /*package*/ String getPassword() {
        return secret.getPlainText();
    }

    /**
     * Get encrypted secret.
     */
    // Exposed for jelly
    public Secret getSecret() {
        return secret;
    }

    public boolean requiresAuthentication() {
        return Util.fixEmpty(login)!=null;
    }

    public HudsonInstance(String name, String url, String login, String password) {
        this.name = name;
        this.url = url;
        this.login = login;
        this.secret = Secret.fromString(password);

        initVariables();
        restoreQueue();
        initPublisherThread();
    }

    public String getUrl() {

        if (url != null && !url.endsWith("/")) {
            url += '/';
        }

        return url;
    }

    public String getName() {
        return name;
    }

    /**
     * Append the build to the publishing queue.
     */
    public void publishNewBuild(AbstractBuild build) {
        publishBuild(build, new StatusInfo(State.PENDING, "Waiting in queue",
                name, null));

    }

    /**
     * Same as previous, but doesn't set status for the build.
     */
    public synchronized void publishBuild(AbstractBuild build, StatusInfo status) {
        publishRequestQueue.add(build);
        StatusAction.setBuildStatusAction(build, status);
        saveQueue();
        notifyAll();
    }

    //Disable aborting until it is properly implemented
    //public void abortTransmission(AbstractBuild request) {
    //    publisherThread.abortTrasmission(request);
    //}

    // XStream init
    private Object readResolve() {
        // Migrate plaintext password to secret
        if (password != null) {
            if (secret == null) {
                secret = Secret.fromString(password);
                password = null;
            }
        }

        initVariables();

        // let's wait until Hudson's initialized
        Hudson.getInstance().getExtensionList(ItemListener.class).add(new ItemListener() {
            @Override
            public void onLoaded() {
                restoreQueue();
                initPublisherThread();
            }
        });

        return this;
    }

    private void initVariables() {
        publishRequestQueue = new LinkedHashSet<AbstractBuild>();
        buildTransmitter = new HTTPBuildTransmitter();
        SimpleHttpConnectionManager connectionManager = new SimpleHttpConnectionManager();
        HttpClientParams params = new HttpClientParams();
        //set SO_TIMEOUT to prevent thread hang-up
        params.setSoTimeout(10 * 60 * 1000);
        client = new HttpClient(params, connectionManager);
        loadProxy();
    }

    void loadProxy(){
        Jenkins j = Jenkins.getInstance();
        ProxyConfiguration proxy = j!=null ? j.proxy : null;
        if(proxy != null) {
            client.getHostConfiguration().setProxy(proxy.name, proxy.port);
            if(proxy.getUserName() != null)
                client.getState().setProxyCredentials(AuthScope.ANY,new UsernamePasswordCredentials(proxy.getUserName(),proxy.getPassword()));
        }
    }
    
    /*package*/ void initPublisherThread() {
        if(publisherThread == null || !publisherThread.isAlive()) {
            publisherThread = new PublisherThread(HudsonInstance.this);
            publisherThread.start();
        }
    }

    HttpClient getHttpClient() {
        return client;
    }

    synchronized void removeRequest(AbstractBuild request, StatusInfo statusInfo) {
        if (publishRequestQueue.contains(request)) {
            publishRequestQueue.remove(request);
            saveQueue();
            StatusAction.setBuildStatusAction(request, statusInfo);
        }
    }
    
    synchronized void postponeRequest(AbstractBuild request) {
        if (publishRequestQueue.contains(request)) {
            publishRequestQueue.remove(request);
            publishRequestQueue.add(request);
        }
    }

    /**
     * Obtains the current queue of builds that are waiting for publication.
     */
    public synchronized List<AbstractBuild> getQueue() {
        return new ArrayList<AbstractBuild>(publishRequestQueue);
    }
    
    /**
     * Gets the thread that does the publication.
     *
     * @return
     *      Can be null during the initialization of Hudson.
     */
    public PublisherThread getPublisherThread() {
        return publisherThread;
    }

    synchronized AbstractBuild nextRequest() {
        // If there is nothing to do let's wait until next
        // PublishRequest
        waitForRequest();
        return publishRequestQueue.iterator().next();
    }

    private void waitForRequest() {
        while (publishRequestQueue.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
        }
    }

    /**
     * Serializes the queue into $HUDSON_HOME
     */
    private void saveQueue() {
        List<RequestHolder> holders = new LinkedList<RequestHolder>();
        for (AbstractBuild request : publishRequestQueue) {
            int n = request.getNumber();
            AbstractProject p = request.getProject();
            RequestHolder holder = new RequestHolder(n,p.getFullName());
            holders.add(holder);
        }
        XmlFile file = new XmlFile(new File(Hudson.getInstance().getRootDir(),
                "bp-" + name + ".xml"));
        try {
            file.write(holders);
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.severe(e.getMessage());
        }
    }

    private void restoreQueue() {
        XmlFile file = new XmlFile(new File(Hudson.getInstance().getRootDir(),
                "bp-" + name + ".xml"));
        if(!file.exists())
            return; // nothing to restore.
        
        try {
            List<RequestHolder> holders = (List<RequestHolder>) file.read();
            for (RequestHolder holder : holders) {
                String projectName = holder.project;
                Item project = Hudson.getInstance().getItemByFullName(
                        projectName);
                if (project instanceof AbstractProject) {
                    Run build = ((AbstractProject) project)
                            .getBuildByNumber(holder.build);
                    if (build != null) {
                        publishRequestQueue.add((AbstractBuild) build);
                    }
                }
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE,"Could not restore publisher queue from "
                    + file.getFile().getAbsolutePath(),e);
        }
    }

    private static class RequestHolder {
        int build;
        String project;

        RequestHolder(int build, String project) {
            this.build = build;
            this.project = project;
        }
    }
}
