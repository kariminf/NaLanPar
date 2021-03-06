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

import kariminf.nalanpar.Types.Phrasal;
import kariminf.nalanpar.Types.Posable;
import kariminf.nalanpar.Types.Terminal;
import kariminf.sentrep.ston.Univ2StonMap;
import kariminf.sentrep.ston.request.ReqAction;
import kariminf.sentrep.ston.request.ReqCreator;
import kariminf.sentrep.ston.request.ReqRolePlayer;
import kariminf.sentrep.ston.request.ReqSentence;
import kariminf.sentrep.types.Determiner;
import kariminf.sentrep.types.Pronoun;
import kariminf.sentrep.types.VerbTense;

public class Text2Ston implements ParseHandler {

	private final static String wordnetPath = 
			"../LangPi/wordnetDB/wordnet.sqlite";
	private ReqCreator rq = new ReqCreator();
	private WNRequestor wordnetReq = null; 
	private Univ2StonMap univ2stonMapper = new Univ2StonMap();
	private int numAction = 0;
	private int numRole = 0;
	/*
	private boolean isNP = false;
	private boolean isVP = false;
	private HashMap<String, ReqRolePlayer> players = new HashMap<String, ReqRolePlayer>();
	private HashMap<String, ReqAction> actions = new HashMap<String, ReqAction>();
	private ArrayList<ReqSentence> sentences = new ArrayList<ReqSentence>();
	 */
	private ArrayDeque<String> openNP = new ArrayDeque<String>();
	private ArrayDeque<String> openVP = new ArrayDeque<String>();
	private ArrayDeque<String> openAdjP = new ArrayDeque<String>();
	private ArrayDeque<String> openID = new ArrayDeque<String>();

	private ArrayDeque<Phrasal> openType = new ArrayDeque<Phrasal>();
	private Posable lastClosedType = Phrasal.S;

	private String lastClosedID = "";

	//This is used to detect if a PP means which
	//For example: The book at home is mine => which is at
	private boolean ppWhich = false;

	private ArrayList<ArrayList<String>> acts ;
	private ArrayList<String> actConj ;

	private ArrayList<ArrayList<String>> disj = new ArrayList<ArrayList<String>>();
	private ArrayList<String> conj = new ArrayList<String>();

	private ArrayList<ArrayList<String>> subjs = null;
	private ArrayList<ArrayList<String>> objs = null;
	private ArrayList<ArrayList<String>> rels = null;

	private class DisjInfo {
		ArrayList<ArrayList<String>> ddisj = null;
		ArrayList<String> dconj = null;
		ArrayList<ArrayList<String>> dsubjs = null;
		ArrayList<ArrayList<String>> dobjs = null;
		ArrayList<ArrayList<String>> drels = null;

		public void switchInfo(){
			ddisj = disj;
			dconj = conj;
			dsubjs = subjs;
			dobjs = objs;
			drels = rels;

			subjs = null;
			objs = null;
			rels = null;

			disj = new ArrayList<ArrayList<String>>();
			conj = new ArrayList<String>();
			disj.add(conj);
		}

		public void switchBackInfo(){
			disj = ddisj;
			conj = dconj;
			subjs = dsubjs;
			objs = dobjs;
			rels = drels;
		}
	}

	private ArrayDeque<DisjInfo> disjCache = new ArrayDeque<DisjInfo>();

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
			//System.out.println("PP inside VP");
			if (rels == null){
				disj = new ArrayList<ArrayList<String>>();
				conj = new ArrayList<String>();
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
			//System.out.println("NP inside VP");
			if (objs == null){
				disj = new ArrayList<ArrayList<String>>();
				conj = new ArrayList<String>();
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
		openType.addFirst(Phrasal.NP);

	}

	@Override
	public void endNP() {
		lastClosedID = openNP.removeFirst();
		openID.removeFirst();
		lastClosedType = Phrasal.NP;
		openType.removeFirst();
	}

	@Override
	public void beginSentence() {
		openType.addFirst(Phrasal.S);

		acts = new ArrayList<ArrayList<String>>();
		actConj = new ArrayList<String>();
		acts.add(actConj);
	}

	@Override
	public void beginVP() {
		numAction++;

		String actId = "act-" + numAction;

		if (lastClosedType == Phrasal.NP){
			if (disj.size() > 0){
				subjs = disj;
				disj = new ArrayList<ArrayList<String>>();
			}	
		}

		//TODO you have to verify if it is a relative action before adding it
		if (! ppWhich)
			actConj.add(actId);

		openVP.addFirst(actId);
		openID.addFirst(actId);
		openType.addFirst(Phrasal.VP);

	}

	@Override
	public void endVP() {
		String id = openVP.removeFirst();
		openID.removeFirst();
		lastClosedType = Phrasal.VP;
		//System.out.println("End verbal phrase: " + id);
		if (objs != null && !objs.isEmpty()){

			//System.out.println("//End verbal phrase: " + id);
			for (ArrayList<String> objConj: objs)
				rq.addThemeConjunctions(id, objConj);
			objs = null;
		}

		openType.removeFirst();
		lastClosedID = id;
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
		//System.out.println("prep. phrase: " + prep);

		if (openType.getFirst() == Phrasal.NP){
			String id = lastClosedID; //openNP.getFirst();

			DisjInfo disjInfo = new DisjInfo();
			disjInfo.switchInfo();

			disjCache.addFirst(disjInfo);

			rq.addRelative("SUBJ", id);

			//To prevent adding the next action to the sentence
			ppWhich = true;

			beginVP();

			addVerb("be", VerbTense.PRESENT);

			ArrayList<String> tmpRel = new ArrayList<String>();
			tmpRel.add(openVP.getFirst());
			//System.out.println("relative: " + openVP.getFirst());
			rq.addRelativeConjunctions(tmpRel);

			beginPP(prep);


			return;
		}

		//TODO research for the type
		String type = "IN";

		//System.out.println("last open type: " + openType.getFirst());
		if (openType.getFirst() == Phrasal.VP){
			//System.out.println("adpositional phrase " + type);
			String id = openVP.getFirst();
			rq.addRelative(type, id);
		}


		openType.addFirst(Phrasal.PP);

	}

	@Override
	public void endPP() {
		lastClosedType = Phrasal.PP;
		openType.removeFirst();

		if (rels != null && !rels.isEmpty()){
			for (ArrayList<String> relConj: rels){
				if (relConj != null)
					rq.addRelativeConjunctions(relConj);
			}

			rels = null;
		}

		if (ppWhich){
			ppWhich = false;

			endVP();

			if (! disjCache.isEmpty()){
				DisjInfo disjInfo = disjCache.removeFirst();
				/*
				conj = disjInfo.conj;
				disj = disjInfo.disj;
				subjs = disjInfo.subjs;
				objs = disjInfo.objs;
				rels = disjInfo.rels;
				 */
				disjInfo.switchBackInfo();
			}

		}
	}

	@Override
	public void addNoun(String val, Determiner det, boolean plural, boolean proper, Pronoun pronoun) {
		String id = "role-" + numRole;
		int nounSynSet = 0;

		conj.add(id);

		if (wordnetReq == null)
			return;


		//Search for the synset (some proper nouns have synsets like Cairo)
		//val = val.toLowerCase();
		Morphology m = new Morphology();
		String lemma = m.lemma(val, "NOUN");
		//System.out.println("==>" + lemma);
		nounSynSet = wordnetReq.getSynset(lemma, "NOUN", false);
		
		//This is just to prevent nam: from being add
		boolean nameNeeded = false;
		if (nounSynSet < 1){
			if(! proper){
				System.out.println("No noun: " + val);
				return;
			}
			//TODO search for its type: city, person, animal, etc.
			nounSynSet = 7846; //person
			nameNeeded = true;
		}



		if (pronoun != null){
			rq.addPronounRolePlayer(id, nounSynSet, univ2stonMapper.getPronoun(pronoun));
		} else {
			rq.addRolePlayer(id, nounSynSet);
		}

		if (plural) rq.setQuantity(id, "PL");
		//If it is a proper noun with a synset; we get rid of the name
		if(proper && nameNeeded) rq.setRoleProperName(id, val);

		//Transform from universal to Ston

		String sdet = "";
		switch (det) {
		case YES:
			sdet = "Y";
			break;

		case NO:
			sdet = "N";
			break;

		default:
			sdet = "NONE";
			break;
		}

		rq.setDefined(id, sdet);
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
		verbSynSet = wordnetReq.getSynset(lemma, "VERB", false);

		if (verbSynSet < 1){
			System.out.println("No verb: " + val);
			return;
		}

		rq.addAction(id, verbSynSet);

		switch(tense){
		case FUTURE:
			break;
		case PAST:
			rq.addVerbSpecif(id, "PA", "", false, false, false);
			break;
		case PRESENT:
			rq.addVerbSpecif(id, "PR", "", false, false, false);
			break;
		default:
			break;

		}

		if (subjs != null && !subjs.isEmpty()){

			for (ArrayList<String> subjConj: subjs)
				rq.addAgentConjunctions(id, subjConj);
			subjs = null;
		}
	}

	@Override
	public void endSentence() {
		lastClosedType = Phrasal.S;
		openType.removeFirst();
		rq.addSentence("AFF");
		for(ArrayList<String> actConj : acts)
			rq.addSentActionConjunctions(actConj);

	}

	@Override
	public void conjected(boolean conj) {

		if (conj){
			//numRole++;
		} else {
			this.conj = new ArrayList<String>();
			disj.add(this.conj);
		}

		if (lastClosedType == Terminal.NOUN){
			endNP();
			beginNP();
		}

	}

	@Override
	public void addPronoun(Pronoun pronoun) {
		String id = "role-" + numRole;

		conj.add(id);
		rq.addPronounRolePlayer(id, 0, univ2stonMapper.getPronoun(pronoun));

	}


}
