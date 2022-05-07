package org.dougmcintosh.index;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.Optional;

/**
 * Entry point for indexing files contained in a set of directories.
 */
public class IndexerCli {
    private static final Logger logger = LoggerFactory.getLogger(IndexerCli.class);
    private static final String OPT_INPUT_DIR = "i";
    private static final String OPT_INPUT_DIR_LONG = "inputdir";
    private static final String OPT_OUTPUT_DIR = "o";
    private static final String OPT_OUTPUT_DIR_LONG = "outputdir";
    private static final String OPT_STOP_WORDS_PATH = "s";
    private static final String OPT_STOP_WORDS_PATH_LONG = "stopwordsfile";
    private static final String OPT_RECURSE = "r";
    private static final String OPT_RECURSE_LONG = "recurse";
    private static final String OPT_WORKERS = "w";
    private static final String OPT_WORKERS_LONG = "workers";
    private static final String OPT_MIN_TOKEN_LENGTH = "l";
    private static final String OPT_MIN_TOKEN_LENGTH_LONG = "mintokenlength";
    private static final String OPT_COMPRESS = "c";
    private static final String OPT_COMPRESS_LONG = "compress";
    private static final String OPT_PRETTY_PRINT = "p";
    private static final String OPT_PRETTY_PRINT_LONG = "pretty";
    private static final String OPT_INDEX_TYPE = "x";
    private static final String OPT_INDEX_TYPE_LONG = "indextype";
    private static final String OPT_HELP = "h";
    private static final String OPT_HELP_LONG = "help";

    public static void main(String... args) {
        final Options opts = setupOptions();
        final CommandLineParser parser = new DefaultParser();

        try {
            final CommandLine cli = parser.parse(opts, args);

            if (cli.hasOption(OPT_HELP)) {
                printUsage(opts);
                return;
            }

            logger.info("Parsed command line options.");

            final String[] inputdirPaths = cli.getOptionValues(OPT_INPUT_DIR);
            final String outputdirPath = cli.getOptionValue(OPT_OUTPUT_DIR);
            final Optional<String> stopWordsPath = Optional.ofNullable(cli.getOptionValue(OPT_STOP_WORDS_PATH));
            final boolean recurse = cli.hasOption(OPT_RECURSE);
            final boolean compress = cli.hasOption(OPT_COMPRESS);
            final boolean prettyPrint = cli.hasOption(OPT_PRETTY_PRINT);
            final Optional<Integer> workers = optionalInteger(cli, OPT_WORKERS);
            final Optional<Integer> minTokenLength = optionalInteger(cli, OPT_MIN_TOKEN_LENGTH);
            final String indexType = cli.getOptionValue(OPT_INDEX_TYPE);

            final IndexerArgs indexerArgs = IndexerArgs.builder()
                .inputdirPaths(inputdirPaths)
                .outputdirPath(outputdirPath)
                .stopwordsPath(stopWordsPath)
                .workers(workers)
                .recurse(recurse)
                .compress(compress)
                .prettyPrint(prettyPrint)
                .minTokenLength(minTokenLength)
                .indexType(indexType)
                .build();

            Indexer.with(indexerArgs).index();
        } catch (ParseException e) {
            System.err.println("Failed to parse command line args: " + e.getMessage());
            printUsage(opts);
            System.exit(1);
        } catch (IndexingException e) {
            System.err.println("One or more indexing tasks failed: " + e.getMessage());
            logger.error("One or more indexing tasks failed.", e);
            System.exit(1);
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            logger.error("Unhandled exception while indexing.", t);
            System.exit(1);
        }
    }

    private static void printUsage(Options opts) {
        PrintWriter w = new PrintWriter(System.out);
        new HelpFormatter().printHelp(100, "java -cp <classpath> " + IndexerCli.class.getName() + " <options>", "", opts, "");
        w.flush();
    }

    private static Options setupOptions() {
        Options opts = new Options();
        opts.addOption(Option.builder(OPT_HELP)
            .desc("Print usage help.")
            .longOpt(OPT_HELP_LONG)
            .required(false)
            .build());
        opts.addOption(Option.builder(OPT_INPUT_DIR)
            .desc("One or more input directories to scan for pdf files.")
            .longOpt(OPT_INPUT_DIR_LONG)
            .required()
            .hasArgs()
            .build());
        opts.addOption(Option.builder(OPT_OUTPUT_DIR)
            .desc("Output directory (must exist) where index will be written.")
            .longOpt(OPT_OUTPUT_DIR_LONG)
            .required()
            .hasArg()
            .build());
        opts.addOption(Option.builder(OPT_STOP_WORDS_PATH)
            .desc("Path to file containing stop words, one per line. Any word in this file will be ignored for indexing.")
            .longOpt(OPT_STOP_WORDS_PATH_LONG)
            .required(false)
            .hasArg()
            .build());
        opts.addOption(Option.builder(OPT_RECURSE)
            .desc("Recursively process provided directory.")
            .longOpt(OPT_RECURSE_LONG)
            .hasArg(false)
            .required(false)
            .build());
        opts.addOption(Option.builder(OPT_WORKERS)
            .desc("Number of worker threads that will consume files from the work queue.")
            .longOpt(OPT_WORKERS_LONG)
            .required(false)
            .hasArg()
            .build());
        opts.addOption(Option.builder(OPT_MIN_TOKEN_LENGTH)
            .desc("Minimum number of characters required for a keyword to be indexed.")
            .longOpt(OPT_MIN_TOKEN_LENGTH_LONG)
            .required(false)
            .hasArg()
            .build());
        opts.addOption(Option.builder(OPT_COMPRESS)
            .desc("gzip compress generated index.")
            .longOpt(OPT_COMPRESS_LONG)
            .hasArg(false)
            .required(false)
            .build());
        opts.addOption(Option.builder(OPT_PRETTY_PRINT)
            .desc("Pretty print generated json index.")
            .longOpt(OPT_PRETTY_PRINT_LONG)
            .hasArg(false)
            .required(false)
            .build());
        opts.addOption(Option.builder(OPT_INDEX_TYPE)
            .desc("Index type. Either lucene or lunr.")
            .longOpt(OPT_INDEX_TYPE_LONG)
            .required()
            .hasArg()
            .build());
        return opts;
    }

    private static Optional<Integer> optionalInteger(CommandLine cli, String opt) {
        Optional<Integer> result = Optional.empty();
        if (cli.hasOption(opt)) {
            result = Optional.of(Integer.valueOf(cli.getOptionValue(opt)));
        }
        return result;
    }
}