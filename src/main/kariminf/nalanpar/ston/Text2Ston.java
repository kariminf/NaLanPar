package kariminf.nalanpar.ston;


import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import edu.stanford.nlp.process.Morphology;

import kariminf.langpi.wordnet.SqliteReqExceptions.LangNotFound;
import kariminf.langpi.wordnet.SqliteReqExceptions.NoSqliteBase;
import kariminf.langpi.wordnet.SqliteRequestor;
import kariminf.langpi.wordnet.WNRequestor;
import kariminf.nalanpar.ParseHandler;
import kariminf.nalanpar.Types.Det;
import kariminf.nalanpar.Types.Phrasal;
import kariminf.nalanpar.Types.Posable;
import kariminf.nalanpar.Types.Terminal;
import kariminf.nalanpar.Types.VerbTense;
import kariminf.sentrep.ston.request.ReqAction;
import kariminf.sentrep.ston.request.ReqCreator;
import kariminf.sentrep.ston.request.ReqRolePlayer;
import kariminf.sentrep.ston.request.ReqSentence;

public class Text2Ston implements ParseHandler {

	private final static String wordnetPath = 
			"../LangPi/wordnetDB/wordnet.sqlite";
	private ReqCreator rq = new ReqCreator();
	private WNRequestor wordnetReq = null; 
	private int numAction = 0;
	private int numRole = 0;
	private boolean isNP = false;
	private boolean isVP = false;
	private HashMap<String, ReqRolePlayer> players = new HashMap<String, ReqRolePlayer>();
	private HashMap<String, ReqAction> actions = new HashMap<String, ReqAction>();
	private ArrayList<ReqSentence> sentences = new ArrayList<ReqSentence>();
	
	private ArrayDeque<String> openNP = new ArrayDeque<String>();
	private ArrayDeque<String> openVP = new ArrayDeque<String>();
	private ArrayDeque<String> openAdjP = new ArrayDeque<String>();
	private ArrayDeque<String> openID = new ArrayDeque<String>();
	
	private ArrayDeque<Phrasal> openType = new ArrayDeque<Phrasal>();
	private Posable lastClosedType = Phrasal.S;
	
	private String lastRoleId = "";
	
	private HashSet<String> acts = new HashSet<String>();
	
	private ArrayList<HashSet<String>> disj = new ArrayList<HashSet<String>>();
	private HashSet<String> conj = new HashSet<String>();
	
	private ArrayList<HashSet<String>> subjs = null;
	private ArrayList<HashSet<String>> objs = null;
	private ArrayList<HashSet<String>> rels = null;
	private String lastRel = "";
	
	public Text2Ston() {
		try {
			wordnetReq = SqliteRequestor.create("eng", wordnetPath);
		} catch (NoSqliteBase | LangNotFound e) {
			System.out.println(e.getMessage());
			wordnetReq = null; 
		}
		
		disj.add(conj);
	}

	@Override
	public void beginNP() {
		numRole++;
		
		Phrasal opentype = openType.getFirst();
		
		String rId = "role-" + numRole;
		
		
		switch(opentype){
		case ADJP:
			break;
		case ADVP:
			break;
		case NP:
		{
			break;
		}
		case PP:
			System.out.println("PP inside VP");
			if (rels == null){
				disj = new ArrayList<HashSet<String>>();
				conj = new HashSet<String>();
				disj.add(conj);
				rels = disj;
			}
			break;
		case S:
		{
			break;
		}
		case VP:
		{
			System.out.println("NP inside VP");
			if (objs == null){
				disj = new ArrayList<HashSet<String>>();
				conj = new HashSet<String>();
				disj.add(conj);
				objs = disj;
			}
			
			break;
		}
		default:
			break;
		
		}
		
		openNP.addFirst(rId);
		openID.addFirst(rId);
		openType.add(Phrasal.NP);
		
	}

	@Override
	public void endNP() {
		openNP.removeFirst();
		openID.removeFirst();
		lastClosedType = Phrasal.NP;
		openType.remove();
	}

	@Override
	public void beginSentence() {
		openType.add(Phrasal.S);
	}

	@Override
	public void beginVP() {
		numAction++;
		
		String actId = "act-" + numAction;
		
		if (lastClosedType == Phrasal.NP){
			if (disj.size() > 0){
				subjs = disj;
				disj = new ArrayList<HashSet<String>>();
			}	
		}
		
		openVP.addFirst(actId);
		openID.addFirst(actId);
		openType.add(Phrasal.VP);
		
	}

	@Override
	public void endVP() {
		String id = openVP.removeFirst();
		openID.removeFirst();
		lastClosedType = Phrasal.VP;
		//System.out.println("End verbal phrase: " + id);
		if (objs != null && !objs.isEmpty()){
			
			//System.out.println("//End verbal phrase: " + id);
			for (HashSet<String> objConj: objs)
				rq.addObjectConjunctions(id, objConj);
			objs = null;
		}
		
		openType.remove();
		
	}

	@Override
	public void endAdjP() {
		lastClosedType = Phrasal.ADJP;
	}

	@Override
	public void beginAdjP() {
		
	}

	@Override
	public void beginAdvP() {
		
	}

	@Override
	public void endAdvP() {
		lastClosedType = Phrasal.ADVP;
	}

	@Override
	public void beginPP(String prep) {
		System.out.println("prep. phrase: " + prep);
		openType.add(Phrasal.PP);
	}

	@Override
	public void endPP() {
		lastClosedType = Phrasal.PP;
		openType.remove();
	}

	@Override
	public void addNoun(String val, Det det, boolean plural, boolean proper) {
		String id = "role-" + numRole;
		int nounSynSet = 0;
		
		conj.add(id);
		
		if (wordnetReq == null)
			return;
		
		if(proper){
			//TODO search for its type: city, person, animal, etc.
			
		} else {
			val = val.toLowerCase();
			Morphology m = new Morphology();
			String lemma = m.lemma(val, "VERB");
			nounSynSet = wordnetReq.getSynset(lemma, "NOUN");
			if (nounSynSet < 1){
				System.out.println("No noun: " + val);
				return;
			}
				
		}
		
		rq.addRolePlayer(id, nounSynSet);
		if (plural)
			rq.setQuantity(id, "PL");
		if(proper)
			rq.setRoleProperName(id, val);
		
		rq.setDefined(id, det.name());
		lastClosedType = Terminal.NOUN;
	}
	
	public String getSTON(){
		return rq.getStructuredRequest();
	}

	@Override
	public void addVerb(String val, VerbTense tense) {
		
		String id = openVP.getFirst();
		int verbSynSet = 0;
		
		if (wordnetReq == null)
			return;
		Morphology m = new Morphology();
		String lemma = m.lemma(val, "VERB");
		//System.out.println("Lemma: " + lemma);
		verbSynSet = wordnetReq.getSynset(lemma, "VERB");
		
		if (verbSynSet < 1){
			System.out.println("No verb: " + val);
			return;
		}
		
		rq.addAction(id, verbSynSet);
		
		switch(tense){
		case FUTURE:
			break;
		case PAST:
			rq.addVerbSpecif(id, "PA", "", false, false);
			break;
		case PRESENT:
			rq.addVerbSpecif(id, "PR", "", false, false);
			break;
		default:
			break;
		
		}
		
		if (subjs != null && !subjs.isEmpty()){
			
			for (HashSet<String> subjConj: subjs)
				rq.addSubjectConjunctions(id, subjConj);
			subjs = null;
		}
	}

	@Override
	public void endSentence() {
		lastClosedType = Phrasal.S;
		openType.remove();
	}

	@Override
	public void conjected(boolean conj) {
		
		if (conj){
			//numRole++;
		} else {
			this.conj = new HashSet<String>();
			disj.add(this.conj);
		}
		
		if (lastClosedType == Terminal.NOUN){
			endNP();
			beginNP();
		}
		
	}

	
	/*
	//TODO add conjunctions and disjunctions ("and", "or")
	public Text2Ston() {
		try {
			wordnetReq = SqliteRequestor.create("eng", wordnetPath);
		} catch (NoSqliteBase | LangNotFound e) {
			System.out.println(e.getMessage());
			wordnetReq = null; 
		}
	}

	@Override
	public void beginNP() {
		isNP = true;
		numRole++;
		String rId = "r" + numRole;
		
		if (isNP && ! isVP)
			subjs.add(rId);
		
		openNP.addFirst(rId);
		openID.addFirst(rId);
		//System.out.println("Start of NP phrase");
		
	}

	@Override
	public void endNP() {
		isNP = false;
		lastRoleId = openNP.removeFirst();
		if (isVP){
			HashSet<String> objs = new HashSet<String>();
			objs.add(lastRoleId);
			rq.addObjectConjunctions("act" + numAction, objs);
		}
		//System.out.println("End of NP phrase");
		
	}

	@Override
	public void beginSentence() {
		//System.out.println("new sentence");
		acts = new HashSet<String>();
		rq.addSentence("AFF");
	}

	@Override
	public void beginVP() {
		
		numAction++;
		isVP = true;
		
		String actId = "act" + numAction;
		
		acts.add(actId);
		
		openVP.addFirst(actId);
		openID.addFirst(actId);
		//System.out.println("Start of VP phrase");
		
	}

	@Override
	public void endVP() {
		isVP = false;
		
		openVP.removeFirst();
		openID.removeFirst();
		//System.out.println("End of VP phrase");
		
	}

	@Override
	public void endAdjP() {
		
		
		//System.out.println("Start of Adjective phrase");
		
	}

	@Override
	public void beginAdjP() {
		//System.out.println("End of Adjective phrase");
		
	}

	@Override
	public void beginAdvP() {
		//System.out.println("Start of Adverb phrase");
		
	}

	@Override
	public void endAdvP() {
		//System.out.println("End of Adverb phrase");
		
	}

	@Override
	public void beginPP() {
		//System.out.println("Start of prepositional phrase");
		
	}

	@Override
	public void endPP() {
		//System.out.println("End of prepositional phrase");
		
	}

	@Override
	public void addNoun(String val, Det det, boolean plural, boolean proper) {
		String id = openNP.getFirst();
		int nounSynSet = 0;
		
		if (wordnetReq == null)
			return;
		
		if(proper){
			//TODO search for its type: city, person, animal, etc.
			
		} else {
			val = val.toLowerCase();
			Morphology m = new Morphology();
			String lemma = m.lemma(val, "VERB");
			nounSynSet = wordnetReq.getSynset(lemma, "NOUN");
			if (nounSynSet < 1){
				System.out.println("No noun: " + val);
				return;
			}
				
		}
		
		rq.addRolePlayer(id, nounSynSet);
		if (plural)
			rq.setQuantity(id, "PL");
		if(proper)
			rq.setRoleProperName(id, val);
		
		rq.setDefined(id, det.name());
		
	}
	
	public String getSTON(){
		return rq.getStructuredRequest();
	}

	@Override
	public void addVerb(String val, VerbTense tense) {
		String id = "act" + numAction;
		int verbSynSet = 0;
		
		if (wordnetReq == null)
			return;
		Morphology m = new Morphology();
		String lemma = m.lemma(val, "VERB");
		//System.out.println("Lemma: " + lemma);
		verbSynSet = wordnetReq.getSynset(lemma, "VERB");
		
		if (verbSynSet < 1){
			System.out.println("No verb: " + val);
			return;
		}
		
		rq.addAction(id, verbSynSet);
		
		switch(tense){
		case FUTURE:
			break;
		case PAST:
			rq.addVerbSpecif(id, "PA", "", false, false);
			break;
		case PRESENT:
			rq.addVerbSpecif(id, "PR", "", false, false);
			break;
		default:
			break;
		
		}
		
		if (! subjs.isEmpty()){
			rq.addSubjectConjunctions(id, subjs);
			subjs = new HashSet<String>();
		}
	}

	@Override
	public void endSentence() {
		rq.addSentActionConjunctions(true, acts);
		
	}*/
	
	

}
