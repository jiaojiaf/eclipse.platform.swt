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
package org.eclipse.swt.internal.carbon;


public class BitMap {
	public int baseAddr;
	public short rowBytes;
	//Rect bounds;
	public short top;
	public short left;
	public short bottom;
	public short right;
	public static final int sizeof = 14;
}
