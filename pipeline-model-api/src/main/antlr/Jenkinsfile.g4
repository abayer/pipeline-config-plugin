/*
 * Main grammar for the Declarative Pipeline syntax
 */
grammar Jenkinsfile;
import LexerRules;

/* Building blocks */
key : ID;

simpleValue : ( STRING | BOOLEAN | INT );

keyValuePair : key simpleValue;

mapKeyValuePair : key mapValue;

mapValue : (simpleValue |
              BRACKET_OPEN
              mapKeyValuePair+
              BRACKET_CLOSE
           );

/* Parameters/arguments and method invocation */
stepPositionalParamExpr : stepPositionalParam (',' stepPositionalParam)*;
stepPositionalParam : stepArg;

stepNamedParamExpr : stepNamedParam (',' stepNamedParam)*;
stepNamedParam : key ':' stepArg;

stepArg : (simpleValue | methodExpr);

stepArgs : LEFT_PAREN? (stepPositionalParamExpr | stepNamedParamExpr) RIGHT_PAREN?;

emptyArgs : LEFT_PAREN RIGHT_PAREN;

methodExpr : ID (stepArgs | emptyArgs);

/* Environment */
environment : 'environment'
                BRACKET_OPEN
                environmentEntry+
                BRACKET_CLOSE;

environmentEntry : ID '=' stepArg;

/* Tools */
tools : 'tools'
          BRACKET_OPEN
          toolEntry+
          BRACKET_CLOSE;

toolEntry : ID STRING;

/* Post */
post : 'post'
         BRACKET_OPEN
         postEntry+
         BRACKET_CLOSE;

postEntry : ID stepBlock;

/* When */
when : 'when'
         BRACKET_OPEN
         whenEntry+
         BRACKET_CLOSE;

whenEntry : ID (stepNamedParamExpr |
                (BRACKET_OPEN
                 whenEntry+
                 BRACKET_CLOSE));

/* Steps */
steps : 'steps' stepBlock;

stepExpr : (methodExpr | (methodExpr stepBlock) | (ID stepBlock));

stepBlock : BRACKET_OPEN stepExpr+ BRACKET_CLOSE;

/* Input */
input : 'input'
          BRACKET_OPEN
          mapKeyValuePair+
          BRACKET_CLOSE;

/* Failfast */
failfast: 'failFast' BOOLEAN;

/* Stages */
stages : 'stages'
           BRACKET_OPEN
           stage+
           BRACKET_CLOSE;

/* Parallel */
parallel : 'parallel'
             BRACKET_OPEN
             stage+
             BRACKET_CLOSE;

/* Stage */
stage : 'stage' LEFT_PAREN STRING RIGHT_PAREN
          BRACKET_OPEN
          (agent |
            when |
            environment |
            tools |
            post |
            failfast |
            options |
            input |
            (steps | parallel | stages))+
          BRACKET_CLOSE;

/* Options */
options : 'options'
            BRACKET_OPEN
            methodExpr+
            BRACKET_CLOSE;

/* Triggers */
triggers : 'triggers'
             BRACKET_OPEN
             methodExpr+
             BRACKET_CLOSE;

/* Parameters */
parameters : 'parameters'
               BRACKET_OPEN
               methodExpr+
               BRACKET_CLOSE;

/* Agent */
agent : 'agent' (('any' | 'none') |
                 (BRACKET_OPEN
                  ID mapValue
                  BRACKET_CLOSE));

/* Libraries */
libExpr : 'lib' LEFT_PAREN STRING RIGHT_PAREN;

libraries : 'libraries'
              BRACKET_OPEN
              libExpr+
              BRACKET_CLOSE;

pipeline : 'pipeline'
             BRACKET_OPEN
             (agent |
               stages |
               libraries |
               triggers |
               parameters |
               options |
               environment |
               tools |
               post)+
             BRACKET_CLOSE
             EOF;
