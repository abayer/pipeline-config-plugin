lexer grammar JenkinsfileLexer;

BRACKET_OPEN : '{';
BRACKET_CLOSE : '}';
LEFT_PAREN : '(';
RIGHT_PAREN : ')';
COLON : ':';
COMMA : ',';
EQUALS : '=';

/* Directive/section names */
ENVIRONMENT : 'environment';
TOOLS : 'tools';
AGENT : 'agent';
WHEN : 'when';
POST : 'post';
STEPS : 'steps';
STAGE : 'stage';
STAGES : 'stages';
OPTIONS : 'options';
TRIGGERS : 'triggers';
INPUT : 'input';
FAILFAST : 'failFast';
AGENT_ANY : 'any';
AGENT_NONE : 'none';
PARALLEL : 'parallel';
PARAMETERS : 'parameters';
LIBRARIES : 'libraries';
LIB : 'lib';
PIPELINE : 'pipeline';

/* Pilfered from the Java.g4 grammar here:
 * https://github.com/antlr/grammars-v4/blob/master/java/Java.g4
 */
COMMENT
    :   '/*' .*? '*/' -> channel(HIDDEN)
    ;
LINE_COMMENT
    :   '//' ~[\r\n]* -> channel(HIDDEN)
    ;

ID : Letter LetterOrDigit*;
INT : DIGIT+;
BOOLEAN : TRUE
          | FALSE
          ;
TRUE : 'true';
FALSE: 'false';
BLOCK_BODY : [a-zA-Z]+;
SQ_STRING : QUOTE_CHAR (STRING_CHAR | DOUBLEQUOTE_CHAR | ESC)* QUOTE_CHAR;
TSQ_STRING : TRIPLE_SINGLEQUOTE TSQ_CHAR* TRIPLE_SINGLEQUOTE;
NON_EXPR_DQ_STRING : DOUBLEQUOTE_CHAR (STRING_CHAR | QUOTE_CHAR | ESC)* DOUBLEQUOTE_CHAR;
NON_EXPR_STRING : SQ_STRING;
/* skip all whitespace */
WS : (' ' | NEWLINE | TAB) -> skip;

/* To be re-used in all numeric lexer rules */
fragment DIGIT : [0-9]      ;
/* Used for crafting rules that respect Windows and Unix newlnies */
fragment NEWLINE : '\r'? '\n' ;
fragment TAB : '\t' ;
fragment ESC : '\\' [btnfr"'\\];
fragment STRING_CHAR : ~('\'' | '"' | '\r' | '\n');
fragment QUOTE_CHAR : '\'';
fragment DOUBLEQUOTE_CHAR : '"';
DQ_OPEN : DOUBLEQUOTE_CHAR -> pushMode(DqStringMode);
fragment Letter : [a-zA-Z$_] ;
fragment LetterOrDigit : [a-zA-Z0-9$_] ;
fragment DOLLAR : '$';
fragment TRIPLE_DOUBLEQUOTE : '"""';
TDQ_OPEN : TRIPLE_DOUBLEQUOTE -> pushMode(TdqStringMode);
fragment TRIPLE_SINGLEQUOTE : '\'\'\'';
fragment DQ_CHAR : ~('"' | '\\' | '$') | ESC;
fragment SQ_CHAR : ~['\\] | ESC;
fragment TDQ_CHAR : ~["\\$]
                  | DOUBLEQUOTE_CHAR { !(_input.LA(1) == '"' && _input.LA(2) == '"') }?
                  | ESC;
fragment TSQ_CHAR : ~['\\]
                  | QUOTE_CHAR { !(_input.LA(1) == '\'' && _input.LA(2) == '\'') }?
                  | ESC;

mode DqStringMode;
DQ_CLOSE
    :   DOUBLEQUOTE_CHAR -> popMode
    ;
DqStrText : DQ_CHAR+;
DqStrExprStart : '${' -> pushMode(GStringExpression);

mode TdqStringMode;
TDQ_CLOSE
    :   TRIPLE_DOUBLEQUOTE -> popMode
    ;
TdqQuote : '"'+;
TdqStrText : TDQ_CHAR+ | '$';
TdqStrExprStart : '${' -> pushMode(GStringExpression);

mode GStringExpression;
GStrExpr_BRACKET_CLOSE : BRACKET_CLOSE -> popMode, type(BRACKET_CLOSE);
GStrExpr_ID : ID -> type(ID);