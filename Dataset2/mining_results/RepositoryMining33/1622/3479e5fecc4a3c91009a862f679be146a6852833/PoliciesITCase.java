/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.fit.console;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.Policies;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.util.tester.FormTester;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class PoliciesITCase extends AbstractConsoleITCase {

    private void createAccountPolicy(final String description) {
        wicketTester.clickLink("body:content:tabbedPanel:tabs-container:tabs:0:link");
        wicketTester.clickLink("body:content:tabbedPanel:panel:container:content:add");
        wicketTester.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        Component modal = wicketTester.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer");

        FormTester formTester = wicketTester.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:fields:0:field:textField", description);
        formTester.setValue("content:fields:1:field:spinner", "1");
        formTester.setValue("content:fields:2:field:checkboxField", true);
        formTester.setValue("content:fields:3:field:paletteField:recorder", "resource-csv");

        wicketTester.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();

        closeCallBack(modal);

        wicketTester.assertComponent("body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", WebMarkupContainer.class);

        Component component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Assert.assertNotNull(component);
        wicketTester.clickLink(component.getPageRelativePath() + ":cells:9:cell:panelEdit:editLink");
        wicketTester.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        wicketTester.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:0:field:textField", description);
        wicketTester.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:1:field:spinner", 1);
        wicketTester.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:2:field:checkboxField", true);
        wicketTester.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:3:field:paletteField:recorder", "resource-csv");

        wicketTester.executeAjaxEvent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:buttons:0:button",
                Constants.ON_CLICK);

        Assert.assertNotNull(findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description));
    }

    private void createPasswordPolicy(final String description) {
        wicketTester.clickLink("body:content:tabbedPanel:tabs-container:tabs:1:link");
        wicketTester.clickLink("body:content:tabbedPanel:panel:container:content:add");
        wicketTester.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        Component modal = wicketTester.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer");

        FormTester formTester = wicketTester.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:fields:0:field:textField", description);
        formTester.setValue("content:fields:1:field:spinner", "1");
        formTester.setValue("content:fields:2:field:checkboxField", true);

        wicketTester.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();

        closeCallBack(modal);

        Component component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Assert.assertNotNull(component);
        wicketTester.clickLink(component.getPageRelativePath() + ":cells:8:cell:panelEdit:editLink");
        wicketTester.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        wicketTester.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:0:field:textField", description);
        wicketTester.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:1:field:spinner", 1);
        wicketTester.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:2:field:checkboxField", true);

        wicketTester.executeAjaxEvent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:buttons:0:button",
                Constants.ON_CLICK);

        Assert.assertNotNull(findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description));
    }

    private void createPullPolicy(final String description) {
        wicketTester.clickLink("body:content:tabbedPanel:tabs-container:tabs:2:link");
        wicketTester.clickLink("body:content:tabbedPanel:panel:container:content:add");
        wicketTester.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        Component modal = wicketTester.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer");

        FormTester formTester = wicketTester.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:fields:0:field:textField", description);

        wicketTester.executeAjaxEvent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit",
                Constants.ON_CLICK);

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();

        closeCallBack(modal);

        Component component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Assert.assertNotNull(component);
        wicketTester.clickLink(component.getPageRelativePath() + ":cells:6:cell:panelEdit:editLink");
        wicketTester.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        wicketTester.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:0:field:textField", description);

        wicketTester.executeAjaxEvent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:buttons:0:button",
                Constants.ON_CLICK);

        Assert.assertNotNull(findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description));
    }

    private void deleteAccountPolicy(final String description) {
        wicketTester.clickLink("body:content:tabbedPanel:tabs-container:tabs:0:link");
        Component component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Assert.assertNotNull(component);

        wicketTester.getRequest().addParameter("confirm", "true");
        wicketTester.clickLink(wicketTester.getComponentFromLastRenderedPage(
                component.getPageRelativePath() + ":cells:9:cell:panelDelete:deleteLink"));

        wicketTester.executeAjaxEvent(wicketTester.getComponentFromLastRenderedPage(
                component.getPageRelativePath() + ":cells:9:cell:panelDelete:deleteLink"), Constants.ON_CLICK);

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();

        Assert.assertNull(findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description));
    }

    private void deletePasswordPolicy(final String description) {
        wicketTester.clickLink("body:content:tabbedPanel:tabs-container:tabs:1:link");
        Component component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Assert.assertNotNull(component);

        wicketTester.getRequest().addParameter("confirm", "true");
        wicketTester.clickLink(
                wicketTester.getComponentFromLastRenderedPage(
                        component.getPageRelativePath() + ":cells:8:cell:panelDelete:deleteLink"));

        wicketTester.executeAjaxEvent(wicketTester.getComponentFromLastRenderedPage(
                component.getPageRelativePath() + ":cells:8:cell:panelDelete:deleteLink"), Constants.ON_CLICK);

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();

        Assert.assertNull(findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description));
    }

    private void deletePullPolicy(final String description) {
        wicketTester.clickLink("body:content:tabbedPanel:tabs-container:tabs:2:link");
        Component component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Assert.assertNotNull(component);

        wicketTester.getRequest().addParameter("confirm", "true");
        wicketTester.clickLink(
                wicketTester.getComponentFromLastRenderedPage(
                        component.getPageRelativePath() + ":cells:6:cell:panelDelete:deleteLink"));

        wicketTester.executeAjaxEvent(wicketTester.getComponentFromLastRenderedPage(
                component.getPageRelativePath() + ":cells:6:cell:panelDelete:deleteLink"), Constants.ON_CLICK);

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();

        Assert.assertNull(findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description));
    }

    @Before
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
        wicketTester.clickLink("body:configurationLI:configurationUL:policiesLI:policies");
        wicketTester.assertRenderedPage(Policies.class);
    }

    @Test
    public void read() {
        Assert.assertNotNull(findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "an account policy"));
    }

    @Test
    public void createDeleteAccountPolicy() {
        final String description = "My Test Account Policy";
        createAccountPolicy(description);
        deleteAccountPolicy(description);
    }

    @Test
    public void cloneDeleteAccountPolicy() {
        final String description = "My Test Account Policy to be cloned";
        createAccountPolicy(description);

        Component component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Assert.assertNotNull(component);
        wicketTester.clickLink(component.getPageRelativePath() + ":cells:9:cell:panelClone:cloneLink");
        wicketTester.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        Component modal = wicketTester.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer");

        FormTester formTester = wicketTester.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:fields:0:field:textField", description + "2");

        wicketTester.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();

        closeCallBack(modal);

        Assert.assertNotNull(findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description));

        deleteAccountPolicy(description);

        Assert.assertNotNull(findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description + "2"));

        deleteAccountPolicy(description + "2");
    }

    @Test
    public void createDeletePasswordPolicy() {
        final String description = "My Test Password Policy";
        createPasswordPolicy(description);
        deletePasswordPolicy(description);
    }

    @Test
    public void cloneDeletePasswordPolicy() {
        final String description = "My Test Password Policy to be cloned";
        createPasswordPolicy(description);

        Component component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Assert.assertNotNull(component);
        wicketTester.clickLink(component.getPageRelativePath() + ":cells:8:cell:panelClone:cloneLink");
        wicketTester.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        Component modal = wicketTester.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer");

        FormTester formTester = wicketTester.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:fields:0:field:textField", description + "2");

        wicketTester.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();

        closeCallBack(modal);

        Assert.assertNotNull(findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description));

        deletePasswordPolicy(description);

        Assert.assertNotNull(findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description + "2"));

        deletePasswordPolicy(description + "2");
    }

    @Test
    public void createDeletePullPolicy() {
        final String description = "My Test Pull Policy";
        createPullPolicy(description);
        deletePullPolicy(description);
    }

    @Test
    public void cloneDeletePullPolicy() {
        final String description = "My Test Pull Policy to be cloned";
        createPullPolicy(description);

        Component component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Assert.assertNotNull(component);
        wicketTester.clickLink(component.getPageRelativePath() + ":cells:6:cell:panelClone:cloneLink");
        wicketTester.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        Component modal = wicketTester.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer");

        FormTester formTester = wicketTester.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:fields:0:field:textField", description + "2");

        wicketTester.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();

        closeCallBack(modal);

        Assert.assertNotNull(findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description));

        deletePullPolicy(description);

        Assert.assertNotNull(findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description + "2"));

        deletePullPolicy(description + "2");
    }

    @Test
    public void createUpdateDeleteAccountPolicy() {
        final String description = "Account Policy To Be Updated";
        createAccountPolicy(description);

        Component component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Assert.assertNotNull(component);
        wicketTester.clickLink(component.getPageRelativePath() + ":cells:9:cell:panelEdit:editLink");
        wicketTester.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        Component modal = wicketTester.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer");

        FormTester formTester = wicketTester.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:fields:1:field:spinner", "2");

        wicketTester.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();

        closeCallBack(modal);

        component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Assert.assertNotNull(component);
        wicketTester.assertLabel(component.getPageRelativePath() + ":cells:7:cell", "2");

        wicketTester.clickLink(component.getPageRelativePath() + ":cells:9:cell:panelEdit:editLink");
        wicketTester.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        wicketTester.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:1:field:spinner", 2);

        wicketTester.executeAjaxEvent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:buttons:0:button",
                Constants.ON_CLICK);

        deleteAccountPolicy(description);
    }

    @Test
    public void createComposeDeleteAccountPolicy() {
        final String description = "Account Policy To Be Composed";
        createAccountPolicy(description);

        Component component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Assert.assertNotNull(component);
        wicketTester.clickLink(component.getPageRelativePath() + ":cells:9:cell:panelCompose:composeLink");
        wicketTester.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        Component modal = wicketTester.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:3:outer");

        wicketTester.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:3:outer:form:content:container:content:add");

        FormTester formTester = wicketTester.newFormTester("body:content:tabbedPanel:panel:outerObjectsRepeater:3:"
                + "outer:form:content:container:content:wizard:form");
        formTester.setValue("view:name:textField", "myrule");
        formTester.setValue("view:configuration:dropDownChoiceField", "1");
        formTester.submit("buttons:next");

        wicketTester.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:3:outer:form:content:"
                + "container:content:wizard:form:view:bean:propView:1:value:spinner", 0);

        formTester = wicketTester.newFormTester("body:content:tabbedPanel:panel:outerObjectsRepeater:3:"
                + "outer:form:content:container:content:wizard:form");
        formTester.submit("buttons:finish");

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();

        component = findComponentByProp("name", "body:content:tabbedPanel:panel:outerObjectsRepeater:3:outer:form:"
                + "content:container:content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable",
                "myrule");

        Assert.assertNotNull(component);

        wicketTester.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:3:outer:form:content:container:content:exit");

        closeCallBack(modal);

        deleteAccountPolicy(description);
    }

    @Test
    public void createUpdateDeletePasswordPolicy() {
        final String description = "Password Policy To Be Updated";
        createPasswordPolicy(description);

        Component component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Assert.assertNotNull(component);
        wicketTester.clickLink(component.getPageRelativePath() + ":cells:8:cell:panelEdit:editLink");
        wicketTester.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        Component modal = wicketTester.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer");

        FormTester formTester = wicketTester.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:fields:1:field:spinner", "2");

        wicketTester.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();

        closeCallBack(modal);

        component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Assert.assertNotNull(component);
        wicketTester.assertLabel(component.getPageRelativePath() + ":cells:6:cell", "2");

        wicketTester.clickLink(component.getPageRelativePath() + ":cells:8:cell:panelEdit:editLink");
        wicketTester.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        wicketTester.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:1:field:spinner", 2);

        wicketTester.executeAjaxEvent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:buttons:0:button",
                Constants.ON_CLICK);

        deletePasswordPolicy(description);
    }

    @Test
    public void createComposeDeletePasswordPolicy() {
        final String description = "Password Policy To Be Composed";
        createPasswordPolicy(description);

        Component component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Assert.assertNotNull(component);
        wicketTester.clickLink(component.getPageRelativePath() + ":cells:8:cell:panelCompose:composeLink");
        wicketTester.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:3:outer", Modal.class);

        Component modal = wicketTester.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:3:outer");

        wicketTester.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:3:outer:form:content:container:content:add");

        FormTester formTester = wicketTester.newFormTester("body:content:tabbedPanel:panel:outerObjectsRepeater:3:"
                + "outer:form:content:container:content:wizard:form");
        formTester.setValue("view:name:textField", "myrule");
        formTester.setValue("view:configuration:dropDownChoiceField", "1");
        formTester.submit("buttons:next");

        wicketTester.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:3:outer:form:content:"
                + "container:content:wizard:form:view:bean:propView:0:value:spinner", 0);

        formTester = wicketTester.newFormTester("body:content:tabbedPanel:panel:outerObjectsRepeater:3:"
                + "outer:form:content:container:content:wizard:form");
        formTester.submit("buttons:finish");

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();

        component = findComponentByProp("name", "body:content:tabbedPanel:panel:outerObjectsRepeater:3:outer:form:"
                + "content:container:content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable",
                "myrule");

        Assert.assertNotNull(component);

        wicketTester.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:3:outer:form:content:container:content:exit");

        closeCallBack(modal);

        deletePasswordPolicy(description);
    }

    @Test
    public void createUpdateDeletePullPolicy() {
        final String description = "Pull Policy To Be Updated";
        createPullPolicy(description);

        Component component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Component modal = wicketTester.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer");

        Assert.assertNotNull(component);
        wicketTester.clickLink(component.getPageRelativePath() + ":cells:6:cell:panelEdit:editLink");
        wicketTester.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        FormTester formTester = wicketTester.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:fields:0:field:textField", description + "2");

        wicketTester.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();

        closeCallBack(modal);

        component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description + "2");

        Assert.assertNotNull(component);
        wicketTester.clickLink(component.getPageRelativePath() + ":cells:6:cell:panelEdit:editLink");
        wicketTester.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        wicketTester.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:0:field:textField", description + "2");

        wicketTester.executeAjaxEvent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:buttons:0:button",
                Constants.ON_CLICK);

        deletePullPolicy(description + "2");
    }

    @Test
    public void createComposeDeletePullPolicy() {
        final String description = "Pull Policy To Be Composed";
        createPullPolicy(description);

        Component component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Assert.assertNotNull(component);
        wicketTester.clickLink(component.getPageRelativePath() + ":cells:6:cell:panelCompose:composeLink");
        wicketTester.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer", Modal.class);

        Component modal = wicketTester.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer");

        FormTester formTester = wicketTester.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer:form");

        wicketTester.executeAjaxEvent("body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer:form:content:"
                + "correlationRules:multiValueContainer:innerForm:content:panelPlus:add", Constants.ON_CLICK);

        formTester.setValue("content:conflictResolutionAction:dropDownChoiceField", "1");
        formTester.setValue("content:correlationRules:multiValueContainer:innerForm:content:view:0:panel:"
                + "jsonRule:paletteField:recorder", "fullname");

        wicketTester.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer:dialog:footer:inputs:0:submit");

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();

        closeCallBack(modal);

        component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Assert.assertNotNull(component);
        wicketTester.clickLink(component.getPageRelativePath() + ":cells:6:cell:panelCompose:composeLink");
        wicketTester.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer", Modal.class);

        wicketTester.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer:form:"
                + "content:conflictResolutionAction:dropDownChoiceField", ConflictResolutionAction.FIRSTMATCH);

        wicketTester.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer:form:"
                + "content:correlationRules:multiValueContainer:innerForm:content:view:0:panel:"
                + "jsonRule:paletteField:recorder", "fullname");

        wicketTester.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer:dialog:footer:buttons:0:button");

        closeCallBack(modal);

        deletePullPolicy(description);
    }
}
