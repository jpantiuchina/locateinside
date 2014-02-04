package locateinside.indexing;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import locateinside.util.FieldName;
import locateinside.util.Log;
import locateinside.util.ProjectLuceneVersion;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;



public abstract class Indexer
{
	public static void updateIndexIfNecessary(Directory directory, File searchDir, boolean forceUpdate) throws IOException
	{
		if (!DirectoryReader.indexExists(directory))
		{
			Log.info("Index does not exist, building for " + searchDir);
			updateIndex(directory, searchDir, false, UpdateMode.CREATE);
			return;
		}

		if (forceUpdate)
		{
			Log.info("Forced index update for " + searchDir);
			updateIndex(directory, searchDir, true, UpdateMode.UPDATE);
		}
	}


	private static void updateIndex(Directory directory, File searchDir, boolean indexExists, UpdateMode mode) throws IOException
	{
		long start = System.currentTimeMillis();

		Analyzer analyzer = new StandardAnalyzer(ProjectLuceneVersion.getValue());
		IndexWriterConfig iwc = new IndexWriterConfig(ProjectLuceneVersion.getValue(), analyzer);

		if (mode == UpdateMode.UPDATE)
			iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
		else
			iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

		IndexWriter writer = new IndexWriter(directory, iwc);
		try
		{
			if (indexExists)
			{
				DirectoryReader reader = DirectoryReader.open(directory);
				try
				{
					IndexSearcher searcher = new IndexSearcher(reader);
					recursivelyIndexDirOrFile(searcher, writer, searchDir, mode);
				}
				finally
				{
					reader.close();
				}
			}
			else
			{
				recursivelyIndexDirOrFile(null, writer, searchDir, mode);
			}
		}
		finally
		{
			writer.close();
		}

		Log.info(String.format("Indexing finished (took %.3f sec)", (System.currentTimeMillis() - start) / 1000.0));
	}



	private static void recursivelyIndexDirOrFile(IndexSearcher searcher, IndexWriter writer, File file, UpdateMode mode) throws IOException
	{
		if (file.isDirectory())
		{
			if (!file.canRead())
			{
				Log.info(file + ": Permission denied");
				return;
			}

			File[] files = file.listFiles();
			if (files != null)
			{
				for (File subfile : files)
				{
					recursivelyIndexDirOrFile(searcher, writer, subfile, mode);
				}
			}
		}
		else
		{
			indexFile(searcher, writer, file, mode);
		}
	}

	private static void indexFile(IndexSearcher searcher, IndexWriter writer, File file, UpdateMode mode) throws IOException
	{
		String filePath = file.getPath();

		if (searcher != null)
		{
			Term docId = new Term(FieldName.FILEPATH, filePath);
			TopDocs existing = searcher.search(new TermQuery(docId), 1);
			if (existing.scoreDocs.length > 0)
			{
				Document existingDoc = searcher.doc(existing.scoreDocs[0].doc);
				String existingModified = existingDoc.get(FieldName.MODIFIED);
				if (Long.parseLong(existingModified) == file.lastModified())
				{
//					Log.info("Skipping " + filePath);
					return;
				}
			}
			writer.deleteDocuments(docId);
		}
		Document doc = new Document();


		// Add the path of the file as a field named "path".  Use a
		// field that is indexed (i.e. searchable), but don't tokenize
		// the field into separate words and don't index term frequency
		// or positional information:

		// File path is cannical because is was canonicazed before

		doc.add(new StringField(FieldName.FILEPATH, filePath,            Field.Store.YES));
		doc.add(new StringField(FieldName.FILENAME, file.getName(),      Field.Store.YES));
		doc.add(new LongField  (FieldName.MODIFIED, file.lastModified(), Field.Store.YES));

		Reader fileContent = FileToText.extractTextFromFile(file);
		try
		{
			if (fileContent != null)
				doc.add(new TextField(FieldName.CONTENTS, fileContent));


			Log.info("Indexing " + filePath);

			writer.addDocument(doc);
		}
		finally
		{
			if (fileContent != null)
				fileContent.close();
		}

//		if (mode == UpdateMode.CREATE)


//		else
//			writer.updateDocument(new Term(FieldName.FILEPATH, filePath), doc);
	}


	private enum UpdateMode {UPDATE, CREATE}
}
