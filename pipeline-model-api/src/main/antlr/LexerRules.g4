lexer grammar LexerRules;

BRACKET_OPEN : '{';
BRACKET_CLOSE : '}';
LEFT_PAREN : '(';
RIGHT_PAREN : ')';

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

/* Pilfered from the Java.g4 grammar here:
 * https://github.com/antlr/grammars-v4/blob/master/java/Java.g4
 */
COMMENT
    :   '/*' .*? '*/' -> channel(HIDDEN)
    ;
LINE_COMMENT
    :   '//' ~[\r\n]* -> channel(HIDDEN)
    ;
