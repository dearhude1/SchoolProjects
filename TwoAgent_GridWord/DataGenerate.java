

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Random;

public class DataGenerate
{
    
    
    public double[] upperBound;
    public double[] lowerBound;
    
    //public int[] baseNums = {100, 20, 50, 13, 10, 15, 200, 24, 39, 17, 20, 100, 9, 15, 800 };
    public int[] baseNums = {100, 100, 100, 100, 100, 100, 100 };
    public Random random;
    
    private int UPPER_ONLY = 0;
    private int LOWER_ONLY = 1;
    private int UPPER_LOWER = 2;
    
    public DataGenerate()
    {
	random = new Random();
	
	upperBound = new double[7];
	lowerBound = new double[7];
	
	for( int dim = 0; dim < 7; dim++ ) {
	    
	    int boundType = random.nextInt(3);
	    
	    if( boundType == UPPER_ONLY ) {
		
		lowerBound[dim] = Double.NEGATIVE_INFINITY;
		
		//upperBound[dim] = (random.nextInt(baseNums[dim])) * (random.nextDouble()); 
		upperBound[dim] = random.nextDouble();
	    }
	    else if( boundType == LOWER_ONLY ) {
		
		upperBound[dim] = Double.POSITIVE_INFINITY;
		
		//lowerBound[dim] = (random.nextInt(baseNums[dim])) * (random.nextDouble()); 
		lowerBound[dim] = random.nextDouble();
	    }
	    else {
		
		//upperBound[dim] = (random.nextInt(baseNums[dim])) * (random.nextDouble());
		//lowerBound[dim] = (random.nextInt(baseNums[dim])) * (random.nextDouble());
		upperBound[dim] = random.nextDouble();
		lowerBound[dim] = random.nextDouble();
		
		if( lowerBound[dim] > upperBound[dim] ) {
		    
		    double temp = upperBound[dim];
		    upperBound[dim] = lowerBound[dim];
		    lowerBound[dim] = temp; 
		}
		else if( Math.abs(lowerBound[dim]-upperBound[dim]) < 0.0000001 ) {
		    
		    upperBound[dim] += (random.nextInt(baseNums[dim])) * (random.nextDouble());
		}
	    }
	}
    }
    
    public boolean classify( double[] data )
    {
	
	for( int dim = 0; dim < 7; dim++ ) {
	    
	    if( data[dim] < lowerBound[dim] || data[dim] > upperBound[dim] ) {
		
		System.out.println(lowerBound[dim]+","+data[dim]+","+upperBound[dim]);
		
		return false;
	    }
	}
	
	return true;
    }

    public double[] generateData()
    {
	double[] data = new double[7];
	for( int dim = 0; dim < 7; dim++ ) {
	    
	    while( true ) {
		
		double value = random.nextDouble();
		if( value >= lowerBound[dim] && value <= upperBound[dim] ) {
		    
		    data[dim] = value;
		    break;
		}
	    }
	}
	
	return data;
    }
    
    public static void main(String[] args)
    {
	
	try {
	    
	    BufferedWriter trainingData = new BufferedWriter(new FileWriter("./trainingData.txt"));
	    
	    DataGenerate dg = new DataGenerate();
		
	    //generate 10000 training data
	    Random rnd = new Random();
	    int[] baseNums = {200, 40, 100, 30, 15, 25, 220, 36, 30, 17, 90, 140, 14, 30, 400 };
	    //int[] baseNums = {150, 150, 150, 150, 150, 150, 150, 150, 150, 150, 150, 150, 150, 150, 150 };
		
	    for( int dataIndex = 1; dataIndex <= 800; dataIndex++ ) {
		    
		//one data
		double[] data = new double[7];
		if( rnd.nextInt(3) == 0 )
		    data = dg.generateData();
		else {
		    for( int dim = 0; dim < 7; dim++ ) {
				
			//data[dim] = rnd.nextInt(baseNums_Test[dim]) * rnd.nextDouble();
			data[dim] = rnd.nextDouble();
		    }
		}
		    
		//write the data
		String line = "";
		for(int dim = 0; dim < 7; dim++ ) {
		    
		    line += String.valueOf(data[dim])+",";
		}
		if( dg.classify(data) )
		    line += "T";
		else
		    line += "F";
		
		trainingData.write( line );
		trainingData.newLine();
	    }
	    trainingData.close();
		
	    //generate 100 test data
	    BufferedWriter testData = new BufferedWriter(new FileWriter("./testData.txt"));
	    //int[] baseNums_Test = {90, 20, 120, 18, 30, 40, 110, 30, 30, 24, 45, 120, 8, 45, 600 };
	    for( int dataIndex = 1; dataIndex <= 100; dataIndex++ ) {
		    
		//one data
		double[] data = new double[7];
		
		if( rnd.nextInt(3) == 0 )
		    data = dg.generateData();
		else {
		    for( int dim = 0; dim < 7; dim++ ) {
				
			//data[dim] = rnd.nextInt(baseNums_Test[dim]) * rnd.nextDouble();
			data[dim] = rnd.nextDouble();
		    }
		}

		    
		//write the data
		String line = "";
		for(int dim = 0; dim < 7; dim++ ) {
		    
		    line += String.valueOf(data[dim])+",";
		}
		if( dg.classify(data) )
		    line += "T";
		else
		    line += "F";
		
		testData.write( line );
		testData.newLine();
	    }
	    testData.close();
	    
	    
	    double sum = 0; 
	    for( int i = 1; i < 40; i++ ) {
		
		sum += Math.pow(0.5, i) * (2+i);
	    }
	    System.out.println("Sum: "+sum);
	    
	}
	catch (Exception e) {
	    // TODO: handle exception
	}
	

    }
}
