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
package org.eclipse.swt.graphics;


public class DeviceData {
	/*
	* The following fields are platform dependent.
	*/
	public String display_name;
	public String application_name;
	public String application_class;

	/*
	* Debug fields - may not be honoured
	* on some SWT platforms.
	*/
	public boolean debug;
	public boolean tracking;
	public Error [] errors;
	public Object [] objects;
}
