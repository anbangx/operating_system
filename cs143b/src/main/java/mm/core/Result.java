package mm.core;

public class Result{
	public double memoryUtilization;
	public double searchRatio;
	
	public Result(double memoryUtilization, double searchRatio){
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