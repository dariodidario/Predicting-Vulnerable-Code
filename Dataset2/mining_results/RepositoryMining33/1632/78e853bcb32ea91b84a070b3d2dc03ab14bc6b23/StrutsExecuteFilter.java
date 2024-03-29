/*
 * $Id: DefaultActionSupport.java 651946 2008-04-27 13:41:38Z apetrelli $
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.struts2.dispatcher.ng;

import org.apache.struts2.StrutsStatics;
import org.apache.struts2.dispatcher.Dispatcher;
import org.apache.struts2.dispatcher.mapper.ActionMapping;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Executes the discovered request information.  This filter requires the {@link StrutsPrepareFilter} to have already
 * been executed in the current chain.
 */
public class StrutsExecuteFilter implements StrutsStatics, Filter {
    private PrepareOperations prepare;
    private ExecuteOperations execute;

    private FilterConfig filterConfig;

    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
    }

    protected synchronized void lazyInit() {
        if (execute == null) {
            InitOperations init = new InitOperations();
            Dispatcher dispatcher = init.findDispatcherOnThread();
            init.initStaticContentLoader(filterConfig, dispatcher);

            prepare = new PrepareOperations(filterConfig.getServletContext(), dispatcher);
            execute = new ExecuteOperations(filterConfig.getServletContext(), dispatcher);
        }

    }

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        // This is necessary since we need the dispatcher instance, which was created by the prepare filter
        lazyInit();

        ActionMapping mapping = prepare.findActionMapping(request, response);
        if (mapping == null) {
            boolean handled = execute.executeStaticResourceRequest(request, response);
            if (!handled) {
                chain.doFilter(request, response);
            }
        } else {
            execute.executeAction(request, response, mapping);
        }
    }

    public void destroy() {
        prepare = null;
        execute = null;
        filterConfig = null;
    }
}