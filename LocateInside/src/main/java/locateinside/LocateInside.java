package locateinside;

import java.io.File;
import java.util.ArrayList;
import locateinside.indexing.Indexer;
import locateinside.searching.Searcher;
import locateinside.util.Log;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;



public abstract class LocateInside
{
	private static final String DEFAULT_INDEX_DIR  = System.getProperty("java.io.tmpdir") +
			File.separator + "LocateInside";
	private static final String DEFAULT_SEARCH_DIR = System.getProperty("user.dir");


	public static void main(String[] args)
	{
		try
		{
			mainThrowingExceptions(args);
		}
		catch (UsageException e)
		{
			System.err.println("USAGE ERROR: " + e.getMessage());
			printUsage();
			System.exit(1);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}


	private static void mainThrowingExceptions(String[] args) throws Exception
	{
		File indexDir  = new File(DEFAULT_INDEX_DIR);
		File searchDir = new File(DEFAULT_SEARCH_DIR);
		boolean forceIndexUpdate = false;

		int i = 0;

		// Find arguments is any
		while (i < args.length && args[i].charAt(0) == '-')
		{
			String arg = args[i];
			if ("-d".equals(arg) && i + 1 < args.length)
			{
				i++;
				indexDir = new File(args[i]);
			}
			else
			if ("-u".equals(arg))
			{
				forceIndexUpdate = true;

			}
			else
				throw new UsageException("Invalid argument " + arg);
			i++;
		}

		// Later we will just call .getPath() and get canonical path
		searchDir = searchDir.getCanonicalFile();

		// Treat everything else as the query
		StringBuilder queryBuilder = new StringBuilder();
		while (i < args.length)
		{
			queryBuilder.append(args[i]);
			if (i + 1 < args.length)
				queryBuilder.append(' ');
			i++;
		}
		String query = queryBuilder.toString().trim();

		if (query.isEmpty())
			throw new UsageException("Empty query");


		Log.info("Using index " + indexDir);
		Directory directory = FSDirectory.open(indexDir);
		Indexer.updateIndexIfNecessary(directory, searchDir, forceIndexUpdate);


		Log.info("Searching for " + query);
	 	ArrayList<File> result = Searcher.search(directory, searchDir, query);

		if (result.size() == 0)
		{
			System.out.println("Nothing was found");
			if (!forceIndexUpdate)
				System.out.println("Try updating the database (-u flag)");
		}
		else
		{
			for (File foundFile : result)
			{
				System.out.println(foundFile);
			}
		}
	}


	public static void printUsage()
	{
		System.err.println("locateinside");
		System.err.println("        Looks for files in currect directory, that contain the text using prebuild index");
		System.err.println("USAGE: locateinside [-u] [-d indexdir] query");
		System.err.println("PARAMETERS:");
		System.err.println("        -u           Update database if exists, by default uses the existing");
		System.err.println("        -d indexdir  Use this datadir, by defaut uses " + DEFAULT_INDEX_DIR);
		System.err.println("EXAMPLE: locateinside i AND love AND you");
	}


	private static class UsageException extends Exception
	{
		private UsageException(String message)
		{
			super(message);
		}
	}
}
