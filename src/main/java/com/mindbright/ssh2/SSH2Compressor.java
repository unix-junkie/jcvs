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

public abstract class SSH2Compressor {

    public final static int COMPRESS_MODE   = 1;
    public final static int UNCOMPRESS_MODE = 2;

    // !!! TODO
    public static SSH2Compressor getInstance(String algorithm, int mode)
	throws SSH2CompressionException
    {
	return getInstance(algorithm, mode, -1);
    }

    public static SSH2Compressor getInstance(String algorithm,
					     int mode, int level)
	throws SSH2CompressionException
    {
	if("zlib".equals(algorithm)) {
	    try {
		Class compCl =
		    Class.forName("com.mindbright.ssh2.SSH2CompressorZLib");
		SSH2Compressor comp = (SSH2Compressor)compCl.newInstance();
		comp.init(mode, level);
		return comp;
	    } catch (Exception e) {
		throw new SSH2CompressionException(e.getMessage());
	    }
	}
	return null;
    }

    public abstract void init(int mode, int level);
    public abstract void compress(SSH2DataBuffer data)
      throws SSH2CompressionException;
    public abstract int uncompress(SSH2DataBuffer data, int len)
      throws SSH2CompressionException;
    public abstract long numOfCompressedBytes();
    public abstract long numOfUncompressedBytes();

}
