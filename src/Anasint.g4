//Analizador sintáctico
parser grammar Anasint;

options{
    tokenVocab=Analex;
}

programa: variables subprogramas instrucciones EOF;

variables: VARIABLES (decl_vars)*;

decl_vars: vars DP tipo PyC;

vars: IDENT (COMA vars)?;

variable: IDENT;

tipo: NUM | LOG | SEQ PA tipo PC | SEQ;

subprogramas: SUBPROGRAMAS (subprograma)*;

subprograma: funcion | procedimiento;

funcion: FUNCION IDENT PA param PC DEV PA param PC variables instrucciones FFUNCION;

procedimiento: PROCEDIMIENTO IDENT PA PC FPROCEDIMIENTO
    | PROCEDIMIENTO IDENT PA param PC variables instrucciones FPROCEDIMIENTO
    ;

param: tipo variable
    | tipo variable (COMA tipo variable)*
    ;

instrucciones: INSTRUCCIONES (instruccion)*;

instruccion: asignacion | condicional | iteracion | ruptura | llamada_funcion | llamada_procedimiento | mostrar | aserto | dev;

asignacion: vars ASIG expr PyC
    | vars ASIG llamada_funcion PyC
    | vars ASIG llamada_procedimiento PyC
    ;

condicional: SI PA condicion PC ENTONCES (bloque)* (alternativa)? FSI instruccion?
    | PA condicion PC
    ;

alternativa : SINO bloque;

condicion: expr (MAYOR | MAYORIGUAL | MENOR | MENORIGUAL | IGUAL | DISTINTO) expr
    | expr (MAYOR | MAYORIGUAL | MENOR | MENORIGUAL | IGUAL | DISTINTO) instruccion
    ;

bloque: instruccion
    | (LLA llamada_funcion LLC)? instruccion
    ;

iteracion: MIENTRAS PA condicion PC HACER bloque FMIENTRAS;

llamada_funcion: IDENT PA expr PC
    | IDENT DP IDENT PA (vars)
    ;

llamada_procedimiento: IDENT PA expr PC;

mostrar: MOSTRAR PA variable PC;

aserto: LLA cuantificador LLC;

ruptura: RUPTURA PyC;

dev: DEV vars PyC
    | DEV expr PyC
    ;

expr: expr_num | expr_bool | expr_sec | vars;


expr_num: expr_num1 MULTI expr_num
    | expr_num1
    ;

expr_num1: expr_num2 (MAS | MENOS) expr_num1
    | expr_num2
    ;

expr_num2: NUMERO
    | vars
    | PA expr_num PC;

expr_bool: expr_bool1 Y expr_bool
    | expr_bool1 O expr_bool
    | expr_bool1
    ;

expr_bool1: NO expr_bool
    | expr_bool2
    ;

expr_bool2: CIERTO
    | FALSO
    ;

expr_sec: sec_vacia
    | CA seq_elems CC
    | IDENT CA IDENT CC
    ;

sec_vacia: CA CC;

seq_elems: expr_num (COMA seq_elems)?
    | expr_bool (COMA seq_elems)?
    | expr_sec (COMA seq_elems)?
    ;

cuantificador: PARATODO variable DP expr
    | EXISTE variable DP expr
    ;