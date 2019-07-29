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

package com.mindbright.ssh;

import java.io.*;

public interface SSHPdu {
  public void   writeTo(OutputStream out) throws IOException;
  public void   readFrom(InputStream in) throws IOException;
  public SSHPdu createPdu() throws IOException;

  public byte[] rawData();
  public void   rawSetData(byte[] raw);
  public int    rawOffset();
  public int    rawSize();
  public void   rawAdjustSize(int size);

  //  public SSHPdu preProcess();
  //  public SSHPdu postProcess();
}
