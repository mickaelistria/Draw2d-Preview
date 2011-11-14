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

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * 
 * @author istria
 *
 */
public class OpenFigureViewerHandler extends AbstractHandler {


	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		ISelection selection = activeWorkbenchWindow.getSelectionService().getSelection();
		if (selection instanceof IStructuredSelection) {
			Object item = ((IStructuredSelection) selection).getFirstElement();
			IFigure figureObject = getFigure(activeWorkbenchWindow, item);
			if (figureObject == null) {
				return null;
			}
			Shell shell = new Shell(activeWorkbenchWindow.getShell(), SWT.RESIZE);
			shell.setText(Messages.bind(Messages.previewTitle, figureObject.getClass().getSimpleName()));
			shell.setLayout(new GridLayout(1, false));
			FigureCanvas canvas = new FigureCanvas(shell);
			GridData canvasLayoutData = new GridData();
			canvasLayoutData.grabExcessHorizontalSpace = true;
			canvasLayoutData.grabExcessVerticalSpace = true;
			canvasLayoutData.horizontalAlignment = SWT.FILL;
			canvasLayoutData.verticalAlignment = SWT.FILL;
			canvas.setLayoutData(canvasLayoutData);
			canvas.setBackground(ColorConstants.white);
			RectangleFigure background = new RectangleFigure();
			background.setForegroundColor(ColorConstants.white);
			background.setBackgroundColor(ColorConstants.white);
			org.eclipse.draw2d.GridLayout rectangleLayout = new org.eclipse.draw2d.GridLayout();
			background.setLayoutManager(rectangleLayout);
			rectangleLayout.marginHeight = 10;
			rectangleLayout.marginWidth = 10;
			background.add(figureObject, new org.eclipse.draw2d.GridData(SWT.FILL, SWT.FILL, true, true));
			canvas.setContents(background);
			shell.pack();
			shell.open();
		}

		return null;
	}
	
	@Override
	public boolean isEnabled() {
		IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		ISelection selection = activeWorkbenchWindow.getSelectionService().getSelection();
		if (selection instanceof IStructuredSelection) {
			Object item = ((IStructuredSelection) selection).getFirstElement();
			return item instanceof ICompilationUnit ||
					item instanceof IType ||
					item instanceof IClassFile ||
					item instanceof IFile;
		}
		return false;
	}

	private IFigure getFigure(IWorkbenchWindow activeWorkbenchWindow, Object item) {
		IType figureClass = null;
		try {
			if (item instanceof ICompilationUnit) {
				figureClass = ((ICompilationUnit) item).getTypes()[0];
			} else if (item instanceof IType) {
				figureClass = (IType) item;
			} else if (item instanceof IClassFile) {
				figureClass = ((IClassFile) item).getType();
			} else if (item instanceof IFile) {
				ICompilationUnit compilationUnit = JavaCore.createCompilationUnitFrom((IFile) item);
				figureClass = compilationUnit.getTypes()[0];
			}
			boolean isFigure = false;
			for (IType type : figureClass.newSupertypeHierarchy(null).getAllInterfaces()) {
				if (type.getFullyQualifiedName().equals(IFigure.class.getName())) {
					isFigure = true;
				}
			}
			if (!isFigure) {
				MessageDialog.openError(activeWorkbenchWindow.getShell(),
						Messages.notADraw2DFigure_title,
						Messages.notADraw2DFigure_message);
				return null;
			}
			List<URL> urls = new ArrayList<URL>();
			URL outputURL = toAbsoluteURL(figureClass.getJavaProject().getOutputLocation());
			urls.add(outputURL);
			for (IClasspathEntry entry : figureClass.getJavaProject().getResolvedClasspath(false)) {
				URL toAdd = null;
				switch (entry.getEntryKind()) {
				case IClasspathEntry.CPE_LIBRARY:
					toAdd = entry.getPath().toFile().toURI().toURL();
					break;
				case IClasspathEntry.CPE_PROJECT:
					IProject project = ResourcesPlugin.getWorkspace().getRoot()
							.getProject(entry.getPath().toString());
					IJavaProject javaProject = (IJavaProject) project.getNature(JavaCore.NATURE_ID);
					toAdd = toAbsoluteURL(javaProject.getOutputLocation());
					break;
				}
				if (toAdd != null) {
					urls.add(toAdd);
				}
			}
			ClassLoader cl = new URLClassLoader(urls.toArray(new URL[urls.size()]), this.getClass().getClassLoader());
			Class<? extends IFigure> figureClazz = (Class<? extends IFigure>) cl.loadClass(figureClass.getFullyQualifiedName());
			for (Constructor<?> cons : figureClazz.getConstructors()) {
				cons.setAccessible(true);
			}
			IFigure figureObject = figureClazz.newInstance();
			return figureObject;
		} catch (JavaModelException ex) {
			MessageDialog.openError(activeWorkbenchWindow.getShell(), Messages.javaError_title, Messages.javaError_message);
			return null;
		} catch (MalformedURLException e) {
			MessageDialog.openError(activeWorkbenchWindow.getShell(), Messages.internalError_title, Messages.internalError_message);
			return null;
		} catch (CoreException e) {
			MessageDialog.openError(activeWorkbenchWindow.getShell(), Messages.internalError_title, Messages.internalError_message);
			Activator.getDefault().getLog().log(e.getStatus());
			return null;
		} catch (ClassNotFoundException e) {
			MessageDialog.openError(activeWorkbenchWindow.getShell(), Messages.internalError_title, Messages.internalError_message);
			return null;
		} catch (InstantiationException e) {
			MessageDialog.openError(activeWorkbenchWindow.getShell(), Messages.couldNotInstantiate_title, Messages.couldNotInstantiate_message);
			return null;
		} catch (IllegalAccessException e) {
			MessageDialog.openError(activeWorkbenchWindow.getShell(), Messages.couldNotInstantiate_title, Messages.couldNotInstantiate_message);
			return null;
		} catch (Error e) {
			ErrorDialog.openError(activeWorkbenchWindow.getShell(), Messages.errorInFigure_title, Messages.errorInFigure_message, new Status(IStatus.ERROR, figureClass.getJavaProject().getElementName(), Messages.errorInFigure_message, e));
			return null;
		}
	}

	private URL toAbsoluteURL(IPath outputLocation) throws MalformedURLException {
		return new File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toString(), outputLocation.toString()).toURI().toURL();
	}

}
