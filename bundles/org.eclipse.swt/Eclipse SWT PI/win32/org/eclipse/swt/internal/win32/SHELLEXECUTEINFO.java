/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.swt.internal.win32;

public class SHELLEXECUTEINFO {
	
	public int cbSize;
	public int fMask; 
	public int hwnd;
	public int lpVerb;
	public int lpFile; 
	public int lpParameters;
	public int lpDirectory;
	public int nShow; 
	public int hInstApp;
	
	// Optional members
	public int lpIDList; 
	public int lpClass;
	public int hkeyClass;
	public int dwHotKey;
//	union {
//		HANDLE hIcon;		
//		HANDLE hMonitor;
//	};
	public int hIcon;
	public int hProcess; 
	
	public static final int sizeof = 60;
}

