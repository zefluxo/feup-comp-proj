grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

@lexer::members {
    public static final int WHITESPACE = 1;
    public static final int COMMENTS = 2;
}


INTEGER : ( [0] | [1-9]([0-9])* ) ;
ID : [a-zA-Z_$][a-zA-Z_$0-9]* ;

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
    : type varName=ID ';'
    ;

argumentDeclaration
    : type varName=ID
    ;

methodDeclaration
    : ('public')? type methodName=ID '(' (argumentDeclaration (',' argumentDeclaration)*)? ')' '{'((varDeclaration) | (statement))* 'return' expression ';' '}' #FuncDeclaration
    | ('public')? 'static' 'void' 'main' '(' type '[' ']' argsName=ID ')' '{' ((varDeclaration) | (statement))* '}' #MainFuncDeclaration
    ;

type
    : 'int' '[' ']' #IntArray
    | 'boolean' #BoolType
    | 'int' #IntType
    | typeName=ID (isArray='[' ']')? #TypeName
    ;

statement
    : '{' (statement)* '}' #MethodStatement
    | 'if' '(' expression ')' statement ('else' statement)? #IfStatement
    | 'while' '(' expression ')' statement #WhileStatement
    | expression ';' #Expr
    | varName=ID '=' expression ';' #Assignment
    | varName=ID '[' expression ']' '=' expression ';' #ArrayAssignment
    ;

expression
    : '(' expression ')' #ParenthesesOp
    | expression '[' expression ']' #ArrayAccess
    | expression '.' methodName=ID '(' (expression (',' expression)*)? ')' #ClassMethodCall
    | expression '.' 'length' #LengthOp
    | '!' expression #NotOp
    | 'new' objName=ID '(' ')' #ObjectDeclaration
    | 'new' 'int' '[' expression ']' #ArrayDeclaration
    | expression op=('*' | '/') expression #BinaryOp
    | expression op=('+' | '-') expression #BinaryOp
    | expression op='<' expression #BinaryOp
    | expression op='&&' expression #BinaryOp
    | val=INTEGER #Integer
    | 'true' #Boolean
    | 'false' #Boolean
    | varName=ID #Identifier
    | 'this' #ObjIdentifier
    ;

COMMENT
    : '/*' .*? '*/' -> channel(COMMENTS)
    ;

LINE_COMMENT
    : '//' ~[\r\n]* -> channel(COMMENTS)
    ;