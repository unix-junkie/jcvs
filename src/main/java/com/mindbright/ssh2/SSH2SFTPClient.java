/******************************************************************************
 *
 * Copyright (c) 1999-2001 AppGate AB. All Rights Reserved.
 * 
 * This file contains Original Code and/or Modifications of Original Code as
 * defined in and that are subject to the MindTerm Public Source License,
 * Version 1.3, (the 'License'). You may not use this file except in compliance
 * with the License.
 * 
 * You should have received a copy of the MindTerm Public Source License
 * along with this software; see the file LICENSE.  If not, write to
 * AppGate AB, Stora Badhusgatan 18-20, 41121 Goteborg, SWEDEN
 *
 *****************************************************************************/

package com.mindbright.ssh2;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.util.Hashtable;
import java.util.Enumeration;

import com.mindbright.util.Queue;

public final class SSH2SFTPClient extends SSH2SFTP {

    private class ReplyLock {
	protected int        expectType;
	protected SFTPPacket replyPkt;

	protected ReplyLock(int expectType) {
	    this.expectType = expectType;
	    this.replyPkt   = null;
	}

	protected synchronized SFTPPacket expect() throws SFTPException {
	    while(replyPkt == null) {
		try {
		    this.wait();
		} catch (InterruptedException e) {
		}
	    }
	    checkType(replyPkt, expectType);
	    return replyPkt;
	}

	protected synchronized void received(SFTPPacket replyPkt) {
	    this.replyPkt = replyPkt;
	    this.notify();
	}

	protected synchronized void cancel() {
	    this.replyPkt = createPacket(SSH_FXP_STATUS);
	    this.replyPkt.writeInt(SSH_FX_CONNECTION_LOST);
	    this.notify();
	}

    }

    private class WriteReplyLock extends ReplyLock {

	private FileHandle handle;
	private int        len;

	protected WriteReplyLock(FileHandle handle, int len) {
	    super(SSH_FXP_STATUS);
	    this.handle = handle;
	    this.len    = len;
	    handle.asyncStart(len);
	}

	protected synchronized void received(SFTPPacket replyPkt) {
	    try {
		if(!handle.isOpen()) {
		    /* Ignore and discard packets after close */
		    return;
		}
		checkType(replyPkt, expectType);
		handle.asyncEnd(len);
	    } catch (SFTPException e) {
		handle.asyncException(e);
	    }
	    releasePacket(replyPkt);
	}

	protected synchronized void cancel() {
	    handle.asyncException(new SFTPDisconnectException());
	    this.notify();
	}

    }

    private class ReadReplyLock extends ReplyLock {

	private FileHandle handle;
	private long       fileOffset;
	private byte[]     buf;
	private int        off;
	private int        len;

	private RandomAccessFile fileTarget;
	private OutputStream     strmTarget;

	private ReadReplyLock(FileHandle handle, long fileOffset, int len) {
	    super(SSH_FXP_DATA);
	    this.handle     = handle;
	    this.fileOffset = fileOffset;
	    this.len        = len;
	    handle.asyncStart(len);
	}

	protected ReadReplyLock(FileHandle handle, long fileOffset,
				OutputStream strmTarget, int len) {
	    this(handle, fileOffset, len);
	    this.strmTarget = strmTarget;
	}

	protected ReadReplyLock(FileHandle handle, long fileOffset,
				RandomAccessFile fileTarget, int len) {
	    this(handle, fileOffset, len);
	    this.fileTarget = fileTarget;
	}

	protected ReadReplyLock(FileHandle handle, long fileOffset,
		      byte[] buf, int off, int len) {
	    this(handle, fileOffset, len);
	    this.buf        = buf;
	    this.off        = off;
	}

	protected synchronized void received(SFTPPacket replyPkt) {
	    try {
		int n;
		if(!handle.isOpen()) {
		    /* Ignore and discard packets after close */
		    return;
		}
		checkType(replyPkt, expectType);
		if(fileTarget != null) {
		    n = replyPkt.readInt();
		    fileTarget.seek(fileOffset);
		    fileTarget.write(replyPkt.getData(), replyPkt.getRPos(), n);
		} else if(strmTarget != null) {
		    if(handle.lastOffset != fileOffset) {
			handle.asyncException(new SFTPException(
				"Out of order packets can't be handled yet!"));
		    }
		    n = replyPkt.readInt();
		    strmTarget.write(replyPkt.getData(), replyPkt.getRPos(), n);
		    handle.lastOffset = fileOffset + n;
		} else {
		    n = replyPkt.readString(buf, off);
		}
		if(n < len) {
		    resend(replyPkt, n);
		} else {
		    handle.asyncEnd(len);
		    releasePacket(replyPkt);
		}
	    } catch (IOException e) {
		handle.asyncException(new SFTPException(e.getMessage()));
	    } catch (SFTPEOFException e) {
		handle.asyncReadEOF();
	    } catch (SFTPException e) {
		handle.asyncException(e);
	    }
	}

	private void resend(SFTPPacket pkt, int n) {
	    int     i  = getNextId();
	    Integer id = new Integer(i);

	    fileOffset += n;
	    len        -= n;
	    off        += n;
	    pkt.reset(SSH_FXP_READ, i);
	    pkt.writeString(handle.getHandle());
	    pkt.writeLong(fileOffset);
	    pkt.writeInt(len);
	    replyLocks.put(id, this);
	    txQueue.putLast(pkt);
	}

	protected synchronized void cancel() {
	    handle.asyncException(new SFTPDisconnectException());
	    this.notify();
	}

    }

    private final static int   POOL_SIZE = 16;

    private SSH2Connection     connection;
    private SSH2SessionChannel session;
    private Queue              txQueue;

    private int                id;
    private int                version;
    private boolean            isBlocking;
    private boolean            isOpen;

    private Hashtable          replyLocks;

    private SFTPPacket[]       pktPool;
    private int                pktPoolCnt;

    public SSH2SFTPClient(SSH2Connection connection, boolean isBlocking)
	throws SFTPException
    {
	this.connection = connection;
	this.id         = 0;
	this.isBlocking = isBlocking;

	this.restart();

	// INIT pkt don't have an id but version is in same place
	//
	SFTPPacket pkt = createPacket();
	pkt.reset(SSH_FXP_INIT, SSH_FILEXFER_VERSION);
	pkt.writeTo(session.stdin);
	pkt.reset();
	pkt.readFrom(session.stdout);
	checkType(pkt, SSH_FXP_VERSION);
	version = pkt.readInt();
	releasePacket(pkt);

	if(!isBlocking) {
	    startNonblocking();
	}
    }

    public void terminate() {
	isOpen = false;
	if(session != null) {
	    session.close();
	}
	cancelAllAsync();
	session = null;
	if(pktPool != null) {
	    // Be nice to the GC
	    for(int i = 0; i < POOL_SIZE; i++) {
		pktPool[i] = null;
	    }
	}
	pktPoolCnt = 0;
    }

    public void restart() throws SFTPException {
	terminate();
	session = connection.newSession();
	if(!session.doSubsystem("sftp")) {
	    // !!! TODO: fix
	    throw new SFTPException("sftp subsystem couldn't be started on server");
	}
	isOpen = true;
	pktPool = new SFTPPacket[POOL_SIZE];
	pktPoolCnt = POOL_SIZE;
	for(int i = 0; i < POOL_SIZE; i++) {
	    pktPool[i] = new SFTPPacket();
	}
    }

    public FileHandle open(String name, int flags, FileAttributes attrs)
	throws SFTPException
    {
	SFTPPacket pkt = createPacket(SSH_FXP_OPEN);
	pkt.writeString(name);
	pkt.writeInt(flags);
	pkt.writeAttrs(attrs);

	pkt = transmitExpectReply(pkt, SSH_FXP_HANDLE);
	FileHandle handle = new FileHandle(name, pkt.readString(), false);
	releasePacket(pkt);
	return handle;
    }

    public void close(FileHandle handle) throws SFTPException {
	SFTPPacket pkt = createPacket(SSH_FXP_CLOSE, handle);
	pkt = transmitExpectReply(pkt, SSH_FXP_STATUS);
	releasePacket(pkt);
	handle.asyncClose();
    }

    public int read(FileHandle handle, long fileOffset,
		    RandomAccessFile fileTarget, int len)
	throws SFTPException, IOException
    {
	if(!handle.isOpen()) {
	    throw new SFTPAsyncAbortException();
	}
	SFTPPacket pkt = createPacket(SSH_FXP_READ, handle);
	pkt.writeLong(fileOffset);
	pkt.writeInt(len);

	if(isBlocking) {
	    try {
		pkt = transmitExpectReply(pkt, SSH_FXP_DATA);
		len = pkt.readInt();
		fileTarget.seek(fileOffset);
		fileTarget.write(pkt.getData(), pkt.getRPos(), len);
		return len;
	    } catch (SFTPEOFException e) {
		return 0;
	    } finally {
		if(pkt != null)
		    releasePacket(pkt);
	    }
	} else {
	    Integer   id    = new Integer(pkt.getId());
	    ReplyLock reply = new ReadReplyLock(handle, fileOffset, fileTarget,
						len);
	    replyLocks.put(id, reply);
	    txQueue.putLast(pkt);
	    return len;
	}
    }

    public int read(FileHandle handle, long fileOffset,
		    byte[] buf, int off, int len)
	throws SFTPException
    {
	if(!handle.isOpen()) {
	    throw new SFTPAsyncAbortException();
	}
	SFTPPacket pkt = createPacket(SSH_FXP_READ, handle);
	pkt.writeLong(fileOffset);
	pkt.writeInt(len);

	if(isBlocking) {
	    try {
		pkt = transmitExpectReply(pkt, SSH_FXP_DATA);
		return pkt.readString(buf, off);
	    } catch (SFTPEOFException e) {
		return 0;
	    } finally {
		if(pkt != null)
		    releasePacket(pkt);
	    }
	} else {
	    if(!isOpen) {
		throw new SFTPDisconnectException();
	    }
	    Integer   id    = new Integer(pkt.getId());
	    ReplyLock reply = new ReadReplyLock(handle, fileOffset,
						buf, off, len);
	    replyLocks.put(id, reply);
	    txQueue.putLast(pkt);
	    return -1;
	}
    }

    public int readFully(FileHandle handle, OutputStream out)
	throws SFTPException, IOException
    {
	if(!handle.isOpen()) {
	    throw new SFTPAsyncAbortException();
	}
	FileAttributes attrs = fstat(handle);

	int len   = (int)attrs.size;
	int foffs = 0;
	int cnt   = 0;

	try {
	    while(foffs < len) {
		int n = (32768 < (len - foffs) ? 32768 :
			 (int)(len - foffs));

		SFTPPacket pkt = createPacket(SSH_FXP_READ, handle);
		pkt.writeLong(foffs);
		pkt.writeInt(n);

		if(isBlocking) {
		    try {
			pkt = transmitExpectReply(pkt, SSH_FXP_DATA);
			n = pkt.readInt();
			out.write(pkt.getData(), pkt.getRPos(), n);
		    } finally {
			if(pkt != null)
			    releasePacket(pkt);
		    }
		} else {
		    Integer   id    = new Integer(pkt.getId());
		    ReplyLock reply = new ReadReplyLock(handle, foffs, out, n);
		    replyLocks.put(id, reply);
		    txQueue.putLast(pkt);
		}

		foffs += n;

		if(!isBlocking && ++cnt == 24) {
		    cnt = 0;
		    handle.asyncWait(12);
		}
	    }

	    if(!isBlocking) {
		handle.asyncWait();
	    }

	} finally {
	    close(handle);
	}

	return (int)attrs.size;
    }

    protected void writeInternal(FileHandle handle, SFTPPacket pkt, int len)
	throws SFTPException
    {
	if(isBlocking) {
	    pkt = transmitExpectReply(pkt, SSH_FXP_STATUS);
	    releasePacket(pkt);
	} else {
	    if(!isOpen) {
		throw new SFTPDisconnectException();
	    }
	    Integer   id    = new Integer(pkt.getId());
	    ReplyLock reply = new WriteReplyLock(handle, len);
	    replyLocks.put(id, reply);
	    txQueue.putLast(pkt);
	}
    }

    public void write(FileHandle handle, long fileOffset,
		      byte[] buf, int off, int len)
	throws SFTPException
    {
	if(!handle.isOpen()) {
	    throw new SFTPAsyncAbortException();
	}
	SFTPPacket pkt = createPacket(SSH_FXP_WRITE, handle);
	pkt.writeLong(fileOffset);
	pkt.writeString(buf, off, len);

	writeInternal(handle, pkt, len);
    }

    public int writeFully(FileHandle handle, InputStream in)
	throws SFTPException, IOException
    {
	if(!handle.isOpen()) {
	    throw new SFTPAsyncAbortException();
	}

	int len   = 0;
	int foffs = 0;
	int cnt   = 0;
	int lPos  = 0;

	try {
	    while(true) {
		SFTPPacket pkt = createPacket(SSH_FXP_WRITE, handle);
		pkt.writeLong(foffs);
		lPos = pkt.getWPos();
		pkt.writeInt(0);

		int n = pkt.getMaxWriteSize();
		n = (n > 32768 ? 32768 : n);

		len = in.read(pkt.getData(), pkt.getWPos(), n);

		if(len > 0) {
		    pkt.setWPos(lPos);
		    pkt.writeInt(len);
		    pkt.setWPos(lPos + 4 + len);
		    writeInternal(handle, pkt, len);
		    foffs += len;
		    if(!isBlocking && ++cnt == 24) {
			cnt = 0;
			handle.asyncWait(12);
		    }

		    // !!! REMOVE, seems to choke scheduler (in linux anyway...)
		    //
		    if((cnt % 6 == 1))
			Thread.yield();

		} else {
		    break;
		}
	    }

	    if(!isBlocking) {
		handle.asyncWait();
	    }

	} finally {
	    close(handle);
	}

	return foffs;
    }

    public FileAttributes lstat(String name)
	throws SFTPException
    {
	SFTPPacket pkt = createPacket(SSH_FXP_LSTAT);
	pkt.writeString(name);

	return statInternal(pkt);
    }

    public FileAttributes stat(String name)
	throws SFTPException
    {
	SFTPPacket pkt = createPacket(SSH_FXP_STAT);
	pkt.writeString(name);

	return statInternal(pkt);
    }

    public FileAttributes fstat(FileHandle handle)
	throws SFTPException
    {
	SFTPPacket pkt = createPacket(SSH_FXP_FSTAT, handle);

	return statInternal(pkt);
    }

    private FileAttributes statInternal(SFTPPacket pkt) throws SFTPException {
	pkt = transmitExpectReply(pkt, SSH_FXP_ATTRS);
	FileAttributes attrs = pkt.readAttrs();
	releasePacket(pkt);
	return attrs;
    }

    public void setstat(String name, FileAttributes attrs)
	throws SFTPException
    {
	SFTPPacket pkt = createPacket(SSH_FXP_SETSTAT);
	pkt.writeString(name);
	pkt.writeAttrs(attrs);

	pkt = transmitExpectReply(pkt, SSH_FXP_STATUS);
	releasePacket(pkt);
    }

    public void fsetstat(FileHandle handle, FileAttributes attrs)
	throws SFTPException
    {
	SFTPPacket pkt = createPacket(SSH_FXP_FSETSTAT, handle);

	pkt.writeAttrs(attrs);

	pkt = transmitExpectReply(pkt, SSH_FXP_STATUS);
	releasePacket(pkt);
    }

    public FileHandle opendir(String path)
	throws SFTPException
    {
	SFTPPacket pkt = createPacket(SSH_FXP_OPENDIR);
	pkt.writeString(path);

	pkt = transmitExpectReply(pkt, SSH_FXP_HANDLE);
	FileHandle handle = new FileHandle(path, pkt.readString(), true);
	releasePacket(pkt);
	return handle;
    }

    public FileAttributes[] readdir(FileHandle handle)
	throws SFTPException
    {
	SFTPPacket pkt = createPacket(SSH_FXP_READDIR, handle);
	
	pkt = transmitExpectReply(pkt, SSH_FXP_NAME);

	int count = pkt.readInt();
	FileAttributes[] list = new FileAttributes[count];

	for(int i = 0; i < count; i++) {
	    String name  = new String(pkt.readString());
	    String lname = new String(pkt.readString());
	    list[i] = pkt.readAttrs();
	    list[i].name  = name;
	    list[i].lname = lname;
	    list[i].hasName = true;
	}
	releasePacket(pkt);

	return list;
    }

    public void remove(String name) throws SFTPException {
	SFTPPacket pkt = createPacket(SSH_FXP_REMOVE);
	pkt.writeString(name);

	pkt = transmitExpectReply(pkt, SSH_FXP_STATUS);
	releasePacket(pkt);
    }

    public void rename(String oldName, String newName)
	throws SFTPException
    {
	SFTPPacket pkt = createPacket(SSH_FXP_RENAME);
	pkt.writeString(oldName);
	pkt.writeString(newName);

	pkt = transmitExpectReply(pkt, SSH_FXP_STATUS);
	releasePacket(pkt);
    }

    public void mkdir(String name, FileAttributes attrs)
	throws SFTPException
    {
	SFTPPacket pkt = createPacket(SSH_FXP_MKDIR);
	pkt.writeString(name);
	pkt.writeAttrs(attrs);

	pkt = transmitExpectReply(pkt, SSH_FXP_STATUS);
	releasePacket(pkt);
    }

    public void rmdir(String name) throws SFTPException {
	SFTPPacket pkt = createPacket(SSH_FXP_RMDIR);
	pkt.writeString(name);

	pkt = transmitExpectReply(pkt, SSH_FXP_STATUS);
	releasePacket(pkt);
    }

    public FileAttributes realpath(String nameIn)
	throws SFTPException
    {
	SFTPPacket pkt = createPacket(SSH_FXP_REALPATH);
	pkt.writeString(nameIn);

	pkt = transmitExpectReply(pkt, SSH_FXP_NAME);
	int            cnt   = pkt.readInt(); // Allways one
	String         name  = new String(pkt.readString());
	String         lname = new String(pkt.readString());
	FileAttributes attrs = pkt.readAttrs();
	attrs.name    = name;
	attrs.lname   = lname;
	attrs.hasName = true;
	releasePacket(pkt);

	return attrs;
    }

    private SFTPPacket transmitExpectReply(SFTPPacket pkt, int expectType)
	throws SFTPException
    {
	if(!isOpen) {
	    throw new SFTPDisconnectException();
	}
	if(isBlocking) {
	    synchronized(this) {
		int expectId = pkt.getId();
		pkt.writeTo(session.stdin);
		pkt.reset();
		pkt.readFrom(session.stdout);
		if(expectId != pkt.readInt()) {
		    throw new SFTPException("SFTP error, invalid packet id");
		}
		checkType(pkt, expectType);
		return pkt;
	    }
	} else {
	    Integer   id    = new Integer(pkt.getId());
	    ReplyLock reply = new ReplyLock(expectType);
	    replyLocks.put(id, reply);
	    txQueue.putLast(pkt);
	    return reply.expect();
	}
    }

    private void startNonblocking() {
	txQueue    = new Queue();
	replyLocks = new Hashtable();
	/*
	 * NOTE: We could implement custom I/O streams and insert them to handle
	 * the work in the tx/rx threads of the SSH2StreamChannel though the
	 * gains are not huge.
	 */
	Thread transmitter = new Thread(new Runnable() {
		public void run() {
		    sftpTransmitLoop();
		}
	    }, "SSH2SFTPTX");
	Thread receiver = new Thread(new Runnable() {
		public void run() {
		    sftpReceiveLoop();
		}
	    }, "SSH2SFTPRX");

	transmitter.setDaemon(true);
	receiver.setDaemon(true);
	transmitter.start();
	receiver.start();
    }

    private void sftpTransmitLoop() {
	SFTPPacket pkt;
	try {
	    while((pkt = (SFTPPacket)txQueue.getFirst()) != null) {
		pkt.writeTo(session.stdin);
		releasePacket(pkt);
	    }
	} catch (SFTPException e) {
	    connection.getLog().error("SSH2SFTPClient",
				      "sftpTransmitLoop",
				      "session was probably closed");
	    terminate();
	}
    }

    private void sftpReceiveLoop() {
	SFTPPacket pkt;
	Integer    id;
	ReplyLock  reply;
	try {
	    while(true) {
		pkt = createPacket();

		pkt.reset();
		pkt.readFrom(session.stdout);

		id    = new Integer(pkt.readInt());
		reply = (ReplyLock)replyLocks.remove(id);
		if(reply == null) {
		    connection.getLog().error("SSH2SFTPClient",
					      "sftpReceiveLoop",
					      "received unsent id: " +
					      id);
		    connection.getLog().debug2("SSH2SFTPClient",
					      "sftpReceiveLoop",
					       "sftp packet: ",
					       pkt.getData(),
					       0,
					       pkt.getLength() + 5);
		    throw new SFTPException("Invalid reply");
		}

		reply.received(pkt);
	    }
	} catch (SFTPDisconnectException e) {
	    connection.getLog().debug("SSH2SFTPClient",
				      "sftpReceiveLoop",
				      "session was closed");
	} catch (SFTPException e) {
	    connection.getLog().error("SSH2SFTPClient",
				      "sftpReceiveLoop",
				      "session was probably closed");
	} finally {
	    terminate();
	}
    }

    private SFTPPacket createPacket(int type, FileHandle handle) {
	SFTPPacket pkt = createPacket(type);
	pkt.writeString(handle.getHandle());
	return pkt;
    }

    private SFTPPacket createPacket(int type) {
	SFTPPacket pkt = createPacket();
	pkt.reset(type, getNextId());
	return pkt;
    }

    private SFTPPacket createPacket() {
	SFTPPacket pkt;
	synchronized(pktPool) {
	    if(pktPoolCnt == 0) {
		pkt = new SFTPPacket();
	    } else {
		pkt = pktPool[--pktPoolCnt];
	    }
	}
	return pkt;
    }

    private void releasePacket(SFTPPacket pkt) {
	synchronized(pktPool) {
	    if(pktPoolCnt < POOL_SIZE) {
		pktPool[pktPoolCnt++] = pkt;
	    }
	}
    }

    private void checkType(SFTPPacket pkt, int type) throws SFTPException {
	if(pkt.getType() == SSH_FXP_STATUS) {
	    int error = pkt.readInt();
	    if(error == SSH_FX_OK)
		return;
	    if(error == SSH_FX_EOF)
		throw new SFTPEOFException();
	    if(error == SSH_FX_NO_SUCH_FILE)
		throw new SFTPNoSuchFileException();
	    if(error == SSH_FX_PERMISSION_DENIED)
		throw new SFTPPermissionDeniedException();
	    if(error == SSH_FX_CONNECTION_LOST)
		throw new SFTPDisconnectException();
	    // !!! TODO: provide error
	    throw new SFTPException("Got error: " + error);
	} else if(pkt.getType() != type) {
	    // !!! TODO: provide fatal error
	    throw new SFTPException("Got unexpected packet: " + pkt.getType());
	}
    }

    private void cancelAllAsync() {
	if(replyLocks == null) {
	    return;
	}
	Enumeration ids = replyLocks.keys();
	while(ids.hasMoreElements()) {
	    ReplyLock l = (ReplyLock)replyLocks.remove(ids.nextElement());
	    l.cancel();
	}
    }

    private synchronized int getNextId() {
	return id++;
    }

}
