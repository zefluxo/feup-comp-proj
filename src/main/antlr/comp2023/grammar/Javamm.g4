grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INTEGER : [0-9]+ ;
ID : [a-zA-Z_][a-zA-Z_0-9]* ;

WS : [ \t\n\r\f]+ -> skip ;

program
    : (importDeclaration)* classDeclaration EOF
    ;

moduleDeclaration
    : modName=ID
    ;

importDeclaration
    : 'import' moduleDeclaration ('.' moduleDeclaration)* ';'
    ;

classDeclaration
    : 'class' className=ID ('extends' superClassName=ID)? '{' (varDeclaration)* (methodDeclaration)* '}'
    ;

varDeclaration
    : varType=type varName=ID ';'
    ;

argumentDeclaration
    : argType=type varName=ID
    ;

methodDeclaration
    : ('public')? retType=type methodName=ID '(' (argumentDeclaration (',' argumentDeclaration)*)? ')' '{' (varDeclaration)* (statement)* 'return' expression ';' '}' #FuncDeclaration
    | ('public')? 'static' 'void' 'main' '(' type '[' ']' ID ')' '{' (varDeclaration)* '}' #MainFuncDeclaration
    ;

type
    : 'int' '[' ']' #IntArray
    | 'boolean' #BoolType
    | 'int' #IntType
    | typeName=ID #TypeName
    ;

statement
    : '{' (statement)* '}' #MethodStatement
    | 'if' '(' expression ')' statement 'else' statement #IfStatement
    | 'while' '(' expression ')' statement #WhileStatement
    | expression ';' #Expr
    | varName=ID '=' value=expression ';' #Assignment
    | varName=ID '[' arrSize=expression ']' '=' array=expression ';' #ArrayAssignment
    ;

expression
    : arg1=expression op=('*' | '/') arg2=expression #BinaryOp
    | arg1=expression op=('+' | '-') arg2=expression #BinaryOp
    | arg1=expression op='<' arg2=expression #BinaryOp
    | arg1=expression op='&&' arg2=expression #BinaryOp
    | arg1=expression op='||' arg2=expression #BinaryOp
    | arg1=expression op='==' arg2=expression #BinaryOp
    | arr=expression '[' index=expression ']' #ArrayAccess
    | expression '.' 'length' #LengthOp
    | expression '.' methodName=ID '(' (expression (',' expression)*)? ')' #ClassMethodCall
    | 'new' 'int' '[' expression ']' #ArrayDeclaration
    | 'new' objName=ID '(' ')' #ObjectDeclaration
    | '!' expression #NotOp
    | '(' expression ')' #ParenthesesOp
    | val=INTEGER #Integer
    | 'true' #Boolean
    | 'false' #Boolean
    | varName=ID #Identifier
    | 'this' #ObjIdentifier
    ;
