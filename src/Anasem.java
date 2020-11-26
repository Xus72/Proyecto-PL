import org.antlr.v4.runtime.tree.ParseTree;

import java.util.*;

public class Anasem extends AnasintBaseVisitor<Object>{

    //Almacén de variables(var,tipo)
    //var=string
    //tipo=string
    public Stack<Map<String,String>> memoria = new Stack<>();
    public Map<String,String> memoriaSubPrograma = new HashMap<>();
    //Almacén de variables para dev
    public Stack<Map<String,String>> memdev = new Stack<>();

    //Contador para saber cuál es el segundo break en la ruptura
    Integer contador = 0;

    //recorremos variables, subprogramas, instrucciones
    @Override
    public Object visitPrograma(Anasint.ProgramaContext ctx) {
        Map<String,String> vars = visitVariables(ctx.variables());
        memoria.push(vars);
        visit(ctx.getChild(1));  //variables
        visit(ctx.getChild(2)); //subprogramas
        visit(ctx.getChild(3)); //instrucciones
        return null;
    }

    //guardo las variables en memoria
    //variables: VARIABLES (decl_vars)*;
    @Override
    public Map<String,String> visitVariables(Anasint.VariablesContext ctx) {
        Map<String,String> variables = new HashMap<>();
        for (int i = 0; i < ctx.decl_vars().size(); i++) {
            Map<String,String> decl = (Map<String, String>) visit(ctx.decl_vars(i));
            variables.putAll(decl);

        }
        return variables;
    }

    //decl_vars: vars DP tipo PyC;
    //Recibimos tipo y variable del tipo y las definimos.
    @Override
    public Object visitDecl_vars(Anasint.Decl_varsContext ctx) {
        Map<String, String> memoria = new HashMap<>();
        List<String> vars = (List<String>) visitVars(ctx.vars());
        String tipo = (String) visitTipo(ctx.tipo());
        for (int i = 0; i < vars.size(); i++) {
            if(memoria.containsValue(vars.get(i))){
                System.out.println("ERROR: Variable " + vars.get(i) + " ya declarada");
            }else {
                memoria.put(vars.get(i), tipo);
                System.out.println("Variable " + vars.get(i) + " declarada con tipo " + tipo);
            }
        }
        return memoria;
    }

    //tipo: t=NUM
    //  | t=LOG
    //  | t=SEQ PA NUM PC
    //  | t=SEQ PA LOG PC
    @Override
    public Object visitTipo(Anasint.TipoContext ctx) {
        String tipo = ctx.getText();
        if(ctx.getChildCount() > 1) {
            visit(ctx.tipo());
        }
        return tipo;
    }

    //vars: v=IDENT
    //  | v=IDENT COMA vars
    @Override
    public Object visitVars(Anasint.VarsContext ctx){
        List<String> vars = new ArrayList<>();
        String var = ctx.IDENT().getText();
        vars.add(var);
        if(ctx.getChildCount() > 1) {
            List<String> vars2 = (List<String>) visit(ctx.vars());
            vars.addAll(vars2);
        }

        return vars;
    }

    //visito las instrucciones que haya una a una
    //Miramos si está activo o no el centinela
    @Override
    public Object visitInstrucciones(Anasint.InstruccionesContext ctx) {
        Boolean ruptura = false;
        for(int i=0; i<ctx.instruccion().size(); i++){
            visit(ctx.instruccion(i));
            ruptura = visitInstruccion(ctx.instruccion(i),ruptura);
        }
        return ruptura;
    }

    //visitamos la instrucción pertinente.
    @Override
    public Object visitInstruccion(Anasint.InstruccionContext ctx) {
        return visit(ctx.getChild(0));
    }

    //DECISIÓN 1.1
    //visitamos subprogramas
    //subprogramas: SUBPROGRAMAS (subprograma)*;
    @Override
    public Object visitSubprogramas(Anasint.SubprogramasContext ctx) {
        for(int i=0; i<ctx.subprograma().size(); i++){
            visit(ctx.subprograma(i));
        }
        return null;
    }

    //subprograma: funcion | procedimiento;
    @Override
    public Object visitSubprograma(Anasint.SubprogramaContext ctx) {
        switch (ctx.start.getType()){
            case Anasint.FUNCION:
                visit(ctx.funcion());
                break;
            case Anasint.PROCEDIMIENTO:
                visit(ctx.procedimiento());
                break;
        }
        return null;
    }

    //funcion: FUNCION IDENT cabecera cuerpo FFUNCION;
    @Override
    public Object visitFuncion(Anasint.FuncionContext ctx) {
        visit(ctx.getChild(2));
        System.out.println("-----------------------SUBPROGRAMAS-----------------------");
        Map<String,String> cabecera = (Map<String,String>) visitCabecera(ctx.cabecera());
        memoriaSubPrograma.putAll(cabecera);
        Map<String,String> cuerpo = (Map<String,String>) visitCuerpo(ctx.cuerpo());
        memoria.pop();
        System.out.println("----------------------------------------------------------");
        return null;
    }

    //procedimiento: PROCEDIMIENTO IDENT cabecera cuerpo FPROCEDIMIENTO;
    @Override
    public Object visitProcedimiento(Anasint.ProcedimientoContext ctx) {
        System.out.println("-----------------------SUBPROGRAMAS-----------------------");
        Map<String,String> cabecera = (Map<String,String>) visitCabecera(ctx.cabecera());
        memoriaSubPrograma.putAll(cabecera);
        Map<String,String> cuerpo = (Map<String,String>) visitCuerpo(ctx.cuerpo());
        memoria.pop();
        System.out.println("----------------------------------------------------------");
        return null;
    }

    //cabecera: PA (param)? PC (DEV)? PA param PC
    //    | PA (param)? PC
    //    ;
    @Override
    public Object visitCabecera(Anasint.CabeceraContext ctx) {
        Map<String,String> memoria_cabecera = new HashMap<>();
        List<String> vars = new ArrayList<>();
        List<String> tipos = new ArrayList<>();
        for(int i=0; i<ctx.param().size(); i++){
            Map<String,List<String>> params = (Map<String, List<String>>) visitParam(ctx.param(i));
            if(params.containsKey(0)){
                for(int j=0; j<params.get(0).size();j++) {
                    vars.add(params.get(0).get(j));
                }
            }
            if(params.containsKey(1)) {
                for (int j = 0; j < params.get(1).size();j++) {
                    tipos.add(params.get(1).get(j));
                }
            }
            for(int j=0;j<vars.size();j++){
                memoria_cabecera.put(vars.get(j), tipos.get(j));
            }
        }
        // Decisión 5: guardamos la especificación de lo que devuelve la función
        memdev.push(memoria_cabecera);
        return memoria_cabecera;
    }

    //param: tipo IDENT (COMA tipo IDENT)*;
    @Override
    public Object visitParam(Anasint.ParamContext ctx) {
        Map<Integer,List<String>> lists = new HashMap<>();
        List<String> tipos = new ArrayList<>();
        List<String> vars = new ArrayList<>();
        for(int i=0; i<ctx.tipo().size(); i++){
            tipos.add(ctx.tipo(i).getText());
            vars.add(ctx.IDENT(i).getText());
        }
        lists.put(0,vars);
        lists.put(1,tipos);
        return lists;
    }

    //cuerpo: variables instrucciones;
    @Override
    public Object visitCuerpo(Anasint.CuerpoContext ctx) {
        Map<String,String> cuerpo = (Map<String, String>) visit(ctx.variables());
        memoriaSubPrograma.putAll(cuerpo);
        memoria.push(memoriaSubPrograma);
        visit(ctx.instrucciones());
        return null;
    }

    //DECISIÓN 2
    //Asignaciones con variables previamente declaradas
    //visitamos las expresiones
    //vars ASIG expr (COMA expr)? PyC #AsigExpr
    @Override
    public Object visitAsignacion(Anasint.AsignacionContext ctx) {
        List<String> vars = (List<String>) visitVars(ctx.vars());
        Map<String,String> ultimo_elemento = memoria.peek();
        for(int i=0; i<vars.size();i++){
            if(!ultimo_elemento.containsKey(vars.get(i))){
                System.out.println("\u001B[31m" +"ERROR linea " + ctx.getStart().getLine() + ": Variable " + vars.get(i) +" no declarada"+"\u001B[0m");
            }
        }
        for(int i=0; i<ctx.expr().size();i++) {
            visit(ctx.expr(i));
        }
        return null;
    }

    //visitar las operaciones númericas
    @Override
    public Object visitExprNum(Anasint.ExprNumContext ctx) {
        visit(ctx.expr_num());
        if(ctx.getChildCount()>1){
            visit(ctx.expr_num());
        }
        return "NUM";
    }

    //expr_num1: expr_num2 POR expr_num1
    //    | expr_num2
    //    ;
    @Override
    public Object visitExpr_num1(Anasint.Expr_num1Context ctx) {
        visit(ctx.expr_num2());
        return "NUM";
    }

    //expr_num2:
    //  MENOS IDENT   #ExprMenosIdent
    @Override
    public Object visitExprMenosIdent(Anasint.ExprMenosIdentContext ctx) {
        Map<String,String> ultimo_elemento = memoria.peek();
        String v = ctx.IDENT().getText();
        if(!ultimo_elemento.containsKey(v)){
            System.out.println("\u001B[31m" +"ERROR linea " + ctx.getStart().getLine() + ": Variable " + " no declarada"+"\u001B[0m");
        }
        return "NUM";
    }

    //| IDENT #ExprIdent
    @Override
    public Object visitExprIdent(Anasint.ExprIdentContext ctx) {
        Map<String,String> ultimo_elemento = memoria.peek();
        String v = ctx.IDENT().getText();
        if(!ultimo_elemento.containsKey(v)){
            System.out.println("\u001B[31m" +"ERROR linea " + ctx.getStart().getLine() + ": Variable " + v + " no declarada"+"\u001B[0m");
        }
        return "NUM";
    }

    //| NUMERO #ExprNumero
    @Override
    public Object visitExprNumero(Anasint.ExprNumeroContext ctx) {
        return "NUM";
    }

    // | PA expr_num PC #ExprExpr
    @Override
    public Object visitExprExpr(Anasint.ExprExprContext ctx) {
        return "NUM";
    }

    //expresión booleana devuelve tipo LOG
    @Override
    public Object visitExprBool(Anasint.ExprBoolContext ctx) {
        return "LOG";
    }


    //DECISIÓN 3: condiciones verdaderas, falsas o indefinidas

    //condicional: SI PA condicion PC ENTONCES bloque (alternativa)? FSI;
    //visito condicion
    //condicional: SI PA condicion PC ENTONCES bloque (alternativa)? FSI;
    //alternativa : SINO bloque;
    @Override
    public Object visitCondicional(Anasint.CondicionalContext ctx) {
        visit(ctx.getChild(2));
        return null;
    }

    //iteracion: MIENTRAS PA condicion PC HACER bloque FMIENTRAS;
    //visito condicion
    //iteracion: MIENTRAS PA condicion PC HACER bloque FMIENTRAS;
    @Override
    public Object visitIteracion(Anasint.IteracionContext ctx) {
        visit(ctx.getChild(2));
        return null;
    }

    //miro que la condicion sea de tipo LOG y si no me da error ya que la condicion debe ser de un tipo booleano
    //condicion: cond;
    @Override
    public Object visitCondicion(Anasint.CondicionContext ctx) {
        if(visit(ctx.getChild(0)) != "LOG"){
            System.out.println("\u001B[31m" + "ERROR linea " + ctx.getStart().getLine() + ": Tipo incorrecto" + "\u001B[0m");
        }
        return null;
    }
    //cond: expr   #CondExpr
    //visitamos la expresion y así después podremos mirar su tipo
    @Override
    public Object visitCondExpr(Anasint.CondExprContext ctx) {
        return visit(ctx.getChild(0));
    }

    //Comparamos los tipos a ambos lados de la expr_bool para que estos sean del mismo tipo y NUM pero que el resultado
    //devuelto sea LOG.
    // |cond expr_bool cond #CondBool
    @Override
    public Object visitCondBool(Anasint.CondBoolContext ctx) {
        String tipo1 = (String) visit(ctx.getChild(0));
        String tipo2 = (String) visit(ctx.getChild(2));
        if(tipo1 != "LOG"){
            System.out.println("\u001B[31m" + "ERROR linea " + ctx.getStart().getLine() + ": Tipo indefinido" + "\u001B[0m");
        }
        if(tipo1 != tipo2){
            System.out.println("\u001B[31m" + "ERROR linea " + ctx.getStart().getLine() + ": Tipo indefinido" + "\u001B[0m");
        }
        return "LOG";
    }

    //Comparamos los tipos a ambos lados de la expr_bool para que estos sean del mismo tipo y LOG al igual que el resultado
    //devuelto sea LOG.
    //|cond instrLogica cond  #CondLog
    @Override
    public Object visitCondLog(Anasint.CondLogContext ctx) {
        String tipo1 = (String) visit(ctx.getChild(0));
        String tipo2 = (String) visit(ctx.getChild(2));
        if(tipo1 != "NUM"){
            System.out.println("\u001B[31m" + "ERROR linea " + ctx.getStart().getLine() + ": Tipo indefinido" + "\u001B[0m");
        }
        if(tipo1 != tipo2){
            System.out.println("\u001B[31m" + "ERROR linea " + ctx.getStart().getLine() + ": Tipo indefinido" + "\u001B[0m");
        }
        return "LOG";
    }

    //DECISIÓN DISEÑO 4
    //Identifica el bloque en el que se encuentra y recoge el valor del centinela
    //cuando ocurre en una instrucción condicional, iteración o ruptura.
    public Boolean visitInstruccion(Anasint.InstruccionContext ctx, Boolean ruptura) {
        Boolean r ;
        switch (ctx.start.getType()){
            case Anasint.SI: r=visitCondicional(ctx.condicional(),ruptura);break;
            case Anasint.MIENTRAS: r=visitIteracion(ctx.iteracion(),ruptura);break;
            case Anasint.RUPTURA: r=visitRuptura(ctx.ruptura(),ruptura);break;
            //Usado para la decisión 5
            case Anasint.DEV: r = visitDEV(ctx, ruptura);break;
            default:r=ruptura;break;
        }
        return r;
    }

    //ruptura: RUPTURA PyC;
    //Activa el centinela.
    public Boolean visitRuptura(Anasint.RupturaContext ctx, Boolean ruptura) {
        Boolean b = true;
        //if(ruptura) System.out.println("Break inalcanzable: (Línea " + ctx.start.getLine() + ")");
        return b;
    }

    //Visitamos el bloque correspondiente a la iteración para ver si se activa el centinela.
    public Boolean visitIteracion(Anasint.IteracionContext ctx,Boolean ruptura) {
        Boolean r;
        visitBloque(ctx.bloque(),ruptura);
        r = ruptura;
        return r;
    }

    //Cuenta los breaks que se encuentra dentro de un bloque, si hay más de uno el resultado es inalcanzable.
    public Boolean visitBloque(Anasint.BloqueContext ctx,Boolean ruptura){
        Boolean r = ruptura;
        this.contador = 0;
        for(int i=0;i<ctx.children.size();i++){
            Integer linea = ctx.instruccion(i).getStart().getLine();

            r = visitInstruccion(ctx.instruccion(i),ruptura);
            if(r==true){
                this.contador++;
                if(this.contador>1 ){
                    System.out.println("\u001B[31m Break inalcanzable: (Línea " + linea + ") \u001B[0m");

                }
            }
        }

        return r;
    }

    //Visitamos el bloque correspondiente a la condicional para ver si se activa el centinela.
    public Boolean visitCondicional(Anasint.CondicionalContext ctx,Boolean ruptura){
        Boolean r;
        visitBloque(ctx.bloque(),ruptura);
        r=ruptura;
        return r;
    }

    //DECISIÓN DISEÑO 5: La devolución de la función debe coincidir con el tipo de la variable.

    public Boolean visitDEV(Anasint.InstruccionContext ctx, Boolean ruptura) {
        Object[] varDev = ctx.parent.getText().split("dev");
        String[] parametro_Func = memdev.get(0).entrySet().toArray()[0].toString().split("=");
        String[] varDevueltas = varDev[1].toString().split(",");
        String tipoDeclarado = "";
        String tipoRecibido = "";
        if (parametro_Func[1].replace(";","").equals("LOG")) {
            tipoDeclarado = parametro_Func[1].replace(";", "");
        }
        for (String varDevuelta : varDevueltas) {
            String varCheck = varDevuelta.replace(";", "");
            if (!memoria.get(0).containsKey(varCheck)) {
                System.out.println("\u001B[31m" + "ERROR linea " + ctx.getStart().getLine() + ": Variable devuelta " + varCheck + " no declarada en el programa." + "\u001B[0m");
                break;
            } else {
                tipoRecibido = memoria.get(0).get(varCheck).trim();
                if (!memdev.get(0).containsKey(varCheck)) {
                    System.out.println("\u001B[31m" + "ERROR linea " + ctx.getStart().getLine() + ": Variable devuelta " + varCheck + " no especificada en la cabecera de la función." + "\u001B[0m");
                } else {
                    tipoDeclarado = memdev.get(0).get(varCheck).trim();
                }
            }
        }
        if (!tipoDeclarado.equals(tipoRecibido)) {
            System.out.println("\u001B[31m" + "ERROR linea " + ctx.getStart().getLine() + ": Declaración de función debe devolver tipo " + tipoDeclarado + " pero ha recibido " + tipoRecibido + "\u001B[0m");
        }
        return ruptura;
    }

}

