package janala.csit6910;

import java.util.ArrayList;
import java.util.List;

/**
 *	This ResultProvider should capture solution from Solvers.
 *	See janala.solvers.* for details
 */
public interface ResultProvider {

	// we should take care of array type in here
	// at least prepare for one dimension array
	public List<Object[][]> results = new ArrayList<Object[][]>();
	
	public void setResult();
	
	public void getResult();
	
}
