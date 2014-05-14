package mm;

public class Result{
	public double memoryUtilization;
	public double searchRatio;
	
	public Result(double memoryUtilization, double searchRatio){
		this.memoryUtilization = memoryUtilization;  
		this.searchRatio = searchRatio;
	}
	
	public String toString(){
		return "[" + memoryUtilization + "," + searchRatio + "]";
	}
	
	public double getMemoryUtilization() {
		return memoryUtilization;
	}

	public void setMemoryUtilization(double memoryUtilization) {
		this.memoryUtilization = memoryUtilization;
	}

	public double getSearchRatio() {
		return searchRatio;
	}

	public void setSearchRatio(double searchRatio) {
		this.searchRatio = searchRatio;
	}
	
}