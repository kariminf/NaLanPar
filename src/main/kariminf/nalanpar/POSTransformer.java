package kariminf.nalanpar;


import kariminf.nalanpar.Types.Posable;
import kariminf.nalanpar.UnivParser.Element;
import kariminf.sentrep.univ.types.Determiner;

public interface POSTransformer {
	
	public Posable getType(String pos);
	
	public Element getTerminalElement(String pos, String val);
	
	public Element getPhrasalElement(String pos, boolean begin);
	
	public Determiner getDet(String val);

}
