/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.systest.ws.algsuite;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.ws.algsuite.server.Server;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;

/**
 * This is a test for AlgorithmSuites. Essentially it checks that a service endpoint will
 * reject a client request that uses a different AlgorithmSuite.
 */
public class AlgorithmSuiteTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(Server.class);
    
    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(Server.class, true)
        );
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
    }
    
    @org.junit.Test
    public void testSecurityPolicy() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = AlgorithmSuiteTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = AlgorithmSuiteTest.class.getResource("DoubleItAlgSuite.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSymmetric128Port");
        
        DoubleItPortType port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // This should succeed as the client + server policies match
        port.doubleIt(25);
        
        portQName = new QName(NAMESPACE, "DoubleItSymmetric128Port2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // This should fail as the client uses Basic128Rsa15 + the server uses Basic128
        try {
            port.doubleIt(25);
            fail("Failure expected on Rsa15 AlgorithmSuite");
        } catch (Exception ex) {
            // expected
        }
        
        // This should fail as the client uses Basic256 + the server uses Basic128
        if (SecurityTestUtil.checkUnrestrictedPoliciesInstalled()) {
            portQName = new QName(NAMESPACE, "DoubleItSymmetric128Port3");
            port = service.getPort(portQName, DoubleItPortType.class);
            updateAddressPort(port, PORT);
            
            // This should fail as the client uses Basic128Rsa15 + the server uses Basic128
            try {
                port.doubleIt(25);
                fail("Failure expected on Basic256 AlgorithmSuite");
            } catch (Exception ex) {
                // expected
            }
        }

        bus.shutdown(true);
    }
    
    // TODO @Ignore'ing this test due to a recent merge from Andrei
    @org.junit.Test
    @org.junit.Ignore
    public void testCombinedPolicy() throws Exception {
        
        if (!SecurityTestUtil.checkUnrestrictedPoliciesInstalled()) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = AlgorithmSuiteTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = AlgorithmSuiteTest.class.getResource("DoubleItAlgSuite.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);

        // The client + server use Basic256 (but there is a sp:TripleDesRsa15 policy in the 
        // WSDL as well)
        QName portQName = new QName(NAMESPACE, "DoubleItSymmetricCombinedPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);

        port.doubleIt(25);
        
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testManualConfigurationEncryption() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = AlgorithmSuiteTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = AlgorithmSuiteTest.class.getResource("DoubleItAlgSuite.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItEncryptionOAEPPort");
        DoubleItPortType port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // This should succeed as the client + server settings match
        port.doubleIt(25);
        
        portQName = new QName(NAMESPACE, "DoubleItEncryptionOAEPPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // This should fail as the client uses RSA 1.5 + the server uses RSA OAEP
        try {
            port.doubleIt(25);
            fail("Failure expected on Rsa15");
        } catch (Exception ex) {
            // expected
        }
        
        // This should fail as the client uses AES-256 and the server uses AES-128
        if (SecurityTestUtil.checkUnrestrictedPoliciesInstalled()) {
            portQName = new QName(NAMESPACE, "DoubleItEncryptionOAEPPort3");
            port = service.getPort(portQName, DoubleItPortType.class);
            updateAddressPort(port, PORT);
            
            // This should fail as the client uses AES-256 and the server uses AES-128
            try {
                port.doubleIt(25);
                fail("Failure expected on AES-256");
            } catch (Exception ex) {
                // expected
            }
        }
        
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testManualConfigurationSignature() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = AlgorithmSuiteTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = AlgorithmSuiteTest.class.getResource("DoubleItAlgSuite.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSignaturePort");
        DoubleItPortType port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // This should succeed as the client + server settings match
        port.doubleIt(25);
        
        // This should fail as the client uses uses RSA-SHA256 + the server uses RSA-SHA1
        if (SecurityTestUtil.checkUnrestrictedPoliciesInstalled()) {
            portQName = new QName(NAMESPACE, "DoubleItSignaturePort2");
            port = service.getPort(portQName, DoubleItPortType.class);
            updateAddressPort(port, PORT);
            
            // This should fail as the client uses uses RSA-SHA256 + the server uses RSA-SHA1
            try {
                port.doubleIt(25);
                fail("Failure expected on SHA-256");
            } catch (Exception ex) {
                // expected
            }
        }
        
        bus.shutdown(true);
    }
    
}