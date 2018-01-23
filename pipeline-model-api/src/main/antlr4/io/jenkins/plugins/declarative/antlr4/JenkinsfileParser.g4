/*
 * Main grammar for the Declarative Pipeline syntax
 */
parser grammar JenkinsfileParser;

options {
    tokenVocab = JenkinsfileLexer;
}

/* Building blocks */
gstring : GSTRING_BEGIN gstringValue (GSTRING_PART gstringValue)* GSTRING_END;

gstringValue : BRACKET_OPEN stepExpr BRACKET_CLOSE;

key : ID;

simpleValue : ( STRING | BOOLEAN | INT | gstring);

keyValuePair : key simpleValue;

mapKeyValuePair : key mapValue;

mapValue : (simpleValue |
              BRACKET_OPEN
              mapKeyValuePair+
              BRACKET_CLOSE
           );

stepPositionalParamExpr : stepPositionalParam (COMMA stepPositionalParam)*;
stepPositionalParam : stepArg;

stepNamedParamExpr : stepNamedParam (COMMA stepNamedParam)*;
stepNamedParam : key COLON stepArg;

stepArg : (simpleValue | methodExpr);

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

toolEntry : ID STRING;

when : WHEN
         BRACKET_OPEN
         whenEntry+
         BRACKET_CLOSE;

whenEntry : ID (stepNamedParamExpr |
                (BRACKET_OPEN
                 whenEntry+
                 BRACKET_CLOSE));

steps : STEPS stepBlock;

stepExpr : (methodExpr | (methodExpr stepBlock) | (ID stepBlock));

stepBlock : BRACKET_OPEN stepExpr+ BRACKET_CLOSE;


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

stage : STAGE LEFT_PAREN STRING RIGHT_PAREN
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

libExpr : LIB LEFT_PAREN STRING RIGHT_PAREN;

libraries : LIBRARIES
              BRACKET_OPEN
              libExpr+
              BRACKET_CLOSE;

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