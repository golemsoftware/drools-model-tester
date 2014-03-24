package net.vojir.droolsserver.drools;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.drools.compiler.compiler.DroolsParserException;
import org.drools.compiler.compiler.PackageBuilder;
import org.drools.core.RuleBase;
import org.drools.core.RuleBaseFactory;
import org.drools.core.StatelessSession;
import org.drools.core.rule.Package;

@SuppressWarnings("restriction")
public class ModelTesterSessionHelper {

	private static String betterARMethod;
	
	/**
	 * Funkce pro vytvo�en� nov�ho DLR �et�zce - inicializace pomoc� v�choz�ch import�
	 */
	public static String prepareDrlString(String drl){
		StringBuffer drlString=new StringBuffer();
		drlString.append("import net.vojir.droolsserver.drools.DrlObj;");
		drlString.append("import net.vojir.droolsserver.drools.DrlAR;");
		drlString.append("import function net.vojir.droolsserver.drools.ModelTesterSessionHelper.isBetterAR;");
		drlString.append(drl);
		return drlString.toString();
	}
	
	/**
	 * Statick� funkce pro vytvo�en� RuleBase na z�klad� DRL �et�zce
	 * @param drlString
	 * @return
	 */
	public static RuleBase prepareRuleBase(String drlString){
		//read in the source
		Reader source = new StringReader(drlString);
				
		PackageBuilder builder = new PackageBuilder();
		
		Package pkg = builder.getPackage();
		if (pkg!=null){
			builder.getPackage().clear();
		}
		
		//this wil parse and compile in one step
		//NOTE: There are 2 methods here, the one argument one is for normal DRL.
		try {
			builder.addPackageFromDrl(source);
		} catch (DroolsParserException | IOException e) {
			//TODO zalogov�n� chyby
			e.printStackTrace();
		}
		
		//get the compiled package (which is serializable)
		pkg = builder.getPackage();
		
		//add the package to a rulebase (deploy the rule package).
		RuleBase ruleBase = RuleBaseFactory.newRuleBase();
		ruleBase.addPackage(pkg);
		return ruleBase;		
	}
    
	/**
	 * Statick� funkce pro vytvo�en� stateless session
	 * @param drlString
	 * @return
	 */
    public static StatelessSession prepareStatelessSession(String drlString){
		RuleBase ruleBase = prepareRuleBase(drlString);
		return ruleBase.newStatelessSession();
	}
    
    
    /**
     * Statick� funkce slou��c� k vyhodnocen�, jestli je nov� vyhodnocovan� pravidlo lep��, ne� p�edchoz� nalezen� varianta
     * @param globalAR
     * @param currentAR
     * @return
     */
    public static boolean isBetterAR(DrlAR globalAR, DrlAR currentAR){
    	if (globalAR.getId().equals("")){
    		return true;
    	}
		switch (getBetterARMethod()) {
			case "longerAntecedent":
				return isBetterAR_longerAntecedent(globalAR, currentAR);
			case "shorterAntecedent":
				return isBetterAR_shorterAntecedent(globalAR, currentAR);
			case "support":
				return isBetterAR_support(globalAR, currentAR);
			case "csCombination":
				return isBetterAR_confidenceSupportCombination(globalAR, currentAR);
			default:
				return isBetterAR_confidence(globalAR, currentAR);
		}
    }
    
    //-----------------------------------------------------------------------------------
    public static boolean isBetterAR_confidence(DrlAR globalAR,DrlAR currentAR){
    	if (currentAR.getConfidenceValue()>globalAR.getConfidenceValue()){
			return true;
		}else if(currentAR.getConfidenceValue()==globalAR.getConfidenceValue()){
			return (currentAR.getSupportValue()>globalAR.getSupportValue());
		}
		return false;
    }
    public static boolean isBetterAR_longerAntecedent(DrlAR globalAR,DrlAR currentAR){
    	if (currentAR.getAntecedentLength()>globalAR.getAntecedentLength()){
			return true;
		}else if(currentAR.getAntecedentLength()==globalAR.getAntecedentLength()){
			return isBetterAR_confidence(globalAR, currentAR);
		}
		return false;
    }
    public static boolean isBetterAR_shorterAntecedent(DrlAR globalAR,DrlAR currentAR){
    	if (currentAR.getAntecedentLength()<globalAR.getAntecedentLength()){
    		System.out.println("shorter");
			return true;
		}else if(currentAR.getAntecedentLength()==globalAR.getAntecedentLength()){
			return isBetterAR_confidence(globalAR, currentAR);
		}
		return false;
    }
    public static boolean isBetterAR_support(DrlAR globalAR,DrlAR currentAR){
    	if (currentAR.getSupportValue()>globalAR.getSupportValue()){
			return true;
		}else if(currentAR.getSupportValue()==globalAR.getSupportValue()){
			return isBetterAR_confidence(globalAR, currentAR);
		}
		return false;
    }
    public static boolean isBetterAR_confidenceSupportCombination(DrlAR globalAR,DrlAR currentAR){
    	double ar1=globalAR.getConfidenceValue()*Math.log(globalAR.getSupportValue());
    	double ar2=currentAR.getConfidenceValue()*Math.log(currentAR.getSupportValue());
    	return (ar2>ar1);
    }
    //-----------------------------------------------------------------------------------
	public static String getBetterARMethod() {
		return betterARMethod;
	}

	public static void setBetterARMethod(String betterARMethod) {
		if (methodExists("isBetterAR_"+betterARMethod)){
			ModelTesterSessionHelper.betterARMethod = betterARMethod;
		}else{
			ModelTesterSessionHelper.betterARMethod="confidence";
		}
	}
    
	/**
	 * Statick� funkce pro ov��en�, zda je definovan� metoda se zvolen�m n�zvem
	 * @param methodName
	 * @return
	 */
	public static boolean methodExists(String methodName){
		try {
			ModelTesterSessionHelper.class.getMethod(methodName);
		}catch (Exception e){
			return false;
		}
		return true;
	}
}
