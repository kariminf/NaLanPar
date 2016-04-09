package kariminf.nalanpar;

public class Types {
	
	public interface Posable {
		
	}
	
	public interface Featured {
		
	}
	
	public static class PhrasalFeature implements Featured {
		private boolean begin = true;
		
		public void setEnd(){
			this.begin = false;
		}
		
		public boolean getbegin(){
			return begin;
		}
	}

	public static enum Phrasal implements Posable {
		NP, //Noun phrase
		VP, //Verbe phrase
		ADJP,//Adjective phrase
		ADVP, //Adverbe phrase
		WHNP;
	}
	
	public static enum Terminal implements Posable {
		ADJ, // adjective
		ADP, // adposition
		ADV, // adverb
		AUX, // auxiliary verb
		CONJ, // coordinating conjunction
		DET, // determiner
		INTJ, // interjection
		NOUN, // noun
		NUM, // numeral
		PART, // particle
		PRON, // pronoun
		PROPN, // proper noun
		PUNCT, // punctuation
		SCONJ, // subordinating conjunction
		SYM, // symbol
		VERB, // verb
		X; // other
	}


}
