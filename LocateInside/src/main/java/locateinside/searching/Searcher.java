package locateinside.searching;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import locateinside.util.FieldName;
import locateinside.util.ProjectLuceneVersion;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;



public abstract class Searcher
{
	public static ArrayList<File> search(Directory directory, File searchDir, String query) throws Exception
	{
		// Need to make path with separator to http://stackoverflow.com/questions/4746671/how-to-check-if-a-given-path-is-possible-child-of-another-path
		String searchDirPathWithSeparator = searchDir.getPath();
		if (!searchDirPathWithSeparator.endsWith(File.separator))
			searchDirPathWithSeparator = searchDirPathWithSeparator + File.separatorChar;

		IndexReader reader = DirectoryReader.open(directory);
		try
		{
			IndexSearcher searcher = new IndexSearcher(reader);
			Analyzer analyzer = new StandardAnalyzer(ProjectLuceneVersion.getValue());
			QueryParser parser = new QueryParser(ProjectLuceneVersion.getValue(), FieldName.CONTENTS, analyzer);


			TopDocs topDocs = searcher.search(parser.parse(query), Integer.MAX_VALUE);

			ArrayList<File> result = new ArrayList<File>();

			for (ScoreDoc scoreDoc : topDocs.scoreDocs)
			{
				Document doc = searcher.doc(scoreDoc.doc, new HashSet<String>(Arrays.asList(new String[]{FieldName.FILEPATH})));
				String filePath = doc.get(FieldName.FILEPATH);
				if (filePath.startsWith(searchDirPathWithSeparator))
				{
					File file = new File(filePath);
					if (file.exists())
						result.add(file);
				}
			}

			return result;
		}
		finally
		{
			reader.close();
		}
	}
}
