import java.util.HashMap;
import java.util.Map.Entry;
import java.util.List;
import java.util.ArrayList;

public abstract class AST{
    public void error(String msg){
	System.err.println(msg);
	System.exit(-1);
    }
};

/* Expressions are similar to arithmetic expressions in the impl
   language: the atomic expressions are just Signal (similar to
   variables in expressions) and they can be composed to larger
   expressions with And (Conjunction), Or (Disjunction), and Not
   (Negation). Moreover, an expression can be using any of the
   functions defined in the definitions. */

abstract class Expr extends AST{
    public abstract Boolean eval(Environment env);
}

class Conjunction extends Expr{
    // Example: Signal1 * Signal2 
    Expr e1,e2;
    Conjunction(Expr e1,Expr e2){this.e1=e1; this.e2=e2;}

    @Override
    public Boolean eval(Environment env) {
        return e1.eval(env) && e2.eval(env);
    }
}

class Disjunction extends Expr{
    // Example: Signal1 + Signal2 
    Expr e1,e2;
    Disjunction(Expr e1,Expr e2){this.e1=e1; this.e2=e2;}

    @Override
    public Boolean eval(Environment env) {
        return e1.eval(env) || e2.eval(env);
    }
}

class Negation extends Expr{
    // Example: /Signal
    Expr e;
    Negation(Expr e){this.e=e;}

    @Override
    public Boolean eval(Environment env) {
        return !e.eval(env);
    }

}

class UseDef extends Expr{
    // Using any of the functions defined by "def"
    // e.g. xor(Signal1,/Signal2) 
    String f;  // the name of the function, e.g. "xor" 
    List<Expr> args;  // arguments, e.g. [Signal1, /Signal2]
    UseDef(String f, List<Expr> args){
	this.f=f; this.args=args;
    }

    @Override
    public Boolean eval(Environment env) {
        // Retrieve function definition
        Def functionDef = env.getDef(f);
        if (functionDef == null) {
            error("Function " + f + " is not defined!");
        }

        // Create a new environment for the function-call
        List<String> formalArgs = functionDef.args;
        if (formalArgs.size() != args.size()) {
            error("Function " + f + " expects " + formalArgs.size() + " arguments, but got " + args.size());
        }

        Environment functionEnv = new Environment(env);

        // Bind actual arguments to formal parameters in the newly created environment
        for (int i = 0; i < formalArgs.size(); i++) {
            String formalArg = formalArgs.get(i);
            boolean actualValue = args.get(i).eval(env);    // Evaluation of argument in current env
            functionEnv.setVariable(formalArg, actualValue);
        }

        return functionDef.e.eval(functionEnv);
    }
}

class Signal extends Expr{
    String varname; // a signal is just identified by a name 
    Signal(String varname){this.varname=varname;}

    @Override
    public Boolean eval(Environment env) {
        return env.getVariable(varname);
    }

}

class Def extends AST{
    // Definition of a function
    // Example: def xor(A,B) = A * /B + /A * B
    String f; // function name, e.g. "xor"
    List<String> args;  // formal arguments, e.g. [A,B]
    Expr e;  // body of the definition, e.g. A * /B + /A * B
    Def(String f, List<String> args, Expr e){
	this.f=f; this.args=args; this.e=e;
    }
}

// An Update is any of the lines " signal = expression "
// in the update section

class Update extends AST{
    // Example Signal1 = /Signal2 
    String name;  // Signal being updated, e.g. "Signal1"
    Expr e;  // The value it receives, e.g., "/Signal2"
    Update(String name, Expr e){this.e=e; this.name=name;}

    public void eval(Environment env) {
        Boolean value = e.eval(env);
        env.setVariable(name, value);
    }
}

/* A Trace is a signal and an array of Booleans, for instance each
   line of the .simulate section that specifies the traces for the
   input signals of the circuit. It is suggested to use this class
   also for the output signals of the circuit in the second
   assignment.
*/

class Trace extends AST{
    // Example Signal = 0101010
    String signal;
    Boolean[] values;
    Trace(String signal, Boolean[] values){
	this.signal=signal;
	this.values=values;
    }

    // Method to produce a visual output for given trace
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(signal).append(" = ");
        for (Boolean value : values) {
            sb.append(value ? "1" : "0");
        }
        return sb.toString();
    }

}

/* The main data structure of this simulator: the entire circuit with
   its inputs, outputs, latches, definitions and updates. Additionally
   for each input signal, it has a Trace as simulation input.
   
   There are two variables that are not part of the abstract syntax
   and thus not initialized by the constructor (so far): simoutputs
   and simlength. It is suggested to use these two variables for
   assignment 2 as follows: 
 
   1. all siminputs should have the same length (this is part of the
   checks that you should implement). set simlength to this length: it
   is the number of simulation cycles that the interpreter should run.

   2. use the simoutputs to store the value of the output signals in
   each simulation cycle, so they can be displayed at the end. These
   traces should also finally have the length simlength.
*/

class Circuit extends AST{
    String name;  
    List<String> inputs; 
    List<String> outputs;
    List<String>  latches;
    List<Def> definitions;
    List<Update> updates;
    List<Trace>  siminputs;
    List<Trace>  simoutputs;
    int simlength;
    Circuit(String name,
	    List<String> inputs,
	    List<String> outputs,
	    List<String>  latches,
	    List<Def> definitions,
	    List<Update> updates,
	    List<Trace>  siminputs){
	this.name=name;
	this.inputs=inputs;
	this.outputs=outputs;
	this.latches=latches;
	this.definitions=definitions;
	this.updates=updates;
	this.siminputs=siminputs;
    }

    // Method to run the simulator
    // The method calls method Initialize and then runs method nextCycle for each simulation cycle
    // @param env the simulation environment
    public void runSimulator(Environment env) {
        initialize(env);

        System.out.println("Starting simulation cycles...");

        for (int i = 1; i < simlength; i++) {
            System.out.println("Cycle " + i);
            nextCycle(env, i);
        }
    }

    // Method to set all latch outputs to 0 in given environment
    public void latchesInit(Environment env) {
        for (String latch : latches) {
            // Add "'"-symbol to get value 0
            env.setVariable(latch + "'", false);
        }
    }

    // Method to set every latch output to current value of latch input
    public void latchesUpdate(Environment env) {
        for (String latch : latches) {
            Boolean latchInputValue = env.getVariable(latch);
            env.setVariable(latch + "'", latchInputValue /* Placeholder-value */ );
        }
    }

    // Method to initialize all input signals, outputs of latches, remaining signals and print environment on screen
    public void initialize(Environment env) {
        // Initialize input signals
        for (String input : inputs) {
            Trace inputTrace = siminputs.stream()
                    .filter(trace -> trace.signal.equals(input))
                    .findFirst()
                    .orElse(null);

            if (inputTrace == null || inputTrace.values.length == 0) {
                error("Input signal + " + input + " is not defined or has no values!");
            }

            // Set value of input signal at time point 0 in env
            env.setVariable(input, inputTrace.values[0]);
        }

        // Determine simlength based on siminputs
        // Ensure all siminputs have same length
        if (siminputs.isEmpty()) {
            error("No simulation inputs provided!");
        } else {
            int length = siminputs.get(0).values.length;
            for (Trace inputTrace : siminputs) {
                if (inputTrace.values.length != length) {
                    error("All siminputs must have same length!");
                }
            }
            simlength = length;
        }

        // Initialize latch-outputs
        latchesInit(env);

        // Call to eval-method for all update-statements to initialize other signals
        for (Update update : updates) {
            update.eval(env);
        }

        // Lastly, print environment
        System.out.println("Environment efter initialization:");
        System.out.println(env);
        System.out.println("Simlength: " + simlength);
    }

    // Method to simulate the next cycle of the Circuit
    // @param env the simulation environment
    // @param i the current cycle number
    public void nextCycle(Environment env, int i) {
        // Update input signals for current cycle
        for (String input: inputs) {
            Trace inputTrace = siminputs.stream()
                    .filter(trace -> trace.signal.equals((input)))
                    .findFirst()
                    .orElse(null);

            if (inputTrace == null || i >= inputTrace.values.length) {
                error("Input signal: " + input + " is not defined for cycle + " + i);
            }

            // Set value of input-signal at time point i to env
            env.setVariable(input, inputTrace.values[i]);
        }

        // Update latches
        latchesUpdate(env);

        // Call eval on all update statements
        for (Update update : updates) {
            update.eval(env);
        }

        System.out.println("Environment efter cycle: " + i + ":");
        System.out.println(env);
    }
}
