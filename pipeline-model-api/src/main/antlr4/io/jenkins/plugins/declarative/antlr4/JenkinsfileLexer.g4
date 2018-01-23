lexer grammar JenkinsfileLexer;

BRACKET_OPEN : '{';
BRACKET_CLOSE : '}';
LEFT_PAREN : '(';
RIGHT_PAREN : ')';
COLON : ':';
COMMA : ',';
EQUALS : '=';

ID : Letter LetterOrDigit*;
INT : DIGIT+;
BOOLEAN : TRUE
          | FALSE
          ;
TRUE : 'true';
FALSE: 'false';
BLOCK_BODY : [a-zA-Z]+;
STRING : (QUOTE_CHAR (STRING_CHAR | DOUBLEQUOTE_CHAR | ESC)* QUOTE_CHAR)
         |
         (DOUBLEQUOTE_CHAR (STRING_CHAR | QUOTE_CHAR | ESC)* DOUBLEQUOTE_CHAR)
         |
         (TRIPLE_SINGLEQUOTE TSQ_CHAR* TRIPLE_SINGLEQUOTE)
         |
         (TRIPLE_DOUBLEQUOTE TDQ_CHAR* TRIPLE_DOUBLEQUOTE)
         ;

/* skip all whitespace */
WS : (' ' | NEWLINE | TAB) -> skip;

/* To be re-used in all numeric lexer rules */
fragment DIGIT : [0-9]      ;
/* Used for crafting rules that respect Windows and Unix newlnies */
fragment NEWLINE : '\r'? '\n' ;
fragment TAB : '\t' ;
fragment ESC : '\\' [btnfr"'\\];
fragment STRING_CHAR : ~['"\r\n];
fragment QUOTE_CHAR : '\'';
fragment DOUBLEQUOTE_CHAR : '"';
fragment Letter : [a-zA-Z$_] ;
fragment LetterOrDigit : [a-zA-Z0-9$_] ;
fragment DOLLAR : '$';
fragment TRIPLE_DOUBLEQUOTE : '"""';
fragment TRIPLE_SINGLEQUOTE : '\'\'\'';
fragment DQ_CHAR : ~["\\$] | ESC;
fragment SQ_CHAR : ~['\\] | ESC;
fragment TDQ_CHAR : ~["\\$]
                  | DOUBLEQUOTE_CHAR { !(_input.LA(1) == '"' && _input.LA(2) == '"') }?
                  | ESC;
fragment TSQ_CHAR : ~['\\]
                  | QUOTE_CHAR { !(_input.LA(1) == '\'' && _input.LA(2) == '\'') }?
                  | ESC;

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

// Groovy gstring
GSTRING_BEGIN
    : DOUBLEQUOTE_CHAR DQ_CHAR* DOLLAR -> pushMode(DQ_GSTRING_MODE), pushMode(GSTRING_TYPE_SELECTOR_MODE)
    ;

TDQ_GSTRING_BEGIN
    : TRIPLE_DOUBLEQUOTE TDQ_CHAR* DOLLAR -> type(GSTRING_BEGIN), pushMode(TDQ_GSTRING_MODE), pushMode(GSTRING_TYPE_SELECTOR_MODE)
    ;

mode DQ_GSTRING_MODE;
GSTRING_END
    :   DOUBLEQUOTE_CHAR -> popMode
    ;
GSTRING_PART
    :   DOLLAR -> pushMode(GSTRING_TYPE_SELECTOR_MODE)
    ;
GSTRING_CHAR
    :   DQ_CHAR -> more
    ;

mode TDQ_GSTRING_MODE;
TDQ_GSTRING_END
    :   TRIPLE_DOUBLEQUOTE -> type(GSTRING_END), popMode
    ;
TDQ_GSTRING_PART
    :   DOLLAR -> type(GSTRING_PART), pushMode(GSTRING_TYPE_SELECTOR_MODE)
    ;
TDQ_GSTRING_CHAR
    :   TDQ_CHAR -> more
    ;

mode GSTRING_TYPE_SELECTOR_MODE;
GSTRING_BRACKET_OPEN
    :   '{' -> type(BRACKET_OPEN), popMode, pushMode(DEFAULT_MODE)
    ;
