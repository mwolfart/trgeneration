import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.apache.commons.cli.*;

public class TRGeneration {
	private static CodeProcessor processor;
	private static List<String> filesToProcess;

	public static void main(String[] args) {
		filesToProcess = new ArrayList<String>();

		if (args.length < 1) {
			System.err.println("You must supply an input file or folder");
			System.exit(1);
		}

		Options options = new Options();
		options.addOption("d", false, "Print debug output"); // does not have a value
		options.addOption("o", true, "PNG output path"); // does not have a value
		options.addOption("g", false, "Print graph structures");
		options.addOption("l", false, "Print line flows");
		options.addOption("t", false, "Print PPC and EC test requirements");
		options.addOption("T", false, "Print complete test requirements");

		CommandLineParser parser = new BasicParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.err.println("Caught ParseException: " + e.getMessage());
		}

		String inputPath = args[args.length - 1];
		File inputFile = new File(inputPath);
		boolean isInputDirectory = inputFile.isDirectory();

		if (isInputDirectory) {
			readDirectory(inputFile);
		} else {
			filesToProcess.add(inputPath);
		}
		
		for (String filePath : filesToProcess) {
			processor = new CodeProcessor(filePath);
			readSource(filePath);
			
			try {
				if (cmd.hasOption("d")) processor.setDebug(true);
				processor.build();
				
				if (cmd.hasOption("g")) processor.writeGraphStructures();
				if (cmd.hasOption("l")) processor.writeLineEdges();
				if (cmd.hasOption("T")) processor.writeTestRequirements();
				if (cmd.hasOption("t")) processor.writePPCandECrequirements();
			} catch(Exception e) {
				System.err.println("Error while processing file " + filePath + ":");
				throw e;
			}
			processor.clear();
		}
	}

	private static void readSource(String path) {
		FileInputStream fstream = null;

		try {
			fstream = new FileInputStream(path);
		} catch (IOException e) {
			System.err.println("Unable opening file " + path + ".\n" + e.getMessage());
			System.exit(1);
		}

		Scanner s = new Scanner(fstream);
		while (s.hasNextLine()) {
			processor.addSourceCodeLine(s.nextLine());
		}
		s.close();
		try {
			fstream.close();
		} catch (IOException e) {
			System.err.println("Error closing file " + path + ".\n" + e.getMessage());
		}
	}

	private static void readDirectory(File dir) {
		for(File f : dir.listFiles()) {
			if (f.isDirectory()) {
				readDirectory(f);
			}
			else if (f.getPath().substring(f.getPath().lastIndexOf('.')+1).equals("java")) {
				filesToProcess.add(f.getPath());
			}
		}
	}
}
