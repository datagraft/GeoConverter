package net.datagraft.shape;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.datagraft.convert.CSV;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Represents a Shape File, by default it supports .shp files for now. User is
 * required to have the .shx file also in the same directory as .shp file.
 * 
 * As an alternative use {@link GeoShapeFromZip} to process a .zip file
 * 
 * @author nive
 *
 */
public class GeoShape implements Shapeable {

	private SimpleFeatureIterator simpleFeatureIterator;
	private FileDataStore store;
	private File sourceFile;

	/**
	 * Creates a GeoShape object using given filepath
	 * 
	 * @param absolute
	 *            filePath
	 * @throws IOException
	 */
	public GeoShape(String filePath) throws IOException {
		this.sourceFile = new File(filePath);
		this.store = FileDataStoreFinder.getDataStore(sourceFile);
		this.simpleFeatureIterator = this.store.getFeatureSource()
				.getFeatures().features();
	}

	/**
	 * Returns Geo data in the form of {@link SimpleFeature} collection
	 * 
	 * @return
	 * @throws IOException
	 */
	public SimpleFeatureIterator getRecords() throws IOException {
		return this.simpleFeatureIterator;
	}

	/**
	 * Transforms the geo records into a vector form that contains headers as
	 * the first row followed by each record values in each rows
	 * 
	 * @param Collection
	 *            of {@link SimpleFeature} records
	 * @return {@link List<List<String>>} of provided geo SimpleFeature data
	 */
	protected List<List<String>> getVectorForm(SimpleFeatureIterator records) {
		LinkedList<List<String>> vectorForm = new LinkedList<List<String>>();
		List<String> headers = null;
		List<String> keys;
		List<String> values;
		while (records.hasNext()) {
			SimpleFeature feature = records.next();

			keys = new ArrayList<String>();
			values = new ArrayList<String>();

			for (Property p : feature.getProperties()) {
				keys.add(p.getName().toString());
				values.add(p.getValue().toString());
			}

			vectorForm.add(values);

			if (headers == null) {
				headers = keys;
			} else {
				if (!keys.equals(headers)) {
					throw new RuntimeException(
							"Keys vary in number, order or content");
				}
			}
			keys = null;

		}
		if (headers != null) {
			vectorForm.addFirst(headers);
		}

		return vectorForm;
	}

	/**
	 * Converts shape file and write the {@link CSV} content in given destination path. 
	 * Uses default {@link CSV} for conversion.
	 * @param destinationPath
	 * @return absolute path of written {@link CSV} file
	 * @throws IOException
	 */
	public String writeCSV(File destinationPath) throws IOException {

		String dest_filename = getFileNameWithoutExtension(this.sourceFile)
				+ ".csv";
		String destAbsolutePath = destinationPath.getAbsolutePath()
				+ File.separator + dest_filename;
			//TODO change this to stream writer
		FileWriter writter = new FileWriter(destAbsolutePath);
		BufferedWriter bw = new BufferedWriter(writter);
		bw.write(convertToCSV());
		bw.close();
		

		return destAbsolutePath;
	}


	/**
	 * Converts geo data into corresponding CSV format custom {@link CSV}
	 * format
	 * 
	 * @param csv
	 * @return CSV string of given vector data
	 * @throws IOException
	 */
	public String convertToCSV(CSV csv) throws IOException {
		StringBuffer fullContent = new StringBuffer();
		StringBuffer propertyStr = new StringBuffer();
		StringBuffer headerStr = new StringBuffer();
		try {

			SimpleFeature f = null;
			boolean headerAttached = false;

			while (simpleFeatureIterator.hasNext()) {
				f = simpleFeatureIterator.next();

				// iterate through all properties of a feature
				for (Property prop : f.getProperties()) {
					propertyStr.append(prop.getValue().toString());
					propertyStr.append(csv.getDelimiter());

					//create header string
					if (!headerAttached) {
						headerStr.append(prop.getType().getName());
						headerStr.append(csv.getDelimiter());
					}
				}
				//add header
				if (!headerAttached) {
					
					fullContent.append(headerStr.toString().substring(0, headerStr.toString().lastIndexOf(csv.getDelimiter())));
					fullContent.append(csv.getNewLine());
					headerAttached = true;
					headerStr = null;
				}
				//add csv value of each property
				fullContent.append(propertyStr.toString().substring(0, propertyStr.toString().lastIndexOf(csv.getDelimiter())));
				fullContent.append(csv.getNewLine());
				//reset property value, this avoids creation of new StringBuilder object for each property
				propertyStr.delete(0, propertyStr.length());
			}

		} finally {
			// close opened resources
			this.simpleFeatureIterator.close();
			this.store.dispose();
			this.sourceFile=null;
			
		}

		return fullContent.toString();
	}

	/**
	 * Converts geo data into corresponding CSV format using given delimiter, quote and newLine parameters
	 * format
	 * 
	 * @param delimiter
	 * @param quote
	 * @param newLine
	 * @return CSV string of given vector data
	 * @throws IOException
	 */
	public String convertToCSV(String delimiter, String quote, String newLine)
			throws IOException {
		return convertToCSV(new CSV(delimiter, quote, newLine));
	}

	/**
	 * Returns a filename without its extension
	 * @param fileName 
	 * @return file name without extension
	 */
	private static String getFileNameWithoutExtension(File f) {
		String s = "";
		int index = f.getName().lastIndexOf('.');
		if (index > 0 && index <= f.getName().length() - 2) {
			s = f.getName().substring(0, index);
		}
		return s;
	}

	/**
	 * Converts geo data into corresponding CSV format using default {@link CSV}
	 * format
	 * 
	 * @param delimiter
	 * @param quote
	 * @param newLine
	 * @return CSV string of given vector data
	 * @throws IOException
	 */
	public String convertToCSV() throws IOException {
		return convertToCSV(new CSV());
	}
}
