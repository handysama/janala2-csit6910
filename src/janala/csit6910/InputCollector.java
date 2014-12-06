package janala.csit6910;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import janala.config.Config;

/**
 * Capture input generated after solved.
 * */
public class InputCollector {

	protected List<Object[]> inputs;
	private String classname;
	
	public InputCollector(String clazz) {
		inputs = new ArrayList<Object[]>();

		// unlike from offline, we can get classname from Config.mainClass
		// online process get classname from -ea in vmArguments
		if (clazz == null || clazz.isEmpty()) {

			// sun.java.command will give us both the main class (or jar) and the program arguments
			String[] programArguments = System.getProperty("sun.java.command").split(" ");

			if (programArguments.length > 0) {
				clazz = programArguments[0];
			}

			// alternative approach
//			java.lang.management.RuntimeMXBean mx = java.lang.management.ManagementFactory.getRuntimeMXBean();
//	  	java.util.List<String> vmArguments = mx.getInputArguments();
//	  	for (String s: vmArguments) {
//	  		System.out.println("args: " + s);
//	  		if (s.contains("-ea")) {
//	  			clazz = s.substring(4, s.length());
//	  			break;
//	  		}
//	  	}
		}
		this.classname = clazz;
	}

	/**
	 * Capture janala inputs file in here, then write JUnit PUTs test.<br />
	 * To process after iterations, we simply use append flag.
	 * */
	public void collectInputFile() {
		DataInputStream in = null;
		BufferedReader br = null;
		List<Object> v = new ArrayList<Object>();
		
		try {
			in = new DataInputStream(new FileInputStream(Config.instance.inputs));
			br = new BufferedReader(new InputStreamReader(in));
			String strLine = br.readLine();
			while (strLine  != null) {
				v.add(strLine);
				strLine = br.readLine();
			}

			inputs.add(v.toArray());			
			//writeFileInput(); // trace purpose

			System.out.println("input: " + inputs.size());
			JunitTestWriter jw = new JunitTestWriter(classname, "JanalaTest", System.getProperty("user.dir"));
			jw.writeJunitTest(inputs);

		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException ex) {}
			if (br != null)
				try {
					br.close();
				} catch (IOException e) {}
		}
	}

	public String getClassname() {
		return classname;
	}

	public void setClassname(String c) {
		classname = c;
	}
	
	/**
	 * Experimental purpose for dump captured inputs.
	 * */
	private void writeFileInput() {
		String workingDir = System.getProperty("user.home") + "/JanalaTest/" ;
		File file = new File(workingDir + classname + ".in");
		boolean append = file.exists();

		if (!append)
			(new File(workingDir)).mkdir();

		PrintWriter out = JunitTestWriter.createPrintWriter(file, append);

		try {
			if (append) {
				out.println(",");
			}
			out.print("{");
			for (int i = 0; i < inputs.size(); ++i) {
				Object[] o = inputs.get(i);
				for (int j = 0; j < o.length; ++j) {
					if (j > 0) out.print(", ");
					out.print( o[j].toString() );
				}
			}
			out.print("}");
		} finally {
			if (out != null)
				out.close();
		}
	}
	
}
