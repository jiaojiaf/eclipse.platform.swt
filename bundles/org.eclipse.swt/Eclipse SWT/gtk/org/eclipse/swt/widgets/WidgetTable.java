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
package org.eclipse.swt.widgets;

 
import org.eclipse.swt.internal.*;
import org.eclipse.swt.internal.gtk.*;

class WidgetTable {
	static int FreeSlot = 0;
	static int GrowSize = 1024;
	static int [] IndexTable = new int [GrowSize];
	static Widget [] WidgetTable = new Widget [GrowSize];
	static final int SWT_OBJECT_INDEX;
	static {
		byte [] buffer = Converter.wcsToMbcs (null, "SWT_OBJECT_INDEX", true);
		SWT_OBJECT_INDEX = OS.g_quark_from_string (buffer);
		for (int i=0; i<GrowSize-1; i++) IndexTable [i] = i + 1;
		IndexTable [GrowSize - 1] = -1;
	}
	
public static synchronized Widget get (int handle) {
	if (handle == 0) return null;
	int index = OS.g_object_get_qdata (handle, SWT_OBJECT_INDEX) - 1;
	if (0 <= index && index < WidgetTable.length) return WidgetTable [index];
	return null;
}

public synchronized static void put(int handle, Widget widget) {
	if (handle == 0) return;
	if (FreeSlot == -1) {
		int length = (FreeSlot = IndexTable.length) + GrowSize;
		int[] newIndexTable = new int[length];
		Widget[] newWidgetTable = new Widget [length];
		System.arraycopy (IndexTable, 0, newIndexTable, 0, FreeSlot);
		System.arraycopy (WidgetTable, 0, newWidgetTable, 0, FreeSlot);
		for (int i = FreeSlot; i < length - 1; i++) {
			newIndexTable[i] = i + 1;
		}
		newIndexTable[length - 1] = -1;
		IndexTable = newIndexTable;
		WidgetTable = newWidgetTable;
	}
	int index = FreeSlot + 1;
	OS.g_object_set_qdata (handle, SWT_OBJECT_INDEX, index);
	int oldSlot = FreeSlot;
	FreeSlot = IndexTable[oldSlot];
	IndexTable [oldSlot] = -2;
	WidgetTable [oldSlot] = widget;
}

public static synchronized Widget remove (int handle) {
	if (handle == 0) return null;
	Widget widget = null;
	int index = OS.g_object_get_qdata (handle, SWT_OBJECT_INDEX) - 1;
	if (0 <= index && index < WidgetTable.length) {
		widget = WidgetTable [index];
		WidgetTable [index] = null;
		IndexTable [index] = FreeSlot;
		FreeSlot = index;
		OS.g_object_set_qdata (handle, SWT_OBJECT_INDEX, 0);
	}
	return widget;
}

public static synchronized Shell [] shells () {
	int length = 0;
	for (int i=0; i<WidgetTable.length; i++) {
		Widget widget = WidgetTable [i];
		if (widget != null && widget instanceof Shell) length++;
	}
	int index = 0;
	Shell [] result = new Shell [length];
	for (int i=0; i<WidgetTable.length; i++) {
		Widget widget = WidgetTable [i];
		if (widget != null && widget instanceof Shell) {
			int j = 0;
			while (j < index) {
				if (result [j] == widget) break;
				j++;
			}
			if (j == index)	result [index++] = (Shell) widget;
		}
	}
	if (index == length) return result;
	Shell [] newResult = new Shell [index];
	System.arraycopy (result, 0, newResult, 0, index);
	return newResult;
}

public static synchronized int size () {
	int size = 0;
	for (int i=0; i<WidgetTable.length; i++) {
		if (WidgetTable [i] != null) size++;
	}
	return size;
}

}
