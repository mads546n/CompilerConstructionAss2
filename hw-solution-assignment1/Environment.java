import java.util.HashMap;
import java.util.Map.Entry;
import java.util.List;
import java.util.ArrayList;

class Environment {
    // Map for the signals: each signal has a current value as a Boolean
    private HashMap<String,Boolean> variableValues = new HashMap<String,Boolean>();

    // Map for defined functions like "xor": for a string like "xor",
    // you can look up its definition    
    private HashMap<String,Def> defs;

    // Standard constructor that does not care about definitions
    // You cannot use getDef() below when using this constructor.
    public Environment(){ this.defs=new HashMap<String,Def>();} 

    // Constructor that compute the map of function definitions, given
    // a the set of definitions as available in Circuit. You can then
    // use getDef() below.    
    public Environment(List<Def> listdefs) {
	defs=new HashMap<String,Def>();
	for(Def d:listdefs)
	    defs.put(d.f,d);
    }

    // This constructor can be used during eval to create a new
    // environment with the same definitions contained in an existing
    // one:
    public Environment(Environment env) { this.defs=env.defs; }

    // Lookup a definition, e.g., "xor"
    public Def getDef(String name){
	Def d=defs.get(name);
	if (d==null){ System.err.println("Function not defined: "+name); System.exit(-1); }  
	return d;
    }

    // return the set of all definitions; this is helpful when
    // creating a new environment during eval: just get the defs from
    // the current environment and using it as an argument to the
    // constructor for the new environemnt
    public HashMap<String,Def> getDefs(){return defs;};
    public void setVariable(String name, Boolean value) {
	variableValues.put(name, value);
    }
    
    public Boolean getVariable(String name){
	Boolean value = variableValues.get(name); 
	if (value == null) { System.err.println("Variable not defined: "+name); System.exit(-1); }
	return value;
    }

    public Boolean hasVariable(String name){
	Boolean v = variableValues.get(name); 
	return (v != null);	
    }
    
    public String toString() {
	String table = "";
	for (Entry<String,Boolean> entry : variableValues.entrySet()) {
	    table += entry.getKey() + "\t-> " + entry.getValue() + "\n";
	}
	return table;
    }   
}

