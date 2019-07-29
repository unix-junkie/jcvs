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

package com.mindbright.asn1;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class ASN1Structure extends ASN1Object {

    protected ASN1Object[] components;
    protected int          count;
    protected Class        ofType;

    protected ASN1Structure(int tag, int size) {
	super(tag | ASN1.TYPE_CONSTRUCTED);
	this.components = new ASN1Object[size];
	this.count      = 0;
    }

    public int getCount() {
	return count;
    }

    // !!! TODO check with a max count
    //
    public ASN1Object getComponent(int index) {
	ASN1Object component = null;
	if(count > 0 && index < count) {
	    component = components[index];
	} else if(ofType != null) {
	    try {
		component = (ASN1Object)ofType.newInstance();
		setComponent(index, component);
	    } catch (Exception e) {
		// !!! TODO Error/Exception
	    }
	}
	return component;
    }

    public ASN1Object getDistinctComponent(int tag) {
	int i;
	for(i = 0; i < count; i++) {
	    if(tag == components[i].getTag()) {
		break;
	    }
	}
	return getComponent(i);
    }

    public void setComponent(int index, ASN1Object component) {
	if(index >= components.length) {
	    ASN1Object[] tmp = components;
	    components = new ASN1Object[(tmp.length + 1) * 2];
	    System.arraycopy(tmp, 0, components, 0, tmp.length);
	}
	components[index] = component;
	if(index >= count) {
	    count = index + 1;
	}
    }

    public int encodeValue(ASN1Encoder encoder, OutputStream out)
	throws IOException
    {
	return encoder.encodeStructure(out, this);
    }

    public void decodeValue(ASN1Decoder decoder, InputStream in, int len)
    	throws IOException
    {
	decoder.decodeStructure(in, len, this);
    }

}
