import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import wniemiec.util.io.search.FileSearcher;
import wniemiec.util.task.Scheduler;

public class TRGeneration {
	private static CodeProcessor processor;
	private static Set<Path> filesToProcess;

	public static void main(String[] args) throws IOException {
		filesToProcess = new HashSet<>();

		if (args.length < 1) {
			System.err.println("You must supply an input file or folder");
			System.exit(1);
		}

		Options options = new Options();
		options.addOption("d", false, "Print debug output"); // does not have a value
		options.addOption("g", false, "Print graph structures");
		options.addOption("l", false, "Print line flows");
		options.addOption("t", false, "Print PPC and EC test requirements");
		options.addOption("T", false, "Print complete test requirements");
		options.addOption("i", false, "Output control flow graph PNG");
		options.addOption("c", false, "Process clean code");
		options.addOption("p", true, "Test timeout");

		CommandLineParser parser = new BasicParser();
		CommandLine cmd_ = null;
		try {
			cmd_ = parser.parse(options, args);
		} catch (ParseException e) {
			System.err.println("ERROR: Caught ParseException: " + e.getMessage());
		}
		
		final CommandLine cmd = cmd_;

		String inputPath = args[args.length - 1];
		File inputFile = new File(inputPath);
		boolean isInputDirectory = inputFile.isDirectory();
		FileSearcher searcher = new FileSearcher(inputFile.toPath());

		if (isInputDirectory) {
			filesToProcess = searcher.searchAllFilesWithExtension("java");
		} else {
			filesToProcess.add(inputFile.toPath());
		}

		int timeoutSecs = -1;
		if (cmd.hasOption("p")) {
			timeoutSecs = Integer.parseInt(cmd.getOptionValue('p'));
		}

		final boolean outputImage = cmd.hasOption("i");
		final boolean processClean = cmd.hasOption("c");		
		final boolean debug = cmd.hasOption("d");
		final boolean writeGraphStructures = cmd.hasOption("g");
		final boolean writeLineEdges = cmd.hasOption("l");
		final boolean writeTestRequirements = cmd.hasOption("T");
		final boolean writePPCandECrequirements = cmd.hasOption("t");
		
		for (Path filePath : filesToProcess) {
			if (debug) {
				System.out.println("Processing file " + filePath + "...");
			}
			
			processor = new CodeProcessor(
					filePath.toAbsolutePath().toString(), 
					processClean, 
					outputImage
			);
			readSource(filePath.toAbsolutePath().toString());
			
			if (timeoutSecs > -1) {			
				final Duration timeout = Duration.ofSeconds(timeoutSecs);
				Scheduler.setTimeoutToRoutine(() -> {
					try {
						processInput(
								processor, 
								writeGraphStructures, 
								writeLineEdges, 
								writeTestRequirements, 
								writePPCandECrequirements, 
								debug
						);
					} catch (Exception e) {
						System.err.println("ERROR: Failed to process file " + filePath + ":" + e.getMessage());
					}
				}, timeout.toMillis());
				
				processor.clear();
			} else {
				try {
					processInput(processor, writeGraphStructures, writeLineEdges, writeTestRequirements, writePPCandECrequirements, debug);
				} catch(Exception e) {
					System.err.println("ERROR: Failed to process file " + filePath + ":" + e.getMessage());
				}
			}
		}
	}

	private static void processInput(CodeProcessor proc, boolean writeGraphStructures, 
			boolean writeLineEdges, boolean writeTestRequirements, 
			boolean writePPCandECrequirements, boolean debug) throws Exception {
		if (debug) proc.setDebug(true);
		proc.build();
		
		if (writeGraphStructures) proc.writeGraphStructures();
		if (writeLineEdges) proc.writeLineEdges();
		if (writeTestRequirements) proc.writeTestRequirements();
		if (writePPCandECrequirements) proc.writePPCandECrequirements();
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
}
