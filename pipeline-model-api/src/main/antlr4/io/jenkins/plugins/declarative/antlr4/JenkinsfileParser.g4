/*
 * Main grammar for the Declarative Pipeline syntax
 */
parser grammar JenkinsfileParser;

options {
    tokenVocab = JenkinsfileLexer;
}

pipeline : PIPELINE
             BRACKET_OPEN
             (agent |
               stages |
               libraries |
               triggers |
               parameters |
               opts |
               environment |
               tools |
               post)+
             BRACKET_CLOSE
             EOF;

stepExpr : (methodExpr | (methodExpr stepBlock) | (ID stepBlock));

stepBlock : BRACKET_OPEN stepExpr+ BRACKET_CLOSE;

stepPositionalParamExpr : stepPositionalParam (COMMA stepPositionalParam)*;
stepPositionalParam : stepArg;

stepNamedParamExpr : stepNamedParam (COMMA stepNamedParam)*;
stepNamedParam : key COLON stepArg;

stepArg : simpleValue;

stepArgs : LEFT_PAREN? (stepPositionalParamExpr | stepNamedParamExpr) RIGHT_PAREN?;

emptyArgs : LEFT_PAREN RIGHT_PAREN;

methodExpr : ID (stepArgs | emptyArgs);

environment : ENVIRONMENT
                BRACKET_OPEN
                environmentEntry+
                BRACKET_CLOSE;

environmentEntry : ID EQUALS stepArg;

tools : TOOLS
          BRACKET_OPEN
          toolEntry+
          BRACKET_CLOSE;

toolEntry : ID NON_EXPR_STRING;

when : WHEN
         BRACKET_OPEN
         whenEntry+
         BRACKET_CLOSE;

whenEntry : ID (stepNamedParamExpr |
                (BRACKET_OPEN
                 whenEntry+
                 BRACKET_CLOSE));

steps : STEPS stepBlock;

post : POST
         BRACKET_OPEN
         postEntry+
         BRACKET_CLOSE;

postEntry : ID stepBlock;

input : INPUT
          BRACKET_OPEN
          mapKeyValuePair+
          BRACKET_CLOSE;

failfast : FAILFAST BOOLEAN;

stages : STAGES
           BRACKET_OPEN
           stage+
           BRACKET_CLOSE;

parallel : PARALLEL
             BRACKET_OPEN
             stage+
             BRACKET_CLOSE;

agent : AGENT ((AGENT_ANY | AGENT_NONE) |
                 (BRACKET_OPEN
                  ID mapValue
                  BRACKET_CLOSE));

stage : STAGE LEFT_PAREN NON_EXPR_STRING RIGHT_PAREN
          BRACKET_OPEN
          (agent |
            when |
            environment |
            tools |
            post |
            failfast |
            opts |
            input |
            (steps | parallel | stages))+
          BRACKET_CLOSE;


opts: OPTIONS
        BRACKET_OPEN
        methodExpr+
        BRACKET_CLOSE;

triggers : TRIGGERS
             BRACKET_OPEN
             methodExpr+
             BRACKET_CLOSE;

parameters : PARAMETERS
               BRACKET_OPEN
               methodExpr+
               BRACKET_CLOSE;

libExpr : LIB LEFT_PAREN NON_EXPR_STRING RIGHT_PAREN;

libraries : LIBRARIES
              BRACKET_OPEN
              libExpr+
              BRACKET_CLOSE;

/* Building blocks */
stringLiteral : sqStringLiteral | tsqStringLiteral | dqStringLiteral | tdqStringLiteral;

sqStringLiteral : SQ_STRING;

tsqStringLiteral : TSQ_STRING;

dqStringLiteral : DQ_OPEN (dqStringContent | dqStringExpression)* DQ_CLOSE;

tdqStringLiteral : TDQ_OPEN (tdqStringContent | tdqStringExpression | dqStringLiteral | TdqQuote)* TDQ_CLOSE;

dqStringContent : DqStrText;

// TODO: Add support for more than just ${bar} - i.e., ${foo.bar}, maybe ${bar(...)} (but probably not that).
dqStringExpression : DqStrExprStart ID BRACKET_CLOSE;

tdqStringContent : TdqStrText;

tdqStringExpression : TdqStrExprStart ID BRACKET_CLOSE;

key : ID;

simpleValue : ( stringLiteral | BOOLEAN | INT );

keyValuePair : key simpleValue;

mapKeyValuePair : key mapValue;

mapValue : (simpleValue |
              BRACKET_OPEN
              mapKeyValuePair+
              BRACKET_CLOSE
           );