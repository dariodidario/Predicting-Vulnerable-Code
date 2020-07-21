package hudson.plugins.build_publisher;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

import java.util.Arrays;

public class BuildPublisherTest {

    public @Rule JenkinsRule j = new JenkinsRule();

    @Test
    public void configRoundtrip() throws Exception {
        JenkinsRule.WebClient wc = j.createWebClient();
        HtmlPage page = wc.goTo("configure");
        HtmlForm config = page.getFormByName("config");
        config.getInputByName("bp.name").setValueAttribute("NAME");
        config.getInputByName("bp.url").setValueAttribute("fake://url.com/");
        config.getInputByName("bp.login").setValueAttribute("LOGIN");
        config.getInputByName("bp.password").setValueAttribute("PASSWORD");
        j.submit(config);

        HudsonInstance[] publicInstances = BuildPublisher.DESCRIPTOR.getPublicInstances();
        assertEquals(Arrays.toString(publicInstances), 1, publicInstances.length);
        HudsonInstance name = publicInstances[0];
        assertEquals("NAME", name.getName());
        assertEquals("fake://url.com/", name.getUrl());
        assertEquals("LOGIN", name.getLogin());
        assertEquals("PASSWORD", name.getPassword());

        page = wc.goTo("configure");
        assertThat(page.getWebResponse().getContentAsString(), not(containsString("PASSWORD")));
    }

    @Test @LocalData
    public void migrateTo_1_22() throws Exception {
        HudsonInstance[] publicInstances = BuildPublisher.DESCRIPTOR.getPublicInstances();
        assertEquals(Arrays.toString(publicInstances), 1, publicInstances.length);
        HudsonInstance name = publicInstances[0];
        assertEquals("NAME", name.getName());
        assertEquals("fake://url.com/", name.getUrl());
        assertEquals("LOGIN", name.getLogin());
        assertEquals("PASSWORD", name.getPassword());
    }
}
