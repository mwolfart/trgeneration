import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.apache.commons.cli.*;

public class TRGeneration {
	private static Graph graph;

	public static void main(String[] args) {

		// ex.Test1();
		// ex.Test2();
		// ex.Test3();
		// ex.Test4();
		// ex.Test5(); // Need to work on

		graph = new Graph();

		if (args.length < 1) {
			System.err.println("You must supply an input file");
			System.exit(1);
		}

		Options options = new Options();
		options.addOption("d", false, "Print debug output"); // does not have a value
		options.addOption("o", true, "PNG output path"); // does not have a value
		options.addOption("g", false, "Print graph structures");
		options.addOption("l", false, "Print line flows");
		options.addOption("t", false, "Print test requirements");

		CommandLineParser parser = new BasicParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.err.println("Caught ParseException: " + e.getMessage());
		}

		String inputPath = args[args.length - 1];
		File inputFile = new File(inputPath);
		List<String> filesToRead = new ArrayList<String>();
		boolean isInputDirectory = inputFile.isDirectory();

		if (isInputDirectory) {
			filesToRead = readDirectory(inputFile, filesToRead);
		} else {
			filesToRead.add(inputPath);
		}
		
		for (String filePath : filesToRead) {
			readSource(filePath);
			String fileDir = (isInputDirectory ? filePath.substring(0, filePath.lastIndexOf("\\")+1) : "");
			
			if (cmd.hasOption("d"))
				graph.setDebug(true);

			graph.build(fileDir);
			if (cmd.hasOption("g"))
				graph.PrintGraphStructures(fileDir);
			if (cmd.hasOption("l"))
				graph.PrintLineEdges(fileDir);
			if (cmd.hasOption("t"))
				graph.PrintTestRequirements(fileDir);
			
			graph.clear();
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
			graph.AddSrcLine(s.nextLine());
		}
		s.close();
		try {
			fstream.close();
		} catch (IOException e) {
			System.err.println("Error closing file " + path + ".\n" + e.getMessage());
		}
	}

	private static List<String> readDirectory(File dir, List<String> foundFiles) {
		for(File f : dir.listFiles()) {
			if (f.isDirectory()) {
				foundFiles = readDirectory(f, foundFiles);
			}
			else if (f.getPath().substring(f.getPath().lastIndexOf('.')+1).equals("java")) {
				foundFiles.add(f.getPath());
			}
		}
		return foundFiles;
	}
}
