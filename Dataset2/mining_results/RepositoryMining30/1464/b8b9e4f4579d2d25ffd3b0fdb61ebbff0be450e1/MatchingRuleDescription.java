/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */

package org.apache.directory.shared.ldap.schema.parsers;

import org.apache.directory.shared.ldap.schema.SchemaObject;
import org.apache.directory.shared.ldap.schema.SchemaObjectType;


/**
 * RFC 4512 - 4.1.3. Matching Rule Description
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MatchingRuleDescription extends SchemaObject
{
    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** The matching rule's Syntax element */
    private String syntax;

    /**
     * Creates a new instance of MatcfhingRuleDescription
     */
    public MatchingRuleDescription( String oid )
    {
        super(  SchemaObjectType.MATCHING_RULE, oid );
    }


    /**
     * @return The matchingRule's syntax description
     */
    public String getSyntax()
    {
        return syntax;
    }


    /**
     * Set the matchingRule's syntax description
     *
     * @param syntax The description
     */
    public void setSyntax( String syntax )
    {
        this.syntax = syntax;
    }
}
