/*
 * ====================================================================
 *
 * License for ISNetworks' MindTerm SCP modifications
 *
 * Copyright (c) 2001 ISNetworks, LLC.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include an acknowlegement that the software contains
 *    code based on contributions made by ISNetworks, and include
 *    a link to http://www.isnetworks.com/.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 */

/**
* Representation of a file on either the local or remote file systems.
* Knows whether it's a directory as well as its name and path.
*/
package com.isnetworks.ssh;

import java.io.*;
import java.util.*;

public final class FileListItem {

    private String  name;
    private String  parent;
    private String  separator;
    private boolean directory;
    private long    size;

    public FileListItem(String name, String parent, boolean directory,
			 String separator) {
	this(name, parent, directory, separator, -1);
    }

    public FileListItem(String name, String parent, boolean directory,
			 String separator, long size) {
	if(!parent.endsWith(separator)) {
	    parent += separator;
	}
	this.name      = name;
	this.parent    = parent;
	this.directory = directory;
	this.separator = separator;
	this.size      = size;
    }

    /**
     * Get fully qualified name
     */
    public String getAbsolutePath() {
	return parent + name;
    }

    /**
     * Get name of file relative to its parent directory
     */
    public String getName() {
	return name;
    }

    /**
     * Get size of file
     */
    public long getSize() {
	return size;
    }

    /**
     * Get full path of directory this file lives in
     */
    public String getParent() {
	return parent;
    }
	
    public boolean isDirectory() {
	return directory;
    }
	
    /**
     * Used to sort files first by directory/non-directory and then by name
     */
    private boolean earlierThan(FileListItem fileListing) {
	// Always put parent directory at the top of the list
	if(name.equals( ".." )) {
	    return true;
	}		
	if(fileListing.name.equals( ".." )) {
	    return false;
	}
		
	if(isDirectory() && !fileListing.isDirectory()) {
	    return true;
	}
	if(!isDirectory() && fileListing.isDirectory()) {
	    return false;
	}
		
	return name.toUpperCase().compareTo( fileListing.name.toUpperCase() ) < 0;
    }

    /**
     * Simple, inefficient bubble sort for array of FileListItems.
     * Only here because java.util.Arrays class does not exist
     * in Java 1.1 so it wouldn't work in an applet.  Should be
     * acceptable since directories typically contain a relatively
     * small number of files.
     */
    public static void sort(FileListItem[] files) {
	for(int i = 0; i < files.length; i++) {
	    for(int j = i; j < files.length; j++) {
		if(!(files[i]).earlierThan(files[j])) {
		    FileListItem temp = files[j];
		    files[j] = files[i];
		    files[i] = temp;
		}
	    }
	}
    }

}
