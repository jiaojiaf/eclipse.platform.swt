package org.eclipse.swt.widgets;

/*
 * Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.
 * This file is made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */

import org.eclipse.swt.*;
import org.eclipse.swt.internal.*;
import org.eclipse.swt.internal.gtk.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.events.*;

/** 
 * Instances of this class implement a selectable user interface
 * object that displays a list of images and strings and issue
 * notificiation when selected.
 * <p>
 * The item children that may be added to instances of this class
 * must be of type <code>TableItem</code>.
 * </p><p>
 * Note that although this class is a subclass of <code>Composite</code>,
 * it does not make sense to add <code>Control</code> children to it,
 * or set a layout on it.
 * </p><p>
 * <dl>
 * <dt><b>Styles:</b></dt>
 * <dd>SINGLE, MULTI, CHECK, FULL_SELECTION, HIDE_SELECTION</dd>
 * <dt><b>Events:</b></dt>
 * <dd>Selection, DefaultSelection</dd>
 * </dl>
 * <p>
 * Note: Only one of the styles SINGLE, and MULTI may be specified.
 * </p><p>
 * IMPORTANT: This class is <em>not</em> intended to be subclassed.
 * </p>
 */
public class Table extends Composite {
	int modelHandle;
	int itemCount, columnCount;
	TableItem [] items;
	TableColumn [] columns;
	ImageList imageList;

/**
 * Constructs a new instance of this class given its parent
 * and a style value describing its behavior and appearance.
 * <p>
 * The style value is either one of the style constants defined in
 * class <code>SWT</code> which is applicable to instances of this
 * class, or must be built by <em>bitwise OR</em>'ing together 
 * (that is, using the <code>int</code> "|" operator) two or more
 * of those <code>SWT</code> style constants. The class description
 * lists the style constants that are applicable to the class.
 * Style bits are also inherited from superclasses.
 * </p>
 *
 * @param parent a composite control which will be the parent of the new instance (cannot be null)
 * @param style the style of control to construct
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the parent</li>
 *    <li>ERROR_INVALID_SUBCLASS - if this class is not an allowed subclass</li>
 * </ul>
 *
 * @see SWT#SINGLE
 * @see SWT#MULTI
 * @see SWT#CHECK
 * @see SWT#FULL_SELECTION
 * @see SWT#HIDE_SELECTION
 * @see Widget#checkSubclass
 * @see Widget#getStyle
 */
public Table (Composite parent, int style) {
	super (parent, checkStyle (style));
}

static int checkStyle (int style) {
	/*
	* To be compatible with Windows, force the H_SCROLL
	* and V_SCROLL style bits.  On Windows, it is not
	* possible to create a table without scroll bars.
	*/
	style |= SWT.H_SCROLL | SWT.V_SCROLL;
	return checkBits (style, SWT.SINGLE, SWT.MULTI, 0, 0, 0, 0);
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when the receiver's selection changes, by sending
 * it one of the messages defined in the <code>SelectionListener</code>
 * interface.
 * <p>
 * When <code>widgetSelected</code> is called, the item field of the event object is valid.
 * If the reciever has <code>SWT.CHECK</code> style set and the check selection changes,
 * the event object detail field contains the value <code>SWT.CHECK</code>.
 * <code>widgetDefaultSelected</code> is typically called when an item is double-clicked.
 * The item field of the event object is valid for default selection, but the detail field is not used.
 * </p>
 *
 * @param listener the listener which should be notified
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see SelectionListener
 * @see #removeSelectionListener
 * @see SelectionEvent
 */
public void addSelectionListener (SelectionListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener (SWT.Selection,typedListener);
	addListener (SWT.DefaultSelection,typedListener);
}

public Point computeSize (int wHint, int hHint, boolean changed) {
	checkWidget ();
	Point size = computeNativeSize(handle, wHint, hHint, changed);
	Rectangle trim = computeTrim (0, 0, size.x, size.y);
	size.x = trim.width;
	size.y = trim.height;
	return size;
}

void createHandle (int index) {
	state |= HANDLE;
	fixedHandle = OS.gtk_fixed_new ();
	if (fixedHandle == 0) error (SWT.ERROR_NO_HANDLES);
	OS.gtk_fixed_set_has_window (fixedHandle, true);
	scrolledHandle = OS.gtk_scrolled_window_new (0, 0);
	if (scrolledHandle == 0) error (SWT.ERROR_NO_HANDLES);
	/*
	* Columns:
	* 0 - check
	* 1 - foreground
	* 2 - background
	* 3 - text
	* 4 - pixbuf
	* 5 - ...
	*/
	int [] types = getColumnTypes (1);
	modelHandle = OS.gtk_list_store_newv (types.length, types);
	if (modelHandle == 0) error (SWT.ERROR_NO_HANDLES);
	handle = OS.gtk_tree_view_new_with_model (modelHandle);
	if (handle == 0) error (SWT.ERROR_NO_HANDLES);
	createColumn (null, 0);
	int parentHandle = parent.parentingHandle ();
	OS.gtk_container_add (parentHandle, fixedHandle);
	OS.gtk_container_add (fixedHandle, scrolledHandle);
	OS.gtk_container_add (scrolledHandle, handle);
	OS.gtk_widget_show (fixedHandle);
	OS.gtk_widget_show (scrolledHandle);
	OS.gtk_widget_show (handle);
	
	int mode = (style & SWT.MULTI) != 0 ? OS.GTK_SELECTION_MULTIPLE : OS.GTK_SELECTION_BROWSE;
	int selectionHandle = OS.gtk_tree_view_get_selection (handle);
	OS.gtk_tree_selection_set_mode (selectionHandle, mode);
	OS.gtk_tree_view_set_headers_visible (handle, false);	
	int hsp = (style & SWT.H_SCROLL) != 0 ? OS.GTK_POLICY_AUTOMATIC : OS.GTK_POLICY_NEVER;
	int vsp = (style & SWT.V_SCROLL) != 0 ? OS.GTK_POLICY_AUTOMATIC : OS.GTK_POLICY_NEVER;
	OS.gtk_scrolled_window_set_policy (scrolledHandle, hsp, vsp);
	if ((style & SWT.BORDER) != 0) OS.gtk_scrolled_window_set_shadow_type (scrolledHandle, OS.GTK_SHADOW_ETCHED_IN);
}

void createColumn (TableColumn column, int index) {
	int modelIndex = 3;
	if (columnCount != 0) {
		int modelLength = OS.gtk_tree_model_get_n_columns (modelHandle);
		boolean [] usedColumns = new boolean [modelLength];
		for (int i=0; i<columnCount; i++) {
			int columnIndex = columns [i].modelIndex;
			usedColumns [columnIndex] = usedColumns [columnIndex + 1] = true;
		}
		while (modelIndex < modelLength) {
			if (!usedColumns [modelIndex]) break;
			modelIndex++;
		}
		if (modelIndex == modelLength) {
			int oldModel = modelHandle;
			int[] types = getColumnTypes (columnCount + 4);
			int newModel = OS.gtk_list_store_newv (types.length, types);
			if (newModel == 0) error (SWT.ERROR_NO_HANDLES);
			int [] ptr = new int [1];
			for (int i=0; i<itemCount; i++) {
				TableItem item = items [i];
				int oldItem = item.handle;
				int newItem = OS.g_malloc (OS.GtkTreeIter_sizeof ());
				if (newItem == 0) error (SWT.ERROR_NO_HANDLES);
				OS.gtk_list_store_insert (newModel, newItem, i);
				for (int j=0; j<modelLength; j++) {
					OS.gtk_tree_model_get (oldModel, oldItem, j, ptr, -1);
					OS.gtk_list_store_set (newModel, newItem, j, ptr [0], -1);
					if (types [j] == OS.G_TYPE_STRING ()) OS.g_free ((ptr [0]));
				}
				OS.gtk_list_store_remove (oldModel, oldItem);
				OS.g_free (oldItem);
				item.handle = newItem;
			}
			OS.gtk_tree_view_set_model (handle, newModel);
			OS.g_object_unref (oldModel);
			modelHandle = newModel;
		}
	}
	int columnHandle = OS.gtk_tree_view_column_new ();
	if (columnHandle == 0) error (SWT.ERROR_NO_HANDLES);
	if (index == 0 && columnCount > 0) {
		TableColumn checkColumn = columns [0];
		createRenderers (checkColumn.handle, checkColumn.modelIndex, false);
	}
	createRenderers (columnHandle, modelIndex, index == 0);
	Display display = getDisplay ();
	OS.g_signal_connect (columnHandle, OS.clicked, display.treeColumnSelectionProc, handle);
	OS.gtk_tree_view_column_set_resizable (columnHandle, true);
	OS.gtk_tree_view_column_set_clickable (columnHandle, true);
	OS.gtk_tree_view_column_set_sizing (columnHandle, OS.GTK_TREE_VIEW_COLUMN_FIXED);
	OS.gtk_tree_view_insert_column (handle, columnHandle, index);
	if (column != null) {
		column.handle = columnHandle;
		column.modelIndex = modelIndex;
	}
}

void createRenderers (int columnHandle, int modelIndex, boolean check) {
	OS.gtk_tree_view_column_clear (columnHandle);
	if ((style & SWT.CHECK) != 0 && check) {
		int checkRenderer = OS.gtk_cell_renderer_toggle_new ();
		if (checkRenderer == 0) error (SWT.ERROR_NO_HANDLES);
		OS.gtk_tree_view_column_pack_start (columnHandle, checkRenderer, false);
		OS.gtk_tree_view_column_add_attribute (columnHandle, checkRenderer, "active", 0);
		Display display = getDisplay ();
		OS.g_signal_connect (checkRenderer, OS.toggled, display.treeToggleProc, handle);
	}
	int pixbufRenderer = OS.gtk_cell_renderer_pixbuf_new ();
	if (pixbufRenderer == 0) error (SWT.ERROR_NO_HANDLES);
	OS.gtk_tree_view_column_pack_start (columnHandle, pixbufRenderer, false);
	OS.gtk_tree_view_column_add_attribute (columnHandle, pixbufRenderer, "pixbuf", modelIndex);
	int textRenderer = OS.gtk_cell_renderer_text_new ();
	if (textRenderer == 0) error (SWT.ERROR_NO_HANDLES);
	OS.gtk_tree_view_column_pack_start (columnHandle, textRenderer, true);
	OS.gtk_tree_view_column_add_attribute (columnHandle, textRenderer, "text", modelIndex + 1);
	OS.gtk_tree_view_column_add_attribute (columnHandle, textRenderer, "foreground-gdk", 1);
	OS.gtk_tree_view_column_add_attribute (columnHandle, textRenderer, "background-gdk", 2);
}

void createItem (TableColumn column, int index) {
	if (!(0 <= index && index <= columnCount)) error (SWT.ERROR_ITEM_NOT_ADDED);
	if (columnCount == 0) {
		column.handle = OS.gtk_tree_view_get_column (handle, 0);
		column.modelIndex = 3;
	} else {
		createColumn (column, index);
	}
	if (columnCount == columns.length) {
		TableColumn [] newColumns = new TableColumn [columns.length + 4];
		System.arraycopy (columns, 0, newColumns, 0, columns.length);
		columns = newColumns;
	}
	System.arraycopy (columns, index, columns, index + 1, columnCount++ - index);
	columns [index] = column;
}

void createItem (TableItem item, int index) {
	if (!(0 <= index && index <= itemCount)) error (SWT.ERROR_ITEM_NOT_ADDED);
	if (itemCount == items.length) {
		TableItem [] newItems = new TableItem [items.length + 4];
		System.arraycopy (items, 0, newItems, 0, items.length);
		items = newItems;
	}
	item.handle = OS.g_malloc (OS.GtkTreeIter_sizeof ());
	if (item.handle == 0) error (SWT.ERROR_NO_HANDLES);
	OS.gtk_list_store_insert (modelHandle, item.handle, index);
	System.arraycopy (items, index, items, index + 1, itemCount++ - index);
	items [index] = item;
}

void createWidget (int index) {
	super.createWidget (index);
	items = new TableItem [4];
	columns = new TableColumn [4];
	itemCount = columnCount = 0;
}

GdkColor defaultBackground () {
	Display display = getDisplay ();
	return display.COLOR_LIST_BACKGROUND;
}

GdkColor defaultForeground () {
	Display display = getDisplay ();
	return display.COLOR_LIST_FOREGROUND;
}

void deregister() {
	super.deregister ();
	WidgetTable.remove (OS.gtk_tree_view_get_selection (handle));
}

/**
 * Deselects the item at the given zero-relative index in the receiver.
 * If the item at the index was already deselected, it remains
 * deselected. Indices that are out of range are ignored.
 *
 * @param index the index of the item to deselect
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void deselect (int index) {
	checkWidget();
	if (index < 0 || index >= itemCount) return;
	blockSignal (handle, SWT.Selection);
	int selection = OS.gtk_tree_view_get_selection (handle);
	OS.gtk_tree_selection_unselect_iter (selection, items [index].handle);
	unblockSignal (handle, SWT.Selection);
}

/**
 * Deselects the items at the given zero-relative indices in the receiver.
 * If the item at the given zero-relative index in the receiver 
 * is selected, it is deselected.  If the item at the index
 * was not selected, it remains deselected.  The range of the
 * indices is inclusive. Indices that are out of range are ignored.
 *
 * @param start the start index of the items to deselect
 * @param end the end index of the items to deselect
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void deselect (int start, int end) {
	checkWidget();
	int selection = OS.gtk_tree_view_get_selection (handle);
	blockSignal (handle, SWT.Selection);
	for (int index=start; index<=end; index++) {
		if (index < 0 || index >= itemCount) continue;
		OS.gtk_tree_selection_unselect_iter (selection, items [index].handle);
	}
	unblockSignal (handle, SWT.Selection);
}

/**
 * Deselects the items at the given zero-relative indices in the receiver.
 * If the item at the given zero-relative index in the receiver 
 * is selected, it is deselected.  If the item at the index
 * was not selected, it remains deselected. Indices that are out
 * of range and duplicate indices are ignored.
 *
 * @param indices the array of indices for the items to deselect
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void deselect (int [] indices) {
	checkWidget();
	if (indices == null) error (SWT.ERROR_NULL_ARGUMENT);
	int selection = OS.gtk_tree_view_get_selection (handle);
	blockSignal (handle, SWT.Selection);
	for (int i=0; i<indices.length; i++) {
		int index = indices[i];
		if (index < 0 || index >= itemCount) continue;
		OS.gtk_tree_selection_unselect_iter (selection, items [index].handle);
	}
	unblockSignal (handle, SWT.Selection);
}

/**
 * Deselects all selected items in the receiver.
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void deselectAll () {
	checkWidget();
	blockSignal (handle, SWT.Selection);
	int selection = OS.gtk_tree_view_get_selection (handle);
	OS.gtk_tree_selection_unselect_all (selection);
	unblockSignal (handle, SWT.Selection);
}

void destroyItem (TableColumn column) {
	int index = 0;
	while (index < columnCount) {
		if (columns [index] == column) break;
		index++;
	}
	if (index == columnCount) return;
	int columnHandle = column.handle;
	System.arraycopy (columns, index + 1, columns, index, --columnCount - index);
	columns [columnCount] = null;
	OS.gtk_tree_view_remove_column (handle, columnHandle);
	if (columnCount == 0) {
		int oldModel = modelHandle;
		int[] types = getColumnTypes (1);
		int newModel = OS.gtk_list_store_newv (types.length, types);
		if (newModel == 0) error (SWT.ERROR_NO_HANDLES);
		int [] ptr = new int [1];
		for (int i=0; i<itemCount; i++) {
			TableItem item = items [i];
			int oldItem = item.handle;
			int newItem = OS.g_malloc (OS.GtkTreeIter_sizeof ());
			if (newItem == 0) error (SWT.ERROR_NO_HANDLES);
			OS.gtk_list_store_insert (newModel, newItem, i);
			for (int j=0; j<3; j++) {
				OS.gtk_tree_model_get (oldModel, oldItem, j, ptr, -1);
				OS.gtk_list_store_set (newModel, newItem, j, ptr [0], -1);
			}
			OS.gtk_tree_model_get (oldModel, oldItem, column.modelIndex, ptr, -1);
			OS.gtk_list_store_set (newModel, newItem, 3, ptr [0], -1);
			OS.gtk_tree_model_get (oldModel, oldItem, column.modelIndex + 1, ptr, -1);
			OS.gtk_list_store_set (newModel, newItem, 3 + 1, ptr [0], -1);
			OS.g_free ( (ptr [0]));
			OS.gtk_list_store_remove (oldModel, oldItem);
			OS.g_free (oldItem);
			item.handle = newItem;
		}
		OS.gtk_tree_view_set_model (handle, newModel);
		OS.g_object_unref (oldModel);
		modelHandle = newModel;
		createColumn (null, 0);
	} else {
		for (int i=0; i<itemCount; i++) {
			int item = items [i].handle;
			int modelIndex = column.modelIndex;
			OS.gtk_list_store_set (modelHandle, item, modelIndex, 0, -1);
			OS.gtk_list_store_set (modelHandle, item, modelIndex + 1, 0, -1);
		}
		if (index == 0) {
			TableColumn checkColumn = columns [0];
			createRenderers (checkColumn.handle, checkColumn.modelIndex, true);
		}
	}
	column.handle = 0;
}

void destroyItem (TableItem item) {
	int index = 0;
	while (index < itemCount) {
		if (items [index] == item) break;
		index++;
	}
	if (index == itemCount) return;
	blockSignal (handle, SWT.Selection);
	int itemHandle = item.handle;
	OS.gtk_list_store_remove (modelHandle, itemHandle);
	OS.g_free (itemHandle);
	item.handle = 0;
	unblockSignal (handle, SWT.Selection);
	System.arraycopy (items, index + 1, items, index, --itemCount - index);
	items [itemCount] = null;
}

GdkColor getBackgroundColor () {
	return getBaseColor ();
}

/**
 * Returns the column at the given, zero-relative index in the
 * receiver. Throws an exception if the index is out of range.
 * If no <code>TableColumn</code>s were created by the programmer,
 * this method will throw <code>ERROR_INVALID_RANGE</code> despite
 * the fact that a single column of data may be visible in the table.
 * This occurs when the programmer uses the table like a list, adding
 * items but never creating a column.
 *
 * @param index the index of the column to return
 * @return the column at the given index
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_RANGE - if the index is not between 0 and the number of elements in the list minus 1 (inclusive)</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public TableColumn getColumn (int index) {
	checkWidget();
	if (!(0 <= index && index < columnCount)) error (SWT.ERROR_CANNOT_GET_ITEM);
	return columns [index];
}

/**
 * Returns the number of columns contained in the receiver.
 * If no <code>TableColumn</code>s were created by the programmer,
 * this value is zero, despite the fact that visually, one column
 * of items is may be visible. This occurs when the programmer uses
 * the table like a list, adding items but never creating a column.
 *
 * @return the number of columns
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception SWTError <ul>
 *    <li>ERROR_CANNOT_GET_COUNT - if the operation fails because of an operating system failure</li>
 * </ul>
 */
public int getColumnCount () {
	checkWidget();
	return columnCount;
}

int[] getColumnTypes (int n) {
	int[] types = new int [(n * 2) + 3];
	types [0] = OS.G_TYPE_BOOLEAN ();
	types [1] =  types [2] = OS.GDK_TYPE_COLOR ();
	for (int i=3; i<types.length; i+=2) {
		types [i] = OS.GDK_TYPE_PIXBUF ();
		types [i + 1] = OS.G_TYPE_STRING ();
	}
	return types;
}

/**
 * Returns an array of <code>TableColumn</code>s which are the
 * columns in the receiver. If no <code>TableColumn</code>s were
 * created by the programmer, the array is empty, despite the fact
 * that visually, one column of items may be visible. This occurs
 * when the programmer uses the table like a list, adding items but
 * never creating a column.
 * <p>
 * Note: This is not the actual structure used by the receiver
 * to maintain its list of items, so modifying the array will
 * not affect the receiver. 
 * </p>
 *
 * @return the items in the receiver
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public TableColumn [] getColumns () {
	checkWidget();
	TableColumn [] result = new TableColumn [columnCount];
	System.arraycopy (columns, 0, result, 0, columnCount);
	return result;
}

TableItem getFocusItem () {
	int [] path = new int [1];
	OS.gtk_tree_view_get_cursor (handle, path, null);
	if (path [0] == 0) return null;
	TableItem item = null;
	int indices = OS.gtk_tree_path_get_indices (path [0]);
	if (indices != 0) {
		int [] index = new int []{-1};
		OS.memmove (index, indices, 4);
		item = items [index [0]]; 
	}
	OS.gtk_tree_path_free (path [0]);
	return item;
} 

GdkColor getForegroundColor () {
	return getTextColor ();
}

/**
 * Returns the width in pixels of a grid line.
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int getGridLineWidth () {
	checkWidget();
	return 0;
}

/**
 * Returns the height of the receiver's header 
 *
 * @return the height of the header or zero if the header is not visible
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * 
 * @since 2.0 
 */
public int getHeaderHeight () {
	checkWidget ();
	if (!OS.gtk_tree_view_get_headers_visible (handle)) return 0;
	OS.gtk_widget_realize (handle);
	int fixedWindow = OS.GTK_WIDGET_WINDOW (fixedHandle);
	int binWindow = OS.gtk_tree_view_get_bin_window (handle);
	int [] binY = new int [1];
	OS.gdk_window_get_origin (binWindow, null, binY);
	int [] fixedY = new int [1];
	OS.gdk_window_get_origin (fixedWindow, null, fixedY);
	return binY [0] - fixedY [0];
}

/**
 * Returns <code>true</code> if the receiver's header is visible,
 * and <code>false</code> otherwise.
 * <p>
 * If one of the receiver's ancestors is not visible or some
 * other condition makes the receiver not visible, this method
 * may still indicate that it is considered visible even though
 * it may not actually be showing.
 * </p>
 *
 * @return the receiver's header's visibility state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public boolean getHeaderVisible () {
	checkWidget();
	return OS.gtk_tree_view_get_headers_visible (handle);
}

/**
 * Returns the item at the given, zero-relative index in the
 * receiver. Throws an exception if the index is out of range.
 *
 * @param index the index of the item to return
 * @return the item at the given index
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_RANGE - if the index is not between 0 and the number of elements in the list minus 1 (inclusive)</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public TableItem getItem (int index) {
	checkWidget();
	if (!(0 <= index && index < itemCount)) error (SWT.ERROR_CANNOT_GET_ITEM);
	return items [index];
}

/**
 * Returns the item at the given point in the receiver
 * or null if no such item exists. The point is in the
 * coordinate system of the receiver.
 *
 * @param point the point used to locate the item
 * @return the item at the given point
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public TableItem getItem (Point pt) {
	checkWidget();
	int [] path = new int [1];
	int clientY = pt.y - getHeaderHeight ();
	if (!OS.gtk_tree_view_get_path_at_pos (handle, pt.x, clientY, path, null, null, null)) return null;
	if (path [0] == 0) return null;
	int indices = OS.gtk_tree_path_get_indices (path [0]);
	TableItem item = null;
	if (indices != 0) {
		int [] index = new int [1];
		OS.memmove (index, indices, 4);
		item = items [index [0]];
	}
	OS.gtk_tree_path_free (path [0]);
	return item;
}

/**
 * Returns the number of items contained in the receiver.
 *
 * @return the number of items
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int getItemCount () {
	checkWidget ();
	return itemCount;
}

/**
 * Returns the height of the area which would be used to
 * display <em>one</em> of the items in the receiver's.
 *
 * @return the height of one item
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int getItemHeight () {
	checkWidget();
	// FIXME
	// I (bgs) am not sure what to do with this.
	// In GTK2, rows may have different heights, so asking
	// this question will only make sense given the item.
	if (itemCount == 0) return 15;
	GdkRectangle rect = new GdkRectangle ();
	int path = OS.gtk_tree_path_new_from_string (Converter.wcsToMbcs (null, "0", true));
	int column = OS.gtk_tree_view_get_column (handle, 0);
	OS.gtk_tree_view_get_cell_area (handle, path, column, rect);
	OS.gtk_tree_path_free (path);
	return rect.height;
}

/**
 * Returns an array of <code>TableItem</code>s which are the items
 * in the receiver. 
 * <p>
 * Note: This is not the actual structure used by the receiver
 * to maintain its list of items, so modifying the array will
 * not affect the receiver. 
 * </p>
 *
 * @return the items in the receiver
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public TableItem [] getItems () {
	checkWidget();
	TableItem [] result = new TableItem [itemCount];
	System.arraycopy (items, 0, result, 0, itemCount);
	return result;
}

/**
 * Returns <code>true</code> if the receiver's lines are visible,
 * and <code>false</code> otherwise.
 * <p>
 * If one of the receiver's ancestors is not visible or some
 * other condition makes the receiver not visible, this method
 * may still indicate that it is considered visible even though
 * it may not actually be showing.
 * </p>
 *
 * @return the visibility state of the lines
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public boolean getLinesVisible() {
	checkWidget();
	return false;
}

/**
 * Returns an array of <code>TableItem</code>s that are currently
 * selected in the receiver. An empty array indicates that no
 * items are selected.
 * <p>
 * Note: This is not the actual structure used by the receiver
 * to maintain its selection, so modifying the array will
 * not affect the receiver. 
 * </p>
 * @return an array representing the selection
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public TableItem [] getSelection () {
	checkWidget();
	Display display = getDisplay ();
	display.treeSelectionLength  = 0;
	display.treeSelection = new int [itemCount];
	int selection = OS.gtk_tree_view_get_selection (handle);
	OS.gtk_tree_selection_selected_foreach (selection, display.treeSelectionProc, handle);
	TableItem [] result = new TableItem [display.treeSelectionLength];
	for (int i=0; i<result.length; i++) result [i] = items [display.treeSelection [i]];
	return result;
}

/**
 * Returns the number of selected items contained in the receiver.
 *
 * @return the number of selected items
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int getSelectionCount () {
	checkWidget();
	Display display = getDisplay ();
	display.treeSelectionLength = 0;
	display.treeSelection = null;
	int selection = OS.gtk_tree_view_get_selection (handle);
	OS.gtk_tree_selection_selected_foreach (selection, display.treeSelectionProc, handle);
	return display.treeSelectionLength;
}

/**
 * Returns the zero-relative index of the item which is currently
 * selected in the receiver, or -1 if no item is selected.
 *
 * @return the index of the selected item
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int getSelectionIndex () {
	checkWidget();
	Display display = getDisplay ();
	display.treeSelectionLength  = 0;
	display.treeSelection = new int [itemCount];
	int selection = OS.gtk_tree_view_get_selection (handle);
	OS.gtk_tree_selection_selected_foreach (selection, display.treeSelectionProc, handle);
	if (display.treeSelectionLength == 0) return -1;
	return display.treeSelection [0];
}

/**
 * Returns the zero-relative indices of the items which are currently
 * selected in the receiver.  The array is empty if no items are selected.
 * <p>
 * Note: This is not the actual structure used by the receiver
 * to maintain its selection, so modifying the array will
 * not affect the receiver. 
 * </p>
 * @return the array of indices of the selected items
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int [] getSelectionIndices () {
	checkWidget();
	Display display = getDisplay ();
	display.treeSelectionLength  = 0;
	display.treeSelection = new int [itemCount];
	int selection = OS.gtk_tree_view_get_selection (handle);
	OS.gtk_tree_selection_selected_foreach (selection, display.treeSelectionProc, handle);
	if (display.treeSelectionLength == display.treeSelection.length) return display.treeSelection;
	int [] result = new int [display.treeSelectionLength];
	System.arraycopy (display.treeSelection, 0, result, 0, display.treeSelectionLength);
	return result;
}

/**
 * Returns the zero-relative index of the item which is currently
 * at the top of the receiver. This index can change when items are
 * scrolled or new items are added or removed.
 *
 * @return the index of the top item
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int getTopIndex () {
	checkWidget();
	int [] path = new int [1];
	if (!OS.gtk_tree_view_get_path_at_pos (handle, 1, 1, path, null, null, null)) return 0;
	if (path [0] == 0) return 0;
	int indices = OS.gtk_tree_path_get_indices (path[0]);
	int[] index = new int [1];
	if (indices != 0) OS.memmove (index, indices, 4);
	OS.gtk_tree_path_free (path [0]);
	return index [0];
}

void hookEvents () {
	//TO DO - get rid of enter/exit for mouse crossing border
	super.hookEvents ();
	Display display = getDisplay ();
	int selection = OS.gtk_tree_view_get_selection(handle);
	OS.g_signal_connect (selection, OS.changed, display.windowProc2, SWT.Selection);
	OS.g_signal_connect (handle, OS.row_activated, display.windowProc4, SWT.DefaultSelection);
}

/**
 * Searches the receiver's list starting at the first column
 * (index 0) until a column is found that is equal to the 
 * argument, and returns the index of that column. If no column
 * is found, returns -1.
 *
 * @param column the search column
 * @return the index of the column
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the string is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int indexOf (TableColumn column) {
	checkWidget();
	if (column == null) error (SWT.ERROR_NULL_ARGUMENT);
	for (int i=0; i<columnCount; i++) {
		if (columns [i] == column) return i;
	}
	return -1;
}

/**
 * Searches the receiver's list starting at the first item
 * (index 0) until an item is found that is equal to the 
 * argument, and returns the index of that item. If no item
 * is found, returns -1.
 *
 * @param item the search item
 * @return the index of the item
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the string is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int indexOf (TableItem item) {
	checkWidget();
	if (item == null) error (SWT.ERROR_NULL_ARGUMENT);
	for (int i=0; i<itemCount; i++) {
		if (items [i] == item) return i;
	}
	return -1;
}

/**
 * Returns <code>true</code> if the item is selected,
 * and <code>false</code> otherwise.  Indices out of
 * range are ignored.
 *
 * @param index the index of the item
 * @return the visibility state of the item at the index
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public boolean isSelected (int index) {
	checkWidget();
	int selection = OS.gtk_tree_view_get_selection (handle);
	byte [] buffer = Converter.wcsToMbcs (null, Integer.toString (index), true);
	int path = OS.gtk_tree_path_new_from_string (buffer);
	boolean answer = OS.gtk_tree_selection_path_is_selected (selection, path);
	OS.gtk_tree_path_free (path);
	return answer;
}

int paintWindow () {
	OS.gtk_widget_realize (handle);
	return OS.gtk_tree_view_get_bin_window (handle);
}

int processDefaultSelection (int int0, int int1, int int2) {
	TableItem item = null;
	int indices = OS.gtk_tree_path_get_indices (int0);
	if (indices != 0) {
		int [] index = new int []{-1};
		OS.memmove (index, indices, 4);
		item = items [index [0]]; 
	}
	Event event = new Event ();
	event.item = item;
	postEvent (SWT.DefaultSelection, event);
	return 0;
}

int processMouseDown (int int0, int int1, int int2) {
	GdkEventButton e = new GdkEventButton ();
	OS.memmove (e, int0, GdkEventButton.sizeof);
	int headerHeight = getHeaderHeight();
	e.y += headerHeight;
	OS.memmove (int0, e, GdkEventButton.sizeof);
	int result = super.processMouseDown (int0, int1, int2);
	e.y -= headerHeight;
	OS.memmove (int0, e, GdkEventButton.sizeof);
	return result;
}

int processMouseMove (int int0, int int1, int int2) {
	GdkEventButton e = new GdkEventButton ();
	OS.memmove (e, int0, GdkEventButton.sizeof);
	int headerHeight = getHeaderHeight ();
	e.y += headerHeight;
	OS.memmove (int0, e, GdkEventButton.sizeof);
	int result = super.processMouseMove (int0, int1, int2);
	e.y -= headerHeight;
	OS.memmove (int0, e, GdkEventButton.sizeof);
	return result;
}

int processMouseUp (int int0, int int1, int int2) {
	GdkEventButton e = new GdkEventButton ();
	OS.memmove (e, int0, GdkEventButton.sizeof);
	int headerHeight = getHeaderHeight();
	e.y += headerHeight;
	OS.memmove (int0, e, GdkEventButton.sizeof);
	int result = super.processMouseUp (int0, int1, int2);
	e.y -= headerHeight;
	OS.memmove (int0, e, GdkEventButton.sizeof);
	return result;
}

int processKeyDown (int callData, int arg1, int int2) {
	int result = super.processKeyDown (callData, arg1, int2);
	if (result != 0) return result;

	/*
	* Feature in GTK.  When an item is default selected using
	* the return key, GTK does not issue notification. The fix is
	* to issue this notification when the return key is pressed.
	*/
	GdkEventKey keyEvent = new GdkEventKey ();
	OS.memmove (keyEvent, callData, GdkEventKey.sizeof);
	int key = keyEvent.keyval;
	switch (key) {
		case OS.GDK_Return:
		case OS.GDK_KP_Enter: {
			Event event = new Event ();
			event.item = getFocusItem (); 
			postEvent (SWT.DefaultSelection, event);
			break;
		}
	}
	return result;
}

int processSelection (int int0, int int1, int int2) {
	TableItem item = getFocusItem ();
	if (item != null) {
		Event event = new Event ();
		event.item = item; 
		postEvent (SWT.Selection, event);
	}
	return 0;
}

int processTreeColumnSelection (int column) {
	for (int i=0; i<columnCount; i++) {
		if (column == columns [i].handle) {
			columns [i].postEvent (SWT.Selection);
			break;
		}
	}
	return 0;
}

int processTreeSelection (int model, int path, int iter, int[] selection, int length) {
	if (selection != null) { 
		int indices = OS.gtk_tree_path_get_indices (path);
		if (indices != 0) {
			int [] index = new int [1];
			OS.memmove (index, indices, 4);
			selection [length] = index [0];
		}
	}
	return 0;
}

int processTreeToggle (int renderer, int arg1) {
	int path = OS.gtk_tree_path_new_from_string (arg1);
	if (path == 0) return 0;
	int indices = OS.gtk_tree_path_get_indices (path);
	if (indices != 0) {
		int [] index = new int [1];
		OS.memmove (index, indices, 4);
		TableItem item = items [index [0]];
		item.setChecked (!item.getChecked ());
		Event event = new Event ();
		event.detail = SWT.CHECK;
		event.item = item;
		postEvent (SWT.Selection, event);
	}
	OS.gtk_tree_path_free (path);
	return 0;
}

void releaseWidget () {
	for (int i=0; i<columnCount; i++) {
		TableColumn column = columns [i];
		if (!column.isDisposed ()) column.releaseResources ();
	}
	columns = null;
	for (int i=0; i<itemCount; i++) {
		TableItem item = items [i];
		if (!item.isDisposed ()) item.releaseResources ();
	}
	items = null;
	super.releaseWidget ();
	if (modelHandle != 0) OS.g_object_unref (modelHandle);
	modelHandle = 0;
	if (imageList != null) {
		imageList.dispose ();
		imageList = null;
	}
}

void register () {
	super.register ();
	WidgetTable.put (OS.gtk_tree_view_get_selection (handle), this);
}

/**
 * Removes the item from the receiver at the given
 * zero-relative index.
 *
 * @param index the index for the item
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_RANGE - if the index is not between 0 and the number of elements in the list minus 1 (inclusive)</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception SWTError <ul>
 *    <li>ERROR_ITEM_NOT_REMOVED - if the operation fails because of an operating system failure</li>
 * </ul>
 */
public void remove (int index) {
	checkWidget();
	if (!(0 <= index && index <= itemCount)) error (SWT.ERROR_ITEM_NOT_REMOVED);
	TableItem item = items [index];
	OS.gtk_list_store_remove (modelHandle, item.handle);
	System.arraycopy (items, index + 1, items, index, --itemCount - index);
	items [itemCount] = null;
	item.releaseResources ();
}

/**
 * Removes the items from the receiver which are
 * between the given zero-relative start and end 
 * indices (inclusive).
 *
 * @param start the start of the range
 * @param end the end of the range
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_RANGE - if either the start or end are not between 0 and the number of elements in the list minus 1 (inclusive)</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception SWTError <ul>
 *    <li>ERROR_ITEM_NOT_REMOVED - if the operation fails because of an operating system failure</li>
 * </ul>
 */
public void remove (int start, int end) {
	checkWidget();
	int index = start;
	while (index <= end) {
		if (index < 0 || index >= itemCount) break;
		TableItem item = items [index];
		OS.gtk_list_store_remove (modelHandle, item.handle);
		item.releaseResources ();
		index++;
	}
	System.arraycopy (items, index, items, start, itemCount - index);
	for (int i=itemCount-(index-start); i<itemCount; i++) items [i] = null;
	itemCount = itemCount - (index - start);
	if (index <= end) error (SWT.ERROR_INVALID_RANGE);
}

/**
 * Removes the items from the receiver's list at the given
 * zero-relative indices.
 *
 * @param indices the array of indices of the items
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_RANGE - if the index is not between 0 and the number of elements in the list minus 1 (inclusive)</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception SWTError <ul>
 *    <li>ERROR_ITEM_NOT_REMOVED - if the operation fails because of an operating system failure</li>
 * </ul>
 */
public void remove (int [] indices) {
	checkWidget();
	if (indices == null) error (SWT.ERROR_NULL_ARGUMENT);
	int [] newIndices = new int [indices.length];
	System.arraycopy (indices, 0, newIndices, 0, indices.length);
	sort (newIndices);
	int last = -1;
	for (int i=0; i<newIndices.length; i++) {
		int index = newIndices [i];
		if (index != last || i == 0) {
			if (index < 0 || index >= itemCount) error (SWT.ERROR_INVALID_RANGE);
			TableItem item = items [index];
			OS.gtk_list_store_remove (modelHandle, item.handle);
			// BUG - disposed callback could remove an item
			item.releaseResources ();
			System.arraycopy (items, index + 1, items, index, --itemCount - index);
			items [itemCount] = null;
			last = index;
		}
	}
}

/**
 * Removes all of the items from the receiver.
 * <p>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void removeAll () {
	checkWidget();
	OS.gtk_list_store_clear (modelHandle);
	int index = itemCount - 1;
	while (index >= 0) {
		TableItem item = items [index];
		item.releaseResources ();
		--index;
	}
	items = new TableItem [4];
	itemCount = 0;
}

/**
 * Removes the listener from the collection of listeners who will
 * be notified when the receiver's selection changes.
 *
 * @param listener the listener which should no longer be notified
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see SelectionListener
 * @see #addSelectionListener
 */
public void removeSelectionListener(SelectionListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook (SWT.Selection, listener);
	eventTable.unhook (SWT.DefaultSelection,listener);	
}

/**
 * Selects the item at the given zero-relative index in the receiver. 
 * If the item at the index was already selected, it remains
 * selected. Indices that are out of range are ignored.
 *
 * @param index the index of the item to select
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void select (int index) {
	checkWidget();
	if (index <0 || index >= itemCount) return;
	blockSignal (handle, SWT.Selection);
	int selection = OS.gtk_tree_view_get_selection (handle);
	OS.gtk_tree_selection_select_iter (selection, items [index].handle);
	unblockSignal (handle, SWT.Selection);
}

/**
 * Selects the items at the given zero-relative indices in the receiver.
 * If the item at the index was already selected, it remains
 * selected. The range of the indices is inclusive. Indices that are
 * out of range are ignored.
 *
 * @param start the start of the range
 * @param end the end of the range
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void select (int start, int end) {
	checkWidget();
	blockSignal (handle, SWT.Selection);
	int selection = OS.gtk_tree_view_get_selection (handle);
	for (int index=start; index<=end; index++) {
		if (index < 0 || index >= itemCount) continue;
		OS.gtk_tree_selection_select_iter (selection, items [index].handle);
	}
	unblockSignal (handle, SWT.Selection);
}

/**
 * Selects the items at the given zero-relative indices in the receiver.
 * If the item at the given zero-relative index in the receiver 
 * is not selected, it is selected.  If the item at the index
 * was selected, it remains selected. Indices that are out
 * of range and duplicate indices are ignored.
 *
 * @param indices the array of indices for the items to select
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the array of indices is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void select (int [] indices) {
	checkWidget();
	if (indices == null) error (SWT.ERROR_NULL_ARGUMENT);
	blockSignal (handle, SWT.Selection);
	int selection = OS.gtk_tree_view_get_selection (handle);
	for (int i=0; i<indices.length; i++) {
		int index = indices [i];
		if (index < 0 || index >= itemCount) continue;
		OS.gtk_tree_selection_select_iter (selection, items [index].handle);
	}
	unblockSignal (handle, SWT.Selection);
}

/**
 * Selects all the items in the receiver.
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void selectAll () {
	checkWidget();
	blockSignal (handle, SWT.Selection);
	int selection = OS.gtk_tree_view_get_selection (handle);
	OS.gtk_tree_selection_select_all (selection);
	unblockSignal (handle, SWT.Selection);
}

void setBackgroundColor (GdkColor color) {
	super.setBackgroundColor (color);
	OS.gtk_widget_modify_base (handle, 0, color);
}

void setForegroundColor (GdkColor color) {
	super.setForegroundColor (color);
	OS.gtk_widget_modify_text (handle, 0, color);
}

/**
 * Marks the receiver's header as visible if the argument is <code>true</code>,
 * and marks it invisible otherwise. 
 * <p>
 * If one of the receiver's ancestors is not visible or some
 * other condition makes the receiver not visible, marking
 * it visible may not actually cause it to be displayed.
 * </p>
 *
 * @param visible the new visibility state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setHeaderVisible (boolean show) {
	checkWidget ();
	OS.gtk_tree_view_set_headers_visible (handle, show);
}

/**
 * Marks the receiver's lines as visible if the argument is <code>true</code>,
 * and marks it invisible otherwise. 
 * <p>
 * If one of the receiver's ancestors is not visible or some
 * other condition makes the receiver not visible, marking
 * it visible may not actually cause it to be displayed.
 * </p>
 *
 * @param visible the new visibility state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setLinesVisible (boolean show) {
	checkWidget();
}

/**
 * Selects the item at the given zero-relative index in the receiver. 
 * The current selected is first cleared, then the new item is selected.
 *
 * @param index the index of the item to select
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see Table#deselectAll()
 * @see Table#select(int)
 */
public void setSelection (int index) {
	deselectAll ();
	select (index);
}

/**
 * Selects the items at the given zero-relative indices in the receiver. 
 * The current selected if first cleared, then the new items are selected.
 *
 * @param start the start index of the items to select
 * @param end the end index of the items to select
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see Table#deselectAll()
 * @see Table#select(int,int)
 */
public void setSelection (int start, int end) {
	deselectAll ();
	select (start, end);
}

/**
 * Selects the items at the given zero-relative indices in the receiver. 
 * The current selected is first cleared, then the new items are selected.
 *
 * @param indices the indices of the items to select
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the array of indices is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see Table#deselectAll()
 * @see Table#select(int[])
 */
public void setSelection (int [] indices) {
	deselectAll ();
	select (indices);
}

/**
 * Sets the receiver's selection to be the given array of items.
 * The current selected is first cleared, then the new items are
 * selected.
 *
 * @param items the array of items
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the array of items is null</li>
 *    <li>ERROR_INVALID_ARGUMENT - if one of the item has been disposed</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see Table#deselectAll()
 * @see Table#select(int)
 */
public void setSelection (TableItem [] items) {
	checkWidget();
	if (items == null) error (SWT.ERROR_NULL_ARGUMENT);
	deselectAll ();
	int length = items.length;
	if (length == 0) return;
	if ((style & SWT.SINGLE) != 0) length = 1;
	for (int i=length-1; i>=0; --i) {
		int index = indexOf (items [i]);
		if (index != -1) select (index);
	}
}

/**
 * Sets the zero-relative index of the item which is currently
 * at the top of the receiver. This index can change when items
 * are scrolled or new items are added and removed.
 *
 * @param index the index of the top item
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setTopIndex (int index) {
	checkWidget();
	if (!(0 <= index && index < itemCount)) return;
	// FIXME - For some reason, sometimes the tree scrolls to the wrong place
	int path = OS.gtk_tree_model_get_path (modelHandle, items [index].handle);
	OS.gtk_tree_view_scroll_to_cell (handle, path, 0, true, 0, 0);
	OS.gtk_tree_path_free (path);
}

/**
 * Shows the item.  If the item is already showing in the receiver,
 * this method simply returns.  Otherwise, the items are scrolled until
 * the item is visible.
 *
 * @param item the item to be shown
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the item is null</li>
 *    <li>ERROR_INVALID_ARGUMENT - if the item has been disposed</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see Table#showSelection()
 */
public void showItem (TableItem item) {
	checkWidget ();
	if (item == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (item.isDisposed()) error (SWT.ERROR_INVALID_ARGUMENT);
	int path = OS.gtk_tree_model_get_path (modelHandle, item.handle);
	OS.gtk_tree_view_scroll_to_cell (handle, path, 0, false, 0, 0);
	OS.gtk_tree_path_free (path);
}

/**
 * Shows the selection.  If the selection is already showing in the receiver,
 * this method simply returns.  Otherwise, the items are scrolled until
 * the selection is visible.
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see Table#showItem(TableItem)
 */
public void showSelection () {
	checkWidget();
	TableItem [] selection = getSelection ();
	if (selection.length == 0) return;
	TableItem item = selection [0];
	int path = OS.gtk_tree_model_get_path (modelHandle, item.handle);
	OS.gtk_tree_view_scroll_to_cell (handle, path, 0, false, 0, 0);
	OS.gtk_tree_path_free (path);
}

}
