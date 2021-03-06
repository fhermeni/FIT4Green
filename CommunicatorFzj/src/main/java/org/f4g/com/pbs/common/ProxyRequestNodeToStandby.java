/**
* ============================== Header ============================== 
* file:          ProxyRequestStartJob.java
* project:       FIT4Green/CommunicatorFzj
* created:       Dec 8, 2010 by brinkers
* 
* $LastChangedDate: 2012-02-21 19:21:17 +0100 (Tue, 21 Feb 2012) $ 
* $LastChangedBy: f4g.julichde $
* $LastChangedRevision: 1167 $
* 
* short description:
*   {To be completed}
* ============================= /Header ==============================
*/
package org.f4g.com.pbs.common;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Executes a shell command to set a node in sleep modus
 * 
 *
 * @author Andre Giesler
 */
public class ProxyRequestNodeToStandby extends ProxyRequest {
	
	/**
	 * 
	 */
	
	static Logger log = Logger.getLogger(ProxyRequestNodeToStandby.class.getName());
	
	private String execCmd;	
	private String execDir;	
	private static final long serialVersionUID = 1L;
	private String node;
	
	public ProxyRequestNodeToStandby(String node, String execCmd, String execDir){
		setExecCmd(execCmd);
		setExecDir(execDir);
		setNode(node);
	}

	/* (non-Javadoc)
	 * @see org.f4g.com.Fzj.ProxyRequest#execute()
	 */
	@Override
	public ProxyResponse execute() throws IOException {
		boolean success = true;
		String message = "";
		
		ProcessBuilder processBuilder = new ProcessBuilder();

		processBuilder.command(execCmd,  "-node", getNode());
		List<String> com_and_args = processBuilder.command();
		for(String s : com_and_args){
			log.debug("arg: " + s);
		}
		processBuilder.directory(new File(execDir));

		Process process = null;
		try {
			process = processBuilder.start();
			try {
				process.waitFor();
			} catch (InterruptedException e) {
				log.error("Cant't get exit code for " + execCmd + 
						" connect" + ". " + e.getMessage());
			}

			if(process.exitValue() !=0) {

				byte[] bytes = new byte[1024];
				InputStream errorStream = process.getErrorStream();
				errorStream.read(bytes);
				String error = new String(bytes);

				log.error(error + "Exit code for " + execCmd + " is not zero");
				
				success = false;
				message = error;
			}
			else
			{
				InputStream inputStream = process.getInputStream();
				InputStreamReader tempReader = new InputStreamReader(
						new BufferedInputStream(inputStream));
				BufferedReader reader = new BufferedReader(tempReader);
				StringBuffer buf = new StringBuffer();
				while (true){
					String line = reader.readLine();
					buf.append(line);
					if (line == null)
						break;
					log.debug(line);
				}
				message = buf.toString();		
			}
		} catch (IOException e1) {
			e1.printStackTrace();
			log.error(e1.getMessage());
		}
		finally {
			if (process != null) {
				close(process.getOutputStream());
				close(process.getInputStream());
				close(process.getErrorStream());
				process.destroy();
				log.debug("Closed all streams and destroyed process");
			}
		}

		ProxyResponseNodeToStandby proxyResponsePowerOff = new ProxyResponseNodeToStandby();
		proxyResponsePowerOff.setSuccess(success);
		proxyResponsePowerOff.setMessage(message);
		return proxyResponsePowerOff;
	}

	private void close(Closeable c) {
	    if (c != null) {
	      try {
	        c.close();
	      } catch (IOException e) {
	    	  log.debug("Error when closing process stream");
	      }
	    }
	  }

	/**
	 * @param nodes_ the nodes_ to set
	 */
	public void setNode(String node_) {
		this.node = node_;
	}

	/**
	 * @return the nodes_
	 */
	public String getNode() {
		return node;
	}

	/**
	 * @return the execCmd
	 */
	public String getExecCmd() {
		return execCmd;
	}

	/**
	 * @param execCmd the execCmd to set
	 */
	public void setExecCmd(String execCmd) {
		this.execCmd = execCmd;
	}

	/**
	 * @return the execDir
	 */
	public String getExecDir() {
		return execDir;
	}

	/**
	 * @param execDir the execDir to set
	 */
	public void setExecDir(String execDir) {
		this.execDir = execDir;
	}

}
