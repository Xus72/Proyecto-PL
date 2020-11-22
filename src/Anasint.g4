//Analizador sintáctico
parser grammar Anasint;

options{
    tokenVocab=Analex;
}

programa: PROGRAMA variables subprogramas instrucciones EOF;

variables: VARIABLES (decl_vars)*;

decl_vars: vars DP tipo PyC;

vars: IDENT (COMA vars)?;

tipo: NUM | LOG | SEQ PA tipo PC;

subprogramas: SUBPROGRAMAS (subprograma)*;

subprograma: funcion | procedimiento;

funcion: FUNCION IDENT cabecera cuerpo FFUNCION;

procedimiento: PROCEDIMIENTO IDENT cabecera cuerpo FPROCEDIMIENTO;

cabecera: PA (param)? PC (DEV)? PA param PC
    | PA (param)? PC
    ;
cuerpo: variables instrucciones;

param: tipo IDENT (COMA tipo IDENT)*;

instrucciones: INSTRUCCIONES (instruccion)*;

instruccion: asignacion | condicional | iteracion | ruptura | llamada_funcion | llamada_procedimiento | mostrar | avance | aserto | dev;

asignacion: vars ASIG expr (COMA expr)? PyC
    | vars ASIG llamada_funcion PyC
    | vars ASIG llamada_procedimiento PyC
    ;

condicional: SI PA condicion PC ENTONCES bloque (alternativa)? FSI;

alternativa : SINO bloque;

condicion: expr (instrLogica|expr_bool) expr;

bloque: (instruccion)*
    | (LLA avance LLC) (instruccion)*
    ;

instrLogica: MAYOR | MAYORIGUAL | MENOR | MENORIGUAL | IGUAL | DISTINTO;

iteracion: MIENTRAS PA condicion PC HACER bloque FMIENTRAS;

llamada_funcion: IDENT PA expr PC;

llamada_procedimiento: IDENT PA expr PC;

mostrar: MOSTRAR PA vars PC PyC;

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

formula: condicion instrLogica condicion
    | condicion expr_bool condicion
    ;
expr: expr_num | expr_bool | expr_sec | llamada_funcion | llamada_procedimiento;

expr_num: expr_num1 (MAS | MENOS) expr_num
    | expr_num1
    ;

expr_num1: expr_num2 POR expr_num1
    | expr_num2
    ;

expr_num2: NUMERO
    | MENOS IDENT
    | IDENT
    | PA expr_num PC;

expr_bool: (Y|O)
    | expr_bool1
    ;

expr_bool1: NO
    | expr_bool2
    ;

expr_bool2: CIERTO
    | FALSO
    | IDENT
    ;

expr_sec: CA CC
    | CA seq_elems CC
    | vars CA expr CC// s[j]
    ;

seq_elems: expr_num (COMA seq_elems)?
    | expr_bool (COMA seq_elems)?
    | expr_sec (COMA seq_elems)?
    | llamada_funcion (COMA seq_elems)?
    | llamada_procedimiento (COMA seq_elems)?
    ;

cuantificador: PARATODO PA vars DP rango COMA formula PC
    | EXISTE PA vars DP rango COMA formula PC
    ;