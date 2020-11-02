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

funcion: FUNCION IDENT cabecera cuerpo FFUNCION;

procedimiento: PROCEDIMIENTO IDENT PA PC variables instrucciones FPROCEDIMIENTO
    | PROCEDIMIENTO IDENT PA param PC variables instrucciones FPROCEDIMIENTO;

cabecera: PA (param)? PC (DEV)? PA param PC;

cuerpo: variables instrucciones;

param: tipo variable
    | tipo variable (COMA tipo variable)*
    ;

instrucciones: INSTRUCCIONES (instruccion)*;

instruccion: asignacion | condicional | iteracion | ruptura | llamada_funcion | llamada_procedimiento | mostrar | avance | aserto | dev;

asignacion: vars ASIG expr PyC
    | vars ASIG llamada_funcion PyC
    ;

condicional: SI PA condicion PC ENTONCES bloque (alternativa)? FSI;

alternativa : SINO bloque;

condicion: expr instrLogica expr;

bloque: (instruccion)*
    | (LLA avance LLC)? (instruccion)*
    | condicional
    ;

instrLogica: MAYOR | MAYORIGUAL | MENOR | MENORIGUAL | IGUAL | DISTINTO;

iteracion: MIENTRAS PA condicion PC HACER bloque FMIENTRAS;

llamada_funcion: IDENT PA expr PC;

llamada_procedimiento: IDENT PA expr PC;

mostrar: MOSTRAR PA vars PC (PyC)?;

avance: IDENT DP IDENT PA (vars) PC;

aserto: LLA cuantificador LLC
    | LLA expr_bool LLC
    ;
ruptura: RUPTURA PyC;

dev: DEV vars PyC
    | DEV expr_bool PyC
    ;

rango: CA vars CC
    | CA expr COMA expr CC
    ;

formula: condicion Y condicion;

expr: expr_num | expr_bool | expr_sec | llamada_funcion;

expr_num: expr_num1 (MAS | MENOS) expr_num
    | expr_num1
    ;

expr_num1: expr_num2 (POR) expr_num1
    | expr_num2
    ;

expr_num2: NUMERO
    | MENOS IDENT
    | IDENT
    | PA expr_num PC;

expr_bool: expr_bool1 (Y|O) expr_bool
    | expr_bool1
    ;

expr_bool1: NO expr_bool2
    | expr_bool2
    ;

expr_bool2: CIERTO
    | FALSO
    ;

expr_sec: sec_vacia
    | CA seq_elems CC
    | vars CA expr CC // s[j]
    ;

sec_vacia: CA CC;

seq_elems: expr_num (COMA seq_elems)?
    | expr_bool (COMA seq_elems)?
    | expr_sec (COMA seq_elems)?
    | llamada_funcion (COMA seq_elems)?
    ;

cuantificador: PARATODO PA vars DP rango COMA formula PC
    | EXISTE PA vars DP rango formula PC
    ;