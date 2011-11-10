/*******************************************************************************
 * Copyright (c) 2011 EBM Websourcing (PetalsLink)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mickael Istria (EBM - PetalsLink) : initial implementation
 *******************************************************************************/
package org.eclipse.draw2d.preview;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	public static String notADraw2DFigure_title;
	public static String notADraw2DFigure_message;
	public static String javaError_message;
	public static String javaError_title;
	public static String internalError_title;
	public static String internalError_message;
	public static String couldNotInstantiate_message;
	public static String couldNotInstantiate_title;
	public static String previewTitle;
	public static String errorInFigure_title;
	public static String errorInFigure_message;

	static {
		initializeMessages("messages", Messages.class);
	}
}
