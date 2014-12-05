package janala.csit6910;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import janala.config.Config;

public class JunitTestWriter {
	
	private static final String linesep = System.getProperty("line.separator");
	private String junitclassname;
	private String packageName;
	private String dirName;

	public JunitTestWriter(String classname, String packageName, String dirName) {
		this.junitclassname = classname;
		this.packageName = packageName;
		this.dirName = dirName;
	}

	/**
	 * Write junit PUTS by supplied inputs, if file already exists we append it.
	 * */
	public void writeJunitTest(List<Object[]> inputs) {
		File workingDir = getDir();
		File file = new File(workingDir, junitclassname + ".java");
		PrintWriter out = null;
		BufferedReader br = null;

		if (!file.exists()) {
			workingDir.mkdir();
			createJunitTemplate(file);
		}

		try {
			br = new BufferedReader(new FileReader(file));
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();
			
			while (line != null) {
				if (line.contains("$count")) {
					int beginIdx = line.lastIndexOf("$") + 1;
					int endIdx = line.lastIndexOf("*");
					int count = Integer.parseInt(line.substring(beginIdx, endIdx).trim());	
					line = count > 0 ? "     ,{" : "      {";
					for (int i = 0; i < inputs.size(); ++i) {
						Object[] o = inputs.get(i);
						for (int j = 0; j < o.length; ++j) {
							if (j > 0) line += ", ";
							line += toCodeString(o[j]);
						}
					}
					line += "}" + linesep;
					line += "      /* $count$" + (count+1) + " */";
				}
				sb.append(line + linesep);
				line = br.readLine();
			}

			out = createPrintWriter(file, false);
			out.print(sb.toString());

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (out != null)
				out.close();			
			if (br != null)
				try {
					br.close();
				} catch (IOException e) {}
		}
	}
	
	/**
	 * Write junit PUTS by supplied inputs and outputs, if file already exists we append it.<br />
	 * @param inputs set of valid inputs
	 * @param output set of return value, we assume at most return can be one-dimensional array.
	 * */
	public void writeJunitTest(List<Object[]> inputs, List<Object[]> output) {
		
		// row count should be equal
		assert inputs.size() == output.size();
		
		File workingDir = getDir();
		File file = new File(workingDir, junitclassname + ".java");
		PrintWriter out = null;
		BufferedReader br = null;

		if (!file.exists()) {
			workingDir.mkdir();
			createJunitTemplate(file);
		}

		try {
			br = new BufferedReader(new FileReader(file));
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				if (line.contains("$count")) {
					int beginIdx = line.lastIndexOf("$") + 1;
					int endIdx = line.lastIndexOf("*");
					int count = Integer.parseInt(line.substring(beginIdx, endIdx).trim());	
					int size = inputs.size();
					line = count > 0 ? "     ,{" : "      {";
		
					for (int i = 0; i < size; ++i) {
						Object[] o = inputs.get(i);
						for (int j = 0; j < o.length; ++j) {
							if (j > 0) line += ", ";
							line += o[j].toString();
						}
						
						o = output.get(i);
						line += (o.length == 0 ? ", " : ", {");
						for (int j = 0; j < o.length; ++j) {
							if (j > 0) line += ", ";
							line += o[j].toString();
						}

						if (o.length > 1)
							line += "}";
					}

					line += "}" + linesep;
					line += "      /* $count$" + (count+1) + " */";
				}
				sb.append(line + linesep);
				line = br.readLine();
			}

			out = createPrintWriter(file, false);
			out.print(sb.toString());

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (out != null)
				out.close();
			if (br != null)
				try {
					br.close();
				} catch (IOException e) {}
		}
	}

	/**
	 * Create initial template file for PUTS
	 * */
	private void createJunitTemplate(File file) {
		if (!file.exists())
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		
		PrintWriter out = createPrintWriter(file, false);
		
		try {
			if (packageName.length() > 0) {
				out.println("package " + packageName + ";");
			}
      out.println();
      out.println("import java.util.Collection;");
      out.println("import java.util.Arrays;");
      out.println();
      out.println("import junit.framework.*;");
      out.println("import org.junit.Test;");
      out.println("import org.junit.runner.RunWith;");
      out.println("import org.junit.runners.Parameterized;");
      out.println("import org.junit.runners.Parameterized.Parameter;");
      out.println("import org.junit.runners.Parameterized.Parameters;");
      out.println();
      out.println("@RunWith(Parameterized.class)");
      out.println("public class " + junitclassname + " extends TestCase {");
      out.println();
      out.println("  @Parameters");
      out.println("  public static Collection<Object[]> data() {");
      out.println("    return Arrays.asList(new Object[][] {");
      out.println("      /* $count$0 */");
      out.println("    });");
      out.println("  }");
      out.println();
//    out.println("  @Parameter(value = 0)");
//    out.println("  public Object fInput;");
//    out.println();
//    out.println("  @Parameter");
//    out.println("  public Object fExpected;");
//    out.println();
      out.println("  public static boolean debug = false;");
      out.println();

      int testCounter = 1;
      out.println("  @Test");
      out.println("  public void test" + testCounter++ + "() throws Throwable {");
      out.println("    if (debug) { System.out.println(); System.out.print(\"" + junitclassname + ".test" + (testCounter-1) + "\"); }");
      out.println();
      out.println("    // TODO: insert test in here");
      out.println();
      out.println("  }");
      out.println("}");
		} finally {
			if (out != null)
				out.close();
		}
	}

	public static PrintWriter createPrintWriter(File file, boolean append) {
    try {
      return new PrintWriter(new BufferedWriter(new FileWriter(file, append)));
    } catch (IOException e) {
      //Log.out.println("Exception thrown while creating text print stream:" + file.getName());
      e.printStackTrace();
      System.exit(1);
      throw new Error("This can't happen");
    }
  }

	/** 
	 * Normalize Object string value to match with code representation string.<br />
	 * for example: if Object were String, we add double quote in beginning and end.
	 * */
  private String toCodeString(Object o) {
    String val = o.toString();
    Class<?> c;

    // try Integer, Long, Float
    try {
    	Integer.parseInt(val);
    	c = Integer.class;
    } catch (NumberFormatException e1) {
    	try {
      	Long.parseLong(val);
      	c = Long.class;
      } catch (NumberFormatException e2) {
    		try {
        	Float.parseFloat(val);
        	c = Float.class;
        } catch (NumberFormatException e3) {
        	c = null;
        }
      }
    }

    if (c != null) {
      if (c.equals(Integer.class)) 
      	; // do nothing
      else if (c.equals(Float.class))
        val = val + "f";
      else if (c.equals(Long.class))
        val = val + "L";
  	} else {
  		if (val.length() == 1)
      	val = "'" + val + "'"; // char
      else
      	val = "\"" + val + "\""; //string    		
  	}

    return val;
  }

	private File getDir() {
		File dir = null;
		if (dirName == null || dirName.length() == 0)
			dir = new File(System.getProperty("user.dir"));
		else
			dir = new File(dirName);
		if (packageName == null)
			return dir;
		packageName = packageName.trim(); // Just in case.
		if (packageName.length() == 0)
			return dir;
		String[] split = packageName.split("\\.");
		for (String s : split) {
			dir = new File(dir, s);
		}
		return dir;
	}

}
