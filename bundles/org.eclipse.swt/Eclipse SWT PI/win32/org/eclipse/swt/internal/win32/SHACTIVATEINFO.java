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

public class SHACTIVATEINFO {
	public int cbSize;
	public int hwndLastFocus;
	public int fSipUp; // :1
	public int fSipOnDeactivation; // :1
	public int fActive; // :1
	public int fReserved; // :29
	public static final int sizeof = 12;
}
