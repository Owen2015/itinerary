package com.sinbad.itinerary.logic.scorer;


public class Scorer {

	//private List<ArrayList<Double>> data=new ArrayList<ArrayList<Double>>();
	protected double[][] data;
	protected double[] factor;
	protected double[] result;
	
	
	public Scorer(double[][] data){
		//this.data=data;
		//this.factor= new double[data[0].length];
		//this.result=new double[data.length];
	}
	public Scorer(){
		
	}
	
	// from matrix of [0-1] to vector [0-1]
	public static double[] score(double[][] data,double[] factor){
		int size=data.length;
		double[] result=new double[size];
		for(int i=0;i<data.length;i++){
			for(int j=0;j<data[i].length;j++){
				result[i]=result[i]+data[i][j]*factor[j];
			}
		}
		return result;
	}
	
	public double[][] getData() {
		return data;
	}
	public void setData(double[][] data) {
		this.data = data;
	}
	public double[] getFactor() {
		return factor;
	}
	public void setFactor(double[] factor) {
		this.factor = factor;
	}
	public double[] getResult() {
		return result;
	}
	public void setResult(double[] result) {
		this.result = result;
	}


}
