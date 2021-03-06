/*
 * RADOS Java - Java bindings for librados
 *
 * Copyright (C) 2013 Wido den Hollander <wido@42on.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package com.ceph.rados;

import com.ceph.rados.Rados;
import com.ceph.rados.RadosException;
import com.ceph.rados.jna.RadosClusterInfo;
import com.ceph.rados.jna.RadosObjectInfo;
import com.ceph.rados.jna.RadosPoolInfo;
import com.ceph.rados.IoCTX;

import java.io.File;
import java.util.Arrays;
import java.util.Random;

import junit.framework.*;

import com.sun.jna.Pointer;

public final class TestRados extends TestCase {

    /**
        All these variables can be overwritten, see the setUp() method
     */
    String configFile = "/etc/ceph/ceph.conf";
    String id = "admin";
    String pool = "data";

    /**
        This test reads it's configuration from the environment
        Possible variables:
        * RADOS_JAVA_ID
        * RADOS_JAVA_CONFIG_FILE
        * RADOS_JAVA_POOL
     */
    @Override
    public void setUp() {
        if (System.getenv("RADOS_JAVA_CONFIG_FILE") != null) {
            this.configFile = System.getenv("RADOS_JAVA_CONFIG_FILE");
        }

        if (System.getenv("RADOS_JAVA_ID") != null) {
            this.id = System.getenv("RADOS_JAVA_ID");
        }

        if (System.getenv("RADOS_JAVA_POOL") != null) {
            this.pool = System.getenv("RADOS_JAVA_POOL");
        }
    }

    /**
        This test verifies if we can get the version out of librados
        It's currently hardcoded to expect at least 0.48.0
     */
    public void testGetVersion() {
        int[] version = Rados.getVersion();
        assertTrue(version[0] >= 0);
        assertTrue(version[1] >= 48);
        assertTrue(version[2] >= 0);
    }

    public void testGetConfSetGet() {
        try {
            Rados r = new Rados(this.id);

            String mon_host = "127.0.0.1";
            r.confSet("mon_host", mon_host);
            assertEquals(mon_host, r.confGet("mon_host"));

            String key = "mySuperSecretKey";
            r.confSet("key", key);
            assertEquals(key, r.confGet("key"));
        } catch (RadosException e) {
            fail(e.getMessage() + ": " + e.getReturnValue());
        }
    }

    public void testConnect() {
        try {
            Rados r = new Rados(this.id);
            r.confReadFile(new File(this.configFile));
            r.connect();
        } catch (RadosException e) {
            fail(e.getMessage() + ": " + e.getReturnValue());
        }
    }

    public void testClusterFsid() {
        try {
            Rados r = new Rados(this.id);
            r.confReadFile(new File(this.configFile));
            r.connect();
            assertNotNull("The fsid returned was null", r.clusterFsid());
        } catch (RadosException e) {
            fail(e.getMessage() + ": " + e.getReturnValue());
        }
    }

    public void testClusterStat() {
        try {
            Rados r = new Rados(this.id);
            r.confReadFile(new File(this.configFile));
            r.connect();
            RadosClusterInfo stat = r.clusterStat();
            assertTrue("Cluster size wasn't greater than 0", stat.kb > 0);
            assertTrue("KB used was not 0 or greater", stat.kb_used >= 0);
            assertTrue("KB available was not greater than 0", stat.kb_avail > 0);
            assertTrue("Number of objects was not 0 or greater", stat.num_objects >= 0);
        } catch (RadosException e) {
            fail(e.getMessage() + ": " + e.getReturnValue());
        }
    }

    public void testPoolList() {
        try {
            Rados r = new Rados(this.id);
            r.confReadFile(new File(this.configFile));
            r.connect();
            String[] pools = r.poolList();
            assertNotNull(pools);
            assertTrue("We expect at least 3 pools (data, metadata, rbd)", pools.length >= 3);
        } catch (RadosException e) {
            fail(e.getMessage() + ": " + e.getReturnValue());
        }
    }

    public void testPoolLookup() {
        try {
            Rados r = new Rados(this.id);
            r.confReadFile(new File(this.configFile));
            r.connect();
            long id = r.poolLookup(this.pool);
            assertTrue("The pool ID should be at least 0", id >= 0);

            String name = r.poolReverseLookup(id);
            assertEquals("The pool names didn't match!", this.pool, name);

        } catch (RadosException e) {
            fail(e.getMessage() + ": " + e.getReturnValue());
        }
    }

    public void testInstanceId() {
        try {
            Rados r = new Rados(this.id);
            r.confReadFile(new File(this.configFile));
            r.connect();
            long id = r.getInstanceId();
            assertTrue("The id should be greater than 0", id > 0);
        } catch (RadosException e) {
            fail(e.getMessage() + ": " + e.getReturnValue());
        }
    }

    public void testIoCtxCreateAndDestroyWithID() {
        try {
            Rados r = new Rados(this.id);
            r.confReadFile(new File(this.configFile));
            r.connect();
            IoCTX io = r.ioCtxCreate(this.pool);
            long id = io.getId();
            assertTrue("The pool ID should be at least 0", id >= 0);
            r.ioCtxDestroy(io);
        } catch (RadosException e) {
            fail(e.getMessage() + ": " + e.getReturnValue());
        }
    }

    public void testIoCtxGetSetAuid() {
        try {
            Rados r = new Rados(this.id);
            r.confReadFile(new File(this.configFile));
            r.connect();
            IoCTX io = r.ioCtxCreate(this.pool);

            /**
               We fetch the auid, try to set it to 42 and set it
               back again to the original value
            */
            long auid = io.getAuid();
            assertTrue("The auid should be at least 0", auid >= 0);

            io.setAuid(42);
            assertEquals("The auid should be 42", 42, io.getAuid());

            io.setAuid(auid);
            assertEquals("The auid should be 0", 0, io.getAuid());

            r.ioCtxDestroy(io);
        } catch (RadosException e) {
            fail(e.getMessage() + ": " + e.getReturnValue());
        }
    }

    public void testIoCtxPoolName() {
        try {
            Rados r = new Rados(this.id);
            r.confReadFile(new File(this.configFile));
            r.connect();

            IoCTX io = r.ioCtxCreate(this.pool);

            assertEquals(this.pool, io.getPoolName());

            r.ioCtxDestroy(io);
        } catch (RadosException e) {
            fail(e.getMessage() + ": " + e.getReturnValue());
        }
    }

    /**
     * This is an pretty extensive test which creates an object
     * writes data, appends, truncates verifies the written data
     * and finally removes the object
     */
    public void testIoCtxWriteListAndRead() {
        try {
            Rados r = new Rados(this.id);
            r.confReadFile(new File(this.configFile));
            r.connect();

            IoCTX io = r.ioCtxCreate(this.pool);

            /**
             * The object we will write to with the data
             */
            String oid = "rados-java";
            String content = "junit wrote this";

            io.write(oid, content);

            String[] objects = io.listObjects();
            assertTrue("We expect at least one object in the pool", objects.length > 0);

            verifyDocument(io, oid, content.getBytes());

            /**
             * We simply append the already written data
             */
            io.append(oid, content);
            assertEquals("The size doesn't match after the append", content.length()*2, io.stat(oid).getSize());

            /**
              * We now resize the object to it's original size
             */
            io.truncate(oid, content.length());
            assertEquals("The size doesn't match after the truncate", content.length(), io.stat(oid).getSize());

            io.remove(oid);

            r.ioCtxDestroy(io);
        } catch (RadosException e) {
            fail(e.getMessage() + ": " + e.getReturnValue());
        }
    }

    /**
     * This test creates an object, appends some data and removes it afterwards
     */
    public void testIoCtxWriteAndAppendBytes() {
        Rados r = null;
        IoCTX io = null;
        /**
         * The object we will write to with the data
         */
        String oid = "rados-java";

        try {
            r = new Rados(this.id);
            r.confReadFile(new File(this.configFile));
            r.connect();

            io = r.ioCtxCreate(this.pool);

            byte[] buffer = new byte[20];
            // use a fix seed so that we always get the same data
            new Random(42).nextBytes(buffer);

            io.write(oid, buffer);

            /**
             * We simply append the parts of the already written data
             */
            io.append(oid, buffer, buffer.length / 2);

            int expectedFileSize = buffer.length + buffer.length / 2;
            assertEquals("The size doesn't match after the append", expectedFileSize, io.stat(oid).getSize());

            byte[] readBuffer = new byte[expectedFileSize];
            io.read(oid, expectedFileSize, 0, readBuffer);
            for (int i = 0; i < buffer.length; i++) {
                assertEquals(buffer[i], readBuffer[i]);
            }
            for (int i = 0; i < buffer.length / 2; i++) {
                assertEquals(buffer[i], readBuffer[i + buffer.length]);
            }
        } catch (RadosException e) {
            fail(e.getMessage() + ": " + e.getReturnValue());
        } finally {
            cleanupObject(r, io, oid);
        }
    }

    /**
     * Use IOContext.writeFull to create a new object, than again writeFull with less data and verify
     * that the file was truncated.
     */
    public void testIoCtxWriteFull() throws Exception {
        /**
         * The object we will write to with the data
         */
        Rados r = null;
        IoCTX io = null;
        String oid = "rados-java_writeFull";
        byte[] content = "junit wrote this".getBytes();

        try {
            r = new Rados(this.id);
            r.confReadFile(new File(this.configFile));
            r.connect();
            io = r.ioCtxCreate(this.pool);

            io.writeFull(oid, content, content.length);

            String[] objects = io.listObjects();
            assertTrue("We expect at least one object in the pool", objects.length > 0);

            verifyDocument(io, oid, content);

            // only write the first 4 bytes
            io.writeFull(oid, content, 4);
            assertEquals("The size doesn't match after the smaller writeFull", 4, io.stat(oid).getSize());

            verifyDocument(io, oid, Arrays.copyOf(content, 4));
        } catch (RadosException e) {
            fail(e.getMessage() + ": " + e.getReturnValue());
        }
        finally {
            cleanupObject(r, io, oid);
        }
    }

    private void verifyDocument(IoCTX io, String oid, byte[] content) throws RadosException {
        byte[] buf = new byte[content.length];
        int len = io.read(oid, content.length, 0, buf);
        assertEquals(len, content.length);
        RadosObjectInfo info = io.stat(oid);

        assertEquals("The object names didn't match", oid, info.getOid());
        assertEquals("The size of what we wrote doesn't match with the stat", content.length, info.getSize());
        assertTrue("The content we read was different from what we wrote", Arrays.equals(content, buf));

        long now = System.currentTimeMillis()/1000;
        assertFalse("The mtime was in the future", now < info.getMtime());
    }

    private void cleanupObject(Rados r, IoCTX io, String oid) {
        try {
            if(r != null) {
                if(io != null) {
                    io.remove(oid);
                }

                r.ioCtxDestroy(io);
            }
        }
        catch (RadosException e) {
        }
    }

    public void testIoCtxPoolStat() {
        try {
            Rados r = new Rados(this.id);
            r.confReadFile(new File(this.configFile));
            r.connect();

            IoCTX io = r.ioCtxCreate(this.pool);

            RadosPoolInfo info = io.poolStat();
            assertTrue(info.num_objects >= 0);

            r.ioCtxDestroy(io);
        } catch (RadosException e) {
            fail(e.getMessage() + ": " + e.getReturnValue());
        }
    }

    public void testIoCtxSnapshot() {
        try {

            String snapname = "my-new-snapshot";

            Rados r = new Rados(this.id);
            r.confReadFile(new File(this.configFile));
            r.connect();

            IoCTX io = r.ioCtxCreate(this.pool);

            io.snapCreate(snapname);

            long snapid = io.snapLookup(snapname);
            long time = io.snapGetStamp(snapid);
            String snapnamebuf = io.snapGetName(snapid);

            Long[] snaps = io.snapList();

            io.snapRemove(snapname);

            assertTrue("There should at least be one snapshot", snaps.length >= 1);
            assertEquals("The snapshot names didn't match", snapname, snapnamebuf);

            long now = System.currentTimeMillis()/1000;
            /* Add 5 seconds to deal with clock differences */
            assertTrue("The timestamp was in the future. Clocks synced?", (now + 5) >= time);

            r.ioCtxDestroy(io);
        } catch (RadosException e) {
            fail(e.getMessage() + ": " + e.getReturnValue());
        }
    }

    public void testListPartial() {
        /**
         * The object we will write to with the data
         */
        Rados r = null;
        IoCTX io = null;
        String oid = "rados-java_item_";
        String content = "junit wrote this ";
        int nb = 100;
        
        try {
            r = new Rados(this.id);
            r.confReadFile(new File(this.configFile));
            r.connect();
            io = r.ioCtxCreate(this.pool);
            System.out.println("Start");
            for (int i = 0; i < nb; i++) {
                byte []bytes = (content + i).getBytes();
                io.writeFull(oid+i, bytes, bytes.length);
            }

            String [] allOids = io.listObjects();
            assertTrue("Global number of items should be " + nb, allOids.length == nb);
            
            // Check reading all items in 10 parts
            ListCtx listCtx = io.listObjectsPartial(nb/10);
            assertTrue("We expect the list to have right now a size of 0", listCtx.size() == 0);
            int totalRead = 0;
            int subnb = 0;
            for (int i = 0; i < 10; i++) {
                subnb = listCtx.nextObjects();
                totalRead += subnb;
                assertTrue("We expect the list to have right now a size of " + (nb / 10), listCtx.size() == nb / 10);
                assertTrue("We expect to have a correct oid", listCtx.getObjects()[0].startsWith(oid));
                String []oids = listCtx.getObjects();
                assertTrue("We expect the subset to have right now a size of " + (nb / 10), oids.length == nb / 10);
            }
            subnb = listCtx.nextObjects();
            assertTrue("We expect the list to have right now a size of " + 0, listCtx.size() == 0);
            totalRead += subnb;
            assertTrue("We expect the number of read items to be " + nb, totalRead == nb);
            listCtx.close();
            
            // Check reading half items in 5 parts (other half being ignored)
            listCtx = io.listObjectsPartial(nb/10);
            assertTrue("We expect the list to have right now a size of 0", listCtx.size() == 0);
            totalRead = 0;
            for (int i = 0; i < 5; i++) {
                subnb = listCtx.nextObjects(nb / 10);
                totalRead += subnb + (nb / 10);
                assertTrue("We expect the list to have right now a size of " + (nb / 10), listCtx.size() == nb / 10);
                assertTrue("We expect to have a correct oid", listCtx.getObjects()[0].startsWith(oid));
                String []oids = listCtx.getObjects();
                assertTrue("We expect the subset to have right now a size of " + (nb / 10), oids.length == nb / 10);
            }
            subnb = listCtx.nextObjects();
            assertTrue("We expect the list to have right now a size of " + 0, listCtx.size() == 0);
            totalRead += subnb;
            assertTrue("We expect the number of read items to be " + nb, totalRead == nb);
            listCtx.close();

            // Check reading some items then close and then check listCtx is empty
            listCtx = io.listObjectsPartial(nb/10);
            assertTrue("We expect the list to have right now a size of 0", listCtx.size() == 0);
            totalRead = 0;
            subnb = listCtx.nextObjects(nb / 10);
            assertTrue("We expect the list to have right now a size of " + (nb / 10), listCtx.size() == nb / 10);
            assertTrue("We expect to have a correct oid", listCtx.getObjects()[0].startsWith(oid));
            listCtx.close();
            subnb = listCtx.nextObjects();
            assertTrue("We expect the list to have right now a size of " + 0, listCtx.size() == 0);
        } catch (RadosException e) {
            fail(e.getMessage() + ": " + e.getReturnValue());
        }
        finally {
            try {
                if(r != null) {
                    if(io != null) {
                        for (int i = 0; i < nb; i++) {
                            io.remove(oid+i);
                        }
                    }
                    r.ioCtxDestroy(io);
                }
            }
            catch (RadosException e) {
            }
        }
    }

    static class RadosFinalizeTest extends Rados {

        public RadosFinalizeTest(String id) {
            super(id);
            // System.err.println(String.format("Initialized with clusterptr: %x, %s", Pointer.nativeValue(this.clusterPtr), this.toString()));
        }

        @Override
        public void finalize() throws Throwable {
            assertTrue(Pointer.nativeValue(this.clusterPtr) > 0);
            // System.err.println(String.format("Finalizing with clusterptr: %x, %s", Pointer.nativeValue(this.clusterPtr), this.toString()));
            super.finalize();
        }
    }

    public void testRadosFinalization() {
        for (int i = 0; i < 10; i++) {
            RadosFinalizeTest r = new RadosFinalizeTest(this.id);
            try {
                r.confReadFile(new File(this.configFile));
                r.connect();
            } catch (RadosException e) {
                fail();
            }

            r = null;
            System.gc();
            System.runFinalization();
        }
    }
}
