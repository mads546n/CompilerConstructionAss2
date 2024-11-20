grammar hw;

/* A small hardware description language */

start   :  'hardware:' name=ID
	   ('inputs:' ins+=ID+)?
	   ('outputs:' outs+=ID+)?
	   'latches:' ls+=ID+
	   defs+=defdecl*
	   'updates:' up+=updatedecl+
	   'siminputs:' simin+=simInp+
	   EOF ;

defdecl : 'def:' f=ID '(' (xs+=ID (',' xs+=ID)*)? ')' '=' e=expr;

updatedecl : write=ID '=' e=expr ;

simInp : in=ID '=' str=BITSTRING ;

expr	: '/' e=expr   	       # Negation
	| e1=expr '*'? e2=expr # Conjunction
	| e1=expr '+' e2=expr # Disjunction
	| f=ID '(' es+=expr (',' es+=expr)* ')' # UseDef
	| x=ID 		       # Signal
	| '(' e=expr ')'       # Parenthesis
	;

ID    : ALPHA (ALPHA|NUM)* '\''?;
BITSTRING : [01]+ ;

fragment
ALPHA : [a-zA-Z_ÆØÅæøå] ;

fragment
NUM   : [0-9] ;

WHITESPACE : [ \n\t\r]+ -> skip;
COMMENT    : '//'~[\n]*  -> skip;
COMMENT2   : '/*' (~[*] | '*'~[/]  )*   '*/'  -> skip;
