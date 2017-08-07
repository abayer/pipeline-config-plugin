/*
 * The MIT License
 *
 * Copyright (c) 2017, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.pipeline.modeldefinition.parser

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FirstParam
import hudson.model.Describable
import hudson.model.Descriptor
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.DynamicVariable
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.*
import org.codehaus.groovy.syntax.Types
import org.jenkinsci.plugins.pipeline.modeldefinition.DescriptorLookupCache
import org.jenkinsci.plugins.pipeline.modeldefinition.Utils
import org.jenkinsci.plugins.pipeline.modeldefinition.ast.*
import org.jenkinsci.plugins.pipeline.modeldefinition.when.DeclarativeStageConditional
import org.jenkinsci.plugins.pipeline.modeldefinition.when.DeclarativeStageConditionalDescriptor
import org.jenkinsci.plugins.structs.SymbolLookup
import org.jenkinsci.plugins.structs.describable.UninstantiatedDescribable
import org.jenkinsci.plugins.workflow.steps.StepDescriptor

import javax.annotation.CheckForNull
import javax.annotation.Nonnull

import static org.codehaus.groovy.ast.tools.GeneralUtils.*

/**
 * Misc. utilities used across both {@link ModelParser} and {@link RuntimeASTTransformer}.
 */
@SuppressFBWarnings(value="SE_NO_SERIALVERSIONID")
class ASTParserUtils {

    /**
     * Attempts to match a method call of the form {@code foo(...)} and
     * return 'foo' as a string.
     */
    static @CheckForNull String matchMethodName(MethodCallExpression exp) {
        def lhs = exp.objectExpression;
        if (lhs instanceof VariableExpression) {
            if (lhs.name.equals("this")) {
                return exp.methodAsString; // getMethodAsString() returns null if the method isn't a constant
            }
        }
        return null;
    }

    // TODO: Remove or otherwise cleanup so that it's not always firing!
    static String printer(String s, int ind) {
        return "${' ' * ind * 2}${s}"
    }

    // TODO: Remove or otherwise cleanup so that it's not always firing!
    static String prettyPrint(ASTNode n, int ind = -1) {
        List<String> s = []
        
        ind++
        if (n instanceof ReturnStatement) {
            s << printer("- return:", ind)
            s << prettyPrint(n.expression, ind)
        } else if (n instanceof ArgumentListExpression) {
            s << printer("- args:", ind)
            n.expressions.each { s << prettyPrint(it, ind) }
        } else if (n instanceof ClosureExpression) {
            s << printer("- closure:", ind)
            s << prettyPrint(n.code, ind)
        } else if (n instanceof BlockStatement) {
            s << printer("- block", ind)
            n.statements.each {
                s << prettyPrint(it, ind)
            }
        } else if (n instanceof ConstructorCallExpression) {
            s << printer("- constructor of ${n.type.typeClass}:", ind)
            n.arguments.each {
                s << prettyPrint(it, ind)
            }
        } else if (n instanceof MapExpression) {
            s << printer("- map:", ind)
            n.mapEntryExpressions.each {
                s << prettyPrint(it, ind)
            }
        } else if (n instanceof ListExpression) {
            s << printer("- list:", ind)
            n.expressions.each {
                s << prettyPrint(it, ind)
            }
        } else if (n instanceof StaticMethodCallExpression) {
            s << printer("- static method '${n.method}':", ind)
            s << prettyPrint(n.receiver, ind)
            s << prettyPrint(n.arguments, ind)
        } else if (n instanceof MethodCallExpression) {
            s << printer("- method '${n.method}':", ind)
            s << prettyPrint(n.receiver, ind)
            s << prettyPrint(n.arguments, ind)
        }else if (n instanceof MapEntryExpression) {
            s << prettyPrint(n.keyExpression, ind)
            s << prettyPrint(n.valueExpression, ind)
        } else if (n instanceof ExpressionStatement) {
            s << prettyPrint(n.expression, ind)
        } else if (n instanceof GStringExpression) {
            s << printer("- gstring:", ind)
            ind++
            s << printer("- strings:", ind)
            n.strings.each { s << prettyPrint(it, ind) }
            s << printer("- values:", ind)
            n.values.each { s << prettyPrint(it, ind) }
        } else if (n instanceof PropertyExpression) {
            s << printer("- property:", ind)
            s << prettyPrint(n.objectExpression, ind)
            s << prettyPrint(n.property, ind)
        } else if (n instanceof ModuleNode) {
            s << printer("- module:", ind)
            ind++
            s << printer("- methods:", ind)
            n.methods.each { s << prettyPrint(it, ind) }
            s << printer("- statements:", ind)
            n.statementBlock.statements.each { s << prettyPrint(it, ind) }
        } else if (n instanceof MethodNode) {
            s << printer("- methodNode:", ind)
            s << prettyPrint(n.code, ind)
        } else if (n instanceof ThrowStatement) {
            s << printer("- throw:", ind)
            s << prettyPrint(n.expression, ind)
        } else if (n instanceof CastExpression) {
            s << printer("- cast:", ind)
            s << prettyPrint(n.expression, ind)
        } else if (n instanceof VariableExpression) {
            s << printer("- var:", ind)
            s << printer("  - name: ${n.name}", ind)
            s << printer("  - accessedVariable: ${n.accessedVariable}", ind)
        } else if (n instanceof PrefixExpression) {
            s << printer("- prefix (${n.operation}):", ind)
            s << prettyPrint(n.expression, ind)
        } else if (n instanceof PostfixExpression) {
            s << printer("- postfix (${n.operation}):", ind)
            s << prettyPrint(n.expression, ind)
        } else if (n instanceof ElvisOperatorExpression) {
            s << printer("- elvis:", ind)
            s << prettyPrint(n.trueExpression, ind)
            s << prettyPrint(n.falseExpression, ind)
        } else {
            s << printer("- ${n}", ind)
        }

        return s.join("\n")
    }

    /**
     * Splits out and returns the {@link BlockStatementMatch} corresponding to  the given {@link MethodCallExpression}.
     */
    @CheckForNull
    static BlockStatementMatch blockStatementFromExpression(@Nonnull MethodCallExpression exp) {
        def methodName = matchMethodName(exp);
        def args = (TupleExpression)exp.arguments;
        int sz = args.expressions.size();
        if (sz>0 && methodName!=null) {
            def last = args.getExpression(sz - 1);
            if (last instanceof ClosureExpression) {
                return new BlockStatementMatch(exp,methodName,last);
            }
        }

        return null
    }

    /**
     * Normalizes a statement to a block of statement by creating a wrapper if need be.
     */
    static BlockStatement asBlock(Statement st) {
        if (st instanceof BlockStatement) {
            return st;
        } else {
            def bs = new BlockStatement();
            bs.addStatement(st);
            return bs;
        }
    }

    /**
     * Attempts to match a given statement as a method call, or return null
     */
    static @CheckForNull MethodCallExpression matchMethodCall(Statement st) {
        if (st instanceof ExpressionStatement) {
            def exp = st.expression;
            if (exp instanceof MethodCallExpression) {
                return exp;
            }
        }
        return null;
    }

    /**
     * Takes a statement and iterates over its contents - if the statement is not a {@link BlockStatement}, it gets
     * wrapped in a new block to simplify iteration.
     */
    static <T> List<T> eachStatement(Statement st, @ClosureParams(FirstParam.class) Closure<T> c) {
        return asBlock(st).statements.collect(c)
    }

    /**
     * Attempts to match AST node as {@link BlockStatementMatch} or
     * return null.
     */
    static @CheckForNull BlockStatementMatch matchBlockStatement(Statement st) {
        def whole = matchMethodCall(st);
        if (whole!=null) {
            return blockStatementFromExpression(whole)
        }

        return null;
    }

    /**
     * Takes a list of {@link ModelASTElement}s corresponding to {@link Describable}s (such as {@link JobProperty}s, etc),
     * and transforms their Groovy AST nodes into AST from {@link #methodCallToDescribable(MethodCallExpression)}.
     */
    @Nonnull
    static Expression transformListOfDescribables(@CheckForNull List<ModelASTElement> children) {
        ListExpression descList = new ListExpression()

        children?.each { d ->
            if (d.sourceLocation instanceof Statement) {
                MethodCallExpression m = matchMethodCall((Statement) d.sourceLocation)
                if (m != null) {
                    descList.addExpression(methodCallToDescribable(m))
                } else {
                    throw new IllegalArgumentException("Expected a method call expression but received ${d.sourceLocation}")
                }
            } else {
                throw new IllegalArgumentException("Expected a statement but received ${d.sourceLocation}")
            }
        }

        return descList
    }

    /**
     * Transforms a container for describables, such as {@link Triggers}, into AST for instantation.
     * @param original A {@link ModelASTElement} such as {@link ModelASTTriggers} or {@link ModelASTBuildParameters}
     * @param children The children for the original element - passed as a separate argument since the getter will
     * be different.
     * @param containerClass The class we will be instantiating, i.e., {@link Parameters} or {@link Triggers}.
     * @return The AST for instantiating the container and its contents.
     */
    static Expression transformDescribableContainer(@CheckForNull ModelASTElement original,
                                                 @CheckForNull List<ModelASTElement> children,
                                                 @Nonnull Class containerClass) {
        if (isGroovyAST(original) && !children?.isEmpty()) {
            return ctorX(ClassHelper.make(containerClass), args(transformListOfDescribables(children)))
        }
        return constX(null)
    }

    /**
     * Transform a when condition, and its children if any exist, into instantiation AST.
     */
    static Expression transformWhenContentToRuntimeAST(@CheckForNull ModelASTWhenContent original) {
        if (original instanceof ModelASTElement && isGroovyAST((ModelASTElement)original)) {
            DeclarativeStageConditionalDescriptor parentDesc =
                (DeclarativeStageConditionalDescriptor) SymbolLookup.get().findDescriptor(
                    DeclarativeStageConditional.class, original.name)
            if (original instanceof ModelASTWhenCondition) {
                ModelASTWhenCondition cond = (ModelASTWhenCondition) original
                if (cond.getSourceLocation() != null && cond.getSourceLocation() instanceof Statement) {
                    MethodCallExpression methCall = matchMethodCall((Statement) cond.getSourceLocation())

                    if (methCall != null) {
                        if (cond.children.isEmpty()) {
                            return methodCallToDescribable(methCall)
                        } else {
                            MapExpression argMap = new MapExpression()
                            if (parentDesc.allowedChildrenCount == 1) {
                                argMap.addMapEntryExpression(constX(UninstantiatedDescribable.ANONYMOUS_KEY),
                                    transformWhenContentToRuntimeAST(cond.children.first()))
                            } else {
                                argMap.addMapEntryExpression(constX(UninstantiatedDescribable.ANONYMOUS_KEY),
                                    new ListExpression(cond.children.collect { transformWhenContentToRuntimeAST(it) }))
                            }
                            return callX(ClassHelper.make(Utils.class),
                                "instantiateDescribable",
                                args(
                                    classX(parentDesc.clazz),
                                    argMap
                                ))
                        }
                    }
                }
            } else if (original instanceof ModelASTWhenExpression) {
                return parentDesc.transformToRuntimeAST(original)
            }
        }
        return constX(null)
    }

    /**
     * Calls {@link #translateEnvironmentValue} and calls the result. This won't be evaluated until the parent closure
     * itself is evaluated, as this is only called for children. May be null if the translation is null.
     */
    @CheckForNull
    static Expression translateEnvironmentValueAndCall(String targetVar, Expression expr, Set<String> keys) {
        Expression translated = translateEnvironmentValue(targetVar, expr, keys)
        if (translated != null) {
            if (translated instanceof ClosureExpression) {
                return callX(translated, "call")
            } else {
                return translated
            }
        } else {
            return null
        }
    }

    /**
     * Recursively translate any nested expressions within the given expression, setting any attempt to reference an
     * environment variable we've defined to instead lazily call the closure defined in the resolver for that value.
     */
    @CheckForNull
    static Expression translateEnvironmentValue(String targetVar, Expression expr, Set<String> keys) {
        Expression body = null

        if (expr instanceof ConstantExpression) {
            // If the expression is a constant, like 1, "foo", etc, just use that.
            return expr
        } else if (expr instanceof ClassExpression) {
            // If the expression is a class, just use that.
            return expr
        } else if (expr instanceof EmptyExpression) {
            // If it's an empty expression, just use that
            return expr
        } else if (expr instanceof BinaryExpression &&
            ((BinaryExpression) expr).getOperation().getType() == Types.PLUS) {
            // If the expression is a binary expression of plusses, translate its components.
            BinaryExpression binExpr = (BinaryExpression) expr
            return plusX(
                translateEnvironmentValueAndCall(targetVar, binExpr.leftExpression, keys),
                translateEnvironmentValueAndCall(targetVar, binExpr.rightExpression, keys)
            )
        } else if (expr instanceof GStringExpression) {
            // If the expression is a GString, translate its values.
            GStringExpression gStrExpr = (GStringExpression) expr
            return new GStringExpression(gStrExpr.text,
                gStrExpr.strings,
                gStrExpr.values.collect { translateEnvironmentValueAndCall(targetVar, it, keys) }
            )
        } else if (expr instanceof PropertyExpression) {
            PropertyExpression propExpr = (PropertyExpression) expr
            if (propExpr.objectExpression instanceof VariableExpression &&
                ((VariableExpression) propExpr.objectExpression).name == "env" &&
                keys.contains(propExpr.propertyAsString)) {
                // If the property this expression refers to is env.whatever, replace with the env getter.
                body = environmentValueGetterCall(propExpr.propertyAsString)
            } else {
                // Otherwise, if the property is still on a variable, translate everything
                return propX(
                    translateEnvironmentValueAndCall(targetVar, propExpr.objectExpression, keys),
                    translateEnvironmentValueAndCall(targetVar, propExpr.property, keys)
                )
            }
        } else if (expr instanceof MethodCallExpression) {
            // If the expression is a method call, translate its arguments.
            MethodCallExpression mce = (MethodCallExpression) expr
            return callX(
                translateEnvironmentValueAndCall(targetVar, mce.objectExpression, keys),
                mce.method,
                args(mce.arguments.collect { a ->
                    translateEnvironmentValueAndCall(targetVar, a, keys)
                })
            )
        } else if (expr instanceof VariableExpression) {
            VariableExpression ve = (VariableExpression) expr
            if (keys.contains(ve.name) && ve.name != targetVar) {
                // If the variable name is one we know is an environment variable, use the env getter, unless the reference
                // is to the same variable we're setting!
                body = environmentValueGetterCall(ve.name)
            } else if (ve.name == "this" || !(ve.accessedVariable instanceof DynamicVariable)) {
                // If the variable is this, or if this is a real variable, not a dynamic variable, just use it.
                return ve
            } else {
                // Otherwise, fall back to getScriptPropOrParam, which will first try script.getProperty(name), then
                // script.getProperty('params').get(name).
                body = callX(
                    varX("this"),
                    constX("getScriptPropOrParam"),
                    args(constX(ve.name))
                )
            }
        } else if (expr instanceof ElvisOperatorExpression) {
            // If the expression is ?:, translate its components.
            ElvisOperatorExpression elvis = (ElvisOperatorExpression) expr
            return new ElvisOperatorExpression(
                translateEnvironmentValueAndCall(targetVar, elvis.trueExpression, keys),
                translateEnvironmentValueAndCall(targetVar, elvis.falseExpression, keys)
            )
        } else if (expr instanceof ClosureExpression) {
            // If the expression is a closure, translate its statements.
            ClosureExpression cl = (ClosureExpression) expr
            BlockStatement closureBlock = block()
            eachStatement(cl.code) { s ->
                closureBlock.addStatement(translateClosureStatement(targetVar, s, keys))
            }
            return closureX(
                cl.parameters,
                closureBlock
            )
        } else if (expr instanceof ArrayExpression) {
            // If the expression is an array, transform its contents.
            ArrayExpression a = (ArrayExpression) expr
            List<Expression> sizes = a.sizeExpression?.collect {
                translateEnvironmentValueAndCall(targetVar, it, keys)
            }
            List<Expression> expressions = a.expressions?.collect {
                translateEnvironmentValueAndCall(targetVar, it, keys)
            }

            return new ArrayExpression(a.elementType, expressions, sizes)
        } else if (expr instanceof ListExpression) {
            // If the expression is a list, transform its contents
            ListExpression l = (ListExpression) expr
            List<Expression> expressions = l.expressions?.collect {
                translateEnvironmentValueAndCall(targetVar, it, keys)
            }

            return new ListExpression(expressions)
        } else if (expr instanceof MapExpression) {
            // If the expression is a map, translate its entries.
            MapExpression m = (MapExpression) expr
            List<MapEntryExpression> entries = m.mapEntryExpressions?.collect {
                translateEnvironmentValueAndCall(targetVar, it, keys)
            }

            return new MapExpression(entries)
        } else if (expr instanceof MapEntryExpression) {
            // If the expression is a map entry, translate its key and value
            MapEntryExpression m = (MapEntryExpression) expr

            return new MapEntryExpression(translateEnvironmentValueAndCall(targetVar, m.keyExpression, keys),
                translateEnvironmentValueAndCall(targetVar, m.valueExpression, keys))
        } else if (expr instanceof BitwiseNegationExpression) {
            // Translate the nested expression - note, no test coverage due to bitwiseNegate not being whitelisted
            return new BitwiseNegationExpression(translateEnvironmentValueAndCall(targetVar, expr.expression, keys))
        } else if (expr instanceof BooleanExpression) {
            // Translate the nested expression
            return new BooleanExpression(translateEnvironmentValueAndCall(targetVar, expr.expression, keys))
        } else if (expr instanceof CastExpression) {
            // Translate the nested expression
            Expression transformed = translateEnvironmentValueAndCall(targetVar, expr.expression, keys)
            def cast = new CastExpression(expr.type, transformed, expr.ignoringAutoboxing)
            return cast
        } else if (expr instanceof ConstructorCallExpression) {
            // Translate the arguments
            return ctorX(expr.type, translateEnvironmentValueAndCall(targetVar, expr.arguments, keys))
        } else if (expr instanceof MethodPointerExpression) {
            // Translate the nested expression and method
            return new MethodPointerExpression(translateEnvironmentValueAndCall(targetVar, expr.expression, keys),
                translateEnvironmentValueAndCall(targetVar, expr.methodName, keys))
        } else if (expr instanceof PostfixExpression) {
            // Translate the nested expression
            return new PostfixExpression(translateEnvironmentValueAndCall(targetVar, expr.expression, keys), expr.operation)
        } else if (expr instanceof PrefixExpression) {
            // Translate the nested expression
            return new PrefixExpression(expr.operation, translateEnvironmentValueAndCall(targetVar, expr.expression, keys))
        } else if (expr instanceof RangeExpression) {
            // Translate the from and to
            return new RangeExpression(translateEnvironmentValueAndCall(targetVar, expr.from, keys),
                translateEnvironmentValueAndCall(targetVar, expr.to, keys),
                expr.inclusive)
        } else if (expr instanceof TernaryExpression) {
            // Translate the true, false and boolean expressions
            TernaryExpression t = (TernaryExpression) expr
            return ternaryX(translateEnvironmentValueAndCall(targetVar, t.booleanExpression, keys),
                translateEnvironmentValueAndCall(targetVar, t.trueExpression, keys),
                translateEnvironmentValueAndCall(targetVar, t.falseExpression, keys))
        } else if (expr instanceof ArgumentListExpression) {
            // Translate the contents
            List<Expression> expressions = expr.expressions.collect {
                translateEnvironmentValueAndCall(targetVar, it, keys)
            }
            return args(expressions)
        } else if (expr instanceof TupleExpression) {
            // Translate the contents
            List<Expression> expressions = expr.expressions.collect {
                translateEnvironmentValueAndCall(targetVar, it, keys)
            }
            return new TupleExpression(expressions)
        } else if (expr instanceof UnaryMinusExpression) {
            // Translate the nested expression - unary ops are also not whitelisted and so aren't tested
            return new UnaryMinusExpression(translateEnvironmentValueAndCall(targetVar, expr.expression, keys))
        } else if (expr instanceof UnaryPlusExpression) {
            // Translate the nested expression
            return new UnaryPlusExpression(translateEnvironmentValueAndCall(targetVar, expr.expression, keys))
        } else if (expr instanceof BinaryExpression) {
            // Translate the component expressions
            return new BinaryExpression(translateEnvironmentValueAndCall(targetVar, expr.leftExpression, keys),
                expr.operation,
                translateEnvironmentValueAndCall(targetVar, expr.rightExpression, keys))
        } else {
            throw new IllegalArgumentException("Got an unexpected " + expr.getClass() + " in environment, please report " +
                "at issues.jenkins-ci.org")
        }

        if (body != null) {
            return closureX(
                block(
                    returnS(
                        body
                    )
                )
            )
        }

        return null
    }

    static Statement translateClosureStatement(String targetVar, Statement s, Set<String> keys) {
        if (s == null) {
            return null
        } else if (s instanceof ExpressionStatement) {
            // Translate the nested expression
            return stmt(translateEnvironmentValueAndCall(targetVar, s.expression, keys))
        } else if (s instanceof ReturnStatement) {
            // Translate the nested expression
            return stmt(translateEnvironmentValueAndCall(targetVar, s.expression, keys))
        } else if (s instanceof IfStatement) {
            // Translate the boolean expression, the if block and the else block
            return ifElseS(translateEnvironmentValueAndCall(targetVar, s.booleanExpression, keys),
                translateClosureStatement(targetVar, s.ifBlock, keys),
                translateClosureStatement(targetVar, s.elseBlock, keys))
        } else if (s instanceof ForStatement) {
            // Translate the collection and loop block
            return new ForStatement(s.variable,
                translateEnvironmentValueAndCall(targetVar, s.collectionExpression, keys),
                translateClosureStatement(targetVar, s.loopBlock, keys))
        } else if (s instanceof WhileStatement) {
            // Translate the boolean expression's contents and the loop block
            BooleanExpression newBool = new BooleanExpression(translateEnvironmentValueAndCall(targetVar,
                s.booleanExpression?.expression, keys))
            return new WhileStatement(newBool, translateClosureStatement(targetVar, s.loopBlock, keys))
        } else if (s instanceof TryCatchStatement) {
            // Translate the try and finally statements, as well as any catch statements
            TryCatchStatement t = (TryCatchStatement) s
            TryCatchStatement newTry = new TryCatchStatement(translateClosureStatement(targetVar, t.tryStatement, keys),
                translateClosureStatement(targetVar, t.finallyStatement, keys))
            t.catchStatements.each { c ->
                newTry.addCatch((CatchStatement)translateClosureStatement(targetVar, c, keys))
            }
            return newTry
        } else if (s instanceof CatchStatement) {
            // Translate the nested statement
            return catchS(s.variable, translateClosureStatement(targetVar, s.code, keys))
        } else {
            throw new IllegalArgumentException("Got an unexpected " + s.getClass() + " in environment, please " +
                "report at issues.jenkins-ci.org")
        }
    }

    /**
     * Generates the method call for fetching the closure for a given environment key and calling it.
     */
    static MethodCallExpression environmentValueGetterCall(String name) {
        return callX(callThisX("getClosure", constX(name)), "call")
    }

    /**
     * Transforms the AST for a "mapped closure" - i.e., a closure of "foo 'bar'" method calls - into a
     * {@link MapExpression}. Recurses for nested "mapped closures" as well.
     * @param original a possibly null {@link ClosureExpression} to transform
     * @return A {@link MapExpression}, or null if the original expression was null.
     */
    @CheckForNull
    static Expression recurseAndTransformMappedClosure(@CheckForNull ClosureExpression original) {
        if (original != null) {
            MapExpression mappedClosure = new MapExpression()
            eachStatement(original.code) { s ->
                MethodCallExpression mce = matchMethodCall(s)
                if (mce != null) {
                    List<Expression> args = methodCallArgs(mce)
                    if (args.size() == 1) {
                        Expression singleArg = args.get(0)
                        if (singleArg instanceof ClosureExpression) {
                            mappedClosure.addMapEntryExpression(mce.method, recurseAndTransformMappedClosure(singleArg))
                        } else if (singleArg instanceof ConstantExpression) {
                            mappedClosure.addMapEntryExpression(mce.method, singleArg)
                        } else {
                            // Lazy evaluation of non-constants
                            mappedClosure.addMapEntryExpression(mce.method, translateEnvironmentValue(mce.methodAsString,
                                singleArg, [] as Set))
                        }
                    }
                }
            }
            return mappedClosure
        }

        return null
    }

    /**
     * Takes a list of expressions used as arguments that could contain describables, and creates a MapExpression
     * suitable for DescribableModel.instantiate.
     * @param args A list of arguments
     * @return A MapExpression
     */
    @CheckForNull
    static Expression argsMap(List<Expression> args) {
        MapExpression tmpMap = new MapExpression()
        args.each { singleArg ->
            if (singleArg instanceof MapExpression) {
                singleArg.mapEntryExpressions.each { entry ->
                    if (entry.valueExpression instanceof MethodCallExpression) {
                        MethodCallExpression m = (MethodCallExpression) entry.valueExpression
                        tmpMap.addMapEntryExpression(entry.keyExpression, methodCallToDescribable(m))
                    } else {
                        tmpMap.addMapEntryExpression(entry)
                    }
                }
            } else {
                if (singleArg instanceof MethodCallExpression) {
                    tmpMap.addMapEntryExpression(constX(UninstantiatedDescribable.ANONYMOUS_KEY),
                        methodCallToDescribable(singleArg))
                } else {
                    tmpMap.addMapEntryExpression(constX(UninstantiatedDescribable.ANONYMOUS_KEY), singleArg)
                }
            }
        }

        return tmpMap
    }

    /**
     * A shortcut for taking the Expression at MethodCallExpression.arguments and turning it into a list of Expressions.
     * @param expr A method call
     * @return A possibly empty list of expressions
     */
    @Nonnull
    static List<Expression> methodCallArgs(@Nonnull MethodCallExpression expr) {
        return ((TupleExpression) expr.arguments).expressions
    }

    /**
     * Transforms a {@link MethodCallExpression} into either a map of name and arguments for steps, or a call to
     * {@link Utils#instantiateDescribable(Class,Map)} that can be invoked at runtime to actually instantiated.
     * @param expr A method call.
     * @return The appropriate transformation, or the original expression if it didn't correspond to a Describable.
     */
    @CheckForNull
    static Expression methodCallToDescribable(MethodCallExpression expr) {
        def methodName = matchMethodName(expr)
        List<Expression> methArgs = methodCallArgs(expr)

        DescriptorLookupCache lookupCache = DescriptorLookupCache.getPublicCache()

        Descriptor<? extends Describable> funcDesc = lookupCache.lookupFunction(methodName)
        StepDescriptor stepDesc = lookupCache.lookupStepDescriptor(methodName)
        // This is the case where we've got a wrapper in options
        if (stepDesc != null || (funcDesc != null && !StepDescriptor.metaStepsOf(methodName).isEmpty())) {
            MapExpression m = new MapExpression()
            m.addMapEntryExpression(constX("name"), constX(methodName))
            m.addMapEntryExpression(constX("args"), argsMap(methArgs))
            return m
        } else if (funcDesc != null) {
            // Ok, now it's a non-executable descriptor. Phew.
            Class<? extends Describable> descType = funcDesc.clazz

            return callX(ClassHelper.make(Utils.class), "instantiateDescribable",
                args(classX(descType), argsMap(methArgs)))
        } else {
            // Not a describable at all!
            return expr
        }
    }

    /**
     * Determine whether this element can be used for Groovy AST transformation
     * @param original
     * @return True if the element isn't null, it has a source location, and that source location is an {@link ASTNode}
     */
    static boolean isGroovyAST(ModelASTElement original) {
        return original != null && original.sourceLocation != null && original.sourceLocation instanceof ASTNode
    }
}
