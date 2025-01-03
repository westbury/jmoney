/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2022 Nigel Westbury <westbury@users.sourceforge.net>
*
*
*  This program is free software; you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation; either version 2 of the License, or
*  (at your option) any later version.
*
*  This program is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU General Public License for more details.
*
*  You should have received a copy of the GNU General Public License
*  along with this program; if not, write to the Free Software
*  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
*
*/

package net.sf.jmoney.importer.wizards;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import txr.debug.ITxrSource;
import txr.debug3x.TxrDebugView;

/**
 * This exception indicates that the TXR match failed. We handle this situation by showing
 * the mismatch in the TXR debugging view.
 *
 * @author Nigel Westbury
 *
 */
public class TxrMismatchException extends Exception {
	
	private static class TxrSourceInWorkspace implements ITxrSource {

		private File txrSrcFile;
		private File txrBinFile;
		private IInvalidator matcherInvalidator;

		TxrSourceInWorkspace(File txrSrcFile, File txrBinFile, IInvalidator matcherInvalidator) {
			this.txrBinFile = txrBinFile;
			this.txrSrcFile = txrSrcFile;
			this.matcherInvalidator = matcherInvalidator;
		}
		@Override
		public boolean isEditable() {
			return true;
		}
		@Override
		public String[] readLines() {
	        try (
	        	BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(txrSrcFile), "UTF-8"))
	        ) {
				return reader.lines().toArray(String[]::new);
	        } catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		@Override
		public void writeChanges(String[] txrLines) {
			String txrData = String.join("\n", txrLines);
			
	        try {
				try (BufferedWriter writer = new BufferedWriter(new FileWriter(txrSrcFile, false))) {
                    writer.write(txrData);
                    System.out.println("Content successfully written to file: " + txrSrcFile.getAbsolutePath());
				}
				
				try (BufferedWriter writer = new BufferedWriter(new FileWriter(txrBinFile, false))) {
                    writer.write(txrData);
                    System.out.println("Content successfully written to file: " + txrBinFile.getAbsolutePath());
				}
		    } catch (IOException e) {
				throw new RuntimeException(e);
			}
	        
	        matcherInvalidator.invalidateMatcher(txrLines);
		}
	}

	private static class TxrSourceInReadOnlyJar implements ITxrSource {

		private URL txrResourceUrl;

		TxrSourceInReadOnlyJar(URL txrResourceUrl) {
			this.txrResourceUrl = txrResourceUrl;
		}

		@Override
		public boolean isEditable() {
			return false;
		}

		@Override
		public String[] readLines() {
			try (
				BufferedReader reader = new BufferedReader(new InputStreamReader(txrResourceUrl.openStream(), "UTF-8"))
			) {
				return reader.lines().toArray(String[]::new);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void writeChanges(String[] txrLines) {
			throw new UnsupportedOperationException();
		}
	}

	private static final long serialVersionUID = 1L;

	private URL resource;
	private String inputText;
	private String sourceDescription;
	
	public interface IInvalidator {
		void invalidateMatcher(String [] txrLines);
	}
	
	public TxrMismatchException(URL resource, String inputText, String sourceDescription) {
		this.resource = resource;
		this.inputText = inputText;
		this.sourceDescription = sourceDescription;
	}

	public void showInDebugView(IWorkbenchWindow window, IInvalidator matcherInvalidator) {
		MessageDialog dialog = new MessageDialog(
				window.getShell(),
				"Data Match Failure",
				null, // accept the default window icon
				"Data in clipboard does not appear to be copied from " + sourceDescription + ".",
				MessageDialog.ERROR,
				new String[] { "Debug", IDialogConstants.CANCEL_LABEL },
				1);
		int resultCode = dialog.open();
		if (resultCode == 0) {
			try {
				TxrDebugView view = (TxrDebugView)window.getActivePage().showView(TxrDebugView.ID, null, IWorkbenchPage.VIEW_ACTIVATE);

				ITxrSource txrSource = createTxrSource(resource, matcherInvalidator);

				view.setTxrAndData(txrSource, inputText.split("\n"));
			} catch (PartInitException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
		}		
	}

	public static ITxrSource createTxrSource(URL txrResourceUrl, IInvalidator matcherInvalidator) throws IOException, URISyntaxException {
		URL txrFileUrl = FileLocator.resolve(txrResourceUrl);
		assert ("file".equals(txrFileUrl.getProtocol()));
		File binFile = new File(txrFileUrl.toURI());
		String binPath = binFile.getAbsolutePath();

		ITxrSource txrSource;
		if (binPath.contains("/bin/")) {
			// Assume it's the bin path in an Eclipse source project.

			String srcPath = binPath.replace("/bin/", "/src/");
			File srcFile = new File(srcPath);

			txrSource = new TxrSourceInWorkspace(srcFile, binFile, matcherInvalidator);
		} else {
			txrSource = new TxrSourceInReadOnlyJar(txrResourceUrl);
		}
		return txrSource;
	}

}
