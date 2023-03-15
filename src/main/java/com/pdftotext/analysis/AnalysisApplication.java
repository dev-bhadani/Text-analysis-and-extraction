package com.pdftotext.analysis;

import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.json.JSONArray;
import org.json.JSONObject;

import opennlp.tools.lemmatizer.DictionaryLemmatizer;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.stemmer.PorterStemmer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.Span;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

public class AnalysisApplication {

	private static final String LuceneConstats = null;
	
	public static String filePath="E:\\Applied Computer Science\\SEM 3\\analysis\\src\\main\\resources\\";

	public static void main(String[] args) throws Exception {
		String text = "";
		try {
			File pdfFile = new File(filePath+"module_manual_IMACS.pdf");
			PDDocument pdfDocument = PDDocument.load(pdfFile);
			PDFTextStripper pdfTextStripper = new PDFTextStripper();
			text = pdfTextStripper.getText(pdfDocument);
			pdfDocument.close();
			
			try {
		      File txtFile = new File(filePath+"Extracted_Text.txt");
		      if (txtFile.createNewFile()) {
		    	  analyzeText(text);
		    	  //writeFile(text);
		      }
		      else {
		    	  analyzeText(text);
		      }
		      //writeFile(text);
		    } catch (IOException e) {
		      System.out.println("An error occurred.");
		      e.printStackTrace();
		    }
			
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		//System.out.println(text);
	}

	private static void analyzeText(String text) throws InvalidFormatException, IOException {	
		//writefile of pdf text
		writeFile(text);
		
		//sentenceDetect(text);
		String[] tokens = tokenization(text);
		String cleanedDataset = cleaning(tokens);
		String[] removedStopWords = removeStopWords(cleanedDataset);
		stemming(removedStopWords);
		//statisticalLemmatization(removedStopWords);
		String[] dataSet = dict_lemmatization(removedStopWords);
		String[] bigrams = biGram(dataSet); 
		//importantTerms(dataSet, bigrams);
		wordCount(dataSet);
		wordCount(bigrams);
		
		dataSet = removeDuplicates(dataSet);
		bigrams = removeDuplicates(bigrams);
		
		//dataSet = removeExtraWords(dataSet);
		//bigrams = removeExtraWords(bigrams);
		//System.out.println("After removing duplicate"+dataSet.length);
		/*for(String print: dataSet) {
			System.out.println(print);
		}*/
		
		/*for(String print: bigrams) {
			System.out.println(print);
		}*/
		
		//escoOccupation(dataSet);
		//escoSkills(dataSet);
		
		escoData(dataSet,"skill");
		escoData(dataSet,"occupation");
		escoData(dataSet,"concept");
		escoData(bigrams,"skill");
		escoData(bigrams,"occupation");
		escoData(bigrams,"concept");
	}

	private static String[] removeExtraWords(String[] dataSet) {
		//System.out.println(dataSet.length);
		List<String> wordsToRemove = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath+"dataSet.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                wordsToRemove.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        dataSet = Arrays.stream(dataSet)
                .filter(word -> !wordsToRemove.contains(word))
                .toArray(String[]::new);
        //System.out.println(dataSet.length);
		return dataSet;
	}

	private static void escoData(String[] dataSet, String type) throws IOException {
		
		System.out.println("\n\n                    |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||");
	    System.out.println("                    ||||||||||||||||||||||      ESCO "+type+"s      ||||||||||||||||||||||");
	    System.out.println("                    |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||\n\n");
	    try {
	    for(int i=0;i<2;i++)
		//for(int i=0;i<dataSet.length;i++) 
		{
    		URL url = new URL("https://ec.europa.eu/esco/api/search?text="+dataSet[i]
    				+"&type="+type+"&limit=5&offset=0&full=false&viewObsolete=false&language=en");;
    		if(dataSet[i].contains(" ")) {
    			String encodedKeyword = URLEncoder.encode(dataSet[i], StandardCharsets.UTF_8);
    			url = new URL("https://ec.europa.eu/esco/api/search?text="+encodedKeyword
        				+"&type="+type+"&limit=5&offset=0&full=false&viewObsolete=false&language=en");
    		}
	        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
	
	        String inputLine;
	        StringBuffer response = new StringBuffer();
	        while ((inputLine = in.readLine()) != null) {
	            response.append(inputLine);
	        }
	        in.close();
	
	        JSONObject json = new JSONObject(response.toString());
	        int total = json.getInt("total");
	        if(total!=0) {
	        String link = json.getJSONObject("_embedded")
	        		.getJSONArray("results").getJSONObject(0).getJSONObject("_links")
	        		.getJSONObject("self").getString("href"); 
	        
	        try {
		        URL subUrl = new URL(link);
		        BufferedReader subBuffer = new BufferedReader(new InputStreamReader(subUrl.openStream()));
		
		        String inputString;
		        StringBuffer subResponse = new StringBuffer();
		        while ((inputString = subBuffer.readLine()) != null) {
		        	subResponse.append(inputString);
		        }
		        subBuffer.close();
		
		        //System.out.println(subResponse.toString());
		        JSONObject newJson = new JSONObject(subResponse.toString());
	        	System.out.println("Module : " + dataSet[i]);
	        	String title = newJson.getString("title");
		        System.out.println(type+" : " + title);
		        String description = (!type.equals("concept"))?newJson.getJSONObject("description").getJSONObject("en-us").getString("literal")
		        		:(newJson.has("description"))?newJson.getJSONObject("description").getJSONObject("en").getString("literal"):null;
		        		
		        JSONObject jsonObj = new JSONObject();
		        if(newJson.has("alternativeLabel")) {
		        	jsonObj = newJson.getJSONObject("alternativeLabel");
			        JSONArray alternativeLables = new JSONArray();
			        
			        if(jsonObj.has("en")) {
			        	alternativeLables = jsonObj.getJSONArray("en");
				        System.out.print("Keywords : ");
				        for(int j=0; j<alternativeLables.length();j++) {
				        	System.out.println((j==0)?""+alternativeLables.getString(j)
				        	:"           "+alternativeLables.getString(j));
				        }
				    }
			    }
		        	        
		        System.out.println("Description : " + description +"\n\n");
		        }catch(IOException e) {
		        	System.out.println("Error : "+e.getMessage());
		        }
	        
	        }
    	}}catch(IOException e) {
    		System.out.println("Error : "+e.getMessage());
        }
		
	}

	private static void writeFile(String text) {
		try {
	      FileWriter myWriter = new FileWriter(filePath+"Extracted_Text.txt");
	      myWriter.write(text);
	      myWriter.close();
	      System.out.println("Successfully text Extracted");
	    } catch (IOException e) {
	      e.printStackTrace();
	    }
	}
	
	public static String[] tokenization(String text) {
        InputStream modelIn = null; //Create an InputStream object of the model
        String[] tokens;
        try {
            modelIn = new FileInputStream(filePath +"en-token.bin");
            //Initialization of model class for Tokens
            TokenizerModel model = new TokenizerModel(modelIn);
            //Initialization of TokenMe class
            TokenizerME tokenizer = new TokenizerME(model);
            //Tokenizing processing...
            tokens = tokenizer.tokenize(text);
            int j=0, length=0;

            //Output
            System.out.println("\n\n                    |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||");
		    System.out.println("                    ||||||||||||||||||||||        Tokenization         ||||||||||||||||||||||");
		    System.out.println("                    |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||\n\n");
           
		    for (int i = 0; i < 20; i++) {
            	System.out.print("'"+tokens[i]+"' ");
            }
            //removeStopWords(tokens);
        } catch (IOException e) {
        	System.out.println("Something went wrong during Tokenizing");
            throw new RuntimeException(e);
        } finally {
            if (modelIn != null) {
                try {
                    modelIn.close();
                } catch (IOException e) {
                }
            }
        }

        return tokens;
    }
	
	public static void sentenceDetect(String text) throws InvalidFormatException,
	IOException {
		
		InputStream inputFile = new FileInputStream(filePath+"en-sent.bin");
		SentenceModel model = new SentenceModel(inputFile);
		SentenceDetectorME sentences = new SentenceDetectorME(model);
		
		String sentence[] = sentences.sentDetect(text);
		
		System.out.println("\n\n                    |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||");
	    System.out.println("                    ||||||||||||||||||||||          Sentences          ||||||||||||||||||||||");
	    System.out.println("                    |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||\n\n");
		for(int i=0;i<2;i++) {
			System.out.print(sentence[i]);
		}
		inputFile.close();
	}
	
	public static String[] removeStopWords(String text) {
		//final String text = "This is a short test!";
		//final List<String> stopWords = Arrays.asList("short","test"); //Filters both words
		CharArraySet stopWords = EnglishAnalyzer.getDefaultStopSet();
		final CharArraySet stopSet = new CharArraySet(stopWords, true);
		String[] str = new String[1];
		try {
		    ArrayList<String> remaining = new ArrayList<String>();
	
		    Analyzer analyzer = new StandardAnalyzer(stopSet); // Filters stop words in the given "stopSet"
		    
		    TokenStream tokenStream = analyzer.tokenStream(LuceneConstats, new StringReader(text));
		    CharTermAttribute term = tokenStream.addAttribute(CharTermAttribute.class);
		    tokenStream.reset();
		    int i=0;
		    System.out.println("\n\n                    |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||");
		    System.out.println("                    ||||||||||||||||||||||      REMOVE STOP WORDS      ||||||||||||||||||||||");
		    System.out.println("                    |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||\n\n");
		   
		    while(tokenStream.incrementToken()){
		        //System.out.print("[" + term.toString() + "] ");
		        remaining.add(term.toString());
		        i++;
		    }	    
		    
		    String[] removedStopWords = new String[i];
		    
		    for(i=0;i<removedStopWords.length;i++) {
		    	removedStopWords[i] = remaining.get(i);
		    	if(i<=20)
		    		System.out.print("["+removedStopWords[i]+"] ");
		    }
		    
		    
		   /* for (i = 1; i < 4; i++) {
            	if(i==1) {
            		j=0; length = 10;}
            	else if(i==2) {
            		j=10; length = 20;}
            	else {
            		j=20; length = 30;}
            	for(;j < length; j++)
            		System.out.print("["+removedStopWords[j]+"] ");
                //tokenText=tokenText+" ,['"+tokens[i]+"']";
            	System.out.println("");
            }*/
	
		    tokenStream.close();
		    analyzer.close();
		    
		    return removedStopWords;
		} catch (IOException e) {
		    e.printStackTrace();
		}
			    
		return str;
	}
	
	public static String[] dict_lemmatization(String[] tokens) throws IOException {
 
            // Parts-Of-Speech Tagging
            // reading parts-of-speech model to a stream
            InputStream posModelIn = new FileInputStream(filePath+"en-pos-maxent.bin");
            
            // loading the parts-of-speech model from stream
            POSModel posModel = new POSModel(posModelIn);
            // initializing the parts-of-speech tagger with model
            POSTaggerME posTagger = new POSTaggerME(posModel);
            
            // Tagger tagging the tokens
            String tags[] = posTagger.tag(tokens);
 
            // loading the dictionary to input stream
            InputStream dictLemmatizer = new FileInputStream(filePath+"en-lemmatizer-dict.txt");
            // loading the lemmatizer with dictionary
            DictionaryLemmatizer lemmatizer = new DictionaryLemmatizer(dictLemmatizer);
 
            // finding the lemmas
            String[] lemmas = lemmatizer.lemmatize(tokens, tags);
            
            for(int i=0;i<lemmas.length;i++) {
            	if(lemmas[i] == "O")
        			lemmas[i] = tokens[i];
            }
 
            // printing the results
            System.out.println("\n\n                    |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||");
		    System.out.println("                    ||||||||||||||||||||||  DICTIONARY LEMMATIZATION   ||||||||||||||||||||||");
		    System.out.println("                    |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||\n\n");
            //System.out.println("WORD  :  LEMMA");
            /*for(int i=0;i< 200;i++){
                //System.out.println(tokens[i]+" -"+tags[i]+" : "+lemmas[i]);
            	System.out.println(tokens[i]+" : "+lemmas[i] +"\n\n");
            }*/
            
            int i = 0, j=0,length=0;
    		for (i = 1; i < 5; i++) {
            	if(i==1) {
            		j=0; length = i*9;}
            	else if(i==2) {
            		j=9; length = i*9;}
            	else if(i==3) {
            		j=18; length = i*9;}
            	else if(i==4) {
            		j=27; length = i*9;}
            	else {
            		j=54; length = i*9;}
            	for(;j < length; j++) {
            		//System.out.println(tokens[i]+" -"+tags[i]+" : "+lemmas[i]);
            		if(lemmas[j] != "O")
            			System.out.print("["+tokens[j]+" : "+lemmas[j]+"] ");
            		else
            			System.out.print("["+tokens[j]+" : "+tokens[j]+"] ");}
                //tokenText=tokenText+" ,['"+tokens[i]+"']";
            	System.out.println("");
            }

		
		return lemmas;
	}
	
	public static String[] stemming(String[] dataSet){

		PorterStemmer porterStemmer = new  PorterStemmer();
		String[] stemmedWords = new String[dataSet.length];
		int index=0;
		
        while(index<dataSet.length){
        	stemmedWords[index]=porterStemmer.stem(dataSet[index]);
        	index++;
        }
        
        System.out.println("\n\n                    |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||");
	    System.out.println("                    ||||||||||||||||||||||        STEMMED WORDS        ||||||||||||||||||||||");
	    System.out.println("                    |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||\n\n");
        
        /*for (index = 0; index < 20; index++) {
            System.out.println(stemmedWords[index]);
        }*/
	    int i = 0, j=0,length=0;
		for (i = 1; i < 4; i++) {
        	if(i==1) {
        		j=0; length = 9;}
        	else if(i==2) {
        		j=9; length = 18;}
        	else {
        		j=18; length = 27;}
        	for(;j < length; j++) {
        		//System.out.println(tokens[i]+" -"+tags[i]+" : "+lemmas[i]);
            	System.out.print("["+stemmedWords[j]+"] ");}
            //tokenText=tokenText+" ,['"+tokens[i]+"']";
        	System.out.println("");
        }
	    
	    return stemmedWords;
    }
	
	public static String cleaning(String[] dataSet) throws IOException {
		String text = "";
        
		for(int i=0;i<dataSet.length;i++)
			text = text+" "+dataSet[i].toString();
		
		text = text.replaceAll("[^a-zA-Z\\s+]", "");
		
		String regex = "\\b\\w{1,3}\\b";
        String outputString = text.replaceAll(regex, "");
        
        String[] arr = outputString.split(" ");
        //System.out.println("After cleaning : "+arr.length);
        System.out.println("\n\n                    |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||");
	    System.out.println("                    ||||||||||||||||||||||           CLEANING          ||||||||||||||||||||||");
	    System.out.println("                    |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||\n\n");
        for(int i=0;i<100;i++)
        	if(arr[i]!="")
        		System.out.println(" ["+arr[i]+"]");
		
		return outputString;
	}

	public static String[] biGram(String[] dataSet) {

        String[] bigrams = new String[dataSet.length-1];
        
        System.out.println("\n\n                    |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||");
	    System.out.println("                    ||||||||||||||||||||||            Bigram           ||||||||||||||||||||||");
	    System.out.println("                    |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||\n\n");
        for (int i = 0; i < dataSet.length - 1; i++) {
            bigrams[i] = (dataSet[i] + " " + dataSet[i + 1]);
            if(i<20)
            	System.out.println(bigrams[i]);
        } 
        
        return bigrams;
	}
	
	public static void importantTerms(String[] dataSet, String[] bigrams) throws IOException {
			//for person name
	        InputStream modelIn = new FileInputStream(filePath+"en-ner-person.bin");
	        TokenNameFinderModel model = new TokenNameFinderModel(modelIn);
	        NameFinderME nameFinder = new NameFinderME(model);
	        
	        //for location
	        InputStream modelInLocation = new FileInputStream(filePath+"en-ner-location.bin");
	        TokenNameFinderModel modelLocation = new TokenNameFinderModel(modelInLocation);
	        NameFinderME locationFinder = new NameFinderME(modelLocation);
	        
	        //for Organization
	        // Load the Organization NameFinder model
	        InputStream modelInOrg = new FileInputStream(filePath+"en-ner-organization.bin");
	        TokenNameFinderModel modelOrg = new TokenNameFinderModel(modelInOrg);
	        NameFinderME orgFinder = new NameFinderME(modelOrg);

	        //String[] stringArray = { "OpenAI is a research company.", "Google is a technology company." };

	        for (String text : dataSet) {
	            Span[] orgSpans = orgFinder.find(text.split(" "));
	            for (Span orgSpan : orgSpans) {
	                text = text.substring(0, orgSpan.getStart()) + text.substring(orgSpan.getEnd());
	            }
	            System.out.println(text);
	        }

	        Span nameSpans[] = locationFinder.find(dataSet);

	        for (Span nameSpan : nameSpans) {
	            for (int i = nameSpan.getStart(); i < nameSpan.getEnd(); i++) {
	                dataSet[i] = "";
	            }
	        }
	        
	        nameSpans = nameFinder.find(bigrams);
	        
	        for (Span nameSpan : nameSpans) {
	            for (int i = nameSpan.getStart(); i < nameSpan.getEnd(); i++) {
	                bigrams[i] = "";
	            }
	        }
	        
	     // load the location name finder model
	        modelIn = new FileInputStream("en-ner-location.bin");
	        model = new TokenNameFinderModel(modelIn);
	        nameFinder = new NameFinderME(model);
	        
	        nameSpans = nameFinder.find(dataSet);
	        
	        for (Span nameSpan : nameSpans) {
	            for (int i = nameSpan.getStart(); i < nameSpan.getEnd(); i++) {
	                dataSet[i] = "";
	            }
	        }

	        nameSpans = nameFinder.find(bigrams);
	        
	        // iterate through the input sentences and identify location names
	        for (String sentence : bigrams) {
	            Span[] locations = nameFinder.find(sentence.split(" "));
	            // remove the location names from the sentence
	            for (Span location : locations) {
	                sentence = sentence.substring(0, location.getStart()) + sentence.substring(location.getEnd());
	            }
	            System.out.println(sentence);}

	        // Joining the tokens to form the output text
	        String outputText = String.join(" ", dataSet);
	}
	
	public static String[] removeDuplicates(String[] arr) {
	    List<String> list = new ArrayList<>();
	    for (String s : arr) {
	        if (!list.contains(s)) {
	            list.add(s);
	        }
	    }
	    String[] outputString = list.toArray(new String[list.size()]);
	    
	    System.out.println("\n\n                    |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||");
	    System.out.println("                    ||||||||||||||||||||||      REMOVE DUPLICATE       ||||||||||||||||||||||");
	    System.out.println("                    |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||\n\n");
	    for(int i=0;i<20;i++) {
	    	System.out.println(outputString[i]);
	    }
	    return outputString;
	}

    public static void wordCount(String[] words) {

        HashMap<String, Integer> wordCount = new HashMap<>();

        for (String word : words) {
            if (wordCount.containsKey(word)) {
                wordCount.put(word, wordCount.get(word) + 1);
            } else {
                wordCount.put(word, 1);
            }
        }
        
        System.out.println("\n\n                    |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||");
	    System.out.println("                    ||||||||||||||||||||||        WORDS COUNTING        ||||||||||||||||||||||");
	    System.out.println("                    |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||\n\n");
	    int i=0;
        for (String word : wordCount.keySet()) {
        	if(i<20)
        		System.out.println(word + " : " + wordCount.get(word));
        	i++;
        }
    }

    public static void escoOccupation(String[] dataSet) throws IOException {
        
    	for(int i=0;i<50;i++) {
    		//System.out.println(dataSet[i]);
    		//dataSet[i].split(" ");
    		URL url = new URL("https://ec.europa.eu/esco/api/search?language=en&type=occupation&text="+dataSet[i]);
    		//URL skillUrl = new URL("https://ec.europa.eu/esco/api/search?language=en&type=skill&text="+dataSet[i]);
	        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
	        //BufferedReader inSkill = new BufferedReader(new InputStreamReader(skillUrl.openStream()));
	
	        String inputLine;
	        StringBuffer response = new StringBuffer();
	        while ((inputLine = in.readLine()) != null) {
	            response.append(inputLine);
	        }
	        in.close();
	
	        JSONObject json = new JSONObject(response.toString());
	        int total = json.getInt("total");
	        //System.out.println(total);
	        if(total != 0  && total>100) {
//	        	if(total>=10)
//	        		total=4;
	        	
	        // Extract the specific information you want to print
	       // for(int j=0;j<2;j++) {}
	        	System.out.println("Keyword : " + dataSet[i]);
		        String occupation = json.getJSONObject("_embedded").getJSONArray("results").getJSONObject(0).getJSONObject("preferredLabel").getString("en-us");
		        String link = json.getJSONObject("_embedded").getJSONArray("results").getJSONObject(0).getString("uri");
		        
		        System.out.println("Occupation: " + occupation);
		        System.out.println("URL : " + link +"\n\n\n");
		     
	        }
    	}
    }

    public static void escoSkills(String[] dataSet) throws IOException {
    	//for(int i=0;i<dataSet.length;i++) 
    	for(int i=0;i<50;i++)
    	{
    		//System.out.println(dataSet[i]);
    		URL url = new URL("https://ec.europa.eu/esco/api/search?language=en&type=skill&text="+dataSet[i]);
	        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

	        String inputLine;
	        StringBuffer response = new StringBuffer();
	        while ((inputLine = in.readLine()) != null) {
	            response.append(inputLine);
	        }
	        in.close();
	
	        JSONObject json = new JSONObject(response.toString());
	        int total = json.getInt("total");
	        //System.out.println(total);
	        if(total != 0  && total>100) {
	        	System.out.println("Keyword :" + dataSet[i]);
		        // Extract the specific information you want to print
		        //for(int j=0;j<2 ;j++) {}
		        String skill = json.getJSONObject("_embedded").getJSONArray("results").getJSONObject(0).getJSONObject("preferredLabel").getString("en-us");
		        String link = json.getJSONObject("_embedded").getJSONArray("results").getJSONObject(0).getString("uri");
		
		        System.out.println("Skill: " + skill);
		        System.out.println("URL : " + link +"\n\n");
	        }
    	}
    }

}