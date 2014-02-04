package locateinside.indexing;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import org.apache.tika.parser.ParsingReader;



abstract class FileToText
{
	private static final int MAX_TEXT_FILE_SIZE = 128 * 1024;

	private static char[] BUFFER = new char[MAX_TEXT_FILE_SIZE];

	static Reader extractTextFromFile(File file) throws IOException
	{
		if (!file.canRead())
			return null;

		if (file.length() > MAX_TEXT_FILE_SIZE)
			return null;

		FileInputStream fis = new FileInputStream(file);
		try
		{

			/*
			// http://stackoverflow.com/questions/14702189/how-make-inputstreamreader-fail-on-invalid-data-for-encoding
			CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
			decoder.onMalformedInput(CodingErrorAction.REPORT);
			decoder.onUnmappableCharacter(CodingErrorAction.REPORT);

			InputStreamReader reader = new InputStreamReader(fis, decoder);
            */

			Reader reader = new ParsingReader(fis);

			/*
			int size = reader.read(BUFFER, 0, BUFFER.length);



			if (size <= 0)
				return null;
              */
			/*

			int whitespaceCount = 0;

			for (int i = 0; i < size; i++)
			{
				char ch = BUFFER[i];

				if (ch <= ' ')
					whitespaceCount++;

				if (ch < ' ' && ch != '\r' && ch != '\n' && ch != '\t' && ch != '\f')
				{
					// Contains strange characters
					return null;
				}
			}


			if (size > 100 && (whitespaceCount * 20 < size)) // Too few whitespaces for a text file
				return null;

			System.out.println(String.valueOf(BUFFER, 0, size));

			*/


			return reader; //String.valueOf(BUFFER, 0, size);
		}
		catch (IOException ignored)
		{
			fis.close();
			return null;
		}
	}
}
