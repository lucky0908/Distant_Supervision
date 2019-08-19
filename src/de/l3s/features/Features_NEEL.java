package de.l3s.features;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import de.l3s.loaders.DataLoaders_NEEL;

public class Features_NEEL extends Features {
	
	
	public Features_NEEL() throws CompressorException, IOException {
		super();
	}

	private static TreeMap<String,String> GTMAP;

	public	static void main(String[] args) throws IOException, CompressorException{
		
		
		DataLoaders_NEEL d = new DataLoaders_NEEL();
		GTMAP = new TreeMap<String, String>();
		GTMAP = d.getGT_MAP();
		
		writeTrainingAndTestSets(d);
	}
	
	
	
	
	
	
	public static void writeTrainingAndTestSets(DataLoaders_NEEL d) throws IOException, CompressorException{	
		OutputStreamWriter pw = new OutputStreamWriter(new FileOutputStream("./resources/neel/dataset.meta.train.neel.csv"),StandardCharsets.UTF_8);		
		OutputStreamWriter pwTest = new OutputStreamWriter(new FileOutputStream("./resources/neel/dataset.meta.test.neel.csv"),StandardCharsets.UTF_8);
		

		TreeMap<String, String> GT_MAP_test = new TreeMap<String, String>();
		GT_MAP_test = d.getGT_MAP_test();
		
		String mention;
		String a;
		String b;
		String t;
		String totA;
		String totB;
		String totT;
		String normA;
		String normB;
		String normT;
		
		TObjectIntHashMap<String> DocWordCountMap = d.getDocWordCountMap();
		TObjectIntHashMap<String> NumRecogMentionMap = d.getNumRecogMentionMap();
		TObjectIntHashMap<String> MentionEntityCountMap = Features.loadMentionEntityCountMap();
		TObjectIntHashMap<String> DocFrequencyMap = d.getDocFrequencyMap();
		TreeMap<String, String> hashTreeMap = d.getDocsContent();
		
		//////////// TRAIN
		TreeMap<String,String> DiffMap = new TreeMap<String, String>();
		BufferedReader trainbuffReader = new BufferedReader(new InputStreamReader(new FileInputStream("/home/joao/datasets/neel/mention_correct.train.tsv"),StandardCharsets.UTF_8));
		
		String line = "";
		while((line = trainbuffReader.readLine() )!= null) {
			String[] elems = line.split("\t");
			mention = elems[0].toLowerCase();
			a = elems[1];
			b = elems[2];
			t = elems[3];
			totA = elems[4];
			totB = elems[5];
			totT = elems[6];
			normA = elems[7];
			normB = elems[8];
			normT = elems[9];
			String vals = a+"\t"+b+"\t"+t +"\t"+totA+"\t"+totB+"\t"+totT+"\t"+normA+"\t"+normB+"\t"+normT;
			DiffMap.put(mention, vals);
			}
		trainbuffReader.close();
		
		BufferedReader bffReader = new BufferedReader(new InputStreamReader(new FileInputStream("/home/joao/datasets/neel/NEEL2016-train_NONNIL.gt"),StandardCharsets.UTF_8));
		line = "";
		while ((line = bffReader.readLine()) != null) {
			String[] elems = line.split("\t");
			String docId = elems[0].toLowerCase();
			docId = docId.replaceAll("\'", "");
			docId = docId.replaceAll("\"", "");
			mention = elems[1].toLowerCase();
			String offset = elems[2];
			String link = elems[3];
			link = link.replaceAll("http://dbpedia.org/resource/", "");
			link = link.replaceAll("_"," ").toLowerCase();
			String key = docId + "\t" + mention + "\t" + offset;
		
			/** Mention-based features **/
			double mention_length =  getMentionLength(mention);
			double mention_gran =    getMentionGranularity(mention);
			String content = hashTreeMap.get(docId);
			content = content.toLowerCase();
			double mention_freq =    getMentionFrequency(content, mention);
			double mention_doc_freq =  getDocFreq(DocFrequencyMap,mention);
			double mention_num_cand =  (double)MentionEntityCountMap.get(mention);//getNumCandidates(mention);
			String mention_norm_pos =  getMentionNormalizedPosition(content,offset);
			double mention_sent_size = getSentenceSize(content, mention, offset);
			String GTLink = GTMAP.get(key); 
			
			String elements = null;
			try{
				elements = DiffMap.get(mention);
			}catch(Exception e){
				continue;
			}
			if(elements!=null){
				elems = elements.split("\t");
				a = elems[0];
				b = elems[1];
				t = elems[2];
				totA = elems[3];
				totB = elems[4];
				totT = elems[5];
				normA = elems[6];
				normB = elems[7];
				normT = elems[8];
				
			}else{
				a = "0.0";
				b = "0.0";
				t =  "0.0";
				totA =  "0.0";
				totB =  "0.0";
				totT =  "0.0";
				normA =  "0.0";
				normB =  "0.0";
				normT =  "0.0";
			}
			
			/** Document-based features **/
			double doc_size = (double) DocWordCountMap.get(docId);
			//String doc_lex  =  getDocumentLex(content);
			double doc_numMentions = (double)NumRecogMentionMap.get(docId);//getNumRecogMentions(docId);
			pw.write("\""+docId + "\"" + "," + "\"" +mention +"\"" + "," +offset + "," + "\"" + GTLink  +"\""+"," + mention_length+ ","+mention_gran+ ","+mention_freq+ ","+mention_doc_freq+ ","+mention_num_cand + ","+mention_norm_pos +","+ mention_sent_size +","+a+","+b+","+t+","+normA+","+normB+","+normT+ "," + totA+ "," +  totB + "," + totT+ "," + doc_size+","+doc_numMentions+ "\n");
		}
		
		bffReader.close();

		pw.flush();
		pw.close();
		
		System.out.println("Finished writing the training set");

//		
//	/////// DEV
//		line = "";
////		DiffMap = new TreeMap<String, String>();
////	BufferedReader testbuffReader = new BufferedReader(new InputStreamReader(new FileInputStream("/home/joao/datasets/neel/mention_correct.train.tsv"),StandardCharsets.UTF_8));
////
////	while((line = testbuffReader.readLine() )!= null) {
////		String[] elems = line.split("\t");
////		mention = elems[0];
////		mention = mention.replaceAll("_"," ");
////		mention = mention.toLowerCase();
////		a = elems[1];
////		b = elems[2];
////		t = elems[3];
////		totA = elems[4];
////		totB = elems[5];
////		totT = elems[6];
////		normA = elems[7];
////		normB = elems[8];
////		normT = elems[9];
////		String vals = a+"\t"+b+"\t"+t +"\t"+totA+"\t"+totB+"\t"+totT+"\t"+normA+"\t"+normB+"\t"+normT;
////		DiffMap.put(mention, vals);
////	}
////	testbuffReader.close();
//	
//	
//	bffReader = new BufferedReader(new InputStreamReader(new FileInputStream("/home/joao/datasets/neel/NEEL2016-dev_NONNIL.gt"),StandardCharsets.UTF_8));
//	line = "";
//	while ((line = bffReader.readLine()) != null) {
//		String[] elems = line.split("\t");
//		String docId = elems[0];
//		mention = elems[1];
//		mention = mention.replaceAll("_"," ");
//		mention = mention.toLowerCase();
//		String offset = elems[2];
//		String link = elems[3];
//		link = link.replaceAll("http://dbpedia.org/resource/", "");
//		link = link.replaceAll("_"," ").toLowerCase();
//		String key = docId + "\t" + mention + "\t" + offset;
//////		/** Mention-based features **/
//		double mention_length =  getMentionLength(mention);
//		double mention_gran =    getMentionGranularity(mention);
//		String content = hashTreeMap.get(docId);
//		content = content.toLowerCase();
//		double mention_freq =    getMentionFrequency(content, mention);
//		double mention_doc_freq =  getDocFreq(DocFrequencyMap,mention);
//		double mention_num_cand =  (double)MentionEntityCountMap.get(mention);//getNumCandidates(mention);
//		String mention_norm_pos =  getMentionNormalizedPosition(content,offset);
//		double mention_sent_size = getSentenceSize(content, mention, offset);
//////	
//		String GTLink = GTMAP.get(key); 
//		String elements = DiffMap.get(mention);
//		if(elements!=null){
//			elems = elements.split("\t");
//			a = elems[0];
//			b = elems[1];
//			t = elems[2];
//			totA = elems[3];
//			totB = elems[4];
//			totT = elems[5];
//			normA = elems[6];
//			normB = elems[7];
//			normT = elems[8];
//			
//		}else{
//			a = "0.0";
//			b = "0.0";
//			t =  "0.0";
//			totA =  "0.0";
//			totB =  "0.0";
//			totT =  "0.0";
//			normA =  "0.0";
//			normB =  "0.0";
//			normT =  "0.0";
//		}
//////		
//////		/** Document-based features **/
//		double doc_size = (double) DocWordCountMap.get(docId);
//		//String doc_lex  =  getDocumentLex(content);
//		double doc_numMentions = (double)NumRecogMentionMap.get(docId);//getNumRecogMentions(docId);
//		pwTest.write("\""+docId + "\"" + "," + "\"" +mention +"\"" + "," +offset + "," + "\"" + GTLink  +"\""+"," + mention_length+ ","+mention_gran+ ","+mention_freq+ ","+mention_doc_freq+ ","+mention_num_cand + ","+mention_norm_pos +","+ mention_sent_size +","+a+","+b+","+t+","+normA+","+normB+","+normT+ "," + totA+ "," +  totB + "," + totT+ "," + doc_size+","+doc_numMentions+ "\n");
//	}
//////	
//	bffReader.close();
//	pwTest.flush();
//	
//		int i=0;
		Iterator<?> it = GT_MAP_test.entrySet().iterator();
		while (it.hasNext()) {
			@SuppressWarnings("rawtypes")
			Map.Entry pair = (Map.Entry)it.next();
			String key = (String) pair.getKey();
			String val = (String) pair.getValue();
//	bffReader = new BufferedReader(new InputStreamReader(new FileInputStream("/home/joao/datasets/neel/NEEL2016-test_NONNIL.gt"),StandardCharsets.UTF_8));
//	line = "";
//	while ((line = bffReader.readLine()) != null) {
		String[] elems = key.split("\t");
		String docId = elems[0].toLowerCase();
		docId = docId.replaceAll("\'", "");
		docId = docId.replaceAll("\"", "");
		mention = elems[1].toLowerCase();
//		mention = mention.replaceAll("_"," ");
//		mention = mention.toLowerCase();
		String offset = elems[2];
//		String link = elems[3];
//		link = link.replaceAll("http://dbpedia.org/resource/", "");
//		link = link.replaceAll("_"," ").toLowerCase();
//		String key = docId + "\t" + mention + "\t" + offset;
	
		/** Mention-based features **/
		double mention_length =  getMentionLength(mention);
		double mention_gran =    getMentionGranularity(mention);
		String content = hashTreeMap.get(docId);
		content = content.toLowerCase();
		//System.out.println(docId + ">" + content);
		double mention_freq =    getMentionFrequency(content, mention);
		double mention_doc_freq =  getDocFreq(DocFrequencyMap,mention);
		double mention_num_cand =  (double)MentionEntityCountMap.get(mention);//getNumCandidates(mention);
		String mention_norm_pos =  getMentionNormalizedPosition(content,offset);
		double mention_sent_size = getSentenceSize(content, mention, offset);
//	
//		String GTLink = GTMAP.get(key);
		String GTLink = val.trim().toLowerCase(); 

//		
		String elements = DiffMap.get(mention);
		if(elements!=null){
			elems = elements.split("\t");
			a = elems[0];
			b = elems[1];
			t = elems[2];
			totA = elems[3];
			totB = elems[4];
			totT = elems[5];
			normA = elems[6];
			normB = elems[7];
			normT = elems[8];
//			
		}else{
			a = "0.0";
			b = "0.0";
			t =  "0.0";
			totA =  "0.0";
			totB =  "0.0";
			totT =  "0.0";
			normA =  "0.0";
			normB =  "0.0";
			normT =  "0.0";
		}

//		/** Document-based features **/
		double doc_size = (double) DocWordCountMap.get(docId);
		//String doc_lex  =  getDocumentLex(content);
		double doc_numMentions = (double)NumRecogMentionMap.get(docId);//getNumRecogMentions(docId);
		pwTest.write("\""+docId + "\"" + "," + "\"" +mention +"\"" + "," +offset + "," + "\"" + GTLink  +"\""+"," + mention_length+ ","+mention_gran+ ","+mention_freq+ ","+mention_doc_freq+ ","+mention_num_cand + ","+mention_norm_pos +","+ mention_sent_size +","+a+","+b+","+t+","+normA+","+normB+","+normT+ "," + totA+ "," +  totB + "," + totT+ "," + doc_size+","+doc_numMentions+ "\n");
	}
	
	bffReader.close();
	pwTest.flush();
	pwTest.close();
	System.out.println("Finished writing the test set");

	}

	
	
	

	
	
	
	
	
	
	
	/**
	 * 				Ms	-	Num of chars of the sentence containing the mention
	 * 
	 * @param docId
	 * @param mention
	 * @param position
	 * @return
	 */
	public static int getSentenceSize(String content, String mention, String position ) {
		int tamEsq = 0;
		int tamDir = 0;
		int offset = Integer.parseInt(position);
		for(int i=0; i < offset; i++){
			tamEsq++;			
			if((content.charAt(i)=='.') ||(content.charAt(i)==';') || (content.charAt(i)=='!' || (content.charAt(i)=='?'))){
				tamEsq = 0;
 			}
		}
		
		for(int i = offset + mention.length() ; i < content.length(); i++){
			tamDir++;			
			if((content.charAt(i)=='.') ||(content.charAt(i)==';') || (content.charAt(i)=='!' || (content.charAt(i)=='?'))){
				break;
			}		
		}
		
		return tamEsq + mention.length()+ tamDir;
	}
}





