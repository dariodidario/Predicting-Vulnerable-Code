/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.metadata.web.resources;

import com.google.common.collect.ImmutableList;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.hadoop.metadata.MetadataServiceClient;
import org.apache.hadoop.metadata.typesystem.TypesDef;
import org.apache.hadoop.metadata.typesystem.json.TypesSerialization;
import org.apache.hadoop.metadata.typesystem.json.TypesSerialization$;
import org.apache.hadoop.metadata.typesystem.types.AttributeDefinition;
import org.apache.hadoop.metadata.typesystem.types.ClassType;
import org.apache.hadoop.metadata.typesystem.types.DataTypes;
import org.apache.hadoop.metadata.typesystem.types.HierarchicalTypeDefinition;
import org.apache.hadoop.metadata.typesystem.types.Multiplicity;
import org.apache.hadoop.metadata.typesystem.types.TraitType;
import org.apache.hadoop.metadata.typesystem.types.utils.TypesUtil;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * Integration test for types jersey resource.
 */
public class TypesJerseyResourceIT extends BaseResourceIT {

    private List<HierarchicalTypeDefinition> typeDefinitions;

    @BeforeClass
    public void setUp() throws Exception {
        super.setUp();

        typeDefinitions = createHiveTypes();
    }

    @AfterClass
    public void tearDown() throws Exception {
        typeDefinitions.clear();
    }

    @Test
    public void testSubmit() throws Exception {
        for (HierarchicalTypeDefinition typeDefinition : typeDefinitions) {
            String typesAsJSON = TypesSerialization.toJson(typeDefinition);
            System.out.println("typesAsJSON = " + typesAsJSON);

            WebResource resource = service
                    .path("api/metadata/types");

            ClientResponse clientResponse = resource
                    .accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON)
                    .method(HttpMethod.POST, ClientResponse.class, typesAsJSON);
            Assert.assertEquals(clientResponse.getStatus(), Response.Status.CREATED.getStatusCode());

            String responseAsString = clientResponse.getEntity(String.class);
            Assert.assertNotNull(responseAsString);

            JSONObject response = new JSONObject(responseAsString);
            Assert.assertNotNull(response.get(MetadataServiceClient.TYPES));
            Assert.assertNotNull(response.get(MetadataServiceClient.REQUEST_ID));
        }
    }

    @Test(dependsOnMethods = "testSubmit")
    public void testGetDefinition() throws Exception {
        for (HierarchicalTypeDefinition typeDefinition : typeDefinitions) {
            System.out.println("typeName = " + typeDefinition.typeName);

            WebResource resource = service
                    .path("api/metadata/types")
                    .path(typeDefinition.typeName);

            ClientResponse clientResponse = resource
                    .accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON)
                    .method(HttpMethod.GET, ClientResponse.class);
            Assert.assertEquals(clientResponse.getStatus(), Response.Status.OK.getStatusCode());

            String responseAsString = clientResponse.getEntity(String.class);
            Assert.assertNotNull(responseAsString);
            JSONObject response = new JSONObject(responseAsString);
            Assert.assertNotNull(response.get(MetadataServiceClient.DEFINITION));
            Assert.assertNotNull(response.get(MetadataServiceClient.REQUEST_ID));

            String typesJson = response.getString(MetadataServiceClient.DEFINITION);
            final TypesDef typesDef = TypesSerialization.fromJson(typesJson);
            List<HierarchicalTypeDefinition<ClassType>> hierarchicalTypeDefinitions = typesDef.classTypesAsJavaList();
            for(HierarchicalTypeDefinition<ClassType> classType : hierarchicalTypeDefinitions) {
                for(AttributeDefinition attrDef : classType.attributeDefinitions) {
                    if("name".equals(attrDef.name)) {
                        Assert.assertEquals(attrDef.isIndexable, true);
                        Assert.assertEquals(attrDef.isUnique, true);
                    }
                }
            }
        }
    }

    @Test
    public void testGetDefinitionForNonexistentType() throws Exception {
        WebResource resource = service
                .path("api/metadata/types")
                .path("blah");

        ClientResponse clientResponse = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .method(HttpMethod.GET, ClientResponse.class);
        Assert.assertEquals(clientResponse.getStatus(), Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test(dependsOnMethods = "testSubmit")
    public void testGetTypeNames() throws Exception {
        WebResource resource = service
                .path("api/metadata/types");

        ClientResponse clientResponse = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .method(HttpMethod.GET, ClientResponse.class);
        Assert.assertEquals(clientResponse.getStatus(), Response.Status.OK.getStatusCode());

        String responseAsString = clientResponse.getEntity(String.class);
        Assert.assertNotNull(responseAsString);

        JSONObject response = new JSONObject(responseAsString);
        Assert.assertNotNull(response.get(MetadataServiceClient.REQUEST_ID));

        final JSONArray list = response.getJSONArray(MetadataServiceClient.RESULTS);
        Assert.assertNotNull(list);
    }

    @Test
    public void testGetTraitNames() throws Exception {
        String[] traitsAdded = addTraits();

        WebResource resource = service
                .path("api/metadata/types");

        ClientResponse clientResponse = resource
                .queryParam("type", DataTypes.TypeCategory.TRAIT.name())
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .method(HttpMethod.GET, ClientResponse.class);
        Assert.assertEquals(clientResponse.getStatus(), Response.Status.OK.getStatusCode());

        String responseAsString = clientResponse.getEntity(String.class);
        Assert.assertNotNull(responseAsString);

        JSONObject response = new JSONObject(responseAsString);
        Assert.assertNotNull(response.get(MetadataServiceClient.REQUEST_ID));

        final JSONArray list = response.getJSONArray(MetadataServiceClient.RESULTS);
        Assert.assertNotNull(list);
        Assert.assertTrue(list.length() >= traitsAdded.length);
    }

    private String[] addTraits() throws Exception {
        String[] traitNames = {
                "class_trait",
                "secure_trait",
                "pii_trait",
                "ssn_trait",
                "salary_trait",
                "sox_trait",
        };

        for (String traitName : traitNames) {
            HierarchicalTypeDefinition<TraitType> traitTypeDef =
                    TypesUtil.createTraitTypeDef(traitName, ImmutableList.<String>of());
            String json = TypesSerialization$.MODULE$.toJson(traitTypeDef, true);
            createType(json);
        }

        return traitNames;
    }

    private List<HierarchicalTypeDefinition> createHiveTypes() throws Exception {
        ArrayList<HierarchicalTypeDefinition> typeDefinitions = new ArrayList<>();

        HierarchicalTypeDefinition<ClassType> databaseTypeDefinition =
                TypesUtil.createClassTypeDef("database",
                        ImmutableList.<String>of(),
                        TypesUtil.createUniqueRequiredAttrDef("name", DataTypes.STRING_TYPE),
                        TypesUtil.createRequiredAttrDef("description", DataTypes.STRING_TYPE));
        typeDefinitions.add(databaseTypeDefinition);

        HierarchicalTypeDefinition<ClassType> tableTypeDefinition = TypesUtil.createClassTypeDef(
                "table",
                ImmutableList.<String>of(),
                TypesUtil.createUniqueRequiredAttrDef("name", DataTypes.STRING_TYPE),
                TypesUtil.createRequiredAttrDef("description", DataTypes.STRING_TYPE),
                TypesUtil.createRequiredAttrDef("type", DataTypes.STRING_TYPE),
                new AttributeDefinition("database",
                        "database", Multiplicity.REQUIRED, false, "database"));
        typeDefinitions.add(tableTypeDefinition);

        HierarchicalTypeDefinition<TraitType> fetlTypeDefinition = TypesUtil.createTraitTypeDef(
                "fetl",
                ImmutableList.<String>of(),
                TypesUtil.createRequiredAttrDef("level", DataTypes.INT_TYPE));
        typeDefinitions.add(fetlTypeDefinition);

        return typeDefinitions;
    }
}
