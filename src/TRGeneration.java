import java.io.FileInputStream;
import java.io.IOException;
import java.util.Scanner;
import org.apache.commons.cli.*;
public class TRGeneration {
	private static Graph graph;
	
	public static void main(String[] args) {
		
		//ex.Test1();
		//ex.Test2();
		//ex.Test3();
		//ex.Test4();
		//ex.Test5();			// Need to work on

		graph = new Graph();
		
		if (args.length < 1){
			System.err.println("You must supply an input file");
			System.exit(1);
		}

		Options options = new Options();
		options.addOption("d", false, "Print debug output"); // does not have a value
		options.addOption("o", true, "PNG output path"); // does not have a value
 		
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = null;
		try{
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.err.println("Caught ParseException: " + e.getMessage());
		}
 
		readSource(args[args.length-1]);
		
		if (cmd.hasOption("d")) graph.setDebug(true);
		
		graph.build();
		graph.getTestRequirements();
	}

	private static void readSource(String path){
		
		FileInputStream fstream = null;
		
		try{
			fstream = new FileInputStream(path);
		}
		catch (IOException e){
			System.err.println("Unable opening file "+path+".\n"+e.getMessage());
			System.exit(1);
		}
		
		Scanner s = new Scanner(fstream);
		while (s.hasNextLine()){
			graph.AddSrcLine(s.nextLine());
		}
		s.close();
		try{
			fstream.close();
		}
		catch (IOException e){
			System.err.println("Error closing file "+path+".\n"+e.getMessage());
		}
		
	}
	
}
