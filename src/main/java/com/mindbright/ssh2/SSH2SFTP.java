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

import java.util.Date;
import java.text.SimpleDateFormat;

public class SSH2SFTP {

    private static final int DATA_BUFFER_SIZE = 34000;

    public static final class FileHandle {

	private byte[]        handle;
	private String        name;
	private boolean       isDirectory;
	private boolean       isOpen;
	private boolean       asyncEOF;
	private int           asyncCnt;
	private int           reqLeft;
	private SFTPException asyncException;
	private AsyncListener listener;

	protected volatile long lastOffset;

	public FileHandle(String name, byte[] handle, boolean isDirectory) {
	    this.name           = name;
	    this.handle         = handle;
	    this.isDirectory    = isDirectory;
	    this.isOpen         = true;
	    this.asyncCnt       = 0;
	    this.reqLeft        = 0;
	    this.asyncException = null;
	    this.lastOffset     = 0L;
	}

	public boolean isDirectory() {
	    return isDirectory;
	}

	public boolean isOpen() {
	    return isOpen;
	}

	public String getName() {
	    return name;
	}

	public byte[] getHandle() {
	    return handle;
	}

	public void addAsyncListener(AsyncListener listener) {
	    this.listener = listener;
	}

	protected synchronized void asyncStart(int len) {
	    asyncCnt++;
	}

	protected synchronized void asyncEnd(int len) {
	    asyncCnt--;
	    if(asyncCnt <= reqLeft) {
		this.notifyAll();
	    }
	    if(listener != null) {
		listener.progress((long)len);
	    }
	}

	protected synchronized void asyncReadEOF() {
	    asyncEOF = true;
	    asyncEnd(0);
	}

	protected synchronized void asyncException(SFTPException e) {
	    asyncException = e;
	    this.notifyAll();
	}

	public synchronized void asyncClose() {
	    if(asyncCnt > 0) {
		asyncException(new SFTPAsyncAbortException());
	    }
	    isOpen = false;
	}

	public synchronized boolean asyncWait() throws SFTPException {
	    return asyncWait(0);
	}

	public synchronized boolean asyncWait(int reqLeft)
	    throws SFTPException
	{
	    if(this.reqLeft < reqLeft) {
		this.reqLeft = reqLeft;
	    }
	    while(asyncCnt > reqLeft && asyncException == null && !asyncEOF) {
		try {
		    this.wait();
		} catch (InterruptedException e) {
		}
	    }
	    if(asyncException != null) {
		throw (SFTPException)asyncException.fillInStackTrace();
	    }
	    boolean eof = asyncEOF;
	    asyncEOF    = false;
	    return eof;
	}

    }

    public static interface AsyncListener {
	public void progress(long size);
    }

    public static final class FileAttributes {

	char[] types = { 'p', 'c', 'd', 'b', '-', 'l', 's', };

	public final static int S_IFMT   = 0170000;
	public final static int S_IFSOCK = 0140000;
	public final static int S_IFLNK  = 0120000;
	public final static int S_IFREG  = 0100000;
	public final static int S_IFBLK  = 0060000;
	public final static int S_IFDIR  = 0040000;
	public final static int S_IFCHR  = 0020000;
	public final static int S_IFIFO  = 0010000;

	public final static int S_ISUID = 0004000;
	public final static int S_ISGID = 0002000;

	public final static int S_IRUSR = 00400;
	public final static int S_IWUSR = 00200;
	public final static int S_IXUSR = 00100;
	public final static int S_IRGRP = 00040;
	public final static int S_IWGRP = 00020;
	public final static int S_IXGRP = 00010;
	public final static int S_IROTH = 00004;
	public final static int S_IWOTH = 00002;
	public final static int S_IXOTH = 00001;

	public boolean hasName;
	public boolean hasSize;
	public boolean hasUserGroup;
	public boolean hasPermissions;
	public boolean hasModTime;
	public String  name;
	public String  lname;
	public long    size;
	public int     uid;
	public int     gid;
	public int     permissions;
	public int     atime;
	public int     mtime;

	public String toString() {
	    return toString(hasName ? name : "<noname>");
	}

	public String toString(String name) {
	    StringBuffer str = new StringBuffer();
	    str.append(permString());
	    str.append("    1 ");
	    str.append(uid);
	    str.append("\t");
	    str.append(gid);
	    str.append("\t");
	    str.append(size);
	    str.append(" ");
	    str.append(modTimeString());
	    str.append(" ");
	    str.append(name);
	    return str.toString();
	}

	public String permString() {
	    StringBuffer str = new StringBuffer();
	    str.append(types[(permissions & S_IFMT) >>> 13]);
	    str.append(rwxString(permissions, 6));
	    str.append(rwxString(permissions, 3));
	    str.append(rwxString(permissions, 0));
	    return str.toString();
	}

	public String modTimeString() {
	    SimpleDateFormat df;
	    long mt  = (mtime * 1000L);
	    long now = System.currentTimeMillis();
	    if((now - mt) > (6 * 30 * 24 * 60 * 60 * 1000L)) {
		df = new SimpleDateFormat("MMM dd  yyyy");
	    } else {
		df = new SimpleDateFormat("MMM dd hh:mm");
	    }
	    return df.format(new Date(mt));
	}

	private String rwxString(int v, int r) {
	    v >>>= r;
	    String rwx = ((((v & 0x04) != 0) ? "r" : "-") +
			  (((v & 0x02) != 0) ? "w" : "-"));
	    if((r == 6 && isSUID()) ||
	       (r == 3 && isSGID())) {
		rwx += (((v & 0x01) != 0) ? "s" : "S");
	    } else {
		rwx += (((v & 0x01) != 0) ? "x" : "-");
	    }
	    return rwx;
	}

	public boolean isSocket() {
	    return ((permissions & S_IFSOCK) == S_IFSOCK);
	}

	public boolean isLink() {
	    return ((permissions & S_IFLNK) == S_IFLNK);
	}

	public boolean isFile() {
	    return ((permissions & S_IFREG) == S_IFREG);
	}

	public boolean isBlock() {
	    return ((permissions & S_IFBLK) == S_IFBLK);
	}

	public boolean isDirectory() {
	    return ((permissions & S_IFDIR) == S_IFDIR);
	}

	public boolean isCharacter() {
	    return ((permissions & S_IFCHR) == S_IFCHR);
	}

	public boolean isFifo() {
	    return ((permissions & S_IFIFO) == S_IFIFO);
	}

	public boolean isSUID() {
	    return ((permissions & S_ISUID) == S_ISUID);
	}

	public boolean isSGID() {
	    return ((permissions & S_ISGID) == S_ISGID);
	}

    }

    public static class SFTPException extends Exception {
	public SFTPException() {
	}

	public SFTPException(String msg) {
	    super(msg);
	}
    }

    public static class SFTPEOFException extends SFTPException {
    }

    public static class SFTPNoSuchFileException extends SFTPException {
    }

    public static class SFTPPermissionDeniedException extends SFTPException {
    }

    public static class SFTPDisconnectException extends SFTPException {
    }

    public static class SFTPAsyncAbortException extends SFTPException {
    }

    protected static final class SFTPPacket extends SSH2DataBuffer {

	private int type;
	private int id;
	private int len;

	public SFTPPacket() {
	    super(DATA_BUFFER_SIZE);
	}

	public void reset(int type, int id) {
	    reset();
	    writeInt(0); // dummy length
	    writeByte(type);
	    writeInt(id);
	    this.type = type;
	    this.id   = id;
	}

	public int getType() {
	    return type;
	}

	public int getId() {
	    return id;
	}

	public int getLength() {
	    return len;
	}

	public void writeLong(long l) {
	    writeInt((int)((l >>> 32) & 0xffffffffL));
	    writeInt((int)(l & 0xffffffff));
	}

	public long readLong() {
	    long h = (long)readInt();
	    long l = (long)readInt();
	    if(h < 0)
		h += 0xffffffffL;
	    if(l < 0)
		l += 0xffffffffL;
	    return (h << 32) | (l & 0xffffffff);
	}

	public void writeAttrs(FileAttributes attrs) {
	    writeInt((attrs.hasSize ? SSH_ATTR_SIZE : 0) |
		     (attrs.hasUserGroup ? SSH_ATTR_UIDGID : 0) |
		     (attrs.hasPermissions ? SSH_ATTR_PERM : 0) |
		     (attrs.hasModTime ? SSH_ATTR_MODTIME : 0));
	    if(attrs.hasSize) {
		writeLong(attrs.size);
	    }
	    if(attrs.hasUserGroup) {
		writeInt(attrs.uid);
		writeInt(attrs.gid);
	    }
	    if(attrs.hasPermissions) {
		writeInt(attrs.permissions);
	    }
	    if(attrs.hasModTime) {
		writeInt(attrs.atime);
		writeInt(attrs.mtime);
	    }
	}

	public FileAttributes readAttrs() {
	    FileAttributes attrs = new FileAttributes();
	    int            flags = readInt();
	    attrs.hasSize        = ((flags & SSH_ATTR_SIZE) != 0);
	    attrs.hasUserGroup   = ((flags & SSH_ATTR_UIDGID) != 0);
	    attrs.hasPermissions = ((flags & SSH_ATTR_PERM) != 0);
	    attrs.hasModTime     = ((flags & SSH_ATTR_MODTIME) != 0);
	    if(attrs.hasSize) {
		attrs.size = readLong();
	    }
	    if(attrs.hasUserGroup) {
		attrs.uid = readInt();
		attrs.gid = readInt();
	    }
	    if(attrs.hasPermissions) {
		attrs.permissions = readInt();
	    }
	    if(attrs.hasModTime) {
		attrs.atime = readInt();
		attrs.mtime = readInt();
	    }
	    return attrs;
	}

	public void readFrom(InputStream in)
	    throws SFTPException
	{
	    int cnt = 0;
	    len = 5;
	    try {
		while(cnt < len) {
		    int n;
		    n = in.read(data, cnt, (len - cnt));
		    if(n == -1) {
			throw new SFTPDisconnectException();
		    }
		    cnt += n;
		    if(cnt == 5) {
			len  = readInt() + 4;
			type = readByte();
		    }
		}
	    } catch (IOException e) {
		throw new SFTPException(e.getMessage());
	    }
	    len -= 5;
	}

	public void writeTo(OutputStream out)
	    throws SFTPException
	{
	    len = getWPos() - 5;
	    setWPos(0);
	    writeInt(len + 1);
	    try {
		out.write(data, 0, len + 5);
	    } catch (IOException e) {
		throw new SFTPException(e.getMessage());
	    }
	}
    }

    /* Version is 3 according to draft minus extension in init */
    protected final static int SSH_FILEXFER_VERSION =    2;

    /* Packet types. */
    protected final static int SSH_FXP_INIT =            1;
    protected final static int SSH_FXP_VERSION =         2;
    protected final static int SSH_FXP_OPEN =            3;
    protected final static int SSH_FXP_CLOSE =           4;
    protected final static int SSH_FXP_READ =            5;
    protected final static int SSH_FXP_WRITE =           6;
    protected final static int SSH_FXP_LSTAT =           7;
    protected final static int SSH_FXP_FSTAT =           8;
    protected final static int SSH_FXP_SETSTAT =         9;
    protected final static int SSH_FXP_FSETSTAT =       10;
    protected final static int SSH_FXP_OPENDIR =        11;
    protected final static int SSH_FXP_READDIR =        12;
    protected final static int SSH_FXP_REMOVE =         13;
    protected final static int SSH_FXP_MKDIR =          14;
    protected final static int SSH_FXP_RMDIR =          15;
    protected final static int SSH_FXP_REALPATH =       16;
    protected final static int SSH_FXP_STAT =           17;
    protected final static int SSH_FXP_RENAME =         18;

    protected final static int SSH_FXP_STATUS =         101;
    protected final static int SSH_FXP_HANDLE =         102;
    protected final static int SSH_FXP_DATA =           103;
    protected final static int SSH_FXP_NAME =           104;
    protected final static int SSH_FXP_ATTRS =          105;
    protected final static int SSH_FXP_EXTENDED =       200;
    protected final static int SSH_FXP_EXTENDED_REPLY = 201;

    /* Status/error codes. */
    public final static int SSH_FX_OK =                0;
    public final static int SSH_FX_EOF =               1;
    public final static int SSH_FX_NO_SUCH_FILE =      2;
    public final static int SSH_FX_PERMISSION_DENIED = 3;
    public final static int SSH_FX_FAILURE =           4;
    public final static int SSH_FX_BAD_MESSAGE =       5;
    public final static int SSH_FX_NO_CONNECTION =     6;
    public final static int SSH_FX_CONNECTION_LOST =   7;
    public final static int SSH_FX_OP_UNSUPPORTED =    8;

    /* Portable versions of O_RDONLY etc. */
    public final static int SSH_FXF_READ =            0x0001;
    public final static int SSH_FXF_WRITE =           0x0002;
    public final static int SSH_FXF_APPEND =          0x0004;
    public final static int SSH_FXF_CREAT =           0x0008;
    public final static int SSH_FXF_TRUNC =           0x0010;
    public final static int SSH_FXF_EXCL =            0x0020;

    /* Flags indicating presence of file attributes. */
    protected final static int SSH_ATTR_SIZE =         0x01;
    protected final static int SSH_ATTR_UIDGID =       0x02;
    protected final static int SSH_ATTR_PERM =         0x04;
    protected final static int SSH_ATTR_MODTIME =      0x08;

}
