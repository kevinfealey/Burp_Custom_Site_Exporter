package burp;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

public class BurpExtender implements IBurpExtender, IContextMenuFactory
{
    private IBurpExtenderCallbacks callbacks;
    private IExtensionHelpers helpers;
    
	public static void main(String [] args)
	{
		
	}
	
    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks)
    {
        this.callbacks = callbacks;
        helpers = callbacks.getHelpers();
        callbacks.setExtensionName("Custom Site Exporter");
        callbacks.registerContextMenuFactory(BurpExtender.this);
    }

    public void writeStringToFile(String Output, File file){
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(file));
            out.write(Output);
            JOptionPane.showMessageDialog(null, "File saved successfully.");
        } catch ( IOException e1 ) {
        	JOptionPane.showMessageDialog(null, "Error saving file: " + e1.getMessage());
        } finally {
        	try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
    }
    
    public String createOutputForAppScanStandard(IHttpRequestResponse tmp[]){
    	callbacks.printOutput("OK, we called CreateOutput: " + tmp.length);
    	String Output = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<requests>\n";
    	for(int i = 0; i < tmp.length; i++){
        	String protocol =  tmp[i].getHttpService().getProtocol();
    		String method = helpers.analyzeRequest(tmp[i].getRequest()).getMethod();
    		String path = "/";
    		String tmpStr = new String(tmp[i].getRequest());
    		int firstslash = tmpStr.indexOf(" ");
    		int secondslash = tmpStr.indexOf(" ", firstslash + 1);
    		int questionmark = tmpStr.indexOf("?", firstslash + 1);
    		if(questionmark < secondslash && questionmark > 0){
    			secondslash = questionmark;
    		}
    		path = tmpStr.substring(firstslash + 1, secondslash).replace("\"", "%22");
    		int port = tmp[i].getHttpService().getPort();
    		String host =  tmp[i].getHttpService().getHost();
    		Output += "\t<request method=\"" + method + "\" scheme=\"" + protocol + "\" httpVersion=\"HTTP/1.1\" host=\"" + host + "\"  port=\"" + port + "\" path=\"" + path + "\"></request>\n";
    	}
    	Output += "</requests>";
    	return Output;
    }
    
    @Override
    public ArrayList<JMenuItem> createMenuItems(IContextMenuInvocation invocation) {
        ArrayList<JMenuItem> menu = new ArrayList<JMenuItem>();
        
        byte ctx = invocation.getInvocationContext();
        
    	JMenu main = new JMenu("Custom Site Exporter");
        JMenuItem item = new JMenuItem("Export From Proxy History", null);
        JMenuItem item2 = new JMenuItem("Export Site Map", null);
        
        FileFilter filter = new FileNameExtensionFilter("XML File","xml");
                
        JFileChooser fileChooser = new JFileChooser(){
        	@Override
            public void approveSelection(){
                File f = getSelectedFile();
                if(f.exists() && getDialogType() == SAVE_DIALOG){
                    int result = JOptionPane.showConfirmDialog(this,"The file exists, overwrite?","Existing file",JOptionPane.YES_NO_CANCEL_OPTION);
                    switch(result){
                        case JOptionPane.YES_OPTION:
                            super.approveSelection();
                            return;
                        case JOptionPane.NO_OPTION:
                            return;
                        case JOptionPane.CLOSED_OPTION:
                            return;
                        case JOptionPane.CANCEL_OPTION:
                            cancelSelection();
                            return;
                    }
                }
                super.approveSelection();
            }        
        };
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileFilter(filter);
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        fileChooser.setSelectedFile(new File("AppScanStandard_ManualExplore_FromBurp.xml"));

        
    	item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {  
            	IHttpRequestResponse[] tmp = callbacks.getProxyHistory();
            	if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION){
            		File outputFile = fileChooser.getSelectedFile();
            		writeStringToFile(createOutputForAppScanStandard(tmp), outputFile);
            	}
            }
        });
    	
    	item2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	IHttpRequestResponse tmp2  = invocation.getSelectedMessages()[0];
            	IHttpRequestResponse[] tmp = callbacks.getSiteMap(tmp2.getHttpService().getProtocol() + "://" + tmp2.getHttpService().getHost());
            	if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION){
            		File outputFile = fileChooser.getSelectedFile();
            		writeStringToFile(createOutputForAppScanStandard(tmp), outputFile);
            	}
            }
        });
    	main.add(item);
    	main.add(item2);
    	menu.add(main);
        
        if (ctx == IContextMenuInvocation.CONTEXT_PROXY_HISTORY) {
        	item2.setEnabled(false);
            item.setEnabled(true);
        }
        if (ctx == IContextMenuInvocation.CONTEXT_TARGET_SITE_MAP_TREE) {
        	item.setEnabled(false);
            item2.setEnabled(true);
        }
        
        
        
        return menu;
    }
}
