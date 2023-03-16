grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INTEGER : ( [0] | [1-9]([0-9])* ) ;
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
    : type varName=ID ';'
    ;

argumentDeclaration
    : type varName=ID
    ;

methodDeclaration
    : ('public')? retType=type methodName=ID '(' (argumentDeclaration (',' argumentDeclaration)*)? ')' '{' (varDeclaration)* (statement)* 'return' expression ';' '}' #FuncDeclaration
    | ('public')? 'static' 'void' 'main' '(' argsType=type '[' ']' argsName=ID ')' '{' (varDeclaration)* '}' #MainFuncDeclaration
    ;

type
    : 'int' '[' ']' #IntArray
    | 'boolean' #BoolType
    | 'int' #IntType
    | typeName=ID (isArray='[' ']')? #TypeName
    ;

statement
    : '{' (statement)* '}' #MethodStatement
    | 'if' '(' expression ')' statement 'else' statement #IfStatement
    | 'while' '(' expression ')' statement #WhileStatement
    | expression ';' #Expr
    | varName=ID '=' expression ';' #Assignment
    | varName=ID '[' expression ']' '=' expression ';' #ArrayAssignment
    ;

expression
    : expression op=('*' | '/') expression #BinaryOp
    | expression op=('+' | '-') expression #BinaryOp
    | expression op='<' expression #BinaryOp
    | expression op='&&' expression #BinaryOp
    | expression op='||' expression #BinaryOp
    | expression op='==' expression #BinaryOp
    | expression '[' expression ']' #ArrayAccess
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
