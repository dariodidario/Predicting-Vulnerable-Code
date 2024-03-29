/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.commons.compress.zip;

import java.util.zip.ZipException;
import junit.framework.TestCase;

/**
 * JUnit testcases AsiExtraField.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class AsiExtraFieldTestCase
    extends TestCase
    implements UnixStat
{
    public AsiExtraFieldTestCase( final String name )
    {
        super( name );
    }

    /**
     * Test file mode magic.
     */
    public void testModes()
    {
        final AsiExtraField field = new AsiExtraField();
        field.setMode( 0123 );
        assertEquals( "plain file", 0100123, field.getMode() );
        field.setDirectory( true );
        assertEquals( "directory", 040123, field.getMode() );
        field.setLinkedFile( "test" );
        assertEquals( "symbolic link", 0120123, field.getMode() );
    }

    private AsiExtraField createField()
    {
        final AsiExtraField field = new AsiExtraField();
        field.setMode( 0123 );
        field.setUserID( 5 );
        field.setGroupId( 6 );
        return field;
    }

    public void testContent1()
    {
        final AsiExtraField field = createField();
        final byte[] data = field.getLocalFileDataData();

        // CRC manually calculated, sorry
        final byte[] expect = {(byte)0xC6, 0x02, 0x78, (byte)0xB6, // CRC
                               0123, (byte)0x80, // mode
                               0, 0, 0, 0, // link length
                               5, 0, 6, 0};                        // uid, gid
        assertEquals( "no link", expect.length, data.length );
        for( int i = 0; i < expect.length; i++ )
        {
            assertEquals( "no link, byte " + i, expect[ i ], data[ i ] );
        }

        field.setLinkedFile( "test" );
    }

    public void testContent2()
    {
        final AsiExtraField field = createField();
        field.setLinkedFile( "test" );

        final byte[] data = field.getLocalFileDataData();
        final byte[] expect = new byte[]{0x75, (byte)0x8E, 0x41, (byte)0xFD, // CRC
                                         0123, (byte)0xA0, // mode
                                         4, 0, 0, 0, // link length
                                         5, 0, 6, 0, // uid, gid
                                         (byte)'t', (byte)'e', (byte)'s', (byte)'t'};
        assertEquals( "no link", expect.length, data.length );
        for( int i = 0; i < expect.length; i++ )
        {
            assertEquals( "no link, byte " + i, expect[ i ], data[ i ] );
        }

    }

    public void testReparse1()
        throws ZipException
    {
        // CRC manually calculated, sorry
        final byte[] data = {(byte)0xC6, 0x02, 0x78, (byte)0xB6, // CRC
                             0123, (byte)0x80, // mode
                             0, 0, 0, 0, // link length
                             5, 0, 6, 0};                        // uid, gid
        final AsiExtraField field = new AsiExtraField();
        field.parseFromLocalFileData( data, 0, data.length );

        assertEquals( "length plain file", data.length,
                      field.getLocalFileDataLength().getValue() );
        assertTrue( "plain file, no link", !field.isLink() );
        assertTrue( "plain file, no dir", !field.isDirectory() );
        assertEquals( "mode plain file", FILE_FLAG | 0123, field.getMode() );
        assertEquals( "uid plain file", 5, field.getUserID() );
        assertEquals( "gid plain file", 6, field.getGroupID() );
    }

    public void testReparse2()
        throws ZipException
    {
        final byte[] data = new byte[]{0x75, (byte)0x8E, 0x41, (byte)0xFD, // CRC
                                       0123, (byte)0xA0, // mode
                                       4, 0, 0, 0, // link length
                                       5, 0, 6, 0, // uid, gid
                                       (byte)'t', (byte)'e', (byte)'s', (byte)'t'};
        final AsiExtraField field = new AsiExtraField();
        field.parseFromLocalFileData( data, 0, data.length );
        assertEquals( "length link", data.length,
                      field.getLocalFileDataLength().getValue() );
        assertTrue( "link, is link", field.isLink() );
        assertTrue( "link, no dir", !field.isDirectory() );
        assertEquals( "mode link", LINK_FLAG | 0123, field.getMode() );
        assertEquals( "uid link", 5, field.getUserID() );
        assertEquals( "gid link", 6, field.getGroupID() );
        assertEquals( "test", field.getLinkedFile() );
    }

    public void testReparse3()
        throws ZipException
    {
        final byte[] data = new byte[]{(byte)0x8E, 0x01, (byte)0xBF, (byte)0x0E, // CRC
                                       0123, (byte)0x40, // mode
                                       0, 0, 0, 0, // link
                                       5, 0, 6, 0};                          // uid, gid
        final AsiExtraField field = new AsiExtraField();
        field.parseFromLocalFileData( data, 0, data.length );
        assertEquals( "length dir", data.length,
                      field.getLocalFileDataLength().getValue() );
        assertTrue( "dir, no link", !field.isLink() );
        assertTrue( "dir, is dir", field.isDirectory() );
        assertEquals( "mode dir", DIR_FLAG | 0123, field.getMode() );
        assertEquals( "uid dir", 5, field.getUserID() );
        assertEquals( "gid dir", 6, field.getGroupID() );
    }

    public void testReparse4()
        throws Exception
    {
        final byte[] data = new byte[]{0, 0, 0, 0, // bad CRC
                                       0123, (byte)0x40, // mode
                                       0, 0, 0, 0, // link
                                       5, 0, 6, 0};                          // uid, gid
        final AsiExtraField field = new AsiExtraField();
        try
        {
            field.parseFromLocalFileData( data, 0, data.length );
            fail( "should raise bad CRC exception" );
        }
        catch( Exception e )
        {
            assertEquals( "bad CRC checksum 0 instead of ebf018e",
                          e.getMessage() );
        }
    }
}
